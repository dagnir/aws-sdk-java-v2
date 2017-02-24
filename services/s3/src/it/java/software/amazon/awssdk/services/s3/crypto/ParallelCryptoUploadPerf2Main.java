package software.amazon.awssdk.services.s3.crypto;

import static software.amazon.awssdk.test.util.DateUtils.yyMMdd_hhmmss;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.UploadObjectObserver;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.UploadObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;

public class ParallelCryptoUploadPerf2Main {
    private static final boolean cleanup = false;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(ParallelCryptoUploadPerf2Main.class);
    private static AmazonS3EncryptionClient eo;
    private static AmazonS3Client s3;
    private static final long OBJECT_SIZE = 25 << 20;   // 100M
    private static final long PART_SIZE = 5 << 20;  // 5M
    private static final int ITERATION = 1;
    private static final long DISK_LIMIT = 25000000;  // ~50M
//    private static final long DISK_LIMIT = PART_SIZE;
//    private static final int CONN_POOL_SIZE = 400;

    public static void main(String[] args) throws Exception {
        beforeClass();
        final File file = CryptoTestUtils.generateRandomAsciiFile(OBJECT_SIZE);
        final ParallelCryptoUploadPerf2Main test = new ParallelCryptoUploadPerf2Main();
        for (int i=0; i < ITERATION; i ++)
            test.pipelinedUploadLargeObject(file);
        afterClass();
    }
    
    static void beforeClass() throws IOException {
        s3 = new AmazonS3Client(CryptoTestUtils.awsTestCredentials());
//                new ClientConfiguration().withMaxConnections(CONN_POOL_SIZE));
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
        s3.shutdown();
    }

    void pipelinedUploadLargeObject(final File fileSrc) throws IOException,
            InterruptedException, ExecutionException {
        final long start = System.nanoTime();
        // Initiate upload
        final String key = "pipelinedUploadLargeObject." + yyMMdd_hhmmss();
        eo.uploadObject(new UploadObjectRequest(TEST_BUCKET, key, fileSrc)
            .withPartSize(PART_SIZE)
            .withDiskLimit(DISK_LIMIT)
            .withUploadObjectObserver(new UploadObjectObserver() {
                @Override
                protected UploadPartResult uploadPart(UploadPartRequest reqUploadPart) {
                  while (true) {
                      try {
                          return super.uploadPart(reqUploadPart);
                      } catch(Exception ex) {
                          System.err.println(ex);
                      }
                  }
                }
            })
        );
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
    }
    
    private static void p(Object o) {
        System.err.println(String.valueOf(o));
    }
}
