package software.amazon.awssdk.services.dynamodbv2.datamodeling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.*;

public class DynamoDBReflectorTest {
    private final DynamoDBReflector reflector = new DynamoDBReflector();
    private final DynamoDBTableSchemaParser schemaParser = new DynamoDBTableSchemaParser();
    
    @Test
    public void test() throws InterruptedException, ExecutionException {
        final int MAX = 100;
        MockTable mt = new MockTable(); // eagerly loads the class
        ExecutorService es = Executors.newFixedThreadPool(MAX);
        List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
        for (int i=0; i < MAX; i++) {
            tasks.add(new Callable<Object>() {
                @Override
                public Object call() {
                    Object ret = null;
                    for (int i=0; i < 10; i++) {
                        ret = reflector.getRelevantGetters(MockTable.class);
                        ret = schemaParser.parseTableIndexes(MockTable.class, reflector);
                    }
                    return ret;
                }
                
            });
        }
        final long start = System.nanoTime();
        List<Future<Object>> futures = es.invokeAll(tasks);
        for (Future<Object> f: futures) {
            Object result = f.get();
            assertNotNull(result);
        }
        final long end = System.nanoTime();
        System.out.println("Elapsed time: " + TimeUnit.NANOSECONDS.toMillis(end-start) + " ms");
    }

    @DynamoDBTable(tableName="Foo")
    public static class MockTable {
        @DynamoDBIndexHashKey(attributeName="primary",globalSecondaryIndexNames={"a", "b", "c"})
        public String getAlpha() {
            return alpha;
        }

        public void setAlpha(String alpha) {
            this.alpha = alpha;
        }

        @DynamoDBIndexHashKey(attributeName="attr2",globalSecondaryIndexNames={"a", "b", "c"})
        public String getBeta() {
            return beta;
        }

        public void setBeta(String beta) {
            this.beta = beta;
        }

        public String getGamma() {
            return gamma;
        }

        public void setGamma(String gamma) {
            this.gamma = gamma;
        }

        private String alpha, beta, gamma;
    }
}
