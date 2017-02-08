package software.amazon.awssdk.services.s3;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SSECustomerKey;
import software.amazon.awssdk.services.s3.transfer.Download;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.util.Md5Utils;

// Used to verify the fix for https://tt.amazon.com/0049303822
public class SSE_C_MD5IntegrationTest extends S3IntegrationTestBase {
    private static String bucketName = CryptoTestUtils.tempBucketName("SSE-C-MD5IntegrationTest");
    private static String key = "key";
    private static AmazonS3Client s3;
    private static File file;
    private static long contentLength = 100;

    @BeforeClass
    public static void setup() throws Exception {
    	setUpCredentials();
        s3 = new AmazonS3TestClient(credentials);
        CryptoTestUtils.tryCreateBucket(s3, bucketName);
        file = CryptoTestUtils.generateRandomAsciiFile(contentLength);
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
        s3.shutdown();
    }

    @Test
    public void test() throws Exception {
        SSECustomerKey sse_c = new SSECustomerKey(CryptoTestUtils.getTestSecretKey());
        s3.putObject(new PutObjectRequest(bucketName, key, file).withSSECustomerKey(sse_c));

        TransferManager tm = new TransferManager(s3);
        File dest = new File("/tmp", UUID.randomUUID().toString());
        Download download = tm.download(new GetObjectRequest(bucketName, key)
            .withSSECustomerKey(sse_c), dest);
        download.waitForCompletion();
        tm.shutdownNow(false);
        byte[] expected = Md5Utils.computeMD5Hash(file);
        byte[] actual = Md5Utils.computeMD5Hash(dest);
        Assert.assertTrue(Arrays.equals(expected, actual));
    }
}
