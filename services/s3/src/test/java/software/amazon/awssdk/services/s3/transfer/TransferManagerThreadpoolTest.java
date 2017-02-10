package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3Client;

/**
 * Tests if the thread pool is shutdown properly as part of garbage collection.
 * Also tests if the thread pool shutdown is skipped when explicitly asked.
 */
public class TransferManagerThreadpoolTest {

    @Test(timeout = 60000)
    public void test() throws InterruptedException {
        final ThreadPoolExecutor threadPoolExecutor = createNewThreadPool();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                new TransferManager(new AmazonS3Client(), threadPoolExecutor,
                        false);
            }
        });

        t.start();
        t.join();

        assertFalse(threadPoolExecutor.isShutdown());

        t = new Thread(new Runnable() {
            @Override
            public void run() {
                new TransferManager(new AmazonS3Client(), threadPoolExecutor);
            }
        });

        t.start();
        t.join();

        for (;;) {
            System.err.println("triggering GC explicitly.");
            System.gc();
            if (threadPoolExecutor.isShutdown())
                return;
            Thread.sleep(5000);
        }

    }

    private ThreadPoolExecutor createNewThreadPool() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }

}
