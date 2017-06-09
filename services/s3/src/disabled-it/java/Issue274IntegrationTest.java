import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.http.TT0036173414IntegrationTest;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

//https://github.com/aws/aws-sdk-java/issues/274
    
public class Issue274IntegrationTest {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = 
            CryptoTestUtils.tempBucketName(TT0036173414IntegrationTest.class);
        private static final String TEST_KEY = "testkey";
    private static AmazonS3Client s3;

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
    public void testInvalidHttpExpiresDate() throws Exception {
        ObjectMetadata omdput = new ObjectMetadata();
        omdput.setHeader("Expires", "Wed Sep 25 10:11:55 BRT 2024");
        PutObjectRequest putreq = new PutObjectRequest(TEST_BUCKET, TEST_KEY,
                new ByteArrayInputStream("Testing".getBytes()), omdput);
        s3.putObject(putreq);
        ObjectMetadata omd = s3.getObjectMetadata(TEST_BUCKET, TEST_KEY);
        Assert.assertNull(omd.getHttpExpiresDate());
    }

    @Test
    public void testValidHttpExpiresDate() throws Exception {
        ObjectMetadata omdput = new ObjectMetadata();
        omdput.setHttpExpiresDate(new Date());
        PutObjectRequest putreq = new PutObjectRequest(TEST_BUCKET, TEST_KEY,
                new ByteArrayInputStream("Testing".getBytes()), omdput);
        s3.putObject(putreq);
        ObjectMetadata omd = s3.getObjectMetadata(TEST_BUCKET, TEST_KEY);
        Assert.assertNotNull(omd.getHttpExpiresDate());
    }

    public static AWSCredentials awsTestCredentials() throws IOException {
        return new PropertiesCredentials(new File(
                System.getProperty("user.home")
                        + "/.aws/awsTestAccount.properties"));
    }
}
