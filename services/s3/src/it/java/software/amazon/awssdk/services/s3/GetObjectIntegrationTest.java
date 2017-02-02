package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.ResponseHeaderOverrides;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectInputStream;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.test.util.SdkAsserts;
import software.amazon.awssdk.util.StringInputStream;

/**
 * Integration tests for the advanced options to the getObject operations (i.e.
 * ranges and constraints).
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class GetObjectIntegrationTest extends S3IntegrationTestBase {

//    private static final Random RANDOM = new Random();
    private static final boolean ANDROID_TESTING = false;

    /** The bucket created and used by these tests */
    private static final String bucketName = "java-get-object-integ-test-" + new Date().getTime();

    /** The key used in these tests */
    private static final String key = "key";

    /** A date earlier than the uploaded object's last modified date */
    private static Date earlierDate;

    /** A date after the uploaded object's last modified date */
    private static Date laterDate;

    /** The ETag of the uploaded object being retrieved */
    private static String etag;

    /** The file containing the test data uploaded to S3 */
    private static File file;

    /** The file size of the file containing the test data uploaded to S3*/
    private static long fileSize = 100000L;

    /** The inputStream containing the test data uploaded to S3 */
    private static byte[] tempData;

    private static final long sleepTimeInMillis = 3000;

    @AfterClass
    public static void tearDown() throws Exception {
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);

        if ( file != null ) {
            file.delete();
        }
    }

    /**
     * Creates and initializes all the test resources needed for these tests.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        S3IntegrationTestBase.setUp();

        if ( !ANDROID_TESTING ) {
            setUpCredentials();
        }

        tempData = tempDataBuffer((int)fileSize);

        AmazonS3 s3 = new AmazonS3Client(credentials);
        s3.createBucket(bucketName);

        ObjectMetadata metadata = null;
        if ( !ANDROID_TESTING ) {
            file = new RandomTempFile("get-object-integ-test", fileSize);
            file.deleteOnExit();
            s3.putObject(bucketName, key, file);
        } else {
            file = getRandomTempFile("foo", fileSize);
            file.deleteOnExit();
            ByteArrayInputStream bais = new ByteArrayInputStream(tempData);

            metadata = new ObjectMetadata();
            metadata.setContentLength(fileSize);

            s3.putObject(new PutObjectRequest(bucketName, key, bais, metadata));
            bais.close();
        }

        metadata = s3.getObjectMetadata(bucketName, key);
        etag = metadata.getETag();

        Date lastModified = metadata.getLastModified();
        earlierDate = new Date(lastModified.getTime() - 1000);
        laterDate = new Date(lastModified.getTime() + 1000);

        // Sleep for a few seconds to make sure the test doesn't run before
        // the future date
        Thread.sleep(1000 * 2);
    }

    /**
     * This test creates an S3 Object, reads the object's content
     * and then calls the close method multiple times.
     */
    @Test
    public void testCloseS3Object(){

        S3Object object=s3.getObject(bucketName, key);

        try {
            drainStream(object.getObjectContent());
            object.close();
            object.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test when not reading through s3object, the close() method should abort
     * HttpConnection and will not read any more data from the wrapped InputStream.
     * Use timeout to test whether a big object is drained or not.
     */
    @Test
    public void testCloseS3Object_abort() throws IOException{
        String keyName = "a-big-object";
        long objectSize = 100000000;	// 100M
        File objectFile = null;

        long abortTimeCriteria = 10L;	// 10ms time criteria for the abort time, this should be much smaller than both the upload and download times.
        objectFile = new RandomTempFile(keyName, objectSize);
        objectFile.deleteOnExit();

        // time uploading this 100M file
        long uploadTime = getS3ObjectUploadTime(bucketName, keyName, objectFile);
        // upload time should be much greater than the abort criteria time
        Assert.assertTrue(uploadTime > abortTimeCriteria);

        // time downloading this 100M file
        long downloadTime = getS3ObjectDownloadTime(bucketName, keyName);
        // download time should be much greater than the abort criteria time
        Assert.assertTrue(downloadTime > abortTimeCriteria);

        S3Object object = s3.getObject(bucketName, keyName);
        // time to abort a partially read S3ObjectInputStream.
        long abortTime = getS3ObjectAbortTime(object, objectSize);
        // The underlying httpRequest should be aborted.
        Assert.assertTrue(object.getObjectContent().getHttpRequest().isAborted());
        // This is to test close a partially read S3ObjectInputStream will not drain the stream
        Assert.assertTrue(abortTime < abortTimeCriteria);
    }

    /**
     * Time to upload objectFile to S3 bucket.
     */
    private long getS3ObjectUploadTime(String bucketName, String keyName, File objectFile) {
        long uploadStartTime = System.currentTimeMillis();
        s3.putObject(new PutObjectRequest(bucketName, keyName, objectFile));
        return System.currentTimeMillis() - uploadStartTime;
    }

    /**
     * Time to completely download a given S3 object.
     */
    private long getS3ObjectDownloadTime(String bucketName, String keyName) throws IOException {
        S3Object object = s3.getObject(bucketName, keyName);
        S3ObjectInputStream s3ois = object.getObjectContent();

        long downloadStartTime = System.currentTimeMillis();
        drainStream(s3ois);
        object.close();
        return System.currentTimeMillis() - downloadStartTime;
    }

    /**
     * Time to close a partially read S3ObjectInputStream. The S3Object object must not be read
     *
     * @param object - Target S3Object to be read. This object must not be read at all.
     * @param objectSize - Target S3 object size. This should be much bigger than the data we partially read.
     * @return The close time.
     */
    private long getS3ObjectAbortTime(S3Object object, long objectSize) throws IOException {
        S3ObjectInputStream s3ois = object.getObjectContent();
        int length = 24;
        Assert.assertTrue(length < objectSize);
        byte[] content = new byte[length];
        // read a very small part of the whole file
        s3ois.read(content);

        long abortStartTime = System.currentTimeMillis();
        object.close();
        return System.currentTimeMillis() - abortStartTime;
    }

    /**
     * Test when reading through s3object, the close() method should return
     * HttpConnection back to connection pool and the wrapped InputStream is
     * normally closed.
     */
    @Test
    public void testCloseS3Object_close() throws IOException {
        S3Object object = s3.getObject(bucketName, key);
        S3ObjectInputStream s3ois = object.getObjectContent();
        drainStream(object.getObjectContent());
        object.close();
        Assert.assertFalse(s3ois.getHttpRequest().isAborted());
    }

    /**
     * Tests that the range argument to getObject is correctly sent in the
     * request when present and correctly returns a range of data.
     */
    @Test
    public void testRange() throws Exception {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key)
            .withRange(50, 100));

        assertEquals(51L, drainStream(object.getObjectContent()));
    }

    /**
     * Tests that the start range argument to getObject is correctly sent in the
     * request when present and correctly returns the rest of the object.
     */
    @Test
    public void testStartRange() throws IOException {
        long start = 50000L;
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key)
                .withRange(start));

        assertEquals(fileSize - start, drainStream(object.getObjectContent()));
    }

    /**
     * Tests that the non-matching ETag constraint is correctly sent when it is
     * specified in a request.
     */
    @Test
    public void testNonMatchingETagConstraint() {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key)
            .withNonmatchingETagConstraint("non-matching-etag")
            .withNonmatchingETagConstraint("another-non-matching-etag"));

        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String( tempData ), object.getObjectContent());
        }

        assertNull(s3.getObject(new GetObjectRequest(bucketName, key)
            .withNonmatchingETagConstraint(etag)));
    }

    /**
     * Tests setting response headers.
     */
    @Test
    public void testResponseHeaders() {
        String override = "OVERRIDE";

        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key).withResponseHeaders(new ResponseHeaderOverrides()
                .withCacheControl(override)));

        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertEquals(override, object.getObjectMetadata().getCacheControl());

        object = s3.getObject(new GetObjectRequest(bucketName, key).withResponseHeaders(new ResponseHeaderOverrides()
                .withContentDisposition(override)));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream( new String( tempData ), object.getObjectContent() );
        }
        assertEquals(override, object.getObjectMetadata().getContentDisposition());

        object = s3.getObject(new GetObjectRequest(bucketName, key).withResponseHeaders(new ResponseHeaderOverrides()
                .withContentEncoding(override)));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertEquals(override, object.getObjectMetadata().getContentEncoding());

        object = s3.getObject(new GetObjectRequest(bucketName, key).withResponseHeaders(new ResponseHeaderOverrides()
                .withContentLanguage(override)));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertEquals(override,
                object.getObjectMetadata().getRawMetadata().get("Content-Language"));

        object = s3.getObject(new GetObjectRequest(bucketName, key).withResponseHeaders(new ResponseHeaderOverrides()
                .withContentType(override)));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertEquals(override, object.getObjectMetadata().getContentType());

        object = s3.getObject(new GetObjectRequest(bucketName, key).withResponseHeaders(new ResponseHeaderOverrides()
                .withExpires("Sat, 01 Jan 2000 00:00:00 GMT")));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertEquals(new Date(946684800000L), object.getObjectMetadata().getHttpExpiresDate());

        object = s3.getObject(new GetObjectRequest(bucketName, key).withResponseHeaders(new ResponseHeaderOverrides()
                .withCacheControl(override).withContentDisposition(override).withContentEncoding(override)
                .withContentLanguage(override).withContentType(override).withExpires("Sat, 01 Jan 2000 00:00:00 GMT")));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertEquals(override, object.getObjectMetadata().getCacheControl());
        assertEquals(override, object.getObjectMetadata().getContentDisposition());
        assertEquals(override, object.getObjectMetadata().getContentEncoding());
        assertEquals(override, object.getObjectMetadata().getRawMetadata().get("Content-Language"));
        assertEquals(override, object.getObjectMetadata().getContentType());
        assertEquals(new Date(946684800000L), object.getObjectMetadata().getHttpExpiresDate());
    }


    /**
     * Tests that the matching ETag constraint is correctly sent when it is
     * specified in a request.
     */
    @Test
    public void testMatchingETagConstraint() {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key)
            .withMatchingETagConstraint(etag)
            .withMatchingETagConstraint("one-that-doesn't-match"));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertNull(object.getObjectMetadata().getVersionId());

        assertNull(s3.getObject(new GetObjectRequest(bucketName, key)
            .withMatchingETagConstraint("another-non-matching-etag")));
    }

    /**
     * Tests that the modified since constraint is correctly sent when it is
     * specified in a request.
     */
    @Test
    public void testModifiedSinceConstraint() {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key)
            .withModifiedSinceConstraint(earlierDate));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertNull(object.getObjectMetadata().getVersionId());

        assertNull(s3.getObject(new GetObjectRequest(bucketName, key)
            .withModifiedSinceConstraint(laterDate)));
    }

    /**
     * Tests that the unmodified since constraint is correctly sent when it is
     * specified in a request.
     */
    @Test
    public void testUnmodifiedSinceConstraint() {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key)
            .withUnmodifiedSinceConstraint(laterDate));
        if ( androidRootDir == null ) {
            SdkAsserts.assertFileEqualsStream(file, object.getObjectContent());
        }
        else {
            SdkAsserts.assertStringEqualsStream(new String(tempData), object.getObjectContent());
        }
        assertNull(object.getObjectMetadata().getVersionId());

        assertNull(s3.getObject(new GetObjectRequest(bucketName, key)
            .withUnmodifiedSinceConstraint(earlierDate)));
    }

    /**
     * Tests that we can download an Amazon S3 object directly to a file.
     */
    @Test
    public void testGetObjectAsFile() throws Exception {
        if ( androidRootDir == null ) {
            File tempFile = File.createTempFile("aws-java-sdk-integ-test", ".dat");
            tempFile.deleteOnExit();
            ObjectMetadata objectMetadata = s3.getObject(new GetObjectRequest(bucketName, key), tempFile);
            assertNotNull(objectMetadata.getLastModified());
            assertNotNull(objectMetadata.getContentType());

            assertTrue(tempFile.exists());
            SdkAsserts.assertFileEqualsStream(tempFile, new FileInputStream(file));
        } else {
            S3Object objectData = s3.getObject(new GetObjectRequest(bucketName, key));
            ObjectMetadata objectMetadata = objectData.getObjectMetadata();
            assertNotNull(objectMetadata.getLastModified());

            SdkAsserts.assertStringEqualsStream(new String(tempData), objectData.getObjectContent());
        }
    }

    /**
     * Tests that we can download a directory-keyed object to a file.
     */
    @Test
    public void testGetDirectoryObjectAsFile() throws Exception {
        File data = new RandomTempFile("get-object-integ-test-directory", 1000L);

        String directoryKey = "a/b/c/d.dat";
        s3.putObject(bucketName, directoryKey, data);

        File tempFile = File.createTempFile("aws-java-sdk-integ-test", ".dat");
//        File tempFile = new File("C:\\tmp\\newfile.dat");
        tempFile.deleteOnExit();
        ObjectMetadata objectMetadata = s3.getObject(new GetObjectRequest(bucketName, directoryKey), tempFile);
        assertNull(objectMetadata.getSSEAlgorithm());
        assertNotNull(objectMetadata.getLastModified());
        assertNotNull(objectMetadata.getContentType());

        assertTrue(tempFile.exists());
        SdkAsserts.assertFileEqualsStream(tempFile, new FileInputStream(data));
    }

    @Test
    public void testServerSideEncryption() {
        String sseKey = "sseKey";
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, sseKey, file)
                .withMetadata(new ObjectMetadata());
        putObjectRequest.getMetadata().setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        PutObjectResult putObject = s3.putObject(putObjectRequest);
        assertEquals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION, putObject.getSSEAlgorithm());
    }

    @Test
    public void testServerSideEncryptionBadAlgorithm() {
        String sseKey = "sseKey";
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, sseKey, file)
                    .withMetadata(new ObjectMetadata());
            putObjectRequest.getMetadata().setSSEAlgorithm("BAD");
            s3.putObject(putObjectRequest);
            fail("Expected exception");
        } catch ( AmazonServiceException expected ) {
        }
    }

    /**
     * Tests getting an object with a bucket name containing periods in a non-us
     * endpoint.
     */
    @Test
    public void testNonUsRegionBucketNamesWithPeriods() {
        AmazonS3 regionalClient = new AmazonS3Client(credentials);
        regionalClient.setEndpoint("s3-sa-east-1.amazonaws.com");
        String regionalBucketName = bucketName + ".with.periods.regional";
        try {
            regionalClient.createBucket(regionalBucketName);
            regionalClient.putObject(regionalBucketName, key, file);

            S3Object objectData = regionalClient.getObject(new GetObjectRequest(regionalBucketName, key));
            ObjectMetadata objectMetadata = objectData.getObjectMetadata();
            assertNotNull(objectMetadata.getLastModified());

            SdkAsserts.assertFileEqualsStream(file, objectData.getObjectContent());
        } finally {
            CryptoTestUtils.deleteBucketAndAllContents(regionalClient, regionalBucketName);
        }
    }

    /**
     * Smoke test of aborting a getObject request.
     */
    @Test
    public void testAbortConnection() throws Exception {
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
        byte[] content = new byte[1024];
        S3ObjectInputStream objectContent = object.getObjectContent();
        objectContent.read(content);
        objectContent.abort();

        objectContent.close();
    }

    @Test
    public void objectInStandardStorageClass_ReturnsNullForStorageClass() {
        S3Object object = s3.getObject(bucketName, key);
        assertNull(object.getObjectMetadata().getStorageClass());
    }

    @Test
    public void objectInReducedRedundancyStorageClass_ReturnsCorrectStorageClass()
            throws AmazonServiceException, AmazonClientException, UnsupportedEncodingException {
        final String storageClassObjectKey = "object-with-reduced-redundancy";
        s3.putObject(new PutObjectRequest(bucketName, storageClassObjectKey, new StringInputStream("hi"), null)
                .withStorageClass(StorageClass.ReducedRedundancy));
        S3Object object = s3.getObject(bucketName, storageClassObjectKey);
        assertEquals(StorageClass.ReducedRedundancy.toString(), object.getObjectMetadata().getStorageClass());
    }

    /**
     * Drains the specified stream, closes it and returns the number of bytes in the stream.
     *
     * @param inputStream
     *            The stream to drain.
     * @return The number of bytes of data contained in the stream.
     * @throws IOException
     *             If any problems were encountered reading from the stream.
     */
    private long drainStream(InputStream inputStream) throws IOException {
        long count = 0;
        for (; inputStream.read() != -1; count++);
        inputStream.close();

        return count;
    }

    /**
     * This test case checks if the read from input stream given by S3 doesn't
     * throw an Socket Exception after garbage collection is called. The read
     * operation is done after calling gc explicitly. This test case is written
     * as part of the forum issue
     * https://forums.aws.amazon.com/thread.jspa?messageID=438171#
     *
     * @throws Exception
     */
    @Test
    public void testGetObjectWithClientSetToNullBeforeReadingFromInputStream()
            throws Exception {

        AmazonS3 s3Local = new AmazonS3Client(credentials);

        S3Object obj = s3Local.getObject(bucketName, key);

        s3Local = null;

        doGarbageCollection();

        drainStream(obj.getObjectContent());
        assertNotNull(obj.getObjectContent());
    }

    /**
     * This method calls the garbage collector explicitly to perform garbage
     * collection.
     */
    private void doGarbageCollection() {

        try {
            System.gc();
            Thread.sleep(sleepTimeInMillis);
            System.gc();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test case failed . Exception received is : " + e.getMessage());
        }

    }
}
