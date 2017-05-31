/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.autoscaling;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.SdkGlobalTime;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.autoscaling.model.AlreadyExistsException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.BlockDeviceMapping;
import software.amazon.awssdk.services.autoscaling.model.CreateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateOrUpdateTagsRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteNotificationConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeletePolicyRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteScheduledActionRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteTagsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingNotificationTypesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import software.amazon.awssdk.services.autoscaling.model.DescribeNotificationConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribePoliciesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribePoliciesResult;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResult;
import software.amazon.awssdk.services.autoscaling.model.DescribeScheduledActionsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScheduledActionsResult;
import software.amazon.awssdk.services.autoscaling.model.DescribeTagsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeTagsResult;
import software.amazon.awssdk.services.autoscaling.model.DescribeTerminationPolicyTypesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeTerminationPolicyTypesResult;
import software.amazon.awssdk.services.autoscaling.model.Ebs;
import software.amazon.awssdk.services.autoscaling.model.ExecutePolicyRequest;
import software.amazon.awssdk.services.autoscaling.model.Filter;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.autoscaling.model.NotificationConfiguration;
import software.amazon.awssdk.services.autoscaling.model.PutNotificationConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.PutScalingPolicyRequest;
import software.amazon.awssdk.services.autoscaling.model.PutScalingPolicyResult;
import software.amazon.awssdk.services.autoscaling.model.PutScheduledUpdateGroupActionRequest;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.ScalingPolicy;
import software.amazon.awssdk.services.autoscaling.model.ScheduledUpdateGroupAction;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.Tag;
import software.amazon.awssdk.services.autoscaling.model.TagDescription;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;


/**
 * Integration tests for the Autoscaling service. TODO: No tests for
 * TerminateInstanceInAutoScalingGroup
 */

public class AutoScalingIntegrationTest extends IntegrationTestBase {

    protected static final String TEST_POLICY_NAME = "TestPolicyName";
    protected static final String TEST_ACTION_NAME = "TestActionName";
    protected static final String TEST_ADJUSTMENT_TYPE = "PercentChangeInCapacity";

    protected static final Integer TEST_SCALING_ADJUSTMENT = new Integer(7);
    protected static final Integer TEST_COOLDOWN = new Integer(3);
    protected static final Integer TEST_MIN_SIZE = new Integer(1);
    protected static final Integer TEST_MAX_SIZE = new Integer(10);
    protected static final Integer TEST_DESIRED_CAPACITY = new Integer(5);
    protected static final List<String> TERMINATION_POLICIES;
    protected static final String TERMINATION_POLICY = "ClosestToNextInstanceHour";

    protected static final Date TEST_ACTION_TIME = new Date(new Date().getTime() + 100000);

    static {
        TERMINATION_POLICIES = new LinkedList<String>();
        TERMINATION_POLICIES.add("ClosestToNextInstanceHour");
        TERMINATION_POLICIES.add("Default");
        TERMINATION_POLICIES.add("NewestInstance");
        TERMINATION_POLICIES.add("OldestInstance");
        TERMINATION_POLICIES.add("OldestLaunchConfiguration");
    }

    /**
     * The name of the launch configuration created by these tests
     */

    protected String launchConfigurationName;
    /**
     * The name of the autoscaling group created by these tests
     */

    protected String autoScalingGroupName;
    /**
     * The name of the SNS topic created by the notification operation tests
     */

    private String topicARN;

    /**
     * Releases any resources created by these tests
     */

    @After
    public void tearDown() {
        if (autoScalingGroupName != null) {
            try {
                autoscaling.deleteAutoScalingGroup(new DeleteAutoScalingGroupRequest().withAutoScalingGroupName(
                        autoScalingGroupName).withForceDelete(true));
            } catch (Exception e) {
                // Ignored.
            }
        }
        if (launchConfigurationName != null) {
            try {
                autoscaling.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest()
                                                              .withLaunchConfigurationName(launchConfigurationName));
            } catch (Exception e) {
                // Ignored.
            }
        }
        if (topicARN != null) {
            try {
                sns.deleteTopic(new DeleteTopicRequest(topicARN));
            } catch (Exception e) {
                // Ignored.
            }
        }
    }


    /*
     * Test Methods
     */


    /**
     * Tests that exceptions are properly thrown from the AutoScaling client when the service
     * returns an error.
     */

    @Test
    public void testExceptionHandling() throws Exception {
        try {
            launchConfigurationName = "integ-test-launch-configuration-" + new Date().getTime();
            createLaunchConfiguration(launchConfigurationName);
            createLaunchConfiguration(launchConfigurationName);
        } catch (AlreadyExistsException aee) {
            assertEquals(400, aee.getStatusCode());
            assertEquals("AlreadyExists", aee.getErrorCode());
            assertEquals(ErrorType.Client, aee.getErrorType());
            assertNotEmpty(aee.getMessage());
            assertNotEmpty(aee.getRequestId());
            assertNotEmpty(aee.getServiceName());
        }
    }


    /**
     * Tests that we can create, describe and delete a launch configuration group.
     */

    @Test
    public void testLaunchConfigurationOperations() throws Exception {
        launchConfigurationName = "java-integ-test-config-" + new Date().getTime();
        String userData = "12345678901234567890123456789012345678901234567890"
                          + "12345678901234567890123456789012345678901234567890"
                          + "12345678901234567890123456789012345678901234567890";

        String encodedUserData = new String(Base64.encodeBase64(userData.getBytes()));

        BlockDeviceMapping blockDeviceMapping1 = new BlockDeviceMapping();
        blockDeviceMapping1.setDeviceName("xvdh");
        blockDeviceMapping1.setNoDevice(true);

        BlockDeviceMapping blockDeviceMapping2 = new BlockDeviceMapping();

        Ebs ebs = new Ebs();
        ebs.setDeleteOnTermination(true);
        ebs.setVolumeType("io1");
        ebs.setVolumeSize(10);
        ebs.setIops(100);

        blockDeviceMapping2.setEbs(ebs);
        blockDeviceMapping2.setDeviceName("/dev/sdh");

        // Create a LaunchConfigurationGroup
        CreateLaunchConfigurationRequest createRequest = new CreateLaunchConfigurationRequest()
                .withLaunchConfigurationName(launchConfigurationName).withImageId(AMI_ID)
                .withInstanceType(INSTANCE_TYPE).withSecurityGroups("default").withUserData(encodedUserData)
                .withAssociatePublicIpAddress(false);

        createRequest.setBlockDeviceMappings(Arrays.asList(blockDeviceMapping1, blockDeviceMapping2));
        autoscaling.createLaunchConfiguration(createRequest);

        // Describe it
        DescribeLaunchConfigurationsRequest describeRequest = new DescribeLaunchConfigurationsRequest()
                .withLaunchConfigurationNames(launchConfigurationName);
        DescribeLaunchConfigurationsResult result = autoscaling.describeLaunchConfigurations(describeRequest);
        List<LaunchConfiguration> launchConfigurations = result.getLaunchConfigurations();
        assertEquals(1, launchConfigurations.size());
        LaunchConfiguration launchConfiguration = launchConfigurations.get(0);
        assertNotNull(launchConfiguration.getCreatedTime());
        assertEquals(AMI_ID, launchConfiguration.getImageId());
        assertEquals(INSTANCE_TYPE, launchConfiguration.getInstanceType());
        assertEquals(launchConfigurationName, launchConfiguration.getLaunchConfigurationName());
        assertEquals("default", launchConfiguration.getSecurityGroups().get(0));
        assertEquals(encodedUserData, launchConfiguration.getUserData());
        assertEquals(false, launchConfiguration.getAssociatePublicIpAddress());
        assertThat(result.getLaunchConfigurations().get(0).getBlockDeviceMappings(),
                   containsInAnyOrder(blockDeviceMapping1, blockDeviceMapping2));

        // Delete it
        autoscaling.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest()
                                                      .withLaunchConfigurationName(launchConfigurationName));
    }


    /**
     * Tests that we can create, describe, set desired capacity and delete autoscaling groups.
     */

    @Test
    public void testAutoScalingGroupOperations() throws Exception {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(AVAILABILITY_ZONE).withMaxSize(2).withMinSize(1);
        autoscaling.createAutoScalingGroup(createRequest);

        // Set desired capacity
        autoscaling.setDesiredCapacity(new SetDesiredCapacityRequest().withAutoScalingGroupName(autoScalingGroupName)
                                                                      .withDesiredCapacity(1));

        // Describe
        DescribeAutoScalingGroupsResult result = autoscaling
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                                                   .withAutoScalingGroupNames(autoScalingGroupName));
        List<AutoScalingGroup> autoScalingGroups = result.getAutoScalingGroups();
        assertEquals(1, autoScalingGroups.size());
        AutoScalingGroup group = autoScalingGroups.get(0);
        // TODO: Commenting out until AutoScaling is ready to release their next API update
        // assertEquals("Active", group.getScalingMode());
        assertEquals(1, group.getAvailabilityZones().size());
        assertEquals(AVAILABILITY_ZONE, group.getAvailabilityZones().get(0));
        assertEquals(autoScalingGroupName, group.getAutoScalingGroupName());
        assertNotNull(group.getCreatedTime());
        assertEquals(1, group.getDesiredCapacity().intValue());
        assertEquals(0, group.getInstances().size());
        assertEquals(launchConfigurationName, group.getLaunchConfigurationName());
        assertEquals(0, group.getLoadBalancerNames().size());
        assertEquals(2, group.getMaxSize().intValue());
        assertEquals(1, group.getMinSize().intValue());
        assertEquals(1, group.getTerminationPolicies().size());
        assertEquals("Default", group.getTerminationPolicies().get(0));

        // Update
        UpdateAutoScalingGroupRequest updateRequest = new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withMaxSize(3).withDefaultCooldown(3).withMinSize(2)
                .withTerminationPolicies(TERMINATION_POLICY);
        autoscaling.updateAutoScalingGroup(updateRequest);

        // Check our updates
        result = autoscaling.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                                                               .withAutoScalingGroupNames(autoScalingGroupName));
        autoScalingGroups = result.getAutoScalingGroups();
        assertEquals(1, autoScalingGroups.size());
        group = autoScalingGroups.get(0);
        assertEquals((double) 3, (double) group.getMaxSize(), 0.0);
        assertEquals((double) 3, (double) group.getDefaultCooldown(), 0.0);
        assertEquals((double) 2, (double) group.getMinSize(), 0.0);
        assertEquals(1, group.getTerminationPolicies().size());
        assertEquals(TERMINATION_POLICY, group.getTerminationPolicies().get(0));

        // Describe the scaling activity of our group
        DescribeScalingActivitiesRequest describeActivityRequest = new DescribeScalingActivitiesRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withMaxRecords(20);
        DescribeScalingActivitiesResult describeScalingActivitiesResult = autoscaling
                .describeScalingActivities(describeActivityRequest);
        describeScalingActivitiesResult.getActivities();
    }


    /**
     * Tests that we can create, describe and delete a policies.
     */

    @Test
    public void testPolicies() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(AVAILABILITY_ZONE).withMaxSize(2).withMinSize(1);
        autoscaling.createAutoScalingGroup(createRequest);

        // Put a Policy
        PutScalingPolicyRequest putScalingPolicyRequest = new PutScalingPolicyRequest();
        putScalingPolicyRequest.setAutoScalingGroupName(autoScalingGroupName);
        putScalingPolicyRequest.setPolicyName(TEST_POLICY_NAME);
        putScalingPolicyRequest.setScalingAdjustment(TEST_SCALING_ADJUSTMENT);
        putScalingPolicyRequest.setAdjustmentType(TEST_ADJUSTMENT_TYPE);
        putScalingPolicyRequest.setCooldown(TEST_COOLDOWN);

        PutScalingPolicyResult putScalingPolicyResult = autoscaling.putScalingPolicy(putScalingPolicyRequest);
        assertNotNull(putScalingPolicyResult);
        assertNotNull(putScalingPolicyResult.getPolicyARN());

        // Describe the Policy
        DescribePoliciesRequest describePoliciesRequest = new DescribePoliciesRequest();
        describePoliciesRequest.setAutoScalingGroupName(autoScalingGroupName);

        DescribePoliciesResult describePoliciesResult = autoscaling.describePolicies(describePoliciesRequest);
        assertNotNull(describePoliciesResult);
        assertEquals(1, describePoliciesResult.getScalingPolicies().size());

        ScalingPolicy policy = describePoliciesResult.getScalingPolicies().get(0);
        assertEquals(autoScalingGroupName, policy.getAutoScalingGroupName());
        assertEquals(TEST_POLICY_NAME, policy.getPolicyName());
        assertEquals(TEST_ADJUSTMENT_TYPE, policy.getAdjustmentType());
        assertEquals(TEST_SCALING_ADJUSTMENT, policy.getScalingAdjustment());
        assertEquals(TEST_COOLDOWN, policy.getCooldown());
        assertEquals(0, policy.getAlarms().size());
        assertNotNull(policy.getPolicyARN());

        // Execute the Policy
        ExecutePolicyRequest executePolicyRequest = new ExecutePolicyRequest();
        executePolicyRequest.setAutoScalingGroupName(autoScalingGroupName);
        executePolicyRequest.setPolicyName(TEST_POLICY_NAME);

        autoscaling.executePolicy(executePolicyRequest);

        // Delete the Policy
        DeletePolicyRequest deletePolicyRequest = new DeletePolicyRequest();
        deletePolicyRequest.setAutoScalingGroupName(autoScalingGroupName);
        deletePolicyRequest.setPolicyName(TEST_POLICY_NAME);

        autoscaling.deletePolicy(deletePolicyRequest);

        describePoliciesRequest = new DescribePoliciesRequest();
        describePoliciesRequest.setAutoScalingGroupName(autoScalingGroupName);

        describePoliciesResult = autoscaling.describePolicies(describePoliciesRequest);
        assertNotNull(describePoliciesResult);
        assertEquals(0, describePoliciesResult.getScalingPolicies().size());
    }


    /**
     * Tests that we can create, describe and delete an action.
     */

    @Test
    public void testActions() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(AVAILABILITY_ZONE).withMaxSize(2).withMinSize(1);
        autoscaling.createAutoScalingGroup(createRequest);

        // Describe the Action
        PutScheduledUpdateGroupActionRequest putScheduledUpdateGroupActionRequest = new PutScheduledUpdateGroupActionRequest();
        putScheduledUpdateGroupActionRequest.setAutoScalingGroupName(autoScalingGroupName);
        putScheduledUpdateGroupActionRequest.setScheduledActionName(TEST_ACTION_NAME);
        putScheduledUpdateGroupActionRequest.setStartTime(TEST_ACTION_TIME);
        putScheduledUpdateGroupActionRequest.setMinSize(TEST_MIN_SIZE);
        putScheduledUpdateGroupActionRequest.setMaxSize(TEST_MAX_SIZE);
        putScheduledUpdateGroupActionRequest.setDesiredCapacity(TEST_DESIRED_CAPACITY);

        autoscaling.putScheduledUpdateGroupAction(putScheduledUpdateGroupActionRequest);

        // Describe the Action
        DescribeScheduledActionsRequest describeScheduledActionsRequest = new DescribeScheduledActionsRequest();
        describeScheduledActionsRequest.setAutoScalingGroupName(autoScalingGroupName);

        DescribeScheduledActionsResult describeScheduledActionsResult = autoscaling
                .describeScheduledActions(describeScheduledActionsRequest);
        assertNotNull(describeScheduledActionsResult);
        assertEquals(1, describeScheduledActionsResult.getScheduledUpdateGroupActions().size());

        ScheduledUpdateGroupAction scheduledUpdateGroupAction = describeScheduledActionsResult
                .getScheduledUpdateGroupActions().get(0);
        assertEquals(autoScalingGroupName, scheduledUpdateGroupAction.getAutoScalingGroupName());
        assertEquals(TEST_ACTION_NAME, scheduledUpdateGroupAction.getScheduledActionName());
        assertEquals(TEST_MIN_SIZE, scheduledUpdateGroupAction.getMinSize());
        assertEquals(TEST_MAX_SIZE, scheduledUpdateGroupAction.getMaxSize());
        assertEquals(TEST_DESIRED_CAPACITY, scheduledUpdateGroupAction.getDesiredCapacity());
        // assertEquals( TEST_ACTION_TIME.getTime(), scheduledUpdateGroupAction.getTime().getTime()
        // );

        // Delete the Action
        DeleteScheduledActionRequest deleteScheduledActionRequest = new DeleteScheduledActionRequest();
        deleteScheduledActionRequest.setAutoScalingGroupName(autoScalingGroupName);
        deleteScheduledActionRequest.setScheduledActionName(TEST_ACTION_NAME);

        autoscaling.deleteScheduledAction(deleteScheduledActionRequest);

        describeScheduledActionsRequest = new DescribeScheduledActionsRequest();
        describeScheduledActionsRequest.setAutoScalingGroupName(autoScalingGroupName);

        describeScheduledActionsResult = autoscaling.describeScheduledActions(describeScheduledActionsRequest);
        assertNotNull(describeScheduledActionsResult);
        assertEquals(0, describeScheduledActionsResult.getScheduledUpdateGroupActions().size());
    }


    /**
     * Tests that we can create, describe and delete an action.
     */

    @Test
    public void testInstancesAndProcesses() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(AVAILABILITY_ZONE).withMaxSize(2).withMinSize(1);
        autoscaling.createAutoScalingGroup(createRequest);

        // Describe Instances
        DescribeAutoScalingInstancesResult describeAutoScalingInstancesResult = autoscaling
                .describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest());
        assertNotNull(describeAutoScalingInstancesResult);

        // Suspend Processes
        SuspendProcessesRequest suspendProcessesRequest = new SuspendProcessesRequest();
        suspendProcessesRequest.setAutoScalingGroupName(autoScalingGroupName);

        autoscaling.suspendProcesses(suspendProcessesRequest);

        // Resume Processes
        ResumeProcessesRequest resumeProcessesRequest = new ResumeProcessesRequest();
        resumeProcessesRequest.setAutoScalingGroupName(autoScalingGroupName);

        autoscaling.resumeProcesses(resumeProcessesRequest);
    }


    /**
     * Tests that we can invoke the notificaiton related operations correctly.
     */

    @Test
    public void testNotificationOperations() throws Exception {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(AVAILABILITY_ZONE).withMaxSize(2).withMinSize(1);
        autoscaling.createAutoScalingGroup(createRequest);

        // Describe Notification Types
        List<String> notificationTypes = autoscaling.describeAutoScalingNotificationTypes(new DescribeAutoScalingNotificationTypesRequest())
                                                    .getAutoScalingNotificationTypes();
        assertTrue(notificationTypes.size() > 1);
        String notificationType = notificationTypes.get(0);
        assertNotEmpty(notificationType);

        // PutNotificationConfiguration
        topicARN = sns.createTopic(
                new CreateTopicRequest("java-sdk-autoscaling-integ-test-" + System.currentTimeMillis())).getTopicArn();
        PutNotificationConfigurationRequest putRequest = new PutNotificationConfigurationRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withNotificationTypes(notificationType)
                .withTopicARN(topicARN);
        autoscaling.putNotificationConfiguration(putRequest);

        // DescribeNotificationConfiguration
        DescribeNotificationConfigurationsRequest describeRequest = new DescribeNotificationConfigurationsRequest()
                .withAutoScalingGroupNames(autoScalingGroupName);
        List<NotificationConfiguration> notificationConfigurations = autoscaling.describeNotificationConfigurations(
                describeRequest).getNotificationConfigurations();
        assertEquals(1, notificationConfigurations.size());
        assertEquals(autoScalingGroupName, notificationConfigurations.get(0).getAutoScalingGroupName());
        assertEquals(notificationType, notificationConfigurations.get(0).getNotificationType());
        assertEquals(topicARN, notificationConfigurations.get(0).getTopicARN());

        // DeleteNotificationConfiguration
        autoscaling.deleteNotificationConfiguration(new DeleteNotificationConfigurationRequest()
                                                            .withAutoScalingGroupName(autoScalingGroupName)
                                                            .withTopicARN(topicARN));
        assertEquals(0, autoscaling.describeNotificationConfigurations(describeRequest).getNotificationConfigurations()
                                   .size());
    }


    /**
     * Tests that we can invoke the tagging operations.
     */

    @Test
    public void testTagging() {
        autoScalingGroupName = "java-integ-test-scaling-group-" + new Date().getTime();
        launchConfigurationName = "java-integ-test-launch-configuration-" + new Date().getTime();
        createLaunchConfiguration(launchConfigurationName);

        // Create an AutoScalingGroup
        CreateAutoScalingGroupRequest createRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(autoScalingGroupName).withLaunchConfigurationName(launchConfigurationName)
                .withAvailabilityZones(AVAILABILITY_ZONE).withMaxSize(2).withMinSize(1);
        autoscaling.createAutoScalingGroup(createRequest);

        Map<String, String> tags = new HashMap<String, String>();
        tags.put("tag1", "value1");
        tags.put("tag2", "value2");
        // The CreateOrUpdateTags API will auto-construct an empty value for the
        // tag, even when we don't provide the "Tags.member.n.Value" parameter at
        // all (which is the case when we set the tag value as null in the
        // request). So if we do `tags.put("tag3", null)`, the test would fail.
        tags.put("tag3", "");
        autoscaling.createOrUpdateTags(new CreateOrUpdateTagsRequest().withTags(convertTagList(tags,
                                                                                               autoScalingGroupName)));

        Filter filter = new Filter().withName("auto-scaling-group").withValues(autoScalingGroupName);

        DescribeTagsResult describeTags = autoscaling.describeTags(new DescribeTagsRequest().withFilters(filter));
        assertEquals(3, describeTags.getTags().size());
        for (TagDescription tag : describeTags.getTags()) {
            assertEquals(autoScalingGroupName, tag.getResourceId());
            assertEquals(tags.get(tag.getKey()), tag.getValue());
            assertTrue(tag.getPropagateAtLaunch());
        }

        // Now delete the tags
        autoscaling.deleteTags(new DeleteTagsRequest().withTags(convertTagList(tags, autoScalingGroupName)));

        describeTags = autoscaling.describeTags(new DescribeTagsRequest().withFilters(filter));
        assertEquals(0, describeTags.getTags().size());
    }

    @Test
    public void testDescribeTerminationPolicyTypes() {
        DescribeTerminationPolicyTypesResult describeAdjustmentTypesResult = autoscaling
                .describeTerminationPolicyTypes(new DescribeTerminationPolicyTypesRequest());
        assertEquals(TERMINATION_POLICIES.toArray(), describeAdjustmentTypesResult.getTerminationPolicyTypes()
                                                                                  .toArray());
    }

    private Collection<Tag> convertTagList(Map<String, String> tags, String groupName) {
        Collection<Tag> converted = new LinkedList<Tag>();
        for (String key : tags.keySet()) {
            Tag tag = new Tag().withKey(key).withValue(tags.get(key)).withResourceType("auto-scaling-group")
                               .withResourceId(groupName).withPropagateAtLaunch(true);
            converted.add(tag);
        }
        return converted;
    }


    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */

    @Test
    public void testClockSkewAs() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        AutoScalingClient clockSkewClient = AutoScalingClient.builder().credentialsProvider(new StaticCredentialsProvider(credentials)).build();
        clockSkewClient.describePolicies(new DescribePoliciesRequest());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() < 60);
    }
}

