import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.util.UnreliableBufferedInputStream;

public class TT0035681099_BufferedInputStreamIntegrationTest {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName("TT0035681099-BufferedInputStreamIntegrationTest");
    private static AmazonS3Client s3;
    private static File dataFile;
    private static final int _10K = 1024*8;
    public static AWSCredentials awsTestCredentials() throws IOException {
        return new PropertiesCredentials(new File(
                System.getProperty("user.home")
                        + "/.aws/awsTestAccount.properties"));
    }
    @BeforeClass
    public static void setup() throws Exception {
        s3 = new AmazonS3Client(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
        dataFile = CryptoTestUtils.generateRandomAsciiFile(_10K); // 10K
    }

    @AfterClass
    public static void tearDown() {
        if (cleanup)
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @Test
    public void testNonRetryableFailure() throws Exception {
        final String key = "testOuterRetry";
        UnreliableBufferedInputStream is = new UnreliableBufferedInputStream(new FileInputStream(dataFile));
        for (int j = 0; j < 2; j++) {
            try {
                s3.putObject(new PutObjectRequest(TEST_BUCKET, key,
                        is, null));
                Assert.fail();
            } catch (RuntimeException expected) {
                // First exception is a simulated IO exception.
                // 2nd exception is due to the BufferedInputStream being closed.
                expected.printStackTrace();
            }
        }
        is.close();
        Assert.assertTrue(
                "expect: 1 but got actual: " + is.getCurrNumberOfErrors(),
                1 == is.getCurrNumberOfErrors());
    }

    // Test inner retry once 
    @Test
    public void testFakeIOException1() throws Exception {
        doTestFakeIOException(1);
    }
    
    // Test inner retry twice
    @Test
    public void testFakeIOException2() throws Exception {
        doTestFakeIOException(2);
    }

    // Test inner retry 3 times
    @Test
    public void testFakeIOException3() throws Exception {
        doTestFakeIOException(3);
    }
    
    private void doTestFakeIOException(int retries) throws Exception {
        final boolean FAKE_IO_EXCEPTION = true;
        final String key = "doTestFakeIOException.retries-" + retries;
        UnreliableBufferedInputStream is = 
                new UnreliableBufferedInputStream(new FileInputStream(dataFile), FAKE_IO_EXCEPTION)
                .withNumberOfErrors(retries)
                ;
        PutObjectResult result = s3.putObject(new PutObjectRequest(TEST_BUCKET, key,
                is, null));
        is.close();
        Assert.assertTrue(
                "expect: " + retries + " but got actual: "
                        + is.getCurrNumberOfErrors(),
                retries == is.getCurrNumberOfErrors());
        System.out.println(result);
        ObjectMetadata om2 = s3.getObjectMetadata(TEST_BUCKET, key);
        final long length = om2.getContentLength();
        System.out.println(length);
        // This assertion would fail (with a zero byte file saved in S3)
        // if not for https://cr.amazon.com/r/2838249
        Assert.assertTrue("expect: " + _10K + " but got actual: " + length,
                length == _10K);
    }
}
