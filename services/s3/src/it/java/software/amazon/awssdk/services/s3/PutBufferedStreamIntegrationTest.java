package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Date;

import org.junit.After;
import org.junit.Test;

import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.internal.Mimetypes;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.test.util.UnreliableRandomInputStream;

public class PutBufferedStreamIntegrationTest extends S3IntegrationTestBase {
    
    private String bucketName = "put-stream-object-integ-test-" + new Date().getTime();
    private String key = "key";

    /** Releases all resources created by this test */
    @After
    public void tearDown() {
        try {s3.deleteObject(bucketName, key);} catch (Exception e) {}
        try {s3.deleteBucket(bucketName);} catch (Exception e) {}
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
        s3.createBucket(bucketName);
        
        long contentLength = 10L * 1024L * 1024L;
        InputStream input = new BufferedInputStream(new RandomInputStream(contentLength));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        s3.putObject(bucketName, key, input, metadata);
        
        metadata = s3.getObjectMetadata(bucketName, key);
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
        s3.createBucket(bucketName);
        
        long contentLength = Constants.DEFAULT_STREAM_BUFFER_SIZE;
        InputStream unreliableInputStream = 
            new BufferedInputStream(new UnreliableRandomInputStream(contentLength));

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        s3.putObject(bucketName, key, unreliableInputStream, metadata);
        
        ObjectMetadata returnedMetadata = s3.getObjectMetadata(bucketName, key);
        assertEquals(contentLength, returnedMetadata.getContentLength());
    }

}
