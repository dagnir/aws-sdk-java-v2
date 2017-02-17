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

import static software.amazon.awssdk.test.util.SdkAsserts.assertFileEqualsFile;
import static software.amazon.awssdk.test.util.SdkAsserts.assertStreamEqualsStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.s3.internal.AwsS3V4Signer;
import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.Region;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.test.AwsIntegrationTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.StringInputStream;

public class SigV4RegionAutoResolveIntegrationTest extends AwsIntegrationTestBase {

    private static final String FRA_BUCKET = CryptoTestUtils.tempBucketName(SigV4RegionAutoResolveIntegrationTest.class);
    private static AmazonS3Client s3_classic;
    private static AmazonS3Client s3_classicWithSigV2;
    private static AmazonS3Client s3_fra;
    private static AmazonS3Client s3_external_1;

    @BeforeClass
    public static void setup() throws IOException {
        s3_classic = new AmazonS3Client(getCredentials(),
                                        new ClientConfiguration().withMaxErrorRetry(0));

        s3_classicWithSigV2 = new AmazonS3Client(getCredentials(), new ClientConfiguration()
                .withMaxErrorRetry(0)
                .withSignerOverride("S3SignerType"));

        s3_fra = new AmazonS3Client(getCredentials());
        s3_fra.setEndpoint("s3.eu-central-1.amazonaws.com");

        // To simulate SigV4 retry, since DNS probably won't have updated
        // in time to cause the full 3-request worst-case.
        s3_external_1 = new AmazonS3Client(
                getCredentials(),
                new ClientConfiguration().withSignerOverride("AWSS3V4SignerType"));

        s3_external_1.setEndpoint("s3-external-1.amazonaws.com");

        if (!s3_fra.doesBucketExist(FRA_BUCKET)) {
            s3_fra.createBucket(FRA_BUCKET, Region.EU_Frankfurt);
        }
    }

    @AfterClass
    public static void deleteBucket() {
        CryptoTestUtils.deleteBucketAndAllContents(s3_classic, FRA_BUCKET);
        s3_classic.shutdown();
        s3_fra.shutdown();
    }

    /**
     * Check that the endpoint and signer-type configured at client-level are
     * not modified.
     */
    @Before
    @After
    public void checkClientConfig() {
        Assert.assertEquals(
                Constants.S3_HOSTNAME,
                s3_classic.getEndpoint().getHost());
        Assert.assertTrue(
                s3_classic.getSignerByUri(s3_classic.getEndpoint())
                        instanceof AwsS3V4Signer);
    }

    /**
     * When the client expliclity uses SigV2 then we may still go into the auth error retry strategy and redirect the request.
     * When we do this, we have to be sure to properly URL encode the resource path. See TT0095127798.
     */
    @Test
    public void putObjectWithSpacesInKeyToFra_CorrectlyUrlEncodesOnAuthErrorRetry() throws UnsupportedEncodingException {
        s3_classicWithSigV2.putObject(FRA_BUCKET, "key with spaces", new StringInputStream("contents"), new ObjectMetadata());
    }

    @Test
    public void testFRA_HeadBucket() {
        Assert.assertTrue(s3_classic.doesBucketExist(FRA_BUCKET));
    }

    @Test
    public void testFRA_ListBucket() {
        for (Bucket bucket : s3_classic.listBuckets()) {
            if (bucket.getName().equals(FRA_BUCKET)) {
                return;
            }
        }
        Assert.fail("Bucket " + FRA_BUCKET + " not found in ListBucket result");
    }

    @Test
    public void testFRA_BucketOperations() {
        Assert.assertEquals("eu-central-1", s3_classic.getBucketLocation(FRA_BUCKET));

        Assert.assertNotNull(s3_classic.listObjects(FRA_BUCKET));
        Assert.assertNotNull(s3_classic.getBucketAcl(FRA_BUCKET));
        Assert.assertNotNull(s3_classic.getBucketPolicy(FRA_BUCKET));

        s3_classic.getBucketCrossOriginConfiguration(FRA_BUCKET);
        s3_classic.getBucketLifecycleConfiguration(FRA_BUCKET);
        s3_classic.getBucketLoggingConfiguration(FRA_BUCKET);
        s3_classic.getBucketNotificationConfiguration(FRA_BUCKET);
        s3_classic.getBucketTaggingConfiguration(FRA_BUCKET);

        s3_classic.setBucketAcl(FRA_BUCKET, CannedAccessControlList.BucketOwnerFullControl);
    }

    @Test
    public void testFRA_ObjectOperations() throws Exception {
        String key = "key";
        long contentLength = 2048;
        File sourcefile = new RandomTempFile("source", contentLength);
        File targetFile = File.createTempFile("target", "");
        sourcefile.deleteOnExit();
        targetFile.deleteOnExit();

        // File
        s3_classic.putObject(FRA_BUCKET, key, sourcefile);
        s3_classic.getObject(new GetObjectRequest(FRA_BUCKET, key), targetFile);
        assertFileEqualsFile(sourcefile, targetFile);

        // InputStream
        ObjectMetadata md = new ObjectMetadata();
        md.setContentLength(contentLength);
        InputStream sourceStream = new FileInputStream(sourcefile);
        s3_classic.putObject(FRA_BUCKET, key, sourceStream, md);
        sourceStream.close();

        S3Object getContent = s3_classic.getObject(FRA_BUCKET, key);
        sourceStream = new FileInputStream(sourcefile);
        assertStreamEqualsStream(sourceStream, getContent.getObjectContent());

        sourceStream.close();
        getContent.close();

        s3_classic.copyObject(FRA_BUCKET, key, FRA_BUCKET, "copy-key");
    }

    @Test
    public void testFRA_MulipartUpload() throws Exception {
        String key = "key";
        long contentLength = 2048;
        File sourcefile = new RandomTempFile("source", contentLength);

        String uploadId = s3_classic.initiateMultipartUpload(
                new InitiateMultipartUploadRequest(FRA_BUCKET, key))
                                    .getUploadId();

        PartETag partETag = s3_classic.uploadPart(new UploadPartRequest()
                                                          .withBucketName(FRA_BUCKET)
                                                          .withKey(key)
                                                          .withFile(sourcefile)
                                                          .withUploadId(uploadId)
                                                          .withPartNumber(1))
                                      .getPartETag();

        s3_classic.completeMultipartUpload(new CompleteMultipartUploadRequest(
                FRA_BUCKET, key, uploadId, Arrays.asList(partETag)));
    }

    @Test
    public void testPutWithNoContentLength() {
        s3_external_1.putObject(
                FRA_BUCKET,
                "empty",
                new ByteArrayInputStream(new byte[0]),
                new ObjectMetadata());

        Assert.assertEquals(0L,
                            s3_fra.getObjectMetadata(FRA_BUCKET, "empty")
                                  .getContentLength());
    }

    @Test
    public void testNonRetryableAuthError() throws IOException {
        AmazonS3Client fra = new AmazonS3Client(getCredentials(),
                                                new ClientConfiguration().withSignerOverride("AWSS3V4SignerType"));
        fra.setEndpoint("s3.eu-central-1.amazonaws.com");
        fra.setSignerRegionOverride("us-east-1");
        try {
            fra.listObjects(FRA_BUCKET);
            Assert.fail("Expected a AuthorizationHeaderMalformed error " +
                        "since the request to eu-central-1 was signed with us-east-1 region string.");
        } catch (AmazonServiceException expected) {
            Assert.assertEquals("AuthorizationHeaderMalformed",
                                expected.getErrorCode());
        }
    }

}
