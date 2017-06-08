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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.NoncurrentVersionTransition;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.Rule;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;

public class LifecycleVersioningIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET_NAME =
            "lifecycle-versioning-integration-test-"
            + System.currentTimeMillis();

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        s3 = new AmazonS3Client(credentials);
        s3.setRegion(Region.getRegion(Regions.US_WEST_2));

        s3.createBucket(BUCKET_NAME);

        s3.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(
                        BUCKET_NAME,
                        new BucketVersioningConfiguration("Enabled")));
    }

    @AfterClass
    public static void cleanUp() {
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    @Test
    public void testRoundTrip() throws Exception {
        s3.setBucketLifecycleConfiguration(
                BUCKET_NAME,
                new BucketLifecycleConfiguration().withRules(
                        new Rule()
                                .withPrefix("/quick")
                                .withId("Quick")
                                .withStatus("Enabled")
                                .withNoncurrentVersionTransition(
                                        new NoncurrentVersionTransition()
                                                .withDays(0)
                                                .withStorageClass(StorageClass.Glacier))
                                .withNoncurrentVersionExpirationInDays(7),

                        new Rule()
                                .withPrefix("/slow")
                                .withId("Slow")
                                .withStatus("Enabled")
                                .withNoncurrentVersionTransition(
                                        new NoncurrentVersionTransition()
                                                .withDays(7)
                                                .withStorageClass(StorageClass.Glacier))
                                .withNoncurrentVersionExpirationInDays(60)
                                                            ));

        BucketLifecycleConfiguration result =
                s3.getBucketLifecycleConfiguration(BUCKET_NAME);

        Assert.assertEquals(2, result.getRules().size());

        Assert.assertEquals(0, result.getRules().get(0).getNoncurrentVersionTransition().getDays());
        Assert.assertEquals(StorageClass.Glacier, result.getRules().get(0).getNoncurrentVersionTransition().getStorageClass());
        Assert.assertEquals(7, result.getRules().get(0).getNoncurrentVersionExpirationInDays());

        Assert.assertEquals(7, result.getRules().get(1).getNoncurrentVersionTransition().getDays());
        Assert.assertEquals(StorageClass.Glacier, result.getRules().get(1).getNoncurrentVersionTransition().getStorageClass());
        Assert.assertEquals(60, result.getRules().get(1).getNoncurrentVersionExpirationInDays());
    }
}
