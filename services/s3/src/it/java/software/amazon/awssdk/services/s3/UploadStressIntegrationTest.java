package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Date;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.test.util.RandomInputStream;

/**
 * Integration test for uploading large objects. Helpful for testing that HTTP
 * client timeouts won't cause problems when uploading large objects, and for
 * verifying that data isn't being buffered inside the client before it's sent
 * to Amazon S3.
 * <p>
 * Be sure that HTTPClient wire level debugging is <b>not</b> enabled, otherwise
 * this test can take a very long time to run (1hr+).
 * 
 * @author Jason Fulghum <fulghum@amazon.com>
 */
@Category(S3Categories.ReallySlow.class)
public class UploadStressIntegrationTest extends S3IntegrationTestBase {

    private final String bucketName = "upload-stress-integ-test-" + new Date().getTime();
    private final String key = "key";
    
    /** Releases all resources created by this test */
    @After
    public void tearDown() {
        try {s3.deleteObject(bucketName, key);} catch (Exception e) {}
        try {s3.deleteBucket(bucketName);} catch (Exception e) {}
    }

    /**
     * Tests that the client can handle uploading and downloading large objects
     * to/from Amazon S3 (without encountering HTTP client timeouts, etc.) and
     * also prints out some very rudimentary performance
     * <p>
     * In order for a box's networking stack to honor higher socket buffer
     * sizes, you might need to adjust the maximum TCP buffer limits through
     * your OS.
     * <p>
     * For example, on an Amazon MacBook, the default socket size is currently
     * set to 64K, with a maximum limit of 4MB, and a TCP window scaling factor
     * of 3.
     * <p>
     * For higher performance, you can adjust the OS settings (particularly the
     * maximum buffer size). For example on a Mac you can bump the max socket
     * buffer size to 16MB and increase the window scaling factor like this:
     * 
     * <pre>
     *   sudo sysctl -w net.inet.tcp.win_scale_factor=8
     *   sudo sysctl -w kern.ipc.maxsockbuf=16777216
     * </pre>
     */
    @SuppressWarnings("static-access")
	@Test
    public void testLargeStream() throws Exception {
    
        int socketBufferSize = 5 * 1024 * 1024;
        ClientConfiguration clientConfiguration = new ClientConfiguration()
            .withSocketBufferSizeHints(socketBufferSize, socketBufferSize);
        s3 = new AmazonS3Client(super.credentials, clientConfiguration); 
      
      
        s3.createBucket(bucketName);
        
        // Upload a 4GB input stream of random ASCII data
        long contentLength = 4L * 1024L * 1024L * 1024L;
        InputStream input = new RandomInputStream(contentLength); 
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        long uploadStartTime = new Date().getTime();
        s3.putObject(bucketName, key, input, metadata);
        long uploadEndTime = new Date().getTime();
        long uploadTime = uploadEndTime - uploadStartTime;
        double uploadThroughput = (double)contentLength / ((double)uploadTime / 1000.0);

        // Check the metadata
        metadata = s3.getObjectMetadata(bucketName, key);
        assertEquals(contentLength, metadata.getContentLength());

        // Download the object
        long downloadStartTime = new Date().getTime();
        InputStream inputStream = s3.getObject(bucketName, key).getObjectContent();
        byte[] buffer = new byte[1024 * 1024];
        int read;
        do { read = inputStream.read(buffer); } while (read > 0);
        long downloadEndTime = new Date().getTime();
        long downloadTime = downloadEndTime - downloadStartTime;
        double downloadThroughput = (double)contentLength / ((double)downloadTime / 1000.0);
        
        
        System.out.println("==================================");
        System.out.println("Throughput Summary");
        System.out.println("==================================");
        System.out.println("Uploaded " + contentLength + " bytes in " + uploadTime + "ms");
        System.out.println("Upload throughput: " + uploadThroughput + " B/s");
        System.out.println("Downloaded " + contentLength + " bytes in " + downloadTime + "ms");
        System.out.println("Download throughput: " + downloadThroughput + " B/s");
    }
    
}
