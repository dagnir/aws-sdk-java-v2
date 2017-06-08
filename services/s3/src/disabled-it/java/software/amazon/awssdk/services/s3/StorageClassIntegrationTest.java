/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3VersionSummary;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.VersionListing;
import software.amazon.awssdk.test.util.RandomInputStream;

/**
 * Integration test for S3 storage class related operations.
 */
@Category(S3Categories.Slow.class)
public class StorageClassIntegrationTest extends S3IntegrationTestBase {

    private static final String KEY = "key";
    private static final String BUCKET_NAME = "java-storage-class-integ-test-" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() {
        s3.createBucket(BUCKET_NAME);
    }

    /**
     * Releases all resources allocated by this test.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        deleteBucketAndAllVersionedContents(BUCKET_NAME);
    }

    /**
     * Tests that we can change an object's storage class, upload objects in a specific storage
     * class, and retrive an object's storage class, both for versioned and unversioned objects.
     */
    @Test
    public void testStorageClasses() throws Exception {
        // Upload an object with standard storage
        putObject(StorageClass.Standard.toString());
        assertStorageClass(StorageClass.Standard);
        assertStorageClassInMetadata(null);

        // Upload an object with reduced redundancy storage
        putObject(StorageClass.ReducedRedundancy.toString());
        assertStorageClass(StorageClass.ReducedRedundancy);
        assertStorageClassInMetadata(StorageClass.ReducedRedundancy.toString());

        // Move to regular storage
        s3.changeObjectStorageClass(BUCKET_NAME, KEY, StorageClass.Standard);
        assertStorageClass(StorageClass.Standard);

        // Move back to reduced redundancy storage
        s3.changeObjectStorageClass(BUCKET_NAME, KEY, StorageClass.ReducedRedundancy);
        assertStorageClass(StorageClass.ReducedRedundancy);

        // Turn on versioning
        s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(BUCKET_NAME,
                                                                                        new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));

        // Upload a new version with standard storage
        putObject(StorageClass.Standard.toString());
        assertStorageClass(StorageClass.Standard);

        // Move to reduced redundancy storage
        s3.changeObjectStorageClass(BUCKET_NAME, KEY, StorageClass.ReducedRedundancy);
        assertStorageClass(StorageClass.ReducedRedundancy);

        // And move back to standard storage
        s3.changeObjectStorageClass(BUCKET_NAME, KEY, StorageClass.Standard);
        assertStorageClass(StorageClass.Standard);
    }

    /*
     * Private Interface
     */

    /**
     * Uploads some random test data to the object stored in the specified bucket and key, and uses
     * the specified storage class.
     */
    private void putObject(String storageClass) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(1230);
        PutObjectRequest request = new PutObjectRequest(BUCKET_NAME, KEY, new RandomInputStream(
                metadata.getContentLength()), metadata);
        request.setStorageClass(storageClass);
        s3.putObject(request);
    }

    /**
     * Asserts that the latest version for the specified object is stored in the expected storage
     * class, otherwise this method fails the current test.
     */
    private void assertStorageClass(StorageClass expectedStorageClass) throws Exception {
        // Short pause for eventual consistency
        Thread.sleep(1000 * 3);

        VersionListing versionListing = s3.listVersions(BUCKET_NAME, KEY);
        for (Iterator<S3VersionSummary> iterator = versionListing.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            S3VersionSummary versionSummary = (S3VersionSummary) iterator.next();

            if (versionSummary.isLatest() && versionSummary.getKey().equals(KEY)) {
                assertEquals(expectedStorageClass.toString(), versionSummary.getStorageClass());
                return;
            }
        }

        fail("Expected an object stored under key '" + KEY + "', but didn't find one");
    }

    /**
     * Asserts that the specified storage class is returned in calls to getObjectMetadata.
     */
    private void assertStorageClassInMetadata(String expectedStorageClass) {
        ObjectMetadata metadata = s3.getObjectMetadata(BUCKET_NAME, KEY);
        assertEquals(expectedStorageClass, metadata.getStorageClass());
    }

}
