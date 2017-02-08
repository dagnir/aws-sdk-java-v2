package software.amazon.awssdk.services.s3;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.services.s3.transfer.Upload;
import software.amazon.awssdk.util.Md5Utils;

// https://issues.amazon.com/JAVA-1035
public class PutFileInputStreamIntegrationTest extends S3IntegrationTestBase {

    private static String bucketName = CryptoTestUtils.tempBucketName(PutFileInputStreamIntegrationTest.class);
    private static String key = "key";
    private static AmazonS3Client s3;
    private static File file;
    private static long contentLength;

    @BeforeClass
    public static void setup() throws Exception {
    	setUpCredentials();
        s3 = new AmazonS3TestClient(credentials,
            new ClientConfiguration().withSignerOverride("AWSS3V4SignerType"));
        CryptoTestUtils.tryCreateBucket(s3, bucketName);
        // make the content length 100 byte larger than the default mark-and-reset limit
        contentLength = new PutObjectRequest(bucketName, key, file).getReadLimit()+100;
        file = CryptoTestUtils.generateRandomAsciiFile(contentLength);
        Assert.assertTrue(contentLength > 0);
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
        s3.shutdown();
    }

    @Test
    public void testPutExceedDefaultResetSize() throws Exception {
        FileInputStream fis = new FileInputStream(file);
        ObjectMetadata meta = new ObjectMetadata();
        s3.putObject(new PutObjectRequest(bucketName, key, fis, meta));
        fis.close();
        byte[] actual = Md5Utils.computeMD5Hash(s3.getObject(bucketName, key).getObjectContent());
        byte[] expected = Md5Utils.computeMD5Hash(file);
        Assert.assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testUploadExceedDefaultResetSize() throws Exception {
        TransferManager tm = new TransferManager(s3);
        FileInputStream fis = new FileInputStream(file);
        ObjectMetadata meta = new ObjectMetadata();
        Upload upload = tm.upload(new PutObjectRequest(bucketName, key, fis, meta));
        upload.waitForCompletion();
        fis.close();
        tm.shutdownNow(false);
        byte[] actual = Md5Utils.computeMD5Hash(s3.getObject(bucketName, key).getObjectContent());
        byte[] expected = Md5Utils.computeMD5Hash(file);
        Assert.assertTrue(Arrays.equals(expected, actual));
    }
}
