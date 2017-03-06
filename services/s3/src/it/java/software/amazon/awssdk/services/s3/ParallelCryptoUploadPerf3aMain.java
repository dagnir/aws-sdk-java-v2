package software.amazon.awssdk.services.s3;

import static software.amazon.awssdk.test.util.DateUtils.yyMMdd_hhmmss;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.UploadObjectRequest;

public class ParallelCryptoUploadPerf3aMain {
    private static final boolean cleanup = false;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(ParallelCryptoUploadPerf3aMain.class);
    private static AmazonS3Client s3;

//    private static final long OBJECT_SIZE = 4L << 30;   // 4G
//    private static final int PART_SIZE = 250 << 20;   // 250M
//    private static final int ITERATION = 40;

    private static final long OBJECT_SIZE = 10L << 20;   // 10M
    private static final int PART_SIZE = 5 << 20;   // 5M
    private static final int ITERATION = 1;

    public static void main(String[] args) throws Exception {
        beforeClass();
        final File file = CryptoTestUtils.generateRandomAsciiFile(OBJECT_SIZE);
        final ParallelCryptoUploadPerf3aMain test = new ParallelCryptoUploadPerf3aMain();
        for (int i=0; i < ITERATION; i ++)
            test.pipelinedUploadLargeObject(file);
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

    void pipelinedUploadLargeObject(final File fileSrc) throws IOException, InterruptedException, ExecutionException {
        final long start = System.nanoTime();
        // Initiate upload
        final String key = "pipelinedUploadLargeObject." + yyMMdd_hhmmss();
        s3.uploadObject(new UploadObjectRequest(TEST_BUCKET, key, fileSrc).withPartSize(PART_SIZE));
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
    }
    
    private static void p(Object o) {
        System.err.println(String.valueOf(o));
    }
}
