package software.amazon.awssdk.services.s3;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Issue461ReproIntegrationTest {
    private static final Logger LOG = Logger.loggerFor(Issue461ReproIntegrationTest.class);

    private static final int MIB = 1 << 20;
    private static final int N_BLOCKS = 5;

    private static final int N_THREADS = 50;

    private static final int ROUNDS = 512;

    private static Path testFile;

    private S3AsyncClient s3;
    private ExecutorService exec;

    private final Map<Long, AtomicLong> progressCounter = new HashMap<>();
    private final Map<Long, AtomicLong> totals = new HashMap<>();

    @BeforeClass
    public static void setup() {
        testFile = generateTestFile();
    }

    @AfterClass
    public static void teardown() {
        try {
            Files.delete(testFile);
        } catch (IOException e) {
            LOG.info(() -> "Could not clean up test file!", e);
        }
    }

    @Before
    public void methodSetup() {
        s3 = S3AsyncClient.builder().region(Region.US_WEST_2).build();
        exec = Executors.newFixedThreadPool(N_THREADS);
    }

    @After
    public void methodTeardown() {
        exec.shutdown();
        s3.close();
    }

    @Test
    public void test() throws InterruptedException {
        CyclicBarrier b = new CyclicBarrier(N_THREADS);
        CountDownLatch latch = new CountDownLatch(N_THREADS);

        for (int t = 0; t < N_THREADS; ++t) {
            final long tId = t;
            progressCounter.put(tId, new AtomicLong(0));
            totals.put(tId, new AtomicLong(0));
            exec.submit(() -> {
                b.await();

                final PutObjectRequest req = PutObjectRequest.builder()
                        .bucket("dongietest")
                        .key("issue-461-thread-" + tId)
                        .build();

                for (int r = 0; r < ROUNDS; ++r) {
                    final int rr = r;
                    boolean done = false;
                    do {
                        try {
                            s3.putObject(req, AsyncRequestBody.fromFile(testFile)).join();
                            synchronized (progressCounter) {
                                progressCounter.get(tId).incrementAndGet();
                            }
                            LOG.info(() -> String.format("Thread %d finished round %d", tId, rr));
                            done = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } while (!done);
                }

                latch.countDown();
                LOG.info(() -> String.format("Thread %d done", tId));

                return null;
            });
        }

        Thread t = new Thread(() -> {
            while (true) {
                LOG.info(() -> "Progress checker thread running");
                Duration d = Duration.ofSeconds(60);
                try {
                    Thread.sleep(d.toMillis());

                    synchronized (progressCounter) {
                        for (long i = 0; i < N_THREADS; ++i) {
                            final long ii = i;
                            long count = progressCounter.get(i).getAndSet(0);
                            long total = totals.get(i).addAndGet(count);
                            if (count == 0L && total < ROUNDS) {
                                LOG.error(() -> String.format("Uh-oh, thread %d hasn't made progress in the last %s seconds! It's probably stuck. Exiting.", ii, d));
                                System.exit(1);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);

        t.run();

        latch.await();
    }

    private static Path generateTestFile() {
        LOG.info(() -> "Generating test file");
        try {
            Path tmp = Files.createTempFile("issue461-test-file-", "");

            byte blk[] = new byte[MIB];
            new Random().nextBytes(blk);

            OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.WRITE);

            for (int i = 0; i < N_BLOCKS; ++i) {
                os.write(blk);
            }
            os.close();
            LOG.info(() -> "Finished generating test file.");
            return tmp;
        } catch (IOException ioe) {
            LOG.info(() -> "Error generating test file", ioe);
            throw new UncheckedIOException(ioe);
        }
    }
}
