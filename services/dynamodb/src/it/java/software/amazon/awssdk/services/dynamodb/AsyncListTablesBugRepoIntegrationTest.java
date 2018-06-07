package software.amazon.awssdk.services.dynamodb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncListTablesBugRepoIntegrationTest {
    private static final Logger LOG = Logger.loggerFor(AsyncListTablesBugRepoIntegrationTest.class);

    private static final int THREADS = 100;

    private DynamoDBAsyncClient ddb;

    private ExecutorService exec;

    private final Map<Long, AtomicLong> counter = new ConcurrentHashMap<>();

    @Before
    public void methodSetup() {
        ddb = DynamoDBAsyncClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("java-integ-test"))
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
            long tId = i;
            counter.put(tId, new AtomicLong(0));
            exec.submit(() -> {
                while (true) {
                    ddb.listTables().join();
                    counter.get(tId).incrementAndGet();
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
               for (long id = 0; id < THREADS; ++id) {
                   final long idd = id;
                   if (counter.get(id).getAndSet(0) == 0L) {
                       LOG.error(() -> "Thread " + idd + " hasn't made progress since the last check!");
                       System.exit(1);
                   }
               }

               LOG.info(() -> "Everything peachy brah");
           }

        });

        progressChecker.setDaemon(true);

        progressChecker.run();

        Thread.sleep(Duration.ofHours(1).toMillis());
    }
}
