package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Date;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.internal.Mimetypes;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.test.util.UnreliableRandomInputStream;

/**
 * Integration tests for uploading objects to Amazon S3 through caller provided
 * InputStreams.
 * 
 * @author Jason Fulghum <fulghum@amazon.com>
 */
@Category(S3Categories.Slow.class)
public class PutBufferedStreamCNIntegrationTest extends S3IntegrationTestBase {
    
    private String bucketName = "put-stream-object-integ-test-" + new Date().getTime();
    private String key = "key";

    /** Releases all resources created by this test */
    @After
    public void tearDown() {
        // Clear the system property by setting to blank
        System.clearProperty(SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE);
        try {cnS3.deleteObject(bucketName, key);} catch (Exception e) {}
        try {cnS3.deleteBucket(bucketName);} catch (Exception e) {}
    }

    /**
     * Tests uploading an object from a stream, without a pre-computed MD5
     * digest for its content and without a content type set. The client will
     * calculate the MD5 digest on the fly, verify it with the ETag header in
     * the response from Amazon S3, and set the content type to the default,
     * application/octet-stream.
     */
    @Test
    public void testNoContentMd5Specified_BufferedInputStream() {
        cnS3.createBucket(bucketName);
        long contentLength = 10L * 1024L * 1024L;
        System.setProperty(
                SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE,
                String.valueOf(contentLength+1));
        InputStream input = new BufferedInputStream(new RandomInputStream(contentLength));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        cnS3.putObject(bucketName, key, input, metadata);
        
        metadata = cnS3.getObjectMetadata(bucketName, key);
        assertEquals(contentLength, metadata.getContentLength());
        assertEquals(Mimetypes.MIMETYPE_OCTET_STREAM, metadata.getContentType());
    }

    /**
     * Tests that when an InputStream uploading data to S3 encounters an
     * IOException, the client can correctly retry the request without the
     * caller's intervention, providing the error was encountered before the
     * client's retryable stream buffer was completely filled.
     */
    @Test
    public void testRequestRetry_BufferedInputStream() {
        cnS3.createBucket(bucketName);
        long contentLength = Constants.DEFAULT_STREAM_BUFFER_SIZE;
        System.setProperty(
                SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE,
                String.valueOf(contentLength+1));
        InputStream unreliableInputStream = 
                new BufferedInputStream(new UnreliableRandomInputStream(contentLength));

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        cnS3.putObject(bucketName, key, unreliableInputStream, metadata);
        
        ObjectMetadata returnedMetadata = cnS3.getObjectMetadata(bucketName, key);
        assertEquals(contentLength, returnedMetadata.getContentLength());
    }

    @Test(expected=ResetException.class)
    public void testResetFailure() {
        cnS3.createBucket(bucketName);
        // content length bigger than the default reset buffer size
        long contentLength = Constants.DEFAULT_STREAM_BUFFER_SIZE*2;
        InputStream unreliableInputStream = 
                new BufferedInputStream(new UnreliableRandomInputStream(contentLength));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        cnS3.putObject(bucketName, key, unreliableInputStream, metadata);
    }
}
