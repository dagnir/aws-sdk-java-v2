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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.HttpMethod;
import software.amazon.awssdk.kms.utils.KmsTestKeyCache;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.SSEAlgorithm;
import software.amazon.awssdk.services.s3.model.SseAwsKeyManagementParams;
import software.amazon.awssdk.services.s3.model.SseCustomerKey;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.util.Base64;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.util.Md5Utils;

/**
 * Test various URL presigning cases with SSE/SSE-C/SSE-KMS using S3 old signer
 * (SigV2).
 * <p>
 * In summary, you can use SigV2 presigned URL for SSE. For SSE-C, it
 * works only if the specific customer key is not specified.
 *
 * You cannot use SigV2 presigned URL for SSE-C if the customer key is specified
 * or else any PUT or GET using such presigned URL would fail. You cannot use
 * SigV2 presigned URL for SSE-KMS, as any PUT or GET using such presigned URL
 * would fail.
 */
public class PresignedUrlSigV2IntegrationTest extends S3IntegrationTestBase {

    private static String BUCKET = CryptoTestUtils
            .tempBucketName(PresignedUrlSigV2IntegrationTest.class);
    private static AmazonS3Client s3SigV2;
    private static File file;
    private static String nonDefaultKmsKeyId;

    @BeforeClass
    public static void setup() throws IOException {
        s3SigV2 = new AmazonS3TestClient(credentials,
                                         new LegacyClientConfiguration().withSignerOverride("S3SignerType"));
        s3SigV2.createBucket(BUCKET);
        file = CryptoTestUtils.generateRandomAsciiFile(100);
        nonDefaultKmsKeyId = KmsTestKeyCache.getInstance(Regions.US_EAST_1, credentials).getNonDefaultKeyId();
    }

    @AfterClass
    public static void after() {
        CryptoTestUtils.deleteBucketAndAllContents(s3SigV2, BUCKET);
        s3SigV2.shutdown();
    }

    /**
     * Retrieves the S3 object with the given HttpGet and compare the content
     * with that of the given file.
     */
    private void downloadFileAndVerify(File srcFile, HttpGet getreq)
            throws IOException, ClientProtocolException, FileNotFoundException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse res = httpclient.execute(getreq);
        InputStream is = res.getEntity().getContent();
        String actual = IOUtils.toString(is);
        httpclient.close();
        FileInputStream fis = new FileInputStream(srcFile);
        String expected = IOUtils.toString(fis);
        fis.close();
        Assert.assertEquals(expected, actual);
    }

    /**
     * Put the given file to S3 using the given HttpPut.
     */
    private CloseableHttpResponse putFileToS3(File file, HttpPut putreq)
            throws IOException, ClientProtocolException {
        putreq.setEntity(new FileEntity(file));
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse res = httpclient.execute(putreq);
        httpclient.close();
        return res;
    }

    /**
     * Test failure of presigned PUT URL using SIGV2 when SSE_C with the
     * customer key specified.
     */
    @Test
    public void failToPresignedPut_SSE_C_withKey() throws IOException {
        String KEY = "SSE_C_withKey-" + file.getName();
        byte[] secretKey = new byte[32];
        // Generate presigned PUT URL with use of SSE-C with the actual
        // customer key
        GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                BUCKET, KEY, HttpMethod.PUT);
        genreq.setSSECustomerKey(new SseCustomerKey(secretKey));
        URL url = s3SigV2.generatePresignedUrl(genreq);
        System.err.println("Presigned PUT URL for SSE-C (with key): " + url);

        // Puts object using presigned url with SSE-C
        HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
        putreq.addHeader(new BasicHeader(
                Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                SSEAlgorithm.AES256.getAlgorithm()));
        putreq.addHeader(new BasicHeader(
                Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, Base64
                .encodeAsString(secretKey)));
        putreq.addHeader(new BasicHeader(
                Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5, Md5Utils
                .md5AsBase64(secretKey)));
        CloseableHttpResponse res = putFileToS3(file, putreq);
        Assert.assertTrue(res.getStatusLine().getStatusCode() / 100 == 4);
    }

    /**
     * Test presigned URL using SIGV2, both for Put presigned-url and Get
     * presgined-url, when SSE_C wihtout the customer key is involved.
     */
    @Test
    public void presignedPutGet_SSE_C_withoutKey() throws IOException {
        String KEY = "SSE_C_withoutKey-" + file.getName();
        byte[] secretKey = new byte[32];
        // Generate presigned PUT URL with use of SSE-C without the actual
        // customer key
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            genreq.setSSECustomerKeyAlgorithm(SSEAlgorithm.getDefault());
            // We can optionally specify the storage class
            genreq.putCustomRequestHeader(Headers.STORAGE_CLASS,
                                          StorageClass.ReducedRedundancy.toString());
            URL url = s3SigV2.generatePresignedUrl(genreq);
            System.err.println("Presigned PUT URL for SSE-C (without key): "
                               + url);

            // Puts object using presigned url with SSE-C
            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                    SSEAlgorithm.AES256.getAlgorithm()));
            putreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, Base64
                    .encodeAsString(secretKey)));
            putreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5, Md5Utils
                    .md5AsBase64(secretKey)));
            putreq.addHeader(new BasicHeader(Headers.STORAGE_CLASS,
                                             StorageClass.ReducedRedundancy.toString()));
            putFileToS3(file, putreq);
        }

        // Generate presigned GET URL with use of SSE-C without the actual
        // customer key
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            genreq.setSSECustomerKeyAlgorithm(SSEAlgorithm.getDefault());
            URL url = s3SigV2.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE-C (without key): "
                               + url);

            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            getreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                    ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION));
            getreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, Base64
                    .encodeAsString(secretKey)));
            getreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5, Md5Utils
                    .md5AsBase64(secretKey)));
            downloadFileAndVerify(file, getreq);
        }
    }

    /**
     * Test presigned URL using SIGV2, both for Put presigned-url and Get
     * presgined-url, when SSE is involved.
     */
    @Test
    public void presignedPutGet_SSE() throws IOException {
        String KEY = "SSE-" + file.getName();
        // Generate presigned PUT URL with use of SSE
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            genreq.setSSEAlgorithm(SSEAlgorithm.getDefault());
            URL url = s3SigV2.generatePresignedUrl(genreq);
            System.err.println("Presigned PUT URL for SSE: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             SSEAlgorithm.AES256.getAlgorithm()));

            putFileToS3(file, putreq);
        }
        // Generate presigned GET URL for SSE object. Note nothing extra needs
        // to be done, SSE or not.
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            URL url = s3SigV2.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE: " + url);
            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            downloadFileAndVerify(file, getreq);
        }
    }

    /**
     * Test the failure of presigned PUT URL for SIGV2 when SSE_KMS is involved.
     */
    @Test
    public void failToPresignedPut_SSE_KMS() throws IOException {
        String KEY = "SSE_KMS-" + file.getName();
        // Generate presigned PUT URL with use of SSE-KMS
        SseAwsKeyManagementParams kmsParam = new SseAwsKeyManagementParams(nonDefaultKmsKeyId);
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                BUCKET, KEY, HttpMethod.PUT)
                .withSSEAlgorithm(kmsParam.getEncryption())
                .withKmsCmkId(kmsParam.getAwsKmsKeyId());
        URL url = s3SigV2.generatePresignedUrl(req);
        System.err.println("Presigned PUT URL for SSE KMS: " + url);

        HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
        putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                         kmsParam.getEncryption()));
        putreq.addHeader(new BasicHeader(
                Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID, kmsParam
                .getAwsKmsKeyId()));
        CloseableHttpResponse res = putFileToS3(file, putreq);
        Assert.assertTrue(res.getStatusLine().getStatusCode() / 100 == 4);
    }

    /**
     * Test failure of SigV2 with presigned PUT URL using SSE_KMS without
     * explicitly specifying CMKID (and therefore using the default KMS CMK ID.)
     */
    @Test
    public void failPresignedPut_SSE_KMS_withDefaultCmkId() throws IOException {
        final String KEY = "SSE_KMS-defaultCmkId-" + file.getName();
        // Generate presigned PUT URL with use of SSE-KMS
        {
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            // Uses the default S3 KMS CMK ID (ie "alias/aws/s3")
            req.setSSEAlgorithm(SSEAlgorithm.KMS.getAlgorithm());
            URL url = s3SigV2.generatePresignedUrl(req);
            System.err.println("Presigned PUT URL for SSE KMS: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             SSEAlgorithm.KMS.getAlgorithm()));
            CloseableHttpResponse res = putFileToS3(file, putreq);
            StatusLine sl = res.getStatusLine();
            Assert.assertTrue(sl.getStatusCode() / 100 == 4);
        }
    }

    /**
     * Test failure of SigV2 with presigned PUT URL using SSE_KMS explicitly
     * specifying the use of the default KMS CMK ID via "alias/aws/s3".
     */
    @Test
    public void failPresignedPut_SSE_KMS_withExplicitDefaultCmkId()
            throws IOException {
        final String KEY = "SSE_KMS-explicitDefaultCmkId-" + file.getName();
        SseAwsKeyManagementParams kmsParam = new SseAwsKeyManagementParams(
                "alias/aws/s3");
        // Generate presigned PUT URL with use of SSE-KMS
        {
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);

            req.setSSEAlgorithm(kmsParam.getEncryption());
            req.setKmsCmkId(kmsParam.getAwsKmsKeyId());

            URL url = s3SigV2.generatePresignedUrl(req);
            System.err.println("Presigned PUT URL for SSE KMS: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             SSEAlgorithm.KMS.getAlgorithm()));
            putreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID,
                    "alias/aws/s3"));
            CloseableHttpResponse res = putFileToS3(file, putreq);
            StatusLine sl = res.getStatusLine();
            Assert.assertTrue(sl.getStatusCode() / 100 == 4);
        }
    }

    @Test
    public void postUploads_SigV2() throws IOException {
        String KEY = "postUploads_SigV2";
        GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                BUCKET, KEY, HttpMethod.POST);
        genreq.addRequestParameter("uploads", null);
        genreq.putCustomRequestHeader(Headers.CONTENT_LENGTH, String.valueOf(0));
        URL url = s3SigV2.generatePresignedUrl(genreq);
        System.err.println("Presigned POST URL: " + url);

        HttpPost post = new HttpPost(URI.create(url.toExternalForm()));
        CloseableHttpResponse res = postToS3(post);
        Assert.assertTrue(res.getStatusLine().getStatusCode() == 200);
    }

    private CloseableHttpResponse postToS3(HttpEntityEnclosingRequestBase req)
            throws IOException, ClientProtocolException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse res = httpclient.execute(req);
        httpclient.close();
        return res;
    }
}
