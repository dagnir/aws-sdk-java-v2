package software.amazon.awssdk.services.s3.transfer;

import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.test.util.RandomTempFile;

@Category(S3Categories.ReallySlow.class)
public class TransferManagerPerformanceTest extends S3IntegrationTestBase {
    
    private static final long SIZE = 101 * MB;
    private static final int  SAMPLES = 5;

    private String bucketName = "java-sdk-tx-man-" + System.currentTimeMillis();
    private TransferManager tm;
    
    @After
    public void tearDown() {
        try { 
            deleteBucketAndAllContents(bucketName);
        } catch (Exception e) {}
    }

    @Test
    public void testPerformance() throws Exception {
        tm = new TransferManager(s3);
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMinimumUploadPartSize(50*MB);
        configuration.setMultipartUploadThreshold(20*MB);
        tm.setConfiguration(configuration);

        s3.createBucket(bucketName);

        // Create test data...
        long startTime = time();
        System.out.println("Creating test data (" + time() + ")");
        RandomTempFile file = new RandomTempFile("file", SIZE);
        elapsed(startTime);

        
         // Test PutObject...
         startTime = time();
         for (int counter = 0; counter < SAMPLES; counter++) {
             System.out.println("Putting " + counter + " (" + time() + ")");
             s3.putObject(bucketName, "key", file);
         }
         elapsed(startTime);
         average(startTime, SAMPLES);
         throughput(startTime, (SAMPLES * SIZE));

         
        // Test Multipart...
        startTime = time();
        for (int counter = 0; counter < SAMPLES; counter++) {
            System.out.println("Uploading " + counter + " (" + time() + ")");
            tm.upload(bucketName, "key", file).waitForCompletion();
        }
        elapsed(startTime);
        average(startTime, SAMPLES);
        throughput(startTime, (SAMPLES * SIZE));

        file.delete();
    }

    private double throughput(long startTime, long totalSize) {
        double throughput = (double) totalSize / (double) (time() - startTime);
        System.out.println(String.format("Throughput: %.2f KB/s", throughput));
        return throughput;
    }

    private long average(long startTime, int count) {
        long average = (time() - startTime) / count;
        System.out.println("Average: " + average);
        return average;
    }

    private long elapsed(long startTime) {
        long elapsed = time() - startTime;
        System.out.println("Elapsed: " + elapsed);
        return elapsed;
    }

    private long time() {
        return System.currentTimeMillis();
    }

}
