package software.amazon.awssdk.services.dynamodbv2.document.quickstart;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.dynamodbv2.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodbv2.document.DynamoDB;

/**
 * Common base class used to initialize and shutdown the DynamoDB instance.
 */
public class QuickStartIntegrationTestBase {
    protected static DynamoDB dynamo;

    protected static String TABLE_NAME = "myTableForMidLevelApi";
    protected static String HASH_KEY_NAME = "myHashKey";
    protected static String RANGE_KEY_NAME = "myRangeKey";

    // local secondary index
    protected static String LSI_NAME = "myLSI";
    protected static String LSI_RANGE_KEY_NAME = "myLsiRangeKey";

    // global secondary index
    protected static String RANGE_GSI_NAME = "myRangeGSI";
    protected static String GSI_HASH_KEY_NAME = "myGsiHashKey";
    protected static String GSI_RANGE_KEY_NAME = "myGsiRangeKey";

    @BeforeClass
    public static void setup() throws InterruptedException {
        //        System.setProperty("javax.net.debug", "ssl");
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsTestCredentials());
        dynamo = new DynamoDB(client);
        new A_CreateTableIntegrationTest().howToCreateTable();
    }

    @AfterClass
    public static void tearDown() {
        dynamo.shutdown();
    }

    protected static AWSCredentials awsTestCredentials() {
        try {
            return new PropertiesCredentials(new File(
                    System.getProperty("user.home")
                    + "/.aws/awsTestAccount.properties"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
