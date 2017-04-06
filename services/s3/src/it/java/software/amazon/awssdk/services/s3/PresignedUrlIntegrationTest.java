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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.HttpMethod;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.MD5DigestCalculatingInputStream;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.ResponseHeaderOverrides;
import software.amazon.awssdk.services.s3.util.SpecialObjectKeyNameGenerator;
import software.amazon.awssdk.services.sts.auth.STSSessionCredentialsProvider;
import software.amazon.awssdk.util.BinaryUtils;

/**
 * Integration tests for pre-signing S3 URLs.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
@Category(S3Categories.Slow.class)
public class PresignedUrlIntegrationTest extends S3IntegrationTestBase {

    private static final int MAX_RETRIES = 3;
    private static final String S3_REGIONAL_ENDPOINT = "s3-us-west-2.amazonaws.com";
    /** The buckets created and used by this test. */
    private static String virtHostBucketName = null;
    private static String pathStyleBucketName = null;
    /** The file of random data uploaded to S3 by this test. */
    private static File file;
    private static String contentMd5;

    @AfterClass
    public static void tearDown() {
        try {
            deleteBucketAndAllContents(virtHostBucketName);
            deleteBucketAndAllContents(pathStyleBucketName);
            file.delete();
        } catch (Exception ex) {
            // Don't let the tearDown screwed up the test
            ex.printStackTrace(System.err);
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();

        s3.setEndpoint(S3_REGIONAL_ENDPOINT);

        virtHostBucketName = "java-presigned-url-virt-host-integ-test-" + new Date().getTime();
        pathStyleBucketName = "java-presigned-url-path-style-integ-test-" + new Date().getTime();

        s3.createBucket(virtHostBucketName);
        s3.createBucket(pathStyleBucketName);

        file = getRandomTempFile("presigned-url-integ-test", 1234L);
        MD5DigestCalculatingInputStream md5Stream = new MD5DigestCalculatingInputStream(new FileInputStream(file));
        while (md5Stream.read() != -1) {
        }
        contentMd5 = BinaryUtils.toBase64(md5Stream.getMd5Digest());
    }

    /**
     * Runs all the tests on special key names.
     */
    @Test
    public void testSpecialKeys() throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                // SigV2
                System.clearProperty(SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY);
                goTestSpecialKeys();
                break;
            } catch (AmazonS3Exception ex) {
                if (i == MAX_RETRIES - 1) {
                    throw ex;
                }
                ex.printStackTrace(System.err);
                Thread.sleep((2 << i) * 1000);
            }
        }
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                // SigV4
                System.setProperty(
                        SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY,
                        "true");
                try {
                    goTestSpecialKeys();
                } finally {
                    System.clearProperty(SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY);
                }
                break;
            } catch (AmazonS3Exception ex) {
                if (i == MAX_RETRIES - 1) {
                    throw ex;
                }
                ex.printStackTrace(System.err);
                Thread.sleep((2 << i) * 1000);
            }
        }
    }

    private void goTestSpecialKeys() throws Exception {
        List<String> keys = SpecialObjectKeyNameGenerator.initAllSpecialKeyNames();

        s3.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(false));
        for (String key : keys) {
            s3.putObject(virtHostBucketName, key, file);
            testGetUrl(s3, virtHostBucketName, key);
            testGetUrlWithContentType(virtHostBucketName, key);
            testGetUrlWithContentMd5(virtHostBucketName, key);
            testGetUrlWithExtraParameters(virtHostBucketName, key);
            testGetUrlWithResponseHeaders(virtHostBucketName, key);
            testHeadUrl(virtHostBucketName, key);
            testPutUrl(virtHostBucketName, key);
            testPutUrlWithContentType(virtHostBucketName, key);
            testPutUrlWithContentMd5(virtHostBucketName, key);
        }

        s3.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        for (String key : keys) {
            s3.putObject(pathStyleBucketName, key, file);
            testGetUrl(s3, pathStyleBucketName, key);
            testGetUrlWithContentType(pathStyleBucketName, key);
            testGetUrlWithContentMd5(pathStyleBucketName, key);
            testGetUrlWithExtraParameters(pathStyleBucketName, key);
            testGetUrlWithResponseHeaders(pathStyleBucketName, key);
            testHeadUrl(pathStyleBucketName, key);
            testPutUrl(pathStyleBucketName, key);
            testPutUrlWithContentType(pathStyleBucketName, key);
            testPutUrlWithContentMd5(pathStyleBucketName, key);
        }
    }

    @Test
    public void testSigV4ExpirationLimit() {
        final Date sevenDaysAndOneMinLater = new Date(new Date().getTime() + 1000 * 60 * (60 * 24 * 7 + 1));
        final GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(virtHostBucketName, "key");
        request.setExpiration(sevenDaysAndOneMinLater);

        // SigV4
        try {
            cnS3.generatePresignedUrl(request);
            fail("AmazonClientException is expected since the expiration exceeds the 7 day limit.");
        } catch (AmazonClientException expected) {
            // Ignored or expected.
        } catch (Exception e) {
            fail("AmazonClientException is expected since the expiration exceeds the 7 day limit.");
        }

        AmazonS3Client s3SigV2 = new AmazonS3TestClient(credentials);
        s3SigV2.generatePresignedUrl(request);
        s3SigV2.shutdown();
    }

    /**
     * Tests a simple pre-signed DELETE URL.
     */
    @Test
    public void testDeleteUrl() throws Exception {

        File file = getRandomTempFile("presigned-url-delete-test", 1234L);
        s3.putObject(virtHostBucketName, "key-delete-test", file);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(virtHostBucketName, "key-delete-test", HttpMethod.DELETE);
        HttpURLConnection connection = connectToPresignedUrl(s3, request);

        assertEquals(204, connection.getResponseCode());
        byte[] content = IOUtils.toByteArray(connection.getInputStream());
        assertEquals(0, content.length);
        file.delete();
    }

    /**
     * Tests that it generates the correct presigned URL using session token
     * credentials.
     */
    @Test
    public void testPresignUrlWithSessionTokenCredential() throws Exception {
        final String key = "presign-url-sts-test";

        STSSessionCredentialsProvider provider = new STSSessionCredentialsProvider(credentials);
        AmazonS3Client s3WithSts = new AmazonS3Client(provider);
        s3WithSts.setEndpoint(S3_REGIONAL_ENDPOINT);
        s3WithSts.putObject(virtHostBucketName, key, file);

        // SigV2
        testGetUrl(s3WithSts, virtHostBucketName, key);

        // SigV4
        System.setProperty(
                SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY, "true");
        try {
            testGetUrl(s3WithSts, virtHostBucketName, key);
        } finally {
            System.clearProperty(SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY);
        }
    }

    /*
     * Helper Methods
     */
    //

    /**
     * Tests a simple pre-signed GET URL, using the specified s3 client.
     */
    private void testGetUrl(AmazonS3Client s3, String bucketName, String key)
            throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            // Test the older (more streamlined) method for creating a
            // pre-signed GET URL
            URL presignedUrl = s3.generatePresignedUrl(bucketName, key,
                                                       new Date(new Date().getTime() + 1000 * 60 * 60));
            System.out.println(presignedUrl.toExternalForm());
            assertFileEqualsStream(file, presignedUrl.openStream());

            // And also test the new request oriented method
            HttpURLConnection connection = connectToPresignedUrl(s3,
                                                                 new GeneratePresignedUrlRequest(bucketName, key));
            if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                continue; // retry to overcome intermittant failure
            }
            assertEquals(200, connection.getResponseCode());
            assertFileEqualsStream(file, connection.getInputStream());

            // Test the URL works with Apache HttpClient
            HttpResponse response = connectToPresignUrlWithApacheHttpClient(s3,
                                                                            new GeneratePresignedUrlRequest(bucketName, key));
            if ((i < MAX_RETRIES - 1) && response.getStatusLine().getStatusCode() != 200) {
                continue; // retry to overcome intermittant failure
            }
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertFileEqualsStream(file, response.getEntity().getContent());
            return;
        }
    }

    /**
     * Tests a simple pre-signed GET URL with content type header.
     */
    private void testGetUrlWithContentType(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setContentType("model/vrm");

            HttpURLConnection connection = connectToPresignedUrlWithHeaders(request);
            if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                continue; // retry to overcome intermittant failure
            }
            assertEquals(200, connection.getResponseCode());
            assertFileEqualsStream(file, connection.getInputStream());
            return;
        }
    }

    /**
     * Tests a simple pre-signed GET URL with content MD5 header.
     */
    private void testGetUrlWithContentMd5(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            request.setContentMd5(contentMd5);

            HttpURLConnection connection = connectToPresignedUrlWithHeaders(request);
            if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                continue; // retry to overcome intermittant failure
            }
            assertEquals(200, connection.getResponseCode());
            assertFileEqualsStream(file, connection.getInputStream());
            return;
        }
    }

    /**
     * Tests a simple pre-signed HEAD URL.
     */
    private void testHeadUrl(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            HttpURLConnection connection = connectToPresignedUrl(
                    s3, new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.HEAD));
            if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                continue; // retry to overcome intermittant failure
            }

            assertNotNull(connection.getHeaderField(Headers.REQUEST_ID));
            assertNotNull(connection.getHeaderField(Headers.ETAG));
            byte[] content = IOUtils.toByteArray(connection.getInputStream());
            assertEquals(0, content.length);
            assertEquals(200, connection.getResponseCode());
            return;
        }
    }

    /**
     * Tests a simple pre-signed PUT URL.
     */
    private void testPutUrl(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
            HttpURLConnection connection = connectToPresignedUrl(s3, request);

            IOUtils.copy(new FileInputStream(file), connection.getOutputStream());

            if (androidRootDir == null) {
                if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                    continue; // retry to overcome intermittant failure
                }
                assertEquals(200, connection.getResponseCode());
                byte[] content = IOUtils.toByteArray(connection.getInputStream());
                assertEquals(0, content.length);
            }

            assertEquals(IOUtils.toString(new FileInputStream(file)),
                         IOUtils.toString(s3.getObject(bucketName, key).getObjectContent()));
            return;
        }
    }

    /**
     * Tests a simple pre-signed PUT URL with content type header.
     */
    private void testPutUrlWithContentType(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
            request.setContentType("model/vrm");
            HttpURLConnection connection = connectToPresignedUrlWithHeaders(request);

            IOUtils.copy(new FileInputStream(file), connection.getOutputStream());

            if (androidRootDir == null) {
                if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                    continue; // retry to overcome intermittant failure
                }
                assertEquals(200, connection.getResponseCode());
                byte[] content = IOUtils.toByteArray(connection.getInputStream());
                assertEquals(0, content.length);
            }

            assertEquals(IOUtils.toString(new FileInputStream(file)),
                         IOUtils.toString(s3.getObject(bucketName, key).getObjectContent()));
            return;
        }
    }

    /**
     * Tests a simple pre-signed PUT URL with content MD5 header.
     */
    private void testPutUrlWithContentMd5(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
            request.setContentMd5(contentMd5);
            HttpURLConnection connection = connectToPresignedUrlWithHeaders(request);

            IOUtils.copy(new FileInputStream(file), connection.getOutputStream());

            if (androidRootDir == null) {
                if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                    continue; // retry to overcome intermittant failure
                }
                System.out.println(connection.getResponseMessage());
                assertEquals(200, connection.getResponseCode());
                byte[] content = IOUtils.toByteArray(connection.getInputStream());
                assertEquals(0, content.length);
            }

            assertEquals(IOUtils.toString(new FileInputStream(file)),
                         IOUtils.toString(s3.getObject(bucketName, key).getObjectContent()));
            return;
        }
    }

    /**
     * Tests a pre-signed GET URL which includes additional request parameters.
     */
    private void testGetUrlWithExtraParameters(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET);
            request.addRequestParameter("torrent", null);

            HttpURLConnection connection = connectToPresignedUrl(s3, request);
            if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                continue; // retry to overcome intermittant failure
            }
            assertEquals(200, connection.getResponseCode());
            assertEquals("application/x-bittorrent", connection.getHeaderField("Content-Type"));
            return;
        }
    }

    /**
     * Tests a pre-signed GET URL which includes response headers
     */
    private void testGetUrlWithResponseHeaders(String bucketName, String key) throws Exception {
        for (int i = 0; i < MAX_RETRIES; i++) {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET)
                    .withResponseHeaders(new ResponseHeaderOverrides().withContentType("OVERRIDE"));

            HttpURLConnection connection = connectToPresignedUrl(s3, request);
            if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                continue; // retry to overcome intermittant failure
            }
            assertEquals(200, connection.getResponseCode());
            assertEquals("OVERRIDE", connection.getHeaderField("Content-Type"));

            request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET)
                    .withResponseHeaders(new ResponseHeaderOverrides().withContentType("OVERRIDE").withContentDisposition("OVERRIDE"));

            connection = connectToPresignedUrl(s3, request);
            if ((i < MAX_RETRIES - 1) && connection.getResponseCode() != 200) {
                continue; // retry to overcome intermittant failure
            }
            assertEquals(200, connection.getResponseCode());
            assertEquals("OVERRIDE", connection.getHeaderField("Content-Type"));
            assertEquals("OVERRIDE", connection.getHeaderField("Content-Disposition"));
            return;
        }
    }

    private HttpURLConnection connectToPresignedUrl(AmazonS3Client s3, GeneratePresignedUrlRequest request) throws Exception {
        URL url = s3.generatePresignedUrl(request);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(request.getMethod().toString());
        connection.setDoOutput(true);
        connection.connect();

        return connection;
    }

    private HttpResponse connectToPresignUrlWithApacheHttpClient(AmazonS3Client s3, GeneratePresignedUrlRequest request)
            throws Exception {
        URL url = s3.generatePresignedUrl(request);

        HttpMethod httpMethod = request.getMethod();
        HttpRequestBase httpRequest = null;
        switch (httpMethod) {
            case GET:
                httpRequest = new HttpGet(URI.create(url.toExternalForm()));
                break;
            case PUT:
                httpRequest = new HttpPut(URI.create(url.toExternalForm()));
                break;
            case HEAD:
                httpRequest = new HttpHead(URI.create(url.toExternalForm()));
                break;
            case DELETE:
                httpRequest = new HttpDelete(URI.create(url.toExternalForm()));
                break;
            case POST:
                httpRequest = new HttpPost(URI.create(url.toExternalForm()));
                break;
            default:
                fail("Unrecognized http method.");
        }
        HttpResponse response = new DefaultHttpClient().execute(httpRequest);
        return response;
    }

    private HttpURLConnection connectToPresignedUrlWithHeaders(GeneratePresignedUrlRequest request) throws Exception {
        URL url = s3.generatePresignedUrl(request);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (request.getContentType() != null) {
            connection.setRequestProperty(Headers.CONTENT_TYPE, request.getContentType());
        }
        if (request.getContentMd5() != null) {
            connection.setRequestProperty(Headers.CONTENT_MD5, request.getContentMd5());
        }
        connection.setRequestMethod(request.getMethod().toString());
        connection.setDoOutput(true);
        connection.connect();

        return connection;
    }

}
