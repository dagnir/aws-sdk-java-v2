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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * Integration tests to make sure that the bucketRegionCache works as expected
 * in different scenarios.
 */
public class BucketRegionCacheIntegrationTest extends S3IntegrationTestBase {

    private static final Map<String, String> cache = AmazonS3Client.getBucketRegionCache();

    private static final String US_EAST_BUCKET = "useast-java-sdk-bucket" + new Date().getTime();

    private static final String EU_WEST_1_BUCKET = "euwest1-java-sdk--bucket" + new Date().getTime();

    private static final String AP_NORTHEAST_1_BUCKET = "apnortheast1-java-sdk--bucket" + new Date().getTime();


    @BeforeClass
    public static void setup() {
        s3.createBucket(US_EAST_BUCKET);
        s3.createBucket(AP_NORTHEAST_1_BUCKET, "ap-northeast-1");
        euS3.createBucket(EU_WEST_1_BUCKET);
    }

    @AfterClass
    public static void tearDown() {
        s3.deleteBucket(US_EAST_BUCKET);
        assertNull(cache.get(US_EAST_BUCKET));

        euS3.deleteBucket(EU_WEST_1_BUCKET);
        assertNull(cache.get(EU_WEST_1_BUCKET));

        s3.deleteBucket(AP_NORTHEAST_1_BUCKET);
        assertNull(cache.get(AP_NORTHEAST_1_BUCKET));
    }

    /**
     * When region info is provided, we don't update or use the bucketRegionCache.
     */
    @Test
    public void testCacheIsNotUpdatedWhenRegionIsConfigured() {
        cache.remove(EU_WEST_1_BUCKET);
        int size = cache.size();

        euS3.headBucket(new HeadBucketRequest(EU_WEST_1_BUCKET));
        assertTrue(cache.size() == size);
        assertFalse(cache.containsKey(EU_WEST_1_BUCKET));

        euS3.listObjects(EU_WEST_1_BUCKET);
        assertTrue(cache.size() == size);

        euS3.listBuckets();
        assertTrue(cache.size() == size);
        assertFalse(cache.containsKey(EU_WEST_1_BUCKET));
    }

    /**
     * When region info is not provided,
     * tests that the bucketRegionCache is updated properly.
     */
    @Test
    public void testCacheIsUpdatedWhenRegionIsNotConfigured() throws InterruptedException {
        cache.remove(US_EAST_BUCKET);
        assertFalse(cache.containsKey(US_EAST_BUCKET));

        int size = cache.size();

        s3.headBucket(new HeadBucketRequest(US_EAST_BUCKET));
        assertTrue(cache.size() == size + 1);
        assertEquals("us-east-1", cache.get(US_EAST_BUCKET));

        s3.listObjects(US_EAST_BUCKET);
        assertTrue(cache.size() == size + 1);
        assertEquals("us-east-1", cache.get(US_EAST_BUCKET));

        s3.listBuckets();
        assertTrue(cache.size() == size + 1);
        assertEquals("us-east-1", cache.get(US_EAST_BUCKET));
    }

    /**
     * Tests that the bucketRegionCache is updated properly
     * when the standard client tries to access bucket in different region.
     */
    @Test
    public void testCacheWhlieAccessingBucketsInDifferentRegion() {
        int size = cache.size();
        assertFalse(cache.containsKey(AP_NORTHEAST_1_BUCKET));

        s3.headBucket(new HeadBucketRequest(AP_NORTHEAST_1_BUCKET));
        assertTrue(cache.size() == size + 1);
        assertEquals("ap-northeast-1", cache.get(AP_NORTHEAST_1_BUCKET));

        s3.listObjects(AP_NORTHEAST_1_BUCKET);
        assertTrue(cache.size() == size + 1);
        assertEquals("ap-northeast-1", cache.get(AP_NORTHEAST_1_BUCKET));
    }

    @Test(expected = AmazonS3Exception.class)
    public void testNonExistentBucketWithEntryInCache() {
        String bucketName = "random-java-bucket" + new Date().getTime();
        cache.put(bucketName, "eu-west-1");

        s3.headBucket(new HeadBucketRequest(bucketName));
    }

    @Test(expected = AmazonS3Exception.class)
    public void testNonExistentBucketWithoutEntryInCache() {
        String bucketName = "random-java-bucket" + new Date().getTime();

        s3.headBucket(new HeadBucketRequest(bucketName));
    }

    /**
     * Tests the case when bucket is deleted and re-created in a
     * different region outside SDK. Now cache has wrong region info
     * for the bucket.
     *
     * When requests are made against this newly created bucket without specifying the region,
     * the first request should fail but cache will be updated. All the subsequent
     * requests should succeed.
     */
    @Test
    public void testCacheHasWrongRegionAndRegionIsNotProvided() {
        //Bucket created in us-east-1 but cache entry is eu-central-1
        cache.put(US_EAST_BUCKET, "eu-central-1");

        assertEquals("eu-central-1", cache.get(US_EAST_BUCKET));
        try {
            s3.headBucket(new HeadBucketRequest(US_EAST_BUCKET));
        } catch (AmazonS3Exception ase) {
            assertEquals(301, ase.getStatusCode());
            assertEquals("us-east-1", cache.get(US_EAST_BUCKET));
        }

        // Subsequent requests after updating the cache should succeed.
        s3.headBucket(new HeadBucketRequest(US_EAST_BUCKET));
        s3.listObjects(US_EAST_BUCKET);
    }

    /**
     * Tests the case when bucket is deleted and re-created in a
     * different region outside SDK. Now cache has wrong region info
     * for the bucket.
     *
     * When requests are made against this newly created bucket with specifying the region,
     * all the requests should fail as client is configured with the wrong region.
     * But the cache is updated after first failed request.
     */
    @Test
    public void testCacheHasWrongRegionAndRegionIsProvided() {
        //Bucket created in eu-west-1 but cache entry is us-west-2
        cache.put(EU_WEST_1_BUCKET, "us-west-2");
        assertEquals("us-west-2", cache.get(EU_WEST_1_BUCKET));

        AmazonS3Client apClient = new AmazonS3Client();
        apClient.configureRegion(Regions.AP_NORTHEAST_1);
        try {
            apClient.headBucket(new HeadBucketRequest(EU_WEST_1_BUCKET));
        } catch (AmazonS3Exception ase) {
            assertEquals(301, ase.getStatusCode());
            assertEquals("eu-west-1", cache.get(EU_WEST_1_BUCKET));
            return;
        }

        try {
            // Subsequent requests after updating the cache should fail.
            apClient.headBucket(new HeadBucketRequest(EU_WEST_1_BUCKET));
        } catch (AmazonS3Exception ase) {
            assertEquals(301, ase.getStatusCode());
            assertEquals("us-west-1", cache.get(EU_WEST_1_BUCKET));
            return;
        }
    }

}
