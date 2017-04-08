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
 * Test various URL presigning cases with SSE/SSE-C/SSE-KMS using S3 SigV4
 * signer.
 */
public class PresignedUrlSigV4IntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = CryptoTestUtils.tempBucketName(PresignedUrlSigV4IntegrationTest.class);

    private static String nonDefaultKmsKeyId;
    private static AmazonS3Client s3SigV4, s3SigV2;
    private static File file;

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        s3SigV2 = new AmazonS3TestClient(credentials);
        s3SigV4 = new AmazonS3TestClient(credentials,
                                         new LegacyClientConfiguration().withSignerOverride("AWSS3V4SignerType"));
        s3SigV4.createBucket(BUCKET);
        file = CryptoTestUtils.generateRandomAsciiFile(100);
        nonDefaultKmsKeyId = KmsTestKeyCache.getInstance(Regions.US_EAST_1, credentials).getNonDefaultKeyId();
    }

    @AfterClass
    public static void after() {
        CryptoTestUtils.deleteBucketAndAllContents(s3SigV4, BUCKET);
        s3SigV2.shutdown();
        s3SigV4.shutdown();
    }

    /**
     * Retrieves the S3 object with the given HttpGet and compare the content
     * with that of the given file.
     */
    private void downloadFileAndVerify(File srcFile, HttpGet getreq) throws IOException,
                                                                            ClientProtocolException, FileNotFoundException {
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


    private StatusLine fire(HttpGet getreq) throws IOException,
                                                   ClientProtocolException, FileNotFoundException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse res = httpclient.execute(getreq);
        StatusLine statusLine = res.getStatusLine();
        httpclient.close();
        return statusLine;
    }

    /**
     * Put the given file to S3 using the given HttpPut.
     */
    private CloseableHttpResponse putFileToS3(File file, HttpPut putreq) throws IOException,
                                                                                ClientProtocolException {
        putreq.setEntity(new FileEntity(file));
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse res = httpclient.execute(putreq);
        httpclient.close();
        return res;
    }

    /**
     * Test presigned URL, both for Put presigned-url and Get presgined-url,
     * when SSE_C with the customer key specified.
     */
    @Test
    public void presignedPutGet_SSE_C_withKey() throws IOException {
        String KEY = "SSE_C_withKey-" + file.getName();
        byte[] secretKey = new byte[32];
        // Generate presigned PUT URL with use of SSE-C with the actual
        // customer key
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            genreq.setSSECustomerKey(new SseCustomerKey(secretKey));
            URL url = s3SigV4.generatePresignedUrl(genreq);
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
            putFileToS3(file, putreq);
        }

        // Generate presigned GET URL with use of SSE-C with the actual
        // customer key
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            genreq.setSSECustomerKey(new SseCustomerKey(secretKey));
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE-C (with key): " + url);

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

        // Generate SigV2 presigned GET URL with use of SSE-C with the actual
        // customer key would lead to failure during GET
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            genreq.setSSECustomerKey(new SseCustomerKey(secretKey));
            URL url = s3SigV2.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE-C (with key): " + url);

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
            StatusLine statusLine = fire(getreq);
            Assert.assertTrue(statusLine.getStatusCode() / 100 == 4);
        }
    }

    /**
     * Test presigned URL, both for Put presigned-url and Get presgined-url,
     * when SSE_C wihtout the customer key is involved.
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
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned PUT URL for SSE-C (without key): " + url);

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
        // customer key.
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            genreq.setSSECustomerKeyAlgorithm(SSEAlgorithm.getDefault());
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE-C (without key): " + url);

            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            getreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                    SSEAlgorithm.AES256.getAlgorithm()));
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
     * Test presigned URL, both for Put presigned-url and Get presgined-url,
     * when SSE is involved.
     */
    @Test
    public void presignedPutGet_SSE() throws IOException {
        String KEY = "SSE-" + file.getName();
        // Generate presigned PUT URL with use of SSE
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            genreq.setSSEAlgorithm(SSEAlgorithm.getDefault());
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned PUT URL for SSE: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             SSEAlgorithm.AES256.getAlgorithm()));

            putFileToS3(file, putreq);
        }
        // Generate presigned GET URL for SSE object.  Note nothing extra needs
        // to be done, SSE or not.
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE: " + url);
            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            downloadFileAndVerify(file, getreq);
        }
    }

    /**
     * Test presigned URL, both for Put presigned-url and Get presgined-url,
     * when SSE_KMS is involved.
     */
    @Test
    public void presignedPutGet_SSE_KMS() throws IOException {
        String KEY = "SSE_KMS-" + file.getName();
        // Generate presigned PUT URL with use of SSE-KMS
        {
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            SseAwsKeyManagementParams kmsParam = new SseAwsKeyManagementParams(
                    nonDefaultKmsKeyId);
            req.withSSEAlgorithm(kmsParam.getEncryption())
               .withKmsCmkId(kmsParam.getAwsKmsKeyId());
            URL url = s3SigV4.generatePresignedUrl(req);
            System.err.println("Presigned PUT URL for SSE KMS: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             kmsParam.getEncryption()));
            putreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID, kmsParam
                    .getAwsKmsKeyId()));
            putFileToS3(file, putreq);
        }
        // Generate presigned GET URL for SSE object.  Note nothing extra needs
        // to be done, SSE or not.
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE KMS: " + url);
            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            downloadFileAndVerify(file, getreq);
        }
        // Generate SigV2 presigned GET URL for SSE object would lead to failure
        // during GET.
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            URL url = s3SigV2.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE KMS: " + url);
            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            StatusLine statusLine = fire(getreq);
            Assert.assertTrue(statusLine.getStatusCode() / 100 == 4);
        }
    }

    /**
     * Test the failure of presigned PUT URL using SSE_KMS when CMK ID is
     * specified during the actual PUT but not during URL pre-signing.
     */
    @Test
    public void failToPresignedPut_SSE_KMS_withoutCmkId() throws IOException {
        String KEY = "SSE_KMS-noCmkId-" + file.getName();
        // Generate presigned PUT URL with use of SSE-KMS
        {
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            // Deliberately bypass the input validation via custom headers
            req.putCustomRequestHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                       SSEAlgorithm.KMS.getAlgorithm());
            URL url = s3SigV4.generatePresignedUrl(req);
            System.err.println("Presigned PUT URL for SSE KMS: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            SseAwsKeyManagementParams kmsParam = new SseAwsKeyManagementParams(
                    KmsTestKeyCache.getInstance(Regions.US_EAST_1, credentials).getNonDefaultKeyId());
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             kmsParam.getEncryption()));
            putreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID, kmsParam
                    .getAwsKmsKeyId()));
            CloseableHttpResponse res = putFileToS3(file, putreq);
            StatusLine sl = res.getStatusLine();
            Assert.assertTrue(sl.getStatusCode() / 100 == 4);
        }
    }

    /**
     * Test presigned PUT/GET URL using SSE_KMS without explicitly specifying
     * CMK ID (and therefore using the default S3's KMS CMK ID.)
     */
    @Test
    public void presignedPutGet_SSE_KMS_withDefaultCmkId() throws IOException {
        final String KEY = "SSE_KMS-defaultCmkId-" + file.getName();
        // Generate presigned PUT URL with use of SSE-KMS
        {
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.PUT);
            // Uses the default S3 KMS CMK ID (ie "alias/aws/s3")
            req.setSSEAlgorithm(SSEAlgorithm.KMS.getAlgorithm());
            URL url = s3SigV4.generatePresignedUrl(req);
            System.err.println("Presigned PUT URL for SSE KMS: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             SSEAlgorithm.KMS.getAlgorithm()));
            CloseableHttpResponse res = putFileToS3(file, putreq);
            StatusLine sl = res.getStatusLine();
            Assert.assertTrue(sl.getStatusCode() / 100 == 2);
        }
        // Generate SigV4 presigned GET URL for SSE object
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE KMS: " + url);
            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            downloadFileAndVerify(file, getreq);
        }
    }

    /**
     * Test presigned PUT/GET URL using SSE_KMS explicitly specifying
     * the use of the default KMS CMK ID via "alias/aws/s3".
     */
    @Test
    public void presignedPutGet_SSE_KMS_withExplicitDefaultCmkId()
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

            URL url = s3SigV4.generatePresignedUrl(req);
            System.err.println("Presigned PUT URL for SSE KMS: " + url);

            HttpPut putreq = new HttpPut(URI.create(url.toExternalForm()));
            putreq.addHeader(new BasicHeader(Headers.SERVER_SIDE_ENCRYPTION,
                                             SSEAlgorithm.KMS.getAlgorithm()));
            putreq.addHeader(new BasicHeader(
                    Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID,
                    "alias/aws/s3"));
            CloseableHttpResponse res = putFileToS3(file, putreq);
            StatusLine sl = res.getStatusLine();
            Assert.assertTrue(sl.getStatusCode() / 100 == 2);
        }
        // Generate SigV4 presigned GET URL for SSE object
        {
            GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                    BUCKET, KEY, HttpMethod.GET);
            URL url = s3SigV4.generatePresignedUrl(genreq);
            System.err.println("Presigned GET URL for SSE KMS: " + url);
            HttpGet getreq = new HttpGet(URI.create(url.toExternalForm()));
            downloadFileAndVerify(file, getreq);
        }
    }

    @Test
    public void postUploads_ZeroByteContent() throws IOException {
        String KEY = "postUploads_ZeroByteContent";
        GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                BUCKET, KEY, HttpMethod.POST);
        genreq.addRequestParameter("uploads", null);
        genreq.setZeroByteContent(true);
        URL url = s3SigV4.generatePresignedUrl(genreq);
        System.err.println("Presigned POST URL: " + url);

        HttpPost post = new HttpPost(URI.create(url.toExternalForm()));
        CloseableHttpResponse res = postToS3(post);
        Assert.assertTrue(res.getStatusLine().getStatusCode() == 200);
    }

    @Test
    public void postUploads_NullContent() throws IOException {
        String KEY = "postUploads_NullContent";
        GeneratePresignedUrlRequest genreq = new GeneratePresignedUrlRequest(
                BUCKET, KEY, HttpMethod.POST);
        genreq.addRequestParameter("uploads", null);
        URL url = s3SigV4.generatePresignedUrl(genreq);
        System.err.println("Presigned POST URL: " + url);

        HttpPost post = new HttpPost(URI.create(url.toExternalForm()));
        CloseableHttpResponse res = postToS3(post);
        Assert.assertTrue(res.getStatusLine().getStatusCode() / 100 == 4);
    }

    private CloseableHttpResponse postToS3(HttpEntityEnclosingRequestBase req)
            throws IOException, ClientProtocolException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse res = httpclient.execute(req);
        httpclient.close();
        return res;
    }
}
