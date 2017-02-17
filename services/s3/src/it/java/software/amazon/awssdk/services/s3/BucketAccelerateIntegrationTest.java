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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.Constants.KB;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketAccelerateConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAccelerateStatus;
import software.amazon.awssdk.services.s3.model.BucketTaggingConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.TagSet;
import software.amazon.awssdk.services.s3.transfer.Download;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.Upload;
import software.amazon.awssdk.test.retry.AssertCallable;
import software.amazon.awssdk.test.retry.RetryableAssertion;
import software.amazon.awssdk.test.retry.RetryableParams;


/**
 * Integration tests for S3 bucket accelerate configuration.
 */
public class BucketAccelerateIntegrationTest extends S3IntegrationTestBase {

    private static final String US_BUCKET_NAME = "s3-accelerate-us-east-1-" + System.currentTimeMillis();
    private static final String EU_BUCKET_NAME = "s3-accelerate-eu-central-1-" + System.currentTimeMillis();
    private static final String KEY_NAME = "key";

    private static AmazonS3Client accelerateClient;
    private static AmazonS3Client euAccelerateClient;
    private static AmazonS3Client euAccelerateClientWithRegion;

    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();

        accelerateClient = new AmazonS3Client(credentials);
        euAccelerateClient = new AmazonS3Client(credentials);
        euAccelerateClientWithRegion = new AmazonS3Client(credentials);
        // enable accelerate mode
        accelerateClient.setS3ClientOptions(S3ClientOptions.builder().setAccelerateModeEnabled(true).build());
        euAccelerateClient.setS3ClientOptions(S3ClientOptions.builder().setAccelerateModeEnabled(true).build());
        euAccelerateClientWithRegion.setS3ClientOptions(S3ClientOptions.builder().setAccelerateModeEnabled(true).build());
        euAccelerateClientWithRegion.configureRegion(Regions.EU_CENTRAL_1);

        setUpBuckets();
    }

    @AfterClass
    public static void cleanup() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, US_BUCKET_NAME);
        CryptoTestUtils.deleteBucketAndAllContents(s3, EU_BUCKET_NAME);
    }

    private static void setUpBuckets() {
        s3.createBucket(new CreateBucketRequest(US_BUCKET_NAME));
        s3.createBucket(new CreateBucketRequest(EU_BUCKET_NAME, Regions.EU_CENTRAL_1.getName()));
    }

    @Test
    public void testControlPlanOperationsUnderAccelerateMode() throws Exception {
        enableAccelerateOnBucket(US_BUCKET_NAME);

        TagSet tags = new TagSet(new HashMap<String, String>() {
            {
                put("foo", "bar");
            }
        });
        accelerateClient.setBucketTaggingConfiguration(US_BUCKET_NAME,
                                                       new BucketTaggingConfiguration().withTagSets(tags));
        accelerateClient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(US_BUCKET_NAME,
                                                            new BucketVersioningConfiguration()
                                                                    .withStatus(BucketVersioningConfiguration.ENABLED)));

        // Retry a couple of times due to eventual consistency
        RetryableAssertion.doRetryableAssert(new AssertCallable() {
            @Override
            public void doAssert() {
                BucketTaggingConfiguration taggingConfiguration = accelerateClient
                        .getBucketTaggingConfiguration(US_BUCKET_NAME);
                assertEquals("bar", taggingConfiguration.getTagSet().getTag("foo"));
            }
        }, new RetryableParams().withMaxAttempts(30).withDelayInMs(200));

        assertEquals(BucketVersioningConfiguration.ENABLED,
                     accelerateClient.getBucketVersioningConfiguration(US_BUCKET_NAME).getStatus());

        accelerateClient.deleteBucketTaggingConfiguration(US_BUCKET_NAME);
        assertNull(accelerateClient.getBucketTaggingConfiguration(US_BUCKET_NAME));
    }

    @Test
    public void getS3AccountOwner_WorksInAccelerateMode() {
        assertNotNull(accelerateClient.getS3AccountOwner());
    }

    @Test
    public void testUpdateAccelerateConfiguration() {

        if (!s3.getBucketAccelerateConfiguration(US_BUCKET_NAME).isAccelerateEnabled()) {
            enableAccelerateOnBucket(US_BUCKET_NAME);
        }
        assertEquals(
                BucketAccelerateStatus.Enabled.toString(),
                s3.getBucketAccelerateConfiguration(US_BUCKET_NAME).getStatus());

        disableAccelerateOnBucket(US_BUCKET_NAME);
        assertEquals(
                BucketAccelerateStatus.Suspended.toString(),
                s3.getBucketAccelerateConfiguration(US_BUCKET_NAME).getStatus());
    }

    @Test
    public void testAccelerateEndpoint() throws Exception {

        if (!s3.getBucketAccelerateConfiguration(US_BUCKET_NAME).isAccelerateEnabled()) {
            enableAccelerateOnBucket(US_BUCKET_NAME);
        }

        // PutObject
        File uploadFile = getRandomTempFile(KEY_NAME, 1000);
        PutObjectRequest putObjectRequest = new PutObjectRequest(US_BUCKET_NAME, KEY_NAME, uploadFile);
        accelerateClient.putObject(putObjectRequest);

        S3ResponseMetadata putObjectMetadata = accelerateClient.getCachedResponseMetadata(putObjectRequest);
        assertNotNull(putObjectMetadata.getCloudFrontId());

        // GetObject
        File downloadFile = File.createTempFile("s3-accelerate-test", "");
        downloadFile.deleteOnExit();
        GetObjectRequest getObjectRequest = new GetObjectRequest(US_BUCKET_NAME, KEY_NAME);
        accelerateClient.getObject(getObjectRequest, downloadFile);

        S3ResponseMetadata getObjectMetadata = accelerateClient.getCachedResponseMetadata(getObjectRequest);
        assertNotNull(getObjectMetadata.getCloudFrontId());
        assertFileEqualsFile(uploadFile, downloadFile);

        // HeadObject
        GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(US_BUCKET_NAME, KEY_NAME);
        ObjectMetadata metadata = accelerateClient.getObjectMetadata(getObjectMetadataRequest);
        Assert.assertEquals(1000, metadata.getContentLength());

        S3ResponseMetadata getObjectMetadataMetadata = accelerateClient.getCachedResponseMetadata(getObjectMetadataRequest);
        assertNotNull(getObjectMetadataMetadata.getCloudFrontId());

        // Presign URL
        Date oneHourLater = new Date(new Date().getTime() + 1000 * 60 * 60);
        URL presignUrl = accelerateClient.generatePresignedUrl(US_BUCKET_NAME, KEY_NAME, oneHourLater);
        assertFileEqualsStream(uploadFile, presignUrl.openStream());
    }

    @Test
    public void testAccelerateEndpoint_TransferManager() throws Exception {
        if (!s3.getBucketAccelerateConfiguration(US_BUCKET_NAME).isAccelerateEnabled()) {
            enableAccelerateOnBucket(US_BUCKET_NAME);
        }

        // Using transfer manager to upload a 1K file.
        File uploadFile = getRandomTempFile(KEY_NAME, 1 * KB);
        TransferManager transferManager = new TransferManager(accelerateClient);
        Upload upload = transferManager.upload(US_BUCKET_NAME, KEY_NAME, uploadFile);
        upload.waitForCompletion();

        // Using transfer manager to download a 1K file.
        File downloadFile = File.createTempFile("s3-accelerate-tx-test", "");
        downloadFile.deleteOnExit();
        Download download = transferManager.download(US_BUCKET_NAME, KEY_NAME, downloadFile);
        download.waitForCompletion();

        assertFileEqualsFile(uploadFile, downloadFile);
    }

    private void enableAccelerateOnBucket(String bucket) {
        s3.setBucketAccelerateConfiguration(bucket, new BucketAccelerateConfiguration(BucketAccelerateStatus.Enabled));
    }

    private void disableAccelerateOnBucket(String bucket) {
        s3.setBucketAccelerateConfiguration(bucket,
                                            new BucketAccelerateConfiguration(BucketAccelerateStatus.Suspended));
    }

    @Test
    public void testUnsupportedOperationsUnderAccelerateMode() {
        try {
            accelerateClient.listBuckets();
        } catch (Exception e) {
            fail("Exception is not expected!");
        }
    }

    /**
     * As we switched to sigv4 as the default signature, no exception will be thrown under
     * accelerate mode to conduct operations on bucket located on eu-central-1. A 307 error
     * code will be returned and we'll use the location header to perform a retry. */
    @Test
    public void testAccelerateMode_notSettingRegion() throws Exception {
        enableAccelerateOnBucket(EU_BUCKET_NAME);

        File uploadFile = getRandomTempFile(KEY_NAME, 1000);
        euAccelerateClient.putObject(EU_BUCKET_NAME, KEY_NAME, uploadFile);

        File downloadFile = File.createTempFile("s3-accelerate-eu-test", "tmp");
        downloadFile.deleteOnExit();
        euAccelerateClientWithRegion.getObject(new GetObjectRequest(EU_BUCKET_NAME, KEY_NAME), downloadFile);
        euAccelerateClientWithRegion.deleteObject(EU_BUCKET_NAME, KEY_NAME);

        assertFileEqualsFile(uploadFile, downloadFile);
    }

    @Test
    public void testAccelerateMode_settingRegionAndSigner() throws Exception {
        enableAccelerateOnBucket(EU_BUCKET_NAME);

        File uploadFile = getRandomTempFile(KEY_NAME, 1000);
        euAccelerateClientWithRegion.putObject(EU_BUCKET_NAME, KEY_NAME, uploadFile);

        File downloadFile = File.createTempFile("s3-accelerate-eu-test", "tmp");
        downloadFile.deleteOnExit();
        euAccelerateClientWithRegion.getObject(new GetObjectRequest(EU_BUCKET_NAME, KEY_NAME), downloadFile);
        euAccelerateClientWithRegion.deleteObject(EU_BUCKET_NAME, KEY_NAME);

        assertFileEqualsFile(uploadFile, downloadFile);
    }

}
