/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except : compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.cloudformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.SDKGlobalTime;
import software.amazon.awssdk.auth.policy.Policy;
import software.amazon.awssdk.auth.policy.Resource;
import software.amazon.awssdk.auth.policy.Statement;
import software.amazon.awssdk.auth.policy.Statement.Effect;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CancelUpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResult;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResult;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResult;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResult;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResult;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyRequest;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyResult;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateResult;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksResult;
import software.amazon.awssdk.services.cloudformation.model.SetStackPolicyRequest;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackResource;
import software.amazon.awssdk.services.cloudformation.model.StackResourceDetail;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResult;

/**
 * Tests of the Stack APIs : CloudFormation
 */
public class StackIntegrationTests extends CloudFormationIntegrationTestBase {

    private static final String STACK_NAME_PREFIX = StackIntegrationTests.class.getName().toString().replace('.', '-');

    /** The initial stack policy which allows access to all resources */
    private static final Policy INIT_STACK_POLICY;
    private static final Logger log = Logger.getLogger(StackIntegrationTests.class);

    private static String testStackName;
    private static String testStackId;
    private final static int PAGINATION_THRESHOLD = 120;

    static {
        INIT_STACK_POLICY = new Policy().withStatements(new Statement(Effect.Allow).withActions(
                new NamedAction("Update:*")).withResources(new Resource("*")));
    }

    // Create a stack to be used by the other tests.
    // Many of the tests ought to be able to run successfully while the stack is
    // being built.
    @BeforeClass
    public static void createTestStacks() throws Exception {
        testStackName = uniqueName();
        CreateStackResult response = cf.createStack(new CreateStackRequest()
                .withTemplateURL(templateUrlForStackIntegrationTests).withStackName(testStackName)
                .withStackPolicyBody(INIT_STACK_POLICY.toJson()));
        testStackId = response.getStackId();
    }

    @AfterClass
    public static void tearDown() {
        CloudFormationIntegrationTestBase.tearDown();
        try {
            cf.deleteStack(new DeleteStackRequest().withStackName(testStackName));
        } catch (Exception e) {
            // do not do any thing here.
        }
    }

    @Test
    public void testDescribeStacks() throws Exception {
        DescribeStacksResult response = cf.describeStacks(new DescribeStacksRequest().withStackName(testStackName));

        assertEquals(1, response.getStacks().size());
        assertEquals(testStackId, response.getStacks().get(0).getStackId());

        response = cf.describeStacks(new DescribeStacksRequest());
        assertTrue(response.getStacks().size() >= 1);
    }

    @Test
    public void testDescribeStackResources() throws Exception {

        DescribeStackResourcesResult response = null;

        int attempt = 0;
        while (attempt++ < 60 && (response == null || response.getStackResources().size() == 0)) {
            Thread.sleep(1000);
            response = cf.describeStackResources(new DescribeStackResourcesRequest().withStackName(testStackName));
        }

        assertTrue(response.getStackResources().size() > 0);
        for (StackResource sr : response.getStackResources()) {
            assertEquals(testStackId, sr.getStackId());
            assertEquals(testStackName, sr.getStackName());
            assertNotNull(sr.getLogicalResourceId());
            assertNotNull(sr.getResourceStatus());
            assertNotNull(sr.getResourceType());
            assertNotNull(sr.getTimestamp());
        }
    }

    @Test
    public void testDescribeStackResource() throws Exception {
        DescribeStackResourcesResult response = null;

        int attempt = 0;
        while (attempt++ < 60 && (response == null || response.getStackResources().size() == 0)) {
            Thread.sleep(1000);
            response = cf.describeStackResources(new DescribeStackResourcesRequest().withStackName(testStackName));
        }

        assertTrue(response.getStackResources().size() > 0);
        for (StackResource sr : response.getStackResources()) {
            assertEquals(testStackId, sr.getStackId());
            assertEquals(testStackName, sr.getStackName());

            DescribeStackResourceResult describeStackResource = cf
                    .describeStackResource(new DescribeStackResourceRequest().withStackName(testStackName)
                            .withLogicalResourceId(sr.getLogicalResourceId()));
            StackResourceDetail detail = describeStackResource.getStackResourceDetail();
            assertNotNull(detail.getLastUpdatedTimestamp());
            assertEquals(sr.getLogicalResourceId(), detail.getLogicalResourceId());
            assertEquals(sr.getPhysicalResourceId(), detail.getPhysicalResourceId());
            assertNotNull(detail.getResourceStatus());
            assertNotNull(detail.getResourceType());
            assertEquals(testStackId, detail.getStackId());
            assertEquals(testStackName, detail.getStackName());
        }
    }

    @Test
    public void testListStackResources() throws Exception {
        waitForStackToChangeStatus(StackStatus.CREATE_IN_PROGRESS);
        List<StackResourceSummary> stackResourceSummaries = cf.listStackResources(
                new ListStackResourcesRequest().withStackName(testStackName)).getStackResourceSummaries();
        for (StackResourceSummary sr : stackResourceSummaries) {
            System.out.println(sr.getPhysicalResourceId());
            assertNotNull(sr.getLogicalResourceId());
            assertNotNull(sr.getPhysicalResourceId());
            assertNotNull(sr.getResourceStatus());
            assertNotNull(sr.getResourceType());
        }
    }

    @Test
    public void testGetStackPolicy() {
        GetStackPolicyResult getStackPolicyResult = cf.getStackPolicy(new GetStackPolicyRequest()
                .withStackName(testStackName));
        Policy returnedPolicy = Policy.fromJson(getStackPolicyResult.getStackPolicyBody());
        assertPolicyEquals(INIT_STACK_POLICY, returnedPolicy);
    }

    @Test
    public void testSetStackPolicy() throws Exception {
        waitForStackToChangeStatus(StackStatus.CREATE_IN_PROGRESS);

        Policy DENY_ALL_POLICY = new Policy().withStatements(new Statement(Effect.Deny).withActions(
                new NamedAction("Update:*")).withResources(new Resource("*")));
        cf.setStackPolicy(new SetStackPolicyRequest().withStackName(testStackName).withStackPolicyBody(
                DENY_ALL_POLICY.toJson()));

        // Compares the policy from GetStackPolicy operation
        GetStackPolicyResult getStackPolicyResult = cf.getStackPolicy(new GetStackPolicyRequest()
                .withStackName(testStackName));
        Policy returnedPolicy = Policy.fromJson(getStackPolicyResult.getStackPolicyBody());
        assertPolicyEquals(DENY_ALL_POLICY, returnedPolicy);
    }

    @Test
    public void testDescribeStackEvents() throws Exception {

        DescribeStackEventsResult response = null;
        int attempt = 0;
        while (attempt++ < 60 && (response == null || response.getStackEvents().size() == 0)) {
            Thread.sleep(1000);
            response = cf.describeStackEvents(new DescribeStackEventsRequest().withStackName(testStackName));
        }

        assertTrue(response.getStackEvents().size() > 0);

        for (StackEvent e : response.getStackEvents()) {
            System.out.println(e.getEventId());
            assertEquals(testStackId, e.getStackId());
            assertEquals(testStackName, e.getStackName());
            assertNotNull(e.getEventId());
            assertNotNull(e.getLogicalResourceId());
            assertNotNull(e.getPhysicalResourceId());
            assertNotNull(e.getResourceStatus());
            assertNotNull(e.getResourceType());
            assertNotNull(e.getTimestamp());
            log.debug(e);
        }
    }

    @Test
    public void testListStacks() throws Exception {
        ListStacksResult listStacksResult = cf.listStacks(new ListStacksRequest());
        assertNotNull(listStacksResult);
        assertNotNull(listStacksResult.getStackSummaries());
        // There should be some deleted stacks, since we deleted at the start of this test
        assertFalse(listStacksResult.getStackSummaries().isEmpty());
        for (StackSummary summary : listStacksResult.getStackSummaries()) {
            assertNotNull(summary);
            assertNotNull(summary.getStackStatus());
            assertNotNull(summary.getCreationTime());
            if (summary.getStackStatus().contains("DELETE")) {
                assertNotNull(summary.getDeletionTime());
            }
            assertNotNull(summary.getStackId());
            assertNotNull(summary.getStackName());
            assertNotNull(summary.getTemplateDescription());
        }

        String nextToken = listStacksResult.getNextToken();
        listStacksResult = cf.listStacks(new ListStacksRequest().withNextToken(nextToken));

        assertNotNull(listStacksResult);
        assertNotNull(listStacksResult.getStackSummaries());
        // There should be some deleted stacks, since we deleted at the start of this test
        assertFalse(listStacksResult.getStackSummaries().isEmpty());
        for (StackSummary summary : listStacksResult.getStackSummaries()) {
            assertNotNull(summary);
            assertNotNull(summary.getStackStatus());
            assertNotNull(summary.getCreationTime());
            if (summary.getStackStatus().contains("DELETE")) {
                assertNotNull(summary.getDeletionTime());
            }
            assertNotNull(summary.getStackId());
            assertNotNull(summary.getStackName());
            assertNotNull(summary.getTemplateDescription());
        }
    }

    @Test
    public void testListStacksFilter() throws Exception {
        ListStacksResult listStacksResult = cf.listStacks(new ListStacksRequest().withStackStatusFilters(
                "CREATE_COMPLETE", "DELETE_COMPLETE"));
        assertNotNull(listStacksResult);
        assertNotNull(listStacksResult.getStackSummaries());

        // There should be some deleted stacks, since we deleted at the start of this test
        assertFalse(listStacksResult.getStackSummaries().isEmpty());
        for (StackSummary summary : listStacksResult.getStackSummaries()) {
            assertNotNull(summary);
            assertNotNull(summary.getStackStatus());
            assertTrue(summary.getStackStatus().equals("CREATE_COMPLETE")
                    || summary.getStackStatus().equals("DELETE_COMPLETE"));
            assertNotNull(summary.getCreationTime());
            if (summary.getStackStatus().contains("DELETE")) {
                assertNotNull(summary.getDeletionTime());
            }
            assertNotNull(summary.getStackId());
            assertNotNull(summary.getStackName());
            assertNotNull(summary.getTemplateDescription());
        }
    }

    @Test
    public void testGetTemplate() {
        GetTemplateResult response = cf.getTemplate(new GetTemplateRequest().withStackName(testStackName));

        assertNotNull(response.getTemplateBody());
        assertTrue(response.getTemplateBody().length() > 0);
    }

    @Test
    public void testCancelUpdateStack() throws Exception {
        waitForStackToChangeStatus(StackStatus.CREATE_IN_PROGRESS);

        List<Stack> stacks = cf.describeStacks(new DescribeStacksRequest().withStackName(testStackName)).getStacks();
        assertEquals(1, stacks.size());

        UpdateStackResult updateStack = cf.updateStack(new UpdateStackRequest().withStackName(testStackName)
                .withTemplateURL(templateUrlForCloudFormationIntegrationTests));
        assertEquals(testStackId, updateStack.getStackId());

        cf.cancelUpdateStack(new CancelUpdateStackRequest().withStackName(testStackName));
        waitForStackToChangeStatus(StackStatus.UPDATE_ROLLBACK_IN_PROGRESS);
    }

    @Test
    public void testUpdateStack() throws Exception {
        List<Stack> stacks = cf.describeStacks(new DescribeStacksRequest().withStackName(testStackName)).getStacks();
        assertEquals(1, stacks.size());

        UpdateStackResult updateStack = cf.updateStack(new UpdateStackRequest().withStackName(testStackName)
                .withTemplateURL(templateUrlForCloudFormationIntegrationTests)
                .withStackPolicyBody(INIT_STACK_POLICY.toJson()));
        assertEquals(testStackId, updateStack.getStackId());
        waitForStackToChangeStatus(StackStatus.UPDATE_IN_PROGRESS);
    }

    @Test
    public void testAlreadyExistsException() {
        try {
            cf.createStack(new CreateStackRequest().withTemplateURL(templateUrlForStackIntegrationTests).withStackName(
                    testStackName));
            fail("Should have thrown an Exception");
        } catch (AlreadyExistsException aex) {
            assertEquals("AlreadyExistsException", aex.getErrorCode());
            assertEquals(ErrorType.Client, aex.getErrorType());
        } catch (Exception e) {
            fail("Should have thrown an AlreadyExists Exception.");
        }
    }

    /** assertEquals between two policy objects (assuming the Statements are in the same order) */
    private static void assertPolicyEquals(Policy expected, Policy actual) {
        assertEquals(expected.getStatements().size(), actual.getStatements().size());

        Iterator<Statement> iter1 = expected.getStatements().iterator();
        Iterator<Statement> iter2 = actual.getStatements().iterator();

        while (iter1.hasNext() && iter2.hasNext()) {
            Statement s1 = iter1.next();
            Statement s2 = iter2.next();
            assertEquals(s1.getEffect(), s2.getEffect());
            assertEquals(s1.getActions().size(), s2.getActions().size());
            for (int i = 0; i < s1.getActions().size(); i++) {
                assertTrue(s1.getActions().get(i).getActionName()
                        .equalsIgnoreCase(s2.getActions().get(i).getActionName()));
            }
            assertEquals(s1.getResources().size(), s2.getResources().size());
            for (int i = 0; i < s1.getResources().size(); i++) {
                assertTrue(s1.getResources().get(i).getId().equalsIgnoreCase(s2.getResources().get(i).getId()));
            }

        }
        // Unnecessary... but just to be safe
        if (iter1.hasNext() || iter2.hasNext()) {
            fail("The two policy have difference number of Statments.");
        }
    }

    private static String uniqueName() {
        return STACK_NAME_PREFIX + "-" + System.currentTimeMillis();
    }

    /**
     * Waits up to 15 minutes for the test stack to transition out of the specified status.
     *
     * @param oldStatus
     *            The expected current status of the test stack; this method will return as soon as
     *            the test stack has a status other than this.
     */
    private void waitForStackToChangeStatus(StackStatus oldStatus) throws Exception {
        long startTime = System.currentTimeMillis();
        long timeoutInMinutes = 35;
        while (true) {
            List<Stack> stacks = cf.describeStacks(new DescribeStacksRequest().withStackName(testStackName))
                    .getStacks();
            assertEquals(1, stacks.size());

            if (!stacks.get(0).getStackStatus().equalsIgnoreCase(oldStatus.toString()))
                return;

            System.out.println("Waiting for stack to change out of status " + oldStatus.toString()
                    + " (current status: " + stacks.get(0).getStackStatus() + ")");

            if ((System.currentTimeMillis() - startTime) > (timeoutInMinutes * 1000 * 60))
                throw new RuntimeException("Waited " + timeoutInMinutes
                        + " minutes, but stack never changed status from " + oldStatus.toString());

            Thread.sleep(1000 * 120);
        }
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SDKGlobalTime.setGlobalTimeOffset(3600);
        // Need to create a new client to have the time offset take affect
        AmazonCloudFormationClient clockSkewClient = new AmazonCloudFormationClient(credentials);
        clockSkewClient.describeStacks();
        assertTrue(SDKGlobalTime.getGlobalTimeOffset() < 60);
    }
}
