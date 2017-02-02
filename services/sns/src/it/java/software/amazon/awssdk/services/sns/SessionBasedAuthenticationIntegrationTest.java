package software.amazon.awssdk.services.sns;

import org.junit.Test;

import software.amazon.awssdk.test.AWSTestBase;

/**
 * Simple smoke test for session management.
 */
public class SessionBasedAuthenticationIntegrationTest extends AWSTestBase {

    /**
     * TODO: reenable
     */
    @Test
    public void testSessions() throws Exception {
        setUpCredentials();

        // RenewableAWSSessionCredentials sessionCredentials = new
        // STSSessionCredentials(credentials);
        // AmazonSNSClient sns = new AmazonSNSClient(sessionCredentials);
        //
        // sns.createTopic(new CreateTopicRequest().withName("java" +
        // this.getClass().getSimpleName()
        // + System.currentTimeMillis()));
    }
}
