package software.amazon.awssdk.services.dynamodbv2;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class SecurityManagerIntegrationTest extends AWSIntegrationTestBase {

    private static final String JAVA_SECURITY_POLICY_PROPERTY = "java.security.policy";

    @AfterClass
    public static void tearDownFixture() {
        System.setSecurityManager(null);
        System.clearProperty(JAVA_SECURITY_POLICY_PROPERTY);
    }

    /**
     * Basic smoke test that the SDK works with a security manager when given appropriate
     * permissions
     */
    @Test
    public void securityManagerEnabled() {
        System.setProperty(JAVA_SECURITY_POLICY_PROPERTY, getPolicyUrl());
        SecurityManager securityManager = new SecurityManager();
        System.setSecurityManager(securityManager);
        AmazonDynamoDBClient ddb = new AmazonDynamoDBClient(getCredentials());
        assertNotNull(ddb.listTables());
    }

    private String getPolicyUrl() {
        return getClass().getResource("security-manager-integ-test.policy").toExternalForm();
    }
}
