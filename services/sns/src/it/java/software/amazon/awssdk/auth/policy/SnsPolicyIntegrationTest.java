package software.amazon.awssdk.auth.policy;

import org.junit.After;
import org.junit.Test;

import software.amazon.awssdk.auth.policy.Statement.Effect;
import software.amazon.awssdk.auth.policy.actions.SNSActions;
import software.amazon.awssdk.auth.policy.conditions.SNSConditionFactory;
import software.amazon.awssdk.services.sns.IntegrationTestBase;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;

/**
 * Integration tests for the service specific access control policy code provided by the SNS client.
 */
public class SnsPolicyIntegrationTest extends IntegrationTestBase {
    private String topicArn;

    /** Releases all test resources */
    @After
    public void tearDown() throws Exception {
        sns.deleteTopic(new DeleteTopicRequest(topicArn));
    }

    /**
     * Tests that we can construct valid policies with SNS specific conditions/resources/etc.
     */
    @Test
    public void testPolicies() throws Exception {
        String topicName = "java-sns-policy-integ-test-" + System.currentTimeMillis();
        topicArn = sns.createTopic(new CreateTopicRequest(topicName)).getTopicArn();

        Policy policy = new Policy().withStatements(new Statement(Effect.Allow).withActions(SNSActions.Subscribe)
                .withPrincipals(Principal.AllUsers).withResources(new Resource(topicArn))
                .withConditions(SNSConditionFactory.newEndpointCondition("*@amazon.com"),
                        SNSConditionFactory.newProtocolCondition("email")));
        sns.setTopicAttributes(new SetTopicAttributesRequest(topicArn, "Policy", policy.toJson()));
    }
}
