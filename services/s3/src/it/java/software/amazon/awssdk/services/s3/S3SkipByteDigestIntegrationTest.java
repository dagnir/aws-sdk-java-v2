package software.amazon.awssdk.services.s3;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.deleteBucketAndAllContents;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.test.util.ConstantInputStream;

public class S3SkipByteDigestIntegrationTest {
    private static final boolean DEBUG = false;
    private static final Random rand = new Random();
    private static final int DATA_SIZE = 1*1024*1024 + rand.nextInt(16);
    private static boolean cleanup = true;
    private static final String TEST_BUCKET = tempBucketName(S3SkipByteDigestIntegrationTest.class);
    private static AmazonS3Client s3;

    @BeforeClass
    public static void setup() throws Exception {
        s3 = new AmazonS3Client(awsTestCredentials());
        tryCreateBucket(s3, TEST_BUCKET);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            deleteBucketAndAllContents(s3, TEST_BUCKET);
        }
        s3.shutdown();
    }

    // https://github.com/aws/aws-sdk-java/issues/232
    @Test
    public void testDigestWithSkippedBytes() throws IOException {
        if (DEBUG)
            debug("DATA_SIZE=" + DATA_SIZE);
        InputStream is = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        s3.putObject(TEST_BUCKET, "key", is, new ObjectMetadata());
        {   // skip some random number of bytes
            final S3Object object = s3.getObject(TEST_BUCKET, "key");
            final InputStream content = object.getObjectContent();
            int n = rand.nextInt(DATA_SIZE);
            if (DEBUG)
                debug("n=" + n);
            content.skip(n);
            IOUtils.toByteArray(content);
            content.close();
        }
        {   // skip 0 bytes;
            final S3Object object = s3.getObject(TEST_BUCKET, "key");
            final InputStream content = object.getObjectContent();
            int n = 0;
            if (DEBUG)
                debug("n=" + n);
            content.skip(n);
            IOUtils.toByteArray(content);
            content.close();
        }
        {   // skip -1 bytes;
            final S3Object object = s3.getObject(TEST_BUCKET, "key");
            final InputStream content = object.getObjectContent();
            int n = -1;
            if (DEBUG)
                debug("n=" + n);
            content.skip(n);
            IOUtils.toByteArray(content);
            content.close();
        }
        {   // skip all bytes
            final S3Object object = s3.getObject(TEST_BUCKET, "key");
            final InputStream content = object.getObjectContent();
            int n = DATA_SIZE;
            if (DEBUG)
                debug("n=" + n);
            content.skip(n);
            IOUtils.toByteArray(content);
            content.close();
        }
        {   // skip more than all bytes
            final S3Object object = s3.getObject(TEST_BUCKET, "key");
            final InputStream content = object.getObjectContent();
            int n = DATA_SIZE + 100;
            if (DEBUG)
                debug("n=" + n);
            content.skip(n);
            IOUtils.toByteArray(content);
            content.close();
        }
    }

    private void debug(Object o) {
        if (DEBUG)
            System.err.println(String.valueOf(o));
    }
}
