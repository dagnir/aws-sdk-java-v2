package software.amazon.awssdk.services.elasticache;

import org.junit.BeforeClass;
import software.amazon.awssdk.test.AWSTestBase;

public class ElastiCacheIntegrationTestBase extends AWSTestBase {

    protected static final String MEMCACHED_ENGINE = "memcached";
    protected static final String REDIS_ENGINE = "redis";

    protected static AmazonElastiCacheClient elasticache;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        elasticache = new AmazonElastiCacheClient(credentials);
    }
}
