package software.amazon.awssdk.services.cloudwatchevents;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResult;
import software.amazon.awssdk.services.cloudwatchevents.model.DisableRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.EnableRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RuleState;
import software.amazon.awssdk.services.cloudwatchevents.AmazonCloudWatchEvents;
import software.amazon.awssdk.services.cloudwatchevents.AmazonCloudWatchEventsClient;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class CloudWatchEventsIntegrationTest extends AWSIntegrationTestBase {

    private static final String RULE_NAME = "rule";
    private static final String RULE_DESCRIPTION = "ruleDescription";
    private static final String EVENT_PATTERN = "{ \"source\": [\"aws.ec2\"] }";

    private static AmazonCloudWatchEvents events;

    @BeforeClass
    public static void setUpClient() throws Exception {
        setUpCredentials();
        events = new AmazonCloudWatchEventsClient(getCredentials());

        events.putRule(new PutRuleRequest()
            .withName(RULE_NAME)
            .withDescription(RULE_DESCRIPTION)
            .withEventPattern(EVENT_PATTERN)
            );

        // By default, a newly created rule is enabled
        Assert.assertEquals(RuleState.ENABLED.toString(),
                events.describeRule(new DescribeRuleRequest().withName(RULE_NAME))
                    .getState());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        events.deleteRule(new DeleteRuleRequest().withName(RULE_NAME));
    }

    @Test
    public void basicTest() {

        events.enableRule(new EnableRuleRequest().withName(RULE_NAME));

        DescribeRuleResult describeRuleResult = events.describeRule(new DescribeRuleRequest()
                .withName(RULE_NAME));

        Assert.assertEquals(RULE_NAME, describeRuleResult.getName());
        Assert.assertEquals(RULE_DESCRIPTION, describeRuleResult.getDescription());
        Assert.assertEquals(RuleState.ENABLED.toString(),
                describeRuleResult.getState());

        events.disableRule(new DisableRuleRequest().withName(RULE_NAME));

        Assert.assertEquals(RuleState.DISABLED.toString(),
                events.describeRule(new DescribeRuleRequest().withName(RULE_NAME))
                    .getState());

    }

}
