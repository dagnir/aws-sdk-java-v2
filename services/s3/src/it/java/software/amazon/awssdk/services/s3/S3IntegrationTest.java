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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GroupGrantee;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.Owner;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.util.StreamUtils;
import software.amazon.awssdk.test.util.UnreliableRandomInputStream;

/**
 * Integration tests for the AWS S3 Java client.
 *
 * @author fulghum@amazon.com
 */
public final class S3IntegrationTest extends S3IntegrationTestBase {

    /** Object contents to use/expect when we get/put a test object */
    private static final String EXPECTED_OBJECT_CONTENTS = "Hello S3 Java client world!!!";

    /** Name of the test bucket these tests will create, test, delete, etc */
    private static final String expectedBucketName = "integ-test-bucket-" + new Date().getTime();

    /** Name of the test CN bucket these tests will create, test, delete, etc */
    private static final String expectedCnBucketName = "integ.test.cn.bucket-foobar-" + new Date().getTime();
    /** Name of the test key these tests will create, test, delete, etc */
    private static final String expectedKey = "integ-test-key-" + new Date().getTime();
    /** Redirect location for a specific object */
    private static final String REDIRECT_LOCATION = "/redirecting...";
    private static final String CREATE_BUCKET_TEST_BUCKET_PREFIX = "test-create-bucket-";
    /** Name of the test S3 account running these tests */
    private final String expectedS3AccountOwnerName = "aws-dr-tools-test";

    /**
     * Ensures that any created test resources are correctly released.
     */
    @AfterClass
    public static void tearDown() {
        if (expectedBucketName != null) {
            deleteBucketAndAllContents(expectedBucketName);
        }

        if (expectedCnBucketName != null) {
            CryptoTestUtils.deleteBucketAndAllContents(cnS3,
                                                       expectedCnBucketName);
        }

        deleteBucketAndAllContentsWithPrefix(CREATE_BUCKET_TEST_BUCKET_PREFIX);
    }

    /**
     * Tests that we can correctly create an S3 bucket in the default location
     * for these tests to use.
     */
    @BeforeClass
    public static void createBucket() {
        CreateBucketRequest request = new CreateBucketRequest(expectedBucketName);
        request.setCannedAcl(CannedAccessControlList.AuthenticatedRead);
        Bucket bucket = s3.createBucket(request);
        assertNotNull(bucket);
        assertEquals(expectedBucketName, bucket.getName());
        S3ResponseMetadata responseMetadata = s3.getCachedResponseMetadata(request);
        assertNotNull(responseMetadata.getHostId());
        assertNotNull(responseMetadata.getRequestId());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(EXPECTED_OBJECT_CONTENTS.getBytes().length);
        s3.putObject(expectedBucketName, expectedKey, new ByteArrayInputStream(EXPECTED_OBJECT_CONTENTS.getBytes()), metadata);

        AccessControlList bucketAcl = s3.getBucketAcl(bucket.getName());
        assertTrue(doesAclContainGroupGrant(bucketAcl, GroupGrantee.AuthenticatedUsers, Permission.Read));
    }

    /**
     * Tests that we can correctly create an S3 bucket in the EU location
     * for these tests to use.
     */
    @BeforeClass
    public static void createCnBucket() {
        cnS3.setEndpoint("s3.cn-north-1.amazonaws.com.cn");
        Bucket bucket = cnS3.createBucket(expectedCnBucketName, "cn-north-1");
        assertNotNull(bucket);
        assertEquals(expectedCnBucketName, bucket.getName());
        assertEquals(cnS3.getBucketLocation(expectedCnBucketName),
                     "cn-north-1");

        String key = "key-with-$extended@-ascii-chars";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength("hello-world".getBytes().length);
        cnS3.putObject(expectedCnBucketName, key, new ByteArrayInputStream("hello-world".getBytes()), metadata);
    }

    @After
    public void after() {
        // Clear the system property by setting to blank
        System.clearProperty(SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE);
    }

    /*
     * Private Helper Methods
     */

    /**
     * Searches the specified list of buckets and returns the bucket with the
     * specified name if found, otherwise returns null.
     *
     * @param buckets
     *            The list of bucket objects to check.
     * @param bucketName
     *            The bucket name to search for in the list of buckets.
     * @return The bucket from the specified list matching the specified bucket
     *         name, otherwise null if no bucket was found with a matching name.
     */
    private Bucket findBucketInList(List<Bucket> buckets, String bucketName) {
        for (Iterator<Bucket> iterator = buckets.iterator(); iterator.hasNext(); ) {
            Bucket bucket = (Bucket) iterator.next();
            if (bucket.getName().equals(bucketName)) {
                return bucket;
            }
        }

        return null;
    }

    /**
     * Returns true if the list of objects contains an object with the expected
     * key.
     *
     * @param objects
     *            The list of objects to check.
     * @param expectedKey
     *            The object key to search for in the list of objects.
     * @return True if the list of objects contains an object with the specified
     *         key.
     */
    private boolean objectListContainsKey(List<S3ObjectSummary> objects, String expectedKey) {
        for (Iterator<S3ObjectSummary> iterator = objects.iterator(); iterator.hasNext(); ) {
            S3ObjectSummary obj = iterator.next();
            if (obj.getKey().equals(expectedKey)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests that the RepeatableInputStreamRequestEntity doesn't mask IO errors
     * with exceptions about being unable to reset the the stream back far
     * enough.
     */
    @Test
    public void testRepeatableRequestEntityPreservesOrignalError() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(1024 * 1024);
        // Default stream buffer is up to 128K so the reset would fail
        UnreliableRandomInputStream inputStream = new UnreliableRandomInputStream(metadata.getContentLength());
        try {
            s3.putObject(expectedBucketName, "key", inputStream, metadata);
            fail("Expected an exception to be thrown");
        } catch (ResetException ace) {
            assertResetException(ace);
        } finally {
            inputStream.close();
        }
    }

    private void assertResetException(ResetException ace) {
        String msg = ace.getMessage();
        assertTrue(msg, msg.contains("If the request involves an input stream, the maximum stream buffer size can be configured via request.getRequestClientOptions().setReadLimit(int)"));
        Throwable cause = ace.getCause();
        String cause_msg = cause.getMessage();
        assertTrue(cause_msg, cause_msg.contains("Resetting to invalid mark"));
    }

    @Test
    public void testRepeatableRequestEntity_InsufficientBufferSize() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        final int len = 1024 * 1024;
        metadata.setContentLength(len);
        // Default stream buffer is up to 128K so the reset would fail
        UnreliableRandomInputStream inputStream = new UnreliableRandomInputStream(metadata.getContentLength());
        // Configure stream buffer to the exact size required minus 1, which just
        // happens to be len/2 due to the way UnreliableRandomInputStream is
        // implemented
        System.setProperty(SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE, String.valueOf(len / 2));
        try {
            s3.putObject(expectedBucketName, "key", inputStream, metadata);
            fail("Expected an exception to be thrown");
        } catch (ResetException ace) {
            assertResetException(ace);
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testRepeatableRequestEntity_ExactBufferSize() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        final int len = 1024 * 1024;
        metadata.setContentLength(len);
        // Configure stream buffer to the exact size required, which just
        // happens to be len/2 + 1 due to the way UnreliableRandomInputStream is
        // implemented
        System.setProperty(SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE, String.valueOf(len / 2 + 1));
        UnreliableRandomInputStream inputStream = new UnreliableRandomInputStream(metadata.getContentLength());
        s3.putObject(expectedBucketName, "key", inputStream, metadata);
        inputStream.close();
    }

    @Test
    public void testRepeatableRequestEntity_MaxBufferSize() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        final int len = 1024 * 1024;
        metadata.setContentLength(len);
        // Configure max stream buffer to the max possible value
        System.setProperty(SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE, String.valueOf(Integer.MAX_VALUE));
        UnreliableRandomInputStream inputStream = new UnreliableRandomInputStream(metadata.getContentLength());
        s3.putObject(expectedBucketName, "key", inputStream, metadata);
        inputStream.close();
    }

    /**
     * Tests that overridden request credentials are correctly used when specified.
     */
    @Test
    public void testOverriddenRequestCredentials() throws Exception {
        s3.listBuckets();

        ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
        listBucketsRequest.setRequestCredentials(new BasicAWSCredentials("foo",
                                                                         "bar"));
        try {
            s3.listBuckets(listBucketsRequest);
            fail("Expected an authentication exception from bogus request credentials.");
        } catch (Exception e) {
        }
    }

    /**
     * Tests that when a bucket name contains ':'s we don't throw a NumberFormatException anymore.
     */
    @Test
    public void testInvalidBucketName() throws Exception {
        try {
            s3.listObjects("aws:s3:::bucket.s3.amazonaws.com");
            fail("Expected an exception, but wasn't thrown");
        } catch (AmazonClientException ace) {
        }
    }

    /**
     * Tests that we can correctly identify when a bucket exists or not.
     */
    @Test
    public void testDoesBucketExist() throws Exception {
        assertTrue(s3.doesBucketExist(expectedBucketName));   // a bucket we own
        assertTrue(cnS3.doesBucketExist(expectedCnBucketName)); // a bucket we own in another region
        assertTrue(s3.doesBucketExist("s3-bucket"));          // a bucket we don't own
        assertFalse(s3.doesBucketExist(                       // a non-existent bucket
                                                              "qweoiuasnxcvmnsfkljawasmnxasqwoiasdlfjamnxjkaoia-" + System.currentTimeMillis()));

        /*
         * The new implementation of @see
         * software.amazon.awssdk.services.s3.AmazonS3#doesBucketExist(java.lang.String)
         *
         * 1. returns true if the bucket exists even if the credentials are
         * wrong.
         * 2. returns false only if the bucket is not available.
         */

        AmazonS3 noCredentialsS3 = new AmazonS3Client();
        assertTrue(noCredentialsS3.doesBucketExist(expectedBucketName));

        AmazonS3 unknownCredentialsS3 = new AmazonS3Client(
                new BasicAWSCredentials("FOO", "BAR"));
        assertTrue(unknownCredentialsS3.doesBucketExist(expectedBucketName));

        AmazonS3 badCredentialsS3 = new AmazonS3Client(new BasicAWSCredentials(
                credentials.getAWSAccessKeyId(), "BAD"));
        assertTrue(badCredentialsS3.doesBucketExist(expectedBucketName));

    }

    @Test
    public void testZeroByteFile() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        String key = "integ-test-key-zero byte file";
        s3.putObject(expectedBucketName, key, new ByteArrayInputStream(new byte[0]), metadata);

        metadata = s3.getObjectMetadata(expectedBucketName, key);
        assertTrue(0 == metadata.getContentLength());
        assertNotNull(metadata.getETag());

        s3.getObject(expectedBucketName, key);
    }

    /**
     * Tests that the library correctly handles object keys containing spaces.
     */
    @Test
    public void testKeyUrlEncoding() throws IOException {
        ObjectMetadata expectedMetadata = new ObjectMetadata();
        expectedMetadata.addUserMetadata("bash", "bar");
        expectedMetadata.addUserMetadata("boo", "foo");

        String key = "integ-test-key-with spaces ~ in it";

        // Upload an object with spaces in the key
        s3.putObject(expectedBucketName, key, new ByteArrayInputStream("FOO!".getBytes()), expectedMetadata);

        // Test listing the key with spaces in it
        List<S3ObjectSummary> objects = s3.listObjects(expectedBucketName).getObjectSummaries();
        objectListContainsKey(objects, key);

        // Test getting an object with spaces in the key
        InputStream inputStream = s3.getObject(expectedBucketName, key).getObjectContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        assertEquals("FOO!", reader.readLine());

        // Test getting object metadata with spaces in the key
        ObjectMetadata objectMetadata = s3.getObjectMetadata(expectedBucketName, key);
        Map<String, String> metadata = objectMetadata.getUserMetadata();
        assertTrue(expectedMetadata.getUserMetadata().size() == metadata.size());
        assertEquals("bar", metadata.get("bash"));
        assertEquals("foo", metadata.get("boo"));

        // Test deleting an object with spaces in the key
        s3.deleteObject(expectedBucketName, key);
    }


    /**
     * Tests that we can retrieve the S3 object metadata we previously set.
     */
    @Test
    public void testGetObjectMetadata() throws ParseException {

        final long EXPIRATION_IN_MILLIS = 1000 * 60 * 60;
        Date expiresDate = new Date(System.currentTimeMillis() + EXPIRATION_IN_MILLIS);

        InputStream input = new ByteArrayInputStream(EXPECTED_OBJECT_CONTENTS.getBytes());

        ObjectMetadata Metadata = new ObjectMetadata();
        Metadata.addUserMetadata("foo", "bar");
        Metadata.addUserMetadata("baz", "bash");
        Metadata.setHttpExpiresDate(expiresDate);
        s3.putObject(expectedBucketName, expectedKey, input, Metadata);
        ObjectMetadata objectMetadata = s3.getObjectMetadata(expectedBucketName, expectedKey);
        Map<String, String> metadataMap = objectMetadata.getUserMetadata();
        assertTrue(2 == metadataMap.size());
        assertEquals("bar", metadataMap.get("foo"));
        assertEquals("bash", metadataMap.get("baz"));
        assertEquals(expiresDate.toString(), objectMetadata.getHttpExpiresDate().toString());
    }

    /**
     * Tests that we can get the object we previously stored in these tests.
     */
    @Test
    public void testGetObject() throws IOException {
        InputStream inputStream = s3.getObject(expectedBucketName, expectedKey).getObjectContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        assertEquals(EXPECTED_OBJECT_CONTENTS, reader.readLine());
    }

    /** Tests that we can create URLs for S3 objects. */
    @Test
    public void testGetUrl() {
        URL url = s3.getUrl(expectedBucketName, expectedKey);

        assertTrue(url.getHost().startsWith(expectedBucketName));
        assertEquals("/" + expectedKey, url.getPath());
    }

    /**
     * Tests that we can get the object we previously stored in these tests.
     */
    @Test
    public void testGetObjectMD5Validation() throws IOException {
        // Close without consuming all the contents
        InputStream inputStream = s3.getObject(expectedBucketName, expectedKey).getObjectContent();
        inputStream.close();

        // Consume all the contents before close
        inputStream = s3.getObject(expectedBucketName, expectedKey).getObjectContent();
        StreamUtils.consumeInputStream(inputStream);
        inputStream.close();

        // Get object with range
        inputStream = s3.getObject(new GetObjectRequest(expectedBucketName, expectedKey).withRange(2, 5)).getObjectContent();
        StreamUtils.consumeInputStream(inputStream);
        inputStream.close();
    }

    @Test
    public void testObjectWithRedirectLocation() throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(EXPECTED_OBJECT_CONTENTS.getBytes().length);
        s3.putObject(new PutObjectRequest(expectedBucketName, expectedKey + 1, new ByteArrayInputStream(EXPECTED_OBJECT_CONTENTS.getBytes()), metadata).withRedirectLocation(REDIRECT_LOCATION));
        S3Object object = s3.getObject(expectedBucketName, expectedKey + 1);
        InputStream inputStream = object.getObjectContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        assertEquals(REDIRECT_LOCATION, object.getRedirectLocation());
        assertEquals(EXPECTED_OBJECT_CONTENTS, reader.readLine());

        // Whether we can successfully change the Object Direction
        s3.setObjectRedirectLocation(expectedBucketName, expectedKey + 1, REDIRECT_LOCATION + 123);

        object = s3.getObject(expectedBucketName, expectedKey + 1);
        inputStream = object.getObjectContent();
        reader = new BufferedReader(new InputStreamReader(inputStream));

        assertEquals(REDIRECT_LOCATION + 123, object.getRedirectLocation());
        assertEquals(EXPECTED_OBJECT_CONTENTS, reader.readLine());

        s3.putObject(new PutObjectRequest(expectedBucketName, expectedKey + 2, REDIRECT_LOCATION));
        object = s3.getObject(expectedBucketName, expectedKey + 2);

        assertEquals(REDIRECT_LOCATION, object.getRedirectLocation());
        assertTrue(0 == object.getObjectMetadata().getContentLength());

    }

    /**
     * Tests that we can list the buckets in our account.
     */
    @Test
    public void testListBuckets() {
        List<Bucket> buckets = s3.listBuckets();
        Bucket bucket = findBucketInList(buckets, expectedBucketName);
        assertNotNull(bucket);
        assertNotNull(bucket.getCreationDate());
        assertNotNull(bucket.getOwner());
        assertEquals(bucket.getOwner(), s3.getS3AccountOwner());
        assertNotNull(bucket.getOwner().getDisplayName());
        assertNotNull(bucket.getOwner().getId());
    }

    /**
     * Tests that we can get an account owner for this account
     */
    //    @Test
    public void testGetS3AccountOwner() {
        Owner owner = s3.getS3AccountOwner();
        assertNotNull(owner);
        assertEquals(owner.getDisplayName(), expectedS3AccountOwnerName, owner.getDisplayName());
    }

    /**
     * Tests that we can list the objects in our test bucket.
     */
    @Test
    public void testListObjects() {
        List<S3ObjectSummary> objects = s3.listObjects(expectedBucketName).getObjectSummaries();
        assertTrue(objectListContainsKey(objects, expectedKey));

        objects = s3.listObjects(expectedBucketName, "non-existant-object-key-prefix-").getObjectSummaries();
        assertNotNull(objects);

        objects = s3.listObjects(new ListObjectsRequest(expectedBucketName, null, null, null, 0)).getObjectSummaries();
        assertTrue(objects.size() == 0);
    }

    /**
     * Tests that we can correctly list objects in a bucket using the prefix
     * parameter.
     */
    @Test
    public void testListObjectsByPrefix() {
        ListObjectsRequest request = new ListObjectsRequest(
                expectedBucketName, expectedKey.substring(0, 5), null, null, null);
        List<S3ObjectSummary> objects = s3.listObjects(request).getObjectSummaries();
        S3ResponseMetadata responseMetadata = s3.getCachedResponseMetadata(request);
        assertNotNull(responseMetadata.getHostId());
        assertNotNull(responseMetadata.getRequestId());

        assertTrue(objectListContainsKey(objects, expectedKey));

        objects = s3.listObjects(new ListObjectsRequest(
                expectedBucketName, "NonExistantKeyPrefix", null, null, null)).getObjectSummaries();
        assertTrue(0 == objects.size());
    }

    /**
     * Tests that we can look up the bucket location for US and CN buckets.
     */
    @Test
    public void testGetBucketLocation() {
        assertEquals("US", s3.getBucketLocation(expectedBucketName));
        assertEquals("cn-north-1", cnS3.getBucketLocation(expectedCnBucketName));
    }

    @Test
    public void testUploadDownloadString() throws Exception {
        String key = "strIntegrationTest";
        String testString = "fooStr";

        s3.putObject(expectedBucketName, key, testString);

        assertEquals(s3.getObjectAsString(expectedBucketName, key), testString);
    }

    @Test
    public void testCreateBucket_gammarEndpoint() {
        doTestCreateBucket(s3gamma, "US", false);
    }

    @Test
    public void testCreateBucket_globalEndpointWithRegionSpecified() {
        doTestCreateBucket(s3, "us-west-2", true);
    }

    @Test
    public void testCreateBucket_globalEndpointWithRegionNotSpecified() {
        doTestCreateBucket(s3, "US", false);
    }

    @Test
    public void testCreateBucket_euEndpointWithRegionNotSpecified() {
        doTestCreateBucket(euS3, "eu-west-1", false);
    }

    @Test(expected = AmazonS3Exception.class)
    public void testCreateBucket_euEndpointWithNonEuRegionSpecified() {
        String expectedErrorCode = "IllegalLocationConstraintException";
        try {
            doTestCreateBucket(euS3, "us-west-1", true);
        } catch (AmazonS3Exception e) {
            assertEquals(expectedErrorCode, e.getErrorCode());
            throw e;
        }
        fail(String.format("Failed test: %s expected!", expectedErrorCode));
    }

    private void doTestCreateBucket(AmazonS3Client s3Client, String bucketRegion, boolean specifyRegion) {
        String bucketName = CREATE_BUCKET_TEST_BUCKET_PREFIX + System.currentTimeMillis();
        if (specifyRegion) {
            s3Client.createBucket(bucketName, bucketRegion);
        } else {
            s3Client.createBucket(bucketName);
        }
        assertEquals(s3Client.getBucketLocation(bucketName), bucketRegion);
    }
}
