package software.amazon.awssdk.auth.policy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.auth.policy.Statement.Effect;
import software.amazon.awssdk.auth.policy.actions.SQSActions;
import software.amazon.awssdk.auth.policy.conditions.DateCondition;
import software.amazon.awssdk.auth.policy.conditions.DateCondition.DateComparisonType;
import software.amazon.awssdk.services.sqs.AmazonSQSAsync;
import software.amazon.awssdk.services.sqs.IntegrationTestBase;
import software.amazon.awssdk.services.sqs.auth.policy.resources.SQSQueueResource;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

/**
 * Integration tests for the service specific access control policy code provided by the SQS client.
 */
public class SqsPolicyIntegrationTest extends IntegrationTestBase {

    /**
     * Doesn't have to be a valid account id, just has to have a value
     **/
    private static final String ACCOUNT_ID = "123456789";

    private String queueUrl;
    private final AmazonSQSAsync sqsClient = getSharedSqsAsyncClient();

    /**
     * Releases all test resources
     */
    @After
    public void tearDown() throws Exception {
        sqsClient.deleteQueue(queueUrl);
    }

    /**
     * Tests that the SQS specific access control policy code works as expected.
     */
    @Test
    public void testPolicies() throws Exception {
        String queueName = getUniqueQueueName();
        queueUrl = sqsClient.createQueue(queueName).getQueueUrl();

        Policy policy = new Policy().withStatements(new Statement(Effect.Allow).withPrincipals(Principal.AllUsers)
                                                            .withActions(SQSActions.SendMessage, SQSActions.ReceiveMessage)
                                                            .withResources(new SQSQueueResource(ACCOUNT_ID, queueName))
                                                            .withConditions(new DateCondition(DateComparisonType.DateLessThan,
                                                                                              new Date())));
        setQueuePolicy(policy);
    }

    private void setQueuePolicy(Policy policy) {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("Policy", policy.toJson());

        sqsClient.setQueueAttributes(new SetQueueAttributesRequest(queueUrl, attributes));
    }
}
