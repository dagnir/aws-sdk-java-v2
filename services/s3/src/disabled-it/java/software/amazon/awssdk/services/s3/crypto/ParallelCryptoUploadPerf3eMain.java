package software.amazon.awssdk.services.s3.crypto;

import static software.amazon.awssdk.test.util.DateUtils.yyMMdd_hhmmss;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;

/**
 * Serial multi-part uploads of encrypted objects using secret key.
 */
public class ParallelCryptoUploadPerf3eMain {
    private static final boolean cleanup = false;
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(ParallelCryptoUploadPerf3eMain.class);
    private static AmazonS3Client s3;

    private static final int ITERATION = 40;
    private static final long OBJECT_SIZE = 4L << 30;   // 4 G
    private static final long PART_SIZE = 1 << 29;  // 0.5G

//    private static final long OBJECT_SIZE = 10 << 20;   // 10M
//    private static final int PART_SIZE = 5 << 20;   // 5 M
//    private static final int ITERATION = 1;

    public static void main(String[] args) throws Exception {
        beforeClass();
        final File file = CryptoTestUtils.generateRandomAsciiFile(OBJECT_SIZE);
        final ParallelCryptoUploadPerf3eMain test = new ParallelCryptoUploadPerf3eMain();
        for (int i=0; i < ITERATION; i ++)
            test.serialUploadLargeObject(file);
        afterClass();
    }
    
    static void beforeClass() throws IOException {
        s3 = new AmazonS3EncryptionClient(CryptoTestUtils.awsTestCredentials(),
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()),
                new CryptoConfiguration());
        s3.createBucket(TEST_BUCKET);
    }
    
    static void afterClass() {
        if (cleanup)
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
        s3.shutdown();
    }

    void serialUploadLargeObject(File fileSrc) throws IOException, InterruptedException, ExecutionException {
        final long start = System.nanoTime();
        // Initiate upload
        final String key = "serialUploadEncrptedObject-" + yyMMdd_hhmmss();
        InitiateMultipartUploadRequest reqInit = new InitiateMultipartUploadRequest(TEST_BUCKET, key);
        InitiateMultipartUploadResult resInit = s3.initiateMultipartUpload(reqInit);
        final String uploadId = resInit.getUploadId();

        // Upload parts in parallel
        final List<PartETag> partETags = new ArrayList<PartETag>();
        final int numParts = (int)Math.ceil(1.0*fileSrc.length() / PART_SIZE);
        long totalBytesUploaded = 0;
        for (int partNumber=1; partNumber <= numParts; partNumber++) {
            final boolean isLastPart = partNumber == numParts;
            final int partSize = isLastPart ? (int)(fileSrc.length() - totalBytesUploaded) : (int)PART_SIZE;
            final UploadPartRequest reqUploadPart = new UploadPartRequest()
                .withBucketName(TEST_BUCKET)
                .withFile(fileSrc)
                .withFileOffset(totalBytesUploaded)
                .withKey(key)
                .withPartNumber(partNumber)
                .withPartSize(partSize)
                .withLastPart(isLastPart)
                .withUploadId(uploadId)
                ;
//            p("partNumber=" + partNumber + ", partSize=" + partSize + ", totalBytesUploaded=" + totalBytesUploaded +", isLastPart=" + isLastPart);
            while (true) {
                try {
//                    final long start = System.nanoTime();
                    UploadPartResult partResult = s3.uploadPart(reqUploadPart);
//                    final long end = System.nanoTime();
//                    final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
//                    p(elapsed + " ms");
                    totalBytesUploaded += partSize;
                    partETags.add(new PartETag(partResult.getPartNumber(), partResult.getETag()));
                    break;
                } catch(Exception ex) {
                    p(ex);
                }
            } 
        }
        // Complete upload
        CompleteMultipartUploadRequest reqComplete = new CompleteMultipartUploadRequest(
                TEST_BUCKET, key, uploadId, partETags);
        s3.completeMultipartUpload(reqComplete);
        final long end = System.nanoTime();
        final long elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
        p(elapsed + " ms");
//        S3Object s3object = eo.getObject(new GetObjectRequest(TEST_BUCKET, key));
//        IOUtils.copy(s3object.getObjectContent(), new FileOutputStream(new File("/tmp/serialUploadLargeObject.download")));
    }
    
    private static void p(Object o) {
        System.err.println(String.valueOf(o));
    }
}
