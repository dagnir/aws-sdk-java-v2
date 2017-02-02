package software.amazon.awssdk.services.dynamodbv2;

import java.util.UUID;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.BasicSessionCredentials;
import software.amazon.awssdk.services.dynamodbv2.model.ResourceNotFoundException;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenServiceClient;
import software.amazon.awssdk.services.securitytoken.model.Credentials;
import software.amazon.awssdk.services.securitytoken.model.GetFederationTokenRequest;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Simple smoke test to make sure the new JSON error unmarshaller works as expected.
 */
public class DynamoDBJavaClientExceptionIntegrationTest extends AWSTestBase {

    private static AmazonDynamoDB ddb;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        ddb = new AmazonDynamoDBClient(credentials);
    }

    @Test
    public void testResourceNotFoundException() {
        try {
            ddb.describeTable(UUID.randomUUID().toString());
            Assert.fail("ResourceNotFoundException is expected.");
        } catch (ResourceNotFoundException e) {
            Assert.assertNotNull(e.getErrorCode());
            Assert.assertNotNull(e.getErrorType());
            Assert.assertNotNull(e.getMessage());
            Assert.assertNotNull(e.getRawResponseContent());
        }
    }

    @Test
    public void testPermissionError() {
        AWSSecurityTokenServiceClient sts =
                new AWSSecurityTokenServiceClient(credentials);

        Credentials creds = sts.getFederationToken(new GetFederationTokenRequest()
                .withName("NoAccess")
                .withPolicy("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Deny\",\"Action\":\"*\",\"Resource\":\"*\"}]}")
                .withDurationSeconds(900)).getCredentials();


        AmazonDynamoDBClient client = new AmazonDynamoDBClient(new BasicSessionCredentials(
                creds.getAccessKeyId(),
                creds.getSecretAccessKey(),
                creds.getSessionToken()));

        try {
            client.listTables();
        } catch (AmazonServiceException e) {
            Assert.assertEquals("AccessDeniedException", e.getErrorCode());
            Assert.assertNotNull(e.getErrorMessage());
            Assert.assertNotNull(e.getMessage());
        }
    }
}
