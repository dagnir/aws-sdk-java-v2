import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
import software.amazon.awssdk.services.s3.util.UnreliableByteArrayInputStream;


public class TT0035681099IntegrationTest {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(TT0035681099IntegrationTest.class);
    private static AmazonS3Client s3;

    public static AWSCredentials awsTestCredentials() throws IOException {
        return new PropertiesCredentials(new File(
                System.getProperty("user.home")
                        + "/.aws/awsTestAccount.properties"));
    }
    @BeforeClass
    public static void setup() throws Exception {
        s3 = new AmazonS3Client(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
    }

    @AfterClass
    public static void tearDown() {
        if (cleanup)
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @Test
    public void testOuterRetry() throws Exception {
        final String key = "testOuterRetry";
        InputStream is = new UnreliableByteArrayInputStream(new byte[100]);
        PutObjectResult result = null;
        boolean retry = false;
        for (int j = 0; j < 2; j++) {
            try {
                result = s3.putObject(new PutObjectRequest(TEST_BUCKET, key,
                        is, null));
                break;
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            retry = true;
        }
        Assert.assertTrue(retry);
        System.out.println(result);
        ObjectMetadata om2 = s3.getObjectMetadata(TEST_BUCKET, key);
        final long length = om2.getContentLength();
        System.out.println(length);
        // This assertion would fail (with a zero byte file saved in S3)
        // if not for https://cr.amazon.com/r/2838249
        Assert.assertTrue("expect: 100 but got actual: " + length,
                length == 100);
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
        UnreliableByteArrayInputStream is = 
            new UnreliableByteArrayInputStream(new byte[100], FAKE_IO_EXCEPTION)
            .withNumberOfErrors(retries)
            ;
        PutObjectResult result = null;
        result = s3.putObject(new PutObjectRequest(TEST_BUCKET, key,
                is, null));
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
        Assert.assertTrue("expect: 100 but got actual: " + length,
                length == 100);
    }

    // Runtime exception would cause the request to fail with no retry
    @Test
    public void testFakeRuntimeException() throws Exception {
        final String key = "testFakeRuntimeException";
        // It would fail permanently, with no retry
        UnreliableByteArrayInputStream is = new UnreliableByteArrayInputStream(new byte[100]).withNumberOfErrors(2);
        try {
            s3.putObject(new PutObjectRequest(TEST_BUCKET, key, is, null));
            Assert.fail();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        // Despite we specified 2 errors, there is no retry so the count will only be 1
        Assert.assertTrue(
                "expect: 1 but got actual: " + is.getCurrNumberOfErrors(),
                1 == is.getCurrNumberOfErrors());
    }
}
