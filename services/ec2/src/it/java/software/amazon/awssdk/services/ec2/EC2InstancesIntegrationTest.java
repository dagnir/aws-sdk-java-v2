/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagement;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.services.identitymanagement.model.InstanceProfile;

/**
 * Integration tests for all E2 Instance operations.
 */
public class EC2InstancesIntegrationTest extends EC2IntegrationTestBase {

    /** The name of an existing security group for tests to use */
    private static String existingGroupName;

    /** The name of an existing key pair for tests to use */
    private static String existingKeyPairName;

    /** The ARN of an existing instance profile for tests to use */
    private static String existingInstanceProfileArn;

    /** Test EC2 instance for all tests to share */
    private static Instance testInstance;

    /**
     * The reservation ID used in later tests, captured when launching our test
     * instance
     */
    private static String expectedReservationId;

    private static final String US_EAST_1_A = "us-east-1a";
    private static final String USER_DATA = "foobarbazbar";
    private static final String INSTANCE_TYPE = InstanceType.T1Micro.toString();

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
            terminateInstance(testInstance.getInstanceId());
        }
    }

    /**
     * Tests that the runInstances method is able to start up EC2 instances, and
     * that all the specified parameters are correctly sent to EC2.
     */
    private static void testRunInstances() throws InterruptedException {

        String idempotencyToken = UUID.randomUUID().toString();

        RunInstancesRequest request = new RunInstancesRequest()
                .withMinCount(1)
                .withMaxCount(1)
                .withImageId(AMI_ID)
                .withKernelId(KERNEL_ID)
                .withRamdiskId(RAMDISK_ID)
                .withInstanceType(INSTANCE_TYPE)
                .withBlockDeviceMappings(new BlockDeviceMapping()
                        .withDeviceName("/dev/sdb1")
                        .withVirtualName("ephemeral2"))
                .withPlacement(new Placement()
                        .withAvailabilityZone(US_EAST_1_A))
                .withMonitoring(true)
                .withUserData(USER_DATA)
                .withKeyName(existingKeyPairName)
                .withSecurityGroups(existingGroupName)
                .withIamInstanceProfile(new IamInstanceProfileSpecification()
                        .withArn(existingInstanceProfileArn))
                .withClientToken(idempotencyToken);

        RunInstancesResult result = ec2.runInstances(request);

        // Capture the reservation ID
        expectedReservationId = result.getReservation().getReservationId();

        List<Instance> instances = result.getReservation().getInstances();
        assertEquals(1, instances.size());
        testInstance = instances.get(0);

        // Wait for the instance to start up, so that we can test additional properties
        testInstance = waitForInstanceToTransitionToState(
                testInstance.getInstanceId(), InstanceStateName.Running);
        assertValidInstance(testInstance, AMI_ID, INSTANCE_TYPE, KERNEL_ID,
                RAMDISK_ID, existingKeyPairName, existingInstanceProfileArn,
                US_EAST_1_A, existingGroupName, InstanceStateName.Running);
        assertEquals(idempotencyToken, testInstance.getClientToken());

        // Tag it
        tagResource(testInstance.getInstanceId(), TAGS);

        // Test Report Instance Status
        ec2.reportInstanceStatus(new ReportInstanceStatusRequest()
                .withInstances(testInstance.getInstanceId())
                .withReasonCodes("other")
                .withStatus("ok")
                );
    }

    /**
     * Tests the DescribeInstanceAttribute Request for fetching the security
     * groups of the given instance. Asserts that number of security groups in
     * the result must be non null and greater than ZERO.
     */
    @Test
    public void testDescribeInstanceAttribute() {

        InstanceAttribute instanceAttribute = ec2.describeInstanceAttribute(
                new DescribeInstanceAttributeRequest()
                        .withAttribute(InstanceAttributeName.GroupSet)
                        .withInstanceId(testInstance.getInstanceId())
                ).getInstanceAttribute();

        assertNotNull(instanceAttribute);
        assertNotNull(instanceAttribute.getGroups());
        List<GroupIdentifier> groups = instanceAttribute.getGroups();
        assertTrue(groups.size() > 0);

        for (GroupIdentifier group : groups) {
            boolean groupIdOrNameExists = group.getGroupId() != null
                    || group.getGroupName() != null;
            assertTrue(groupIdOrNameExists);
        }
    }

    /**
     * Tests that describe instance status returns valid values.
     */
    @Test
    public void testDescribeInstanceStatus() {

        DescribeInstanceStatusResult describeInstanceStatusResult =
                ec2.describeInstanceStatus();
        assertNotNull(describeInstanceStatusResult.getInstanceStatuses());
        assertFalse(describeInstanceStatusResult.getInstanceStatuses().isEmpty());

        InstanceStatus instanceStatus = describeInstanceStatusResult
                .getInstanceStatuses().get(0);
        assertNotNull(instanceStatus.getAvailabilityZone());
        assertNotNull(instanceStatus.getInstanceId());
        assertNotNull(instanceStatus.getInstanceState().getCode());
        assertNotNull(instanceStatus.getInstanceState().getName());

        // Test filtering
        describeInstanceStatusResult = ec2.describeInstanceStatus(
                new DescribeInstanceStatusRequest()
                        .withInstanceIds(testInstance.getInstanceId())
                );
        assertEquals(1, describeInstanceStatusResult.getInstanceStatuses().size());
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
            result = ec2.getConsoleOutput(new GetConsoleOutputRequest()
                    .withInstanceId(testInstance.getInstanceId()));

            if (result.getOutput() != null && result.getOutput().length() > 0) {
                break;
            }

            System.out.println("Wait 30 seconds for the console output to show up...");
            try {Thread.sleep(30 * 1000); } catch (InterruptedException e) {}
        }

        assertEquals(testInstance.getInstanceId(), result.getInstanceId());
        assertRecent(result.getTimestamp());
        Assert.assertThat(result.getOutput(), Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(result.getDecodedOutput(), Matchers.not(Matchers
                .isEmptyOrNullString()));
    }

    /**
     * Tests that the no-arg method form of DescribeInstances correctly returns
     * all running instances, including the one we started earlier.
     */
    @Test
    public void testDescribeInstances() {
        DescribeInstancesResult result = ec2.describeInstances();

        Map<String, Reservation> reservationsById = convertReservationListToMap(result.getReservations());
        Reservation reservation = reservationsById.get(expectedReservationId);
        assertValidReservation(reservation, existingGroupName);

        List<Instance> instances = reservation.getInstances();
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
                new DescribeInstancesRequest()
                        .withInstanceIds(testInstance.getInstanceId())
                ).getReservations();

        assertEquals(1, reservations.size());
        Reservation reservation = reservations.get(0);
        assertValidReservation(reservation, existingGroupName);

        List<Instance> instances = reservation.getInstances();
        assertEquals(1, instances.size());
        assertValidInstance(instances.get(0), AMI_ID, INSTANCE_TYPE, KERNEL_ID,
                RAMDISK_ID, existingKeyPairName, existingInstanceProfileArn,
                US_EAST_1_A, existingGroupName, InstanceStateName.Running);

        assertEqualUnorderedTagLists(TAGS, instances.get(0).getTags());
    }

    /**
     * Tests that the instances are correctly returned when we use a filter.
     */
    @Test
    public void testDescribeInstancesWithFilter() {

        List<Reservation> reservations = ec2.describeInstances(
                new DescribeInstancesRequest()
                        .withFilters(new Filter()
                                .withName("instance-id")
                                .withValues(testInstance.getInstanceId()))
                ).getReservations();

        assertEquals(1, reservations.size());
        Reservation reservation = reservations.get(0);
        assertValidReservation(reservation, existingGroupName);

        List<Instance> instances = reservation.getInstances();
        assertEquals(1, instances.size());
        assertValidInstance(instances.get(0), AMI_ID, INSTANCE_TYPE, KERNEL_ID,
                RAMDISK_ID, existingKeyPairName, existingInstanceProfileArn,
                US_EAST_1_A, existingGroupName, InstanceStateName.Running);

        assertEqualUnorderedTagLists(TAGS, instances.get(0).getTags());
    }

    /**
     * Tests that the EC2 client is able to call the EC2 RebootInstances method
     * correctly.
     */
    @Test
    public void testRebootInstances() {

        ec2.rebootInstances(new RebootInstancesRequest()
                .withInstanceIds(testInstance.getInstanceId()));
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
                new MonitorInstancesRequest()
                        .withInstanceIds(testInstance.getInstanceId())
                ).getInstanceMonitorings();

        assertEquals(1, monitorings.size());
        assertEquals(testInstance.getInstanceId(), monitorings.get(0).getInstanceId());

        String monitoringState = monitorings.get(0).getMonitoring().getState();
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
                new UnmonitorInstancesRequest()
                        .withInstanceIds(testInstance.getInstanceId())
                ).getInstanceMonitorings();

        assertEquals(1, monitorings.size());
        assertEquals(testInstance.getInstanceId(), monitorings.get(0).getInstanceId());

        String monitoringState = monitorings.get(0).getMonitoring().getState();
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
        String spotInstanceRequestId=null;
        String imageId = AMI_ID;

        try {
            // Create a new VPC
            vpcId = ec2.createVpc(new CreateVpcRequest()
                            .withCidrBlock("192.0.0.0/16")
                    ).getVpc().getVpcId();

            // Create a new Subnet inside the VPC
            subnetId = ec2.createSubnet(new CreateSubnetRequest()
                            .withCidrBlock("192.0.2.0/24")
                            .withVpcId(vpcId)
                            .withAvailabilityZone(US_EAST_1_A)
                    ).getSubnet().getSubnetId();

            // Create a new instance inside the subnet
            List<Instance> instances = ec2.runInstances(
                    new RunInstancesRequest()
                            .withImageId(imageId)
                            .withMinCount(1)
                            .withMaxCount(1)
                            .withInstanceType(InstanceType.T1Micro)
                            .withMonitoring(false)
                            .withNetworkInterfaces(new InstanceNetworkInterfaceSpecification()
                                    .withDeviceIndex(0)
                                    .withAssociatePublicIpAddress(true)
                                    .withSubnetId(subnetId)
                            )
                    ).getReservation().getInstances();

            assertEquals(1, instances.size());
            instanceId = instances.get(0).getInstanceId();

            // Wait for the instance to start up, so that we can check its public IP
            Instance instance = waitForInstanceToTransitionToState(
                    instanceId, InstanceStateName.Running);

            String publicIp = instance.getPublicIpAddress();
            assertTrue(publicIp != null &&
                       publicIp.trim().length() != 0);

            // Test spot instances with Public IP
            spotInstanceRequestId = ec2.requestSpotInstances(
                    new RequestSpotInstancesRequest()
                            .withSpotPrice("0.05")
                            .withType(SpotInstanceType.OneTime)
                            .withLaunchSpecification(new LaunchSpecification()
                                    .withImageId(imageId)
                                    .withInstanceType(InstanceType.T1Micro)
                                    .withNetworkInterfaces(new InstanceNetworkInterfaceSpecification()
                                            .withDeviceIndex(0)
                                            .withAssociatePublicIpAddress(true)
                                            .withSubnetId(subnetId)
                                    )
                            )
                    ).getSpotInstanceRequests().get(0).getSpotInstanceRequestId();

            List<SpotInstanceRequest> spotRequests = ec2.describeSpotInstanceRequests(
                    new DescribeSpotInstanceRequestsRequest()
                            .withSpotInstanceRequestIds(spotInstanceRequestId)
                    ).getSpotInstanceRequests();
            assertEquals(1 ,spotRequests.size());
        }

        finally {
            if (spotInstanceRequestId != null) {
                ec2.cancelSpotInstanceRequests(new CancelSpotInstanceRequestsRequest()
                        .withSpotInstanceRequestIds(spotInstanceRequestId));
            }
            if (instanceId != null) {
                terminateInstance(instanceId);
                waitForInstanceToTransitionToState(
                        instanceId, InstanceStateName.Terminated);
            }
            if (subnetId != null) {
                ec2.deleteSubnet(new DeleteSubnetRequest(subnetId));
                System.out.println("Wait 10 seconds for subnet " + subnetId + " to be fully deleted...");
                Thread.sleep(10 * 1000);
            }
            if (vpcId != null) {
                ec2.deleteVpc(new DeleteVpcRequest(vpcId));
            }
        }
    }


    /* Private test utilities */

    /**
     * Asserts that the existing test data required by these tests (an existing
     * security group and key pair) is available and stores the name of an
     * existing security group and key pair for these tests to use.
     */
    private static void initializeTestData() {
        List<SecurityGroup> groups = ec2.describeSecurityGroups().getSecurityGroups();
        assertThat("No existing security groups to test with", groups, not(empty()));
        existingGroupName = groups.get(0).getGroupName();

        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs().getKeyPairs();
        assertThat("No existing key pairs to test with", groups, not(empty()));
        existingKeyPairName = keyPairs.get(0).getKeyName();

        existingInstanceProfileArn = findValidInstanceProfile();
    }

    /**
     * Find a valid instance profile which has at least one role associated with it.
     */
    private static String findValidInstanceProfile() {
        AmazonIdentityManagement iam = new AmazonIdentityManagementClient();
        List<InstanceProfile> profiles = iam.listInstanceProfiles().getInstanceProfiles();
        for (InstanceProfile profile : profiles) {
            if (profile.getRoles() != null && profile.getRoles().size() > 0) {
                return profile.getArn();
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
        Map<String, Reservation> reservationsById = new HashMap<String, Reservation>();
        for (Reservation reservation : reservations) {
            reservationsById.put(reservation.getReservationId(), reservation);
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

//        List<String> groupNames = reservation.getGroupNames();
//        assertEquals(1, groupNames.size());
//        assertEquals(expectedSecurityGroup, groupNames.get(0));
        assertTrue(reservation.getOwnerId().length() > 2);
        assertTrue(reservation.getReservationId().length() > 2);
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

        assertEquals(expectedAmiId,              instance.getImageId());
        assertEquals(expectedInstanceType,       instance.getInstanceType());
        assertEquals(expectedKernelId,           instance.getKernelId());
        assertEquals(expectedRamdiskId,          instance.getRamdiskId());
        assertEquals(expectedKeyName,            instance.getKeyName());
        assertEquals(expectedInstanceProfileArn, instance.getIamInstanceProfile().getArn());
        assertEquals(expectedAvZone,             instance.getPlacement().getAvailabilityZone());
        assertEquals(expectedState.toString(),   instance.getState().getName());

        List<GroupIdentifier> securityGroups = instance.getSecurityGroups();
        assertEquals(1, securityGroups.size());
        assertEquals(expectedSecurityGroup, securityGroups.get(0).getGroupName());

        assertStringNotEmpty(instance.getMonitoring().getState());
        assertRecent(instance.getLaunchTime());
        assertStringNotEmpty(instance.getInstanceId());
        assertNotNull(instance.getState().getCode());
        assertStringNotEmpty(instance.getState().getName());
        assertStringNotEmpty(instance.getIamInstanceProfile().getId());

        /*
         * If the instance is running, we expect it to have an IP address,
         * public DNS name and private DNS name.
         */
        if (InstanceStateName.Running.equals(instance.getState().getName())) {
            assertStringNotEmpty(instance.getPublicIpAddress());
            assertStringNotEmpty(instance.getPrivateIpAddress());
            assertStringNotEmpty(instance.getPublicDnsName());
            assertStringNotEmpty(instance.getPrivateDnsName());
        }
    }
}
