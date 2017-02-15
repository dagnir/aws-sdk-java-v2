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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CreateImageRequest;
import software.amazon.awssdk.services.ec2.model.DeregisterImageRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceAttribute;
import software.amazon.awssdk.services.ec2.model.InstanceAttributeName;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceStateChange;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ResetInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

/**
 * Integration tests for the EC2 Boot from EBS API additions in the 2009-10-31
 * API version.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class BootFromEbsIntegrationTest extends EC2IntegrationTestBase {

    /**
     * The public EBS-backed AMI launched in these tests
     *
     * AMI ID: ami-84db39ed
     * AMI Name: fedora-8-i386-v1.14-std
     * Description: Fedora 8 v1.14 i386 std-root lvm-swap lvm-storage
     * Platform: Fedora
     * Root Device Type: ebs
     */
    private static final String EBS_AMI_ID = "ami-84db39ed";

    /** The id of the instance started by these tests */
    private static String instanceId;

    @BeforeClass
    public static void startInstance() throws InterruptedException {

        Instance instance = ec2.runInstances(
                new RunInstancesRequest()
                        .withImageId(EBS_AMI_ID)
                        .withMaxCount(1)
                        .withMinCount(1)
                        .withUserData("foobarbazbar")
                        .withDisableApiTermination(false)
                        .withInstanceInitiatedShutdownBehavior("terminate")
                                            ).getReservation().getInstances().get(0);
        instanceId = instance.getInstanceId();

        waitForInstanceToTransitionToState(
                instanceId, InstanceStateName.Running);
    }

    /** Releases all resources allocated by these tests */
    @AfterClass
    public static void tearDown() throws Exception {
        if (instanceId != null) {
            terminateInstance(instanceId);
        }
    }

    /**
     * Tests that we can describe our EBS-backed instances, including EBS
     * specific data such as instance block device mappings, root device info,
     * etc.
     */
    @Test
    public void testDescribeInstances() throws Exception {

        List<Instance> instances = ec2.describeInstances(
                new DescribeInstancesRequest()
                        .withInstanceIds(instanceId)
                                                        ).getReservations().get(0).getInstances();

        assertEquals(1, instances.size());
        assertThat(instances.get(0).getRootDeviceName(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertThat(instances.get(0).getRootDeviceName(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertThat(instances.get(0).getRootDeviceType(), Matchers.not(Matchers.isEmptyOrNullString()));

        List<InstanceBlockDeviceMapping> deviceMappings =
                instances.get(0).getBlockDeviceMappings();
        for (InstanceBlockDeviceMapping deviceMapping : deviceMappings) {
            assertThat(deviceMapping.getDeviceName(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(deviceMapping.getEbs().getVolumeId(), Matchers.not(Matchers.isEmptyOrNullString()));
        }
    }

    /**
     * Tests that we can describe an instance's attributes.
     */
    @Test
    public void testDescribeInstanceAttributes() throws Exception {

        // kernel
        InstanceAttribute attribute = getInstanceAttribute(instanceId, InstanceAttributeName.Kernel);
        assertEquals(instanceId, attribute.getInstanceId());
        assertThat(attribute.getKernelId(), Matchers.not(Matchers.isEmptyOrNullString()));

        // instanceType
        attribute = getInstanceAttribute(instanceId, InstanceAttributeName.InstanceType);
        assertEquals(instanceId, attribute.getInstanceId());
        assertThat(attribute.getInstanceType(), Matchers.not(Matchers.isEmptyOrNullString()));

        // ramdisk
        attribute = getInstanceAttribute(instanceId, InstanceAttributeName.Ramdisk);
        assertEquals(instanceId, attribute.getInstanceId());
        assertThat(attribute.getRamdiskId(), Matchers.not(Matchers.isEmptyOrNullString()));

        // userData
        attribute = getInstanceAttribute(instanceId, InstanceAttributeName.UserData);
        assertEquals(instanceId, attribute.getInstanceId());
        assertThat(attribute.getUserData(), Matchers.not(Matchers.isEmptyOrNullString()));

        // disableApiTermination
        attribute = getInstanceAttribute(instanceId, InstanceAttributeName.DisableApiTermination);
        assertEquals(instanceId, attribute.getInstanceId());
        assertFalse(attribute.isDisableApiTermination());

        // instanceInitiatedShutdownBehavior
        attribute = getInstanceAttribute(instanceId, InstanceAttributeName.InstanceInitiatedShutdownBehavior);
        assertEquals(instanceId, attribute.getInstanceId());
        assertThat(attribute.getInstanceInitiatedShutdownBehavior(), Matchers.not(Matchers.isEmptyOrNullString()));

        // rootDeviceName
        attribute = getInstanceAttribute(instanceId, InstanceAttributeName.RootDeviceName);
        assertEquals(instanceId, attribute.getInstanceId());
        assertThat(attribute.getRootDeviceName(), Matchers.not(Matchers.isEmptyOrNullString()));

        // blockDeviceMapping
        attribute = getInstanceAttribute(instanceId, InstanceAttributeName.BlockDeviceMapping);
        assertEquals(instanceId, attribute.getInstanceId());
        InstanceBlockDeviceMapping ibdm = attribute.getBlockDeviceMappings().get(0);

        assertThat(ibdm.getDeviceName(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertNotNull(ibdm.getEbs().getAttachTime());
        assertThat(ibdm.getEbs().getStatus(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertThat(ibdm.getEbs().getVolumeId(), Matchers.not(Matchers.isEmptyOrNullString()));
    }

    /**
     * Tests that we can correctly describe an instance with an EBS block device
     * mapping.
     */
    @Test
    public void testDescribeImageWithEbsBlockDeviceMapping() throws Exception {
        Image image = ec2.describeImages(
                new DescribeImagesRequest()
                        .withImageIds(EBS_AMI_ID)
                                        ).getImages().get(0);

        assertThat(image.getName(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertThat(image.getRootDeviceName(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertThat(image.getRootDeviceType(), Matchers.not(Matchers.isEmptyOrNullString()));

        for (BlockDeviceMapping deviceMapping : image.getBlockDeviceMappings()) {
            assertThat(deviceMapping.getDeviceName(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertThat(deviceMapping.getEbs().getSnapshotId(), Matchers.not(Matchers.isEmptyOrNullString()));
            assertTrue(deviceMapping.getEbs().getVolumeSize() > 0);
            assertNotNull(deviceMapping.getEbs().isDeleteOnTermination());
        }
    }

    /**
     * Tests that we can call CreateImage for an EBS-backed instance to create a
     * new image.
     */
    @Test
    public void testCreateImage() throws Exception {

        String imageId = ec2.createImage(
                new CreateImageRequest()
                        .withDescription("foo description")
                        .withInstanceId(instanceId)
                        .withName("foo AMI name " + new Date().getTime())
                        .withNoReboot(false)
                                        ).getImageId();
        assertThat(imageId, Matchers.not(Matchers.isEmptyOrNullString()));

        // Wait a few seconds to make sure we can correctly deregister the new
        // image.
        Thread.sleep(1000 * 10);
        ec2.deregisterImage(new DeregisterImageRequest()
                                    .withImageId(imageId));
    }

    /**
     * Tests that we can modify and reset instance attributes for our EBS-backed
     * instances.
     */
    @Test
    public void testModifyAndResetInstanceAttributes() throws Exception {

        ec2.modifyInstanceAttribute(
                new ModifyInstanceAttributeRequest()
                        .withInstanceId(instanceId)
                        .withAttribute(InstanceAttributeName.DisableApiTermination)
                        .withValue("false")
                                   );

        // The instance needs to be in "stopped" state before its "kernal"
        // attribute is reset.
        stopInstance(instanceId);

        ec2.resetInstanceAttribute(new ResetInstanceAttributeRequest()
                                           .withInstanceId(instanceId)
                                           .withAttribute(InstanceAttributeName.Kernel)
                                  );
    }

    /*
     * Private Interface
     */

    /**
     * Stop the given instance
     */
    private void stopInstance(String instanceId) throws Exception {

        List<InstanceStateChange> stoppingInstances =
                ec2.stopInstances(new StopInstancesRequest()
                                          .withInstanceIds(instanceId)
                                 ).getStoppingInstances();

        assertEquals(1, stoppingInstances.size());
        assertEquals(instanceId, stoppingInstances.get(0).getInstanceId());
        assertThat(stoppingInstances.get(0).getCurrentState().getName(), Matchers.not(Matchers.isEmptyOrNullString()));
        assertThat(stoppingInstances.get(0).getPreviousState().getName(), Matchers.not(Matchers.isEmptyOrNullString()));

        waitForInstanceToTransitionToState(
                instanceId, InstanceStateName.Stopped);
    }

    private InstanceAttribute getInstanceAttribute(
            String instanceId, InstanceAttributeName attrName) throws Exception {
        return getInstanceAttribute(instanceId, attrName.toString());
    }

    /**
     * Returns the specified attribute for the specified instance.
     *
     * @param instanceId
     *            The ID of the instance whose attribute is desired.
     * @param attribute
     *            The name of the attribute to retrieve.
     *
     * @return The specified attribute for the specified instance.
     *
     * @throws Exception
     *             If any problems were encountering retrieving the attribute.
     */
    private InstanceAttribute getInstanceAttribute(
            String instanceId, String attribute) throws Exception {
        return ec2.describeInstanceAttribute(
                new DescribeInstanceAttributeRequest()
                        .withInstanceId(instanceId)
                        .withAttribute(attribute)
                                            ).getInstanceAttribute();
    }

}
