package software.amazon.awssdk.services.dynamodb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.utils.Logger;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncListTablesBugReproIntegrationTest {
    private static final Logger LOG = Logger.loggerFor(AsyncListTablesBugReproIntegrationTest.class);

    private static final int THREADS = 100;

    private DynamoDBAsyncClient ddb;

    private ExecutorService exec;

    private final AtomicLong[] counter = new AtomicLong[THREADS];

    @Before
    public void methodSetup() {
        ddb = DynamoDBAsyncClient.builder()
                .region(Region.US_WEST_2)
                .build();

        exec = Executors.newFixedThreadPool(THREADS);
    }

    @After
    public void methodTeardown() {
        exec.shutdown();
        ddb.close();
    }

    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < THREADS; ++i) {
            int tId = i;
            counter[i] = new AtomicLong(0);
            exec.submit(() -> {
                while (true) {
                    try {
                        ddb.listTables().join();
                        counter[tId].incrementAndGet();
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
            });
        }

        Thread progressChecker = new Thread(() -> {
           Duration d = Duration.ofSeconds(15);
           while (true) {
               try {
                   Thread.sleep(d.toMillis());
               } catch (InterruptedException ie) {
                   ie.printStackTrace();
               }
               LOG.info(() -> "Progress thread running");
               for (int id = 0; id < THREADS; ++id) {
                   final int idd = id;
                   if (counter[idd].getAndSet(0) == 0L) {
                       LOG.error(() -> "Thread " + idd + " hasn't made progress since the last check!");
                       System.exit(1);
                   }
               }
               LOG.info(() -> "Everything looks okay...");
           }

        });

        progressChecker.setDaemon(true);

        progressChecker.run();

        Thread.sleep(Duration.ofHours(1).toMillis());
    }
}
