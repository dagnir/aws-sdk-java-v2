package software.amazon.awssdk.services.s3.crypto;

import static software.amazon.awssdk.test.util.DateUtils.yyMMdd_hhmmss;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ParallelCryptoUploadPerf1Main {
    private static final boolean cleanup = true;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(ParallelCryptoUploadPerf1Main.class);
    private static AmazonS3EncryptionClient eo;

    public static void main(String[] args) throws Exception {
        beforeClass();
        final File file = CryptoTestUtils.generateRandomAsciiFile(4L << 30);    // 4G
        final ParallelCryptoUploadPerf1Main test = new ParallelCryptoUploadPerf1Main();
        for (int i=0; i < 40; i ++)
            test.putLargeObject(file);
        afterClass();
    }
    
    static void beforeClass() throws IOException {
        eo = new AmazonS3EncryptionClient(CryptoTestUtils.awsTestCredentials(),
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()),
                new CryptoConfiguration()
                        .withStorageMode(CryptoStorageMode.ObjectMetadata));
        eo.createBucket(TEST_BUCKET);
    }
    
    static void afterClass() {
        if (cleanup)
            CryptoTestUtils.deleteBucketAndAllContents(eo, TEST_BUCKET);
        eo.shutdown();
    }

    void putLargeObject(File file) throws IOException {
        final long start = System.nanoTime();
        final String key = "putLargeObject." + yyMMdd_hhmmss();
        eo.putObject(new PutObjectRequest(TEST_BUCKET, key, file));
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
        eo.deleteObject(TEST_BUCKET, key);
        // putLargeObject 10M took 16215 ms
        // putLargeObject 100M took 356,349 ms
    }

    private static void p(Object o) {
        System.err.println(String.valueOf(o));
    }
}
