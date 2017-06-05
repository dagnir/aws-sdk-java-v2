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

package software.amazon.awssdk.services.ec2;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CancelSpotInstanceRequestsRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResult;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResult;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputRequest;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputResult;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceAttribute;
import software.amazon.awssdk.services.ec2.model.InstanceAttributeName;
import software.amazon.awssdk.services.ec2.model.InstanceMonitoring;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.LaunchSpecification;
import software.amazon.awssdk.services.ec2.model.MonitorInstancesRequest;
import software.amazon.awssdk.services.ec2.model.MonitoringState;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.RebootInstancesRequest;
import software.amazon.awssdk.services.ec2.model.ReportInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResult;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.SpotInstanceRequest;
import software.amazon.awssdk.services.ec2.model.SpotInstanceType;
import software.amazon.awssdk.services.ec2.model.UnmonitorInstancesRequest;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesRequest;

/**
 * Integration tests for all E2 Instance operations.
 */
public class EC2InstancesIntegrationTest extends EC2IntegrationTestBase {

    private static final String US_EAST_1_A = "us-east-1a";
    private static final String USER_DATA = "foobarbazbar";
    private static final String INSTANCE_TYPE = InstanceType.T1Micro.toString();
    /** The name of an existing security group for tests to use. */
    private static String existingGroupName;
    /** The name of an existing key pair for tests to use. */
    private static String existingKeyPairName;
    /** The ARN of an existing instance profile for tests to use. */
    private static String existingInstanceProfileArn;
    /** Test EC2 instance for all tests to share. */
    private static Instance testInstance;
    /**
     * The reservation ID used in later tests, captured when launching our test
     * instance
     */
    private static String expectedReservationId;

    /**
     * Setup the shared instance for testing
     */
    @BeforeClass
    public static void setupInstance() throws InterruptedException {
        initializeTestData();
        testRunInstances();
    }

    /**
     * Ensures that all EC2 resources are correctly released after tests.
     */
    @AfterClass
    public static void tearDown() {
        if (testInstance != null) {
            terminateInstance(testInstance.instanceId());
        }
    }

    /**
     * Tests that the runInstances method is able to start up EC2 instances, and
     * that all the specified parameters are correctly sent to EC2.
     */
    private static void testRunInstances() throws InterruptedException {

        String idempotencyToken = UUID.randomUUID().toString();

        RunInstancesRequest request =
                RunInstancesRequest.builder()
                                   .minCount(1)
                                   .maxCount(1)
                                   .imageId(AMI_ID)
                                   .kernelId(KERNEL_ID)
                                   .ramdiskId(RAMDISK_ID)
                                   .instanceType(INSTANCE_TYPE)
                                   .blockDeviceMappings(BlockDeviceMapping.builder()
                                                                          .deviceName("/dev/sdb1")
                                                                          .virtualName("ephemeral2")
                                                                          .build())
                                   .placement(Placement.builder()
                                                       .availabilityZone(US_EAST_1_A).build())
                                   .monitoring(true)
                                   .userData(USER_DATA)
                                   .keyName(existingKeyPairName)
                                   .securityGroups(existingGroupName)
                                   .iamInstanceProfile(IamInstanceProfileSpecification.builder()
                                                                                      .arn(existingInstanceProfileArn)
                                                                                      .build())
                                   .clientToken(idempotencyToken).build();

        RunInstancesResult result = ec2.runInstances(request);

        // Capture the reservation ID
        expectedReservationId = result.reservation().reservationId();

        List<Instance> instances = result.reservation().instances();
        assertEquals(1, instances.size());
        testInstance = instances.get(0);

        // Wait for the instance to start up, so that we can test additional properties
        testInstance = waitForInstanceToTransitionToState(
                testInstance.instanceId(), InstanceStateName.Running);
        assertValidInstance(testInstance, AMI_ID, INSTANCE_TYPE, KERNEL_ID,
                            RAMDISK_ID, existingKeyPairName, existingInstanceProfileArn,
                            US_EAST_1_A, existingGroupName, InstanceStateName.Running);
        assertEquals(idempotencyToken, testInstance.clientToken());

        // Tag it
        tagResource(testInstance.instanceId(), TAGS);

        // Test Report Instance Status
        ec2.reportInstanceStatus(ReportInstanceStatusRequest.builder()
                                                            .instances(testInstance.instanceId())
                                                            .reasonCodes("other")
                                                            .status("ok").build());
    }

    /**
     * Asserts that the existing test data required by these tests (an existing
     * security group and key pair) is available and stores the name of an
     * existing security group and key pair for these tests to use.
     */
    private static void initializeTestData() {
        List<SecurityGroup> groups = ec2.describeSecurityGroups(DescribeSecurityGroupsRequest.builder().build()).securityGroups();
        assertThat("No existing security groups to test with", groups, not(empty()));
        existingGroupName = groups.get(0).groupName();

        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs(DescribeKeyPairsRequest.builder().build()).keyPairs();
        assertThat("No existing key pairs to test with", groups, not(empty()));
        existingKeyPairName = keyPairs.get(0).keyName();

        existingInstanceProfileArn = findValidInstanceProfile();
    }

    /**
     * Find a valid instance profile which has at least one role associated with it.
     */
    private static String findValidInstanceProfile() {
        IAMClient iam = IAMClient.builder().build();
        List<InstanceProfile> profiles = iam.listInstanceProfiles(ListInstanceProfilesRequest.builder().build())
                                            .instanceProfiles();
        for (InstanceProfile profile : profiles) {
            if (profile.roles() != null && profile.roles().size() > 0) {
                return profile.arn();
            }
        }
        Assert.fail("No valid instance profile to test with");
        return null;
    }

    /**
     * Converts a list of reservations to a map keyed by reservation ID.
     *
     * @param reservations
     *            The list of reservations to convert.
     * @return A map of the specified reservations, keyed by reservation ID.
     */
    private static Map<String, Reservation> convertReservationListToMap(
            List<Reservation> reservations) {
        Map<String, Reservation> reservationsById = new HashMap<>();
        for (Reservation reservation : reservations) {
            reservationsById.put(reservation.reservationId(), reservation);
        }

        return reservationsById;
    }

    /**
     * Asserts that the specified reservation has one security group with the
     * specified name and that the other fields in the reservation object aren't
     * empty.
     *
     * @param reservation
     *            The reservation to check.
     * @param expectedSecurityGroup
     *            The single, expected security group for the specified
     *            reservation.
     */
    private static void assertValidReservation(Reservation reservation, String expectedSecurityGroup) {
        assertNotNull(reservation);

        //        List<String> groupNames = reservation.groupNames();
        //        assertEquals(1, groupNames.size());
        //        assertEquals(expectedSecurityGroup, groupNames.get(0));
        assertTrue(reservation.ownerId().length() > 2);
        assertTrue(reservation.reservationId().length() > 2);
    }

    /**
     * Tests an EC2 instance against the specified expected values as well as
     * asserting that other fields in the instance are reasonable (ex: not
     * null).
     */
    private static void assertValidInstance(
            Instance instance,
            String expectedAmiId,
            String expectedInstanceType,
            String expectedKernelId,
            String expectedRamdiskId,
            String expectedKeyName,
            String expectedInstanceProfileArn,
            String expectedAvZone,
            String expectedSecurityGroup,
            InstanceStateName expectedState) {

        assertEquals(expectedAmiId, instance.imageId());
        assertEquals(expectedInstanceType, instance.instanceType());
        assertEquals(expectedKernelId, instance.kernelId());
        assertEquals(expectedRamdiskId, instance.ramdiskId());
        assertEquals(expectedKeyName, instance.keyName());
        assertEquals(expectedInstanceProfileArn, instance.iamInstanceProfile().arn());
        assertEquals(expectedAvZone, instance.placement().availabilityZone());
        assertEquals(expectedState.toString(), instance.state().name());

        List<GroupIdentifier> securityGroups = instance.securityGroups();
        assertEquals(1, securityGroups.size());
        assertEquals(expectedSecurityGroup, securityGroups.get(0).groupName());

        assertStringNotEmpty(instance.monitoring().state());
        assertRecent(instance.launchTime());
        assertStringNotEmpty(instance.instanceId());
        assertNotNull(instance.state().code());
        assertStringNotEmpty(instance.state().name());
        assertStringNotEmpty(instance.iamInstanceProfile().id());

        /*
         * If the instance is running, we expect it to have an IP address,
         * public DNS name and private DNS name.
         */
        if (InstanceStateName.Running.equals(instance.state().name())) {
            assertStringNotEmpty(instance.publicIpAddress());
            assertStringNotEmpty(instance.privateIpAddress());
            assertStringNotEmpty(instance.publicDnsName());
            assertStringNotEmpty(instance.privateDnsName());
        }
    }

    /**
     * Tests the DescribeInstanceAttribute Request for fetching the security
     * groups of the given instance. Asserts that number of security groups in
     * the result must be non null and greater than ZERO.
     */
    @Test
    public void testDescribeInstanceAttribute() {

        InstanceAttribute instanceAttribute = ec2.describeInstanceAttribute(
                DescribeInstanceAttributeRequest.builder()
                                                .attribute(InstanceAttributeName.GroupSet)
                                                .instanceId(testInstance.instanceId()).build()
                                                                           ).instanceAttribute();

        assertNotNull(instanceAttribute);
        assertNotNull(instanceAttribute.groups());
        List<GroupIdentifier> groups = instanceAttribute.groups();
        assertTrue(groups.size() > 0);

        for (GroupIdentifier group : groups) {
            boolean groupIdOrNameExists = group.groupId() != null
                                          || group.groupName() != null;
            assertTrue(groupIdOrNameExists);
        }
    }

    /**
     * Tests that describe instance status returns valid values.
     */
    @Test
    public void testDescribeInstanceStatus() {

        DescribeInstanceStatusResult describeInstanceStatusResult =
                ec2.describeInstanceStatus(DescribeInstanceStatusRequest.builder().build());
        assertNotNull(describeInstanceStatusResult.instanceStatuses());
        assertFalse(describeInstanceStatusResult.instanceStatuses().isEmpty());

        InstanceStatus instanceStatus = describeInstanceStatusResult
                .instanceStatuses().get(0);
        assertNotNull(instanceStatus.availabilityZone());
        assertNotNull(instanceStatus.instanceId());
        assertNotNull(instanceStatus.instanceState().code());
        assertNotNull(instanceStatus.instanceState().name());

        // Test filtering
        describeInstanceStatusResult = ec2.describeInstanceStatus(
                DescribeInstanceStatusRequest.builder()
                                             .instanceIds(testInstance.instanceId()).build()
                                                                 );
        assertEquals(1, describeInstanceStatusResult.instanceStatuses().size());
    }

    /**
     * Tests that the GetConsoleOutput operation returns data.
     */
    @Test
    public void testGetConsoleOutput() {

        /*
         * Console output isn't immediately available right after an instance
         * launches, so we need to try calling/waiting a few times in case
         * console output isn't available yet.
         */
        GetConsoleOutputResult result = null;

        for (int tries = 0; tries < 30; tries++) {
            result = ec2.getConsoleOutput(GetConsoleOutputRequest.builder()
                                                                 .instanceId(testInstance.instanceId()).build());

            if (result.output() != null && result.output().length() > 0) {
                break;
            }

            System.out.println("Wait 30 seconds for the console output to show up...");
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                // Ignored or expected.
            }
        }

        assertEquals(testInstance.instanceId(), result.instanceId());
        assertRecent(result.timestamp());
        Assert.assertThat(result.output(), Matchers.not(Matchers.isEmptyOrNullString()));
    }

    /**
     * Tests that the no-arg method form of DescribeInstances correctly returns
     * all running instances, including the one we started earlier.
     */
    @Test
    public void testDescribeInstances() {
        DescribeInstancesResult result = ec2.describeInstances(DescribeInstancesRequest.builder().build());

        Map<String, Reservation> reservationsById = convertReservationListToMap(result.reservations());
        Reservation reservation = reservationsById.get(expectedReservationId);
        assertValidReservation(reservation, existingGroupName);

        List<Instance> instances = reservation.instances();
        assertEquals(1, instances.size());
        assertValidInstance(instances.get(0), AMI_ID, INSTANCE_TYPE, KERNEL_ID,
                            RAMDISK_ID, existingKeyPairName, existingInstanceProfileArn,
                            US_EAST_1_A, existingGroupName, InstanceStateName.Running);
    }

    /**
     * Tests that the instances are correctly returned when we call
     * DescribeInstances with an explicit list of instances to describe.
     */
    @Test
    public void testDescribeInstancesById() {

        List<Reservation> reservations = ec2.describeInstances(
                DescribeInstancesRequest.builder()
                                        .instanceIds(testInstance.instanceId()).build()).reservations();

        assertEquals(1, reservations.size());
        Reservation reservation = reservations.get(0);
        assertValidReservation(reservation, existingGroupName);

        List<Instance> instances = reservation.instances();
        assertEquals(1, instances.size());
        assertValidInstance(instances.get(0), AMI_ID, INSTANCE_TYPE, KERNEL_ID,
                            RAMDISK_ID, existingKeyPairName, existingInstanceProfileArn,
                            US_EAST_1_A, existingGroupName, InstanceStateName.Running);

        assertEqualUnorderedTagLists(TAGS, instances.get(0).tags());
    }


    /* Private test utilities. */

    /**
     * Tests that the instances are correctly returned when we use a filter.
     */
    @Test
    public void testDescribeInstancesWithFilter() {

        List<Reservation> reservations = ec2.describeInstances(
                DescribeInstancesRequest.builder()
                                        .filters(Filter.builder()
                                                       .name("instance-id")
                                                       .values(testInstance.instanceId()).build()).build()
                                                              ).reservations();

        assertEquals(1, reservations.size());
        Reservation reservation = reservations.get(0);
        assertValidReservation(reservation, existingGroupName);

        List<Instance> instances = reservation.instances();
        assertEquals(1, instances.size());
        assertValidInstance(instances.get(0), AMI_ID, INSTANCE_TYPE, KERNEL_ID,
                            RAMDISK_ID, existingKeyPairName, existingInstanceProfileArn,
                            US_EAST_1_A, existingGroupName, InstanceStateName.Running);

        assertEqualUnorderedTagLists(TAGS, instances.get(0).tags());
    }

    /**
     * Tests that the EC2 client is able to call the EC2 RebootInstances method
     * correctly.
     */
    @Test
    public void testRebootInstances() {

        ec2.rebootInstances(RebootInstancesRequest.builder()
                                                  .instanceIds(testInstance.instanceId()).build());
        /*
         * There's not an easy way to check that an instance is rebooting, so we
         * assume that if we send the message without getting an exception (ex:
         * for not sending any instance IDs) then EC2 correctly rebooted the
         * instance.
         */
    }

    /**
     * Tests that the MonitorInstances operation correctly enables monitoring
     * for out test instance.
     */
    @Test
    public void testMonitorInstances() {

        List<InstanceMonitoring> monitorings = ec2.monitorInstances(
                MonitorInstancesRequest.builder()
                                       .instanceIds(testInstance.instanceId()).build()
                                                                   ).instanceMonitorings();

        assertEquals(1, monitorings.size());
        assertEquals(testInstance.instanceId(), monitorings.get(0).instanceId());

        String monitoringState = monitorings.get(0).monitoring().state();
        assertTrue(MonitoringState.Pending.toString().equals(monitoringState)
                   || MonitoringState.Enabled.toString().equals(monitoringState));
    }

    /**
     * Tests that the UnmonitorInstances operation correctly disables monitoring
     * for our test instance.
     */
    @Test
    public void testUnmonitorInstances() {

        List<InstanceMonitoring> monitorings = ec2.unmonitorInstances(
                UnmonitorInstancesRequest.builder()
                                         .instanceIds(testInstance.instanceId()).build()
                                                                     ).instanceMonitorings();

        assertEquals(1, monitorings.size());
        assertEquals(testInstance.instanceId(), monitorings.get(0).instanceId());

        String monitoringState = monitorings.get(0).monitoring().state();
        assertTrue(MonitoringState.Disabled.toString().equals(monitoringState)
                   // apparently "disabling" is not yet included in MonitoringState enum
                   || "disabling".equals(monitoringState));
    }

    /**
     * Tests the Public IP in VPC feature.
     */
    @Test
    public void testPublicIpInNonDefaultVpc() throws InterruptedException {

        String instanceId = null;
        String vpcId = null;
        String subnetId = null;
        String spotInstanceRequestId = null;
        String imageId = AMI_ID;

        try {
            // Create a new VPC
            vpcId = ec2.createVpc(CreateVpcRequest.builder()
                                                  .cidrBlock("192.0.0.0/16").build()
                                 ).vpc().vpcId();

            // Create a new Subnet inside the VPC
            subnetId = ec2.createSubnet(CreateSubnetRequest.builder()
                                                           .cidrBlock("192.0.2.0/24")
                                                           .vpcId(vpcId)
                                                           .availabilityZone(US_EAST_1_A).build()
                                       ).subnet().subnetId();

            // Create a new instance inside the subnet
            List<Instance> instances = ec2.runInstances(
                    RunInstancesRequest.builder()
                                       .imageId(imageId)
                                       .minCount(1)
                                       .maxCount(1)
                                       .instanceType(InstanceType.T1Micro)
                                       .monitoring(false)
                                       .networkInterfaces(InstanceNetworkInterfaceSpecification.builder()
                                                                                               .deviceIndex(0)
                                                                                               .associatePublicIpAddress(true)
                                                                                               .subnetId(subnetId).build()
                                                         ).build()
                                                       ).reservation().instances();

            assertEquals(1, instances.size());
            instanceId = instances.get(0).instanceId();

            // Wait for the instance to start up, so that we can check its public IP
            Instance instance = waitForInstanceToTransitionToState(
                    instanceId, InstanceStateName.Running);

            String publicIp = instance.publicIpAddress();
            assertTrue(publicIp != null &&
                       publicIp.trim().length() != 0);

            // Test spot instances with Public IP
            spotInstanceRequestId = ec2.requestSpotInstances(
                    RequestSpotInstancesRequest.builder()
                                               .spotPrice("0.05")
                                               .type(SpotInstanceType.OneTime)
                                               .launchSpecification(LaunchSpecification.builder()
                                                                                       .imageId(imageId)
                                                                                       .instanceType(InstanceType.T1Micro)
                                                                                       .networkInterfaces(
                                                                                               InstanceNetworkInterfaceSpecification
                                                                                                       .builder()
                                                                                                       .deviceIndex(0)
                                                                                                       .associatePublicIpAddress(true)
                                                                                                       .subnetId(subnetId).build()
                                                                                                         ).build()
                                                                   ).build()
                                                            ).spotInstanceRequests().get(0).spotInstanceRequestId();

            List<SpotInstanceRequest> spotRequests = ec2.describeSpotInstanceRequests(
                    DescribeSpotInstanceRequestsRequest.builder()
                                                       .spotInstanceRequestIds(spotInstanceRequestId).build()).spotInstanceRequests();
            assertEquals(1, spotRequests.size());
        } finally {
            if (spotInstanceRequestId != null) {
                ec2.cancelSpotInstanceRequests(CancelSpotInstanceRequestsRequest.builder()
                                                                                .spotInstanceRequestIds(spotInstanceRequestId).build());
            }
            if (instanceId != null) {
                terminateInstance(instanceId);
                waitForInstanceToTransitionToState(
                        instanceId, InstanceStateName.Terminated);
            }
            if (subnetId != null) {
                ec2.deleteSubnet(DeleteSubnetRequest.builder().subnetId(subnetId).build());
                System.out.println("Wait 10 seconds for subnet " + subnetId + " to be fully deleted...");
                Thread.sleep(10 * 1000);
            }
            if (vpcId != null) {
                ec2.deleteVpc(DeleteVpcRequest.builder().vpcId(vpcId).build());
            }
        }
    }
}
