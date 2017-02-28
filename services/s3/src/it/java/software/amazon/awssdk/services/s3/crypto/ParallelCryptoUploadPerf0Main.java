package software.amazon.awssdk.services.s3.crypto;

import static software.amazon.awssdk.test.util.DateUtils.yyMMdd_hhmmss;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ParallelCryptoUploadPerf0Main {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(ParallelCryptoUploadPerf0Main.class);
    private static AmazonS3Client s3;

    public static void main(String[] args) throws Exception {
        beforeClass();
        final File file = CryptoTestUtils.generateRandomAsciiFile(4L << 30);    // 4G
        final ParallelCryptoUploadPerf0Main test = new ParallelCryptoUploadPerf0Main();
        for (int i=0; i < 40; i++) {
            try {
                test.putLargeObject(file);
            } catch(Exception ex) {
                p(ex);
                i--;
            }
        }
        afterClass();
    }
    
    static void beforeClass() throws IOException {
        s3 = new AmazonS3Client(CryptoTestUtils.awsTestCredentials());
        s3.createBucket(TEST_BUCKET);
    }
    
    static void afterClass() {
        if (cleanup)
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
        s3.shutdown();
    }

    void putLargeObject(File file) throws IOException {
        final long start = System.nanoTime();
        final String key = "putLargeObject." + yyMMdd_hhmmss();
        s3.putObject(new PutObjectRequest(TEST_BUCKET, key, file));
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
        s3.deleteObject(TEST_BUCKET, key);
        // putLargeObject 10M took 16215 ms
        // putLargeObject 100M took 356,349 ms
    }

    private static void p(Object o) {
        System.err.println(String.valueOf(o));
    }
}
