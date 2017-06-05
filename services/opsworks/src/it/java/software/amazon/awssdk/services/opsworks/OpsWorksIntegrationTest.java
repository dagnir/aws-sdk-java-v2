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

package software.amazon.awssdk.services.opsworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import software.amazon.awssdk.services.opsworks.model.AppType;
import software.amazon.awssdk.services.opsworks.model.AssignVolumeRequest;
import software.amazon.awssdk.services.opsworks.model.AssociateElasticIpRequest;
import software.amazon.awssdk.services.opsworks.model.AttachElasticLoadBalancerRequest;
import software.amazon.awssdk.services.opsworks.model.CreateAppRequest;
import software.amazon.awssdk.services.opsworks.model.CreateAppResult;
import software.amazon.awssdk.services.opsworks.model.CreateDeploymentRequest;
import software.amazon.awssdk.services.opsworks.model.CreateDeploymentResult;
import software.amazon.awssdk.services.opsworks.model.CreateInstanceRequest;
import software.amazon.awssdk.services.opsworks.model.CreateInstanceResult;
import software.amazon.awssdk.services.opsworks.model.CreateLayerRequest;
import software.amazon.awssdk.services.opsworks.model.CreateLayerResult;
import software.amazon.awssdk.services.opsworks.model.CreateStackRequest;
import software.amazon.awssdk.services.opsworks.model.CreateStackResult;
import software.amazon.awssdk.services.opsworks.model.CreateUserProfileRequest;
import software.amazon.awssdk.services.opsworks.model.CreateUserProfileResult;
import software.amazon.awssdk.services.opsworks.model.DeleteAppRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteInstanceRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteLayerRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteStackRequest;
import software.amazon.awssdk.services.opsworks.model.DeleteUserProfileRequest;
import software.amazon.awssdk.services.opsworks.model.DeploymentCommand;
import software.amazon.awssdk.services.opsworks.model.DeploymentCommandName;
import software.amazon.awssdk.services.opsworks.model.DeregisterElasticIpRequest;
import software.amazon.awssdk.services.opsworks.model.DeregisterVolumeRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeAppsRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeAppsResult;
import software.amazon.awssdk.services.opsworks.model.DescribeDeploymentsRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeDeploymentsResult;
import software.amazon.awssdk.services.opsworks.model.DescribeElasticLoadBalancersRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeElasticLoadBalancersResult;
import software.amazon.awssdk.services.opsworks.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeInstancesResult;
import software.amazon.awssdk.services.opsworks.model.DescribeLayersRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeLayersResult;
import software.amazon.awssdk.services.opsworks.model.DescribeStacksRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeStacksResult;
import software.amazon.awssdk.services.opsworks.model.DescribeUserProfilesRequest;
import software.amazon.awssdk.services.opsworks.model.DescribeUserProfilesResult;
import software.amazon.awssdk.services.opsworks.model.DetachElasticLoadBalancerRequest;
import software.amazon.awssdk.services.opsworks.model.DisassociateElasticIpRequest;
import software.amazon.awssdk.services.opsworks.model.LayerAttributesKeys;
import software.amazon.awssdk.services.opsworks.model.LayerType;
import software.amazon.awssdk.services.opsworks.model.RegisterElasticIpRequest;
import software.amazon.awssdk.services.opsworks.model.RegisterElasticIpResult;
import software.amazon.awssdk.services.opsworks.model.RegisterVolumeRequest;
import software.amazon.awssdk.services.opsworks.model.RegisterVolumeResult;
import software.amazon.awssdk.services.opsworks.model.StackAttributesKeys;
import software.amazon.awssdk.services.opsworks.model.StackConfigurationManager;
import software.amazon.awssdk.services.opsworks.model.UnassignVolumeRequest;
import software.amazon.awssdk.services.opsworks.model.UpdateElasticIpRequest;
import software.amazon.awssdk.services.opsworks.model.UpdateInstanceRequest;
import software.amazon.awssdk.services.opsworks.model.UpdateLayerRequest;
import software.amazon.awssdk.services.opsworks.model.UpdateMyUserProfileRequest;
import software.amazon.awssdk.services.opsworks.model.UpdateStackRequest;
import software.amazon.awssdk.services.opsworks.model.UpdateUserProfileRequest;
import software.amazon.awssdk.services.opsworks.model.UpdateVolumeRequest;

public class OpsWorksIntegrationTest extends IntegrationTestBase {

    private static String stackId = null;
    private static String registeredVolumeId = null;
    private static String layerId = null;
    private static String deploymentId = null;
    private static String instanceId = null;
    private static String appId = null;
    /** Configurations for the new stack to be created. **/
    private final String STACK_NAME = "java-stack" + System.currentTimeMillis();
    private final String STACK_REGION = "us-east-1";
    private final String STACK_CONFIG_MANAGER_NAME = "Chef";
    private final String STACK_CONFIG_MANAGER_VERSION = "11.4";
    private final String IAM_ROLE = "arn:aws:iam::599169622985:role/aws-opsworks-service-role";
    private final String DEFAULT_IAM_INSTANCE_PROFILE =
            "arn:aws:iam::599169622985:instance-profile/aws-opsworks-ec2-role.1381193218933";
    private final String SSH_PUBLIC_KEY = "public_key";
    private final String INSTANCE_TYPE = "m1.medium";
    private final String USER_NAME = "user1";
    private final String NEW_INSTANCE_TYPE = "m1.small";
    private final String AMI_ID = "ami-0174e368";
    private final String LAYER_NAME = "my-layer";
    private final String NEW_LAYER_NAME = "new-layer";
    private final String STACK_ATTRIBUTE_KEY = StackAttributesKeys.Color.toString();
    private final String LAYER_ATTRIBUTE_KEY = LayerAttributesKeys.RubyVersion.toString();
    private final String LAYER_ATTRIBUTE_VALUE = "1.8.7";
    private final String STACK_ATTRIBUTE_VALUE = "rgb(135, 61, 98)";
    private final String LAYER_TYPE = LayerType.RailsApp.toString();
    private final String LAYER_SHORT_NAME = "rails-app";
    private final String EBS_VOLUME_ID = "vol-6f643e20";
    private final String ELASTIC_IP_ADDRESS = "54.235.87.203";
    /**
     * Configurations for the running stack used for app deployment tests.
     * (Stack name: java-sdk-app-deployment-test-stack)
     */
    private final String APP_DEPLOYMENT_TEST_STACK_ID = "d44a8b47-a418-4112-9326-8f2231d2bc4e";
    private final String APP_DEPLOYMENT_TEST_RUNNING_INSTANCE_ID = "10d8a89f-66ad-4884-be1e-cf503cbf7a14";
    private final String APP_NAME = "my-app";
    private final String APP_SHORT_NAME = "app-short-name";

    @AfterClass
    public static void teardown() throws FileNotFoundException, IOException, InterruptedException {
        try {
            opsWorks.deleteStack(DeleteStackRequest.builder().stackId(stackId).build());
        } catch (AmazonServiceException e) {
            // Ignored or expected.
        }

        try {
            elb.deleteLoadBalancer(DeleteLoadBalancerRequest.builder().loadBalancerName(loadBalancerName).build());
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testServiceOperations() throws InterruptedException {
        // Create Stack
        CreateStackResult createStackResult = opsWorks
                .createStack(CreateStackRequest.builder()
                                               .name(STACK_NAME)
                                               .region(STACK_REGION)
                                               .defaultInstanceProfileArn(
                                                       DEFAULT_IAM_INSTANCE_PROFILE)
                                               .serviceRoleArn(IAM_ROLE)
                                               .configurationManager(
                                                       StackConfigurationManager.builder().name(
                                                               STACK_CONFIG_MANAGER_NAME).version(
                                                               STACK_CONFIG_MANAGER_VERSION).build()).build());
        stackId = createStackResult.stackId();
        assertNotNull(stackId);

        DescribeStacksResult describeStacksResult =
                opsWorks.describeStacks(DescribeStacksRequest.builder().stackIds(stackId).build());
        assertEquals(describeStacksResult.stacks().size(), 1);
        assertEquals(describeStacksResult.stacks().get(0).name(), STACK_NAME);
        assertEquals(describeStacksResult.stacks().get(0).stackId(), stackId);
        assertEquals(describeStacksResult.stacks().get(0).region(), STACK_REGION);
        assertEquals(describeStacksResult.stacks().get(0).serviceRoleArn(), IAM_ROLE);
        assertEquals(describeStacksResult.stacks().get(0).configurationManager().name(), STACK_CONFIG_MANAGER_NAME);
        assertEquals(describeStacksResult.stacks().get(0).configurationManager().version(), STACK_CONFIG_MANAGER_VERSION);

        // Update the stack
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(STACK_ATTRIBUTE_KEY, STACK_ATTRIBUTE_VALUE);
        opsWorks.updateStack(
                UpdateStackRequest.builder().stackId(stackId).attributes(attributes).serviceRoleArn(IAM_ROLE).build());

        // Describe the updated stack
        describeStacksResult = opsWorks.describeStacks(DescribeStacksRequest.builder().stackIds(stackId).build());
        assertEquals(STACK_ATTRIBUTE_VALUE, describeStacksResult.stacks().get(0).attributes().get(STACK_ATTRIBUTE_KEY));

        // List all the stacks
        opsWorks.describeStacks(DescribeStacksRequest.builder().build());
        assertTrue(describeStacksResult.stacks().size() > 0);


        // Register a EBS volume with the stack
        RegisterVolumeResult registerVolumeResult = opsWorks
                .registerVolume(RegisterVolumeRequest.builder()
                                                     .stackId(stackId).ec2VolumeId(EBS_VOLUME_ID).build());
        registeredVolumeId = registerVolumeResult.volumeId();

        // Update the volume
        opsWorks.updateVolume(UpdateVolumeRequest.builder().volumeId(
                registeredVolumeId).mountPoint("/vol/mountpoint").build());


        // Register an EIP with the stack
        RegisterElasticIpResult registerElasticIpResult = opsWorks
                .registerElasticIp(RegisterElasticIpRequest.builder().stackId(
                        stackId).elasticIp(ELASTIC_IP_ADDRESS).build());
        assertEquals(ELASTIC_IP_ADDRESS, registerElasticIpResult.elasticIp());

        // Update the EIP and add a new name for it.
        opsWorks.updateElasticIp(UpdateElasticIpRequest.builder().elasticIp(
                ELASTIC_IP_ADDRESS).name("test-eip").build());


        // Create the layer
        CreateLayerResult createLayerResult = opsWorks
                .createLayer(CreateLayerRequest.builder().stackId(stackId)
                                               .type(LAYER_TYPE).name(LAYER_NAME)
                                               .installUpdatesOnBoot(false).build());
        layerId = createLayerResult.layerId();
        assertNotNull(layerId);

        // Describe the layer
        DescribeLayersResult describeLayersResult =
                opsWorks.describeLayers(DescribeLayersRequest.builder().layerIds(layerId).build());
        assertEquals(1, describeLayersResult.layers().size());
        assertEquals(LAYER_NAME, describeLayersResult.layers().get(0).name());
        assertEquals(LAYER_TYPE, describeLayersResult.layers().get(0).type());
        assertEquals(LAYER_SHORT_NAME, describeLayersResult.layers().get(0).shortname());

        // Update the layer
        attributes.clear();
        attributes.put(LAYER_ATTRIBUTE_KEY, LAYER_ATTRIBUTE_VALUE);
        opsWorks.updateLayer(UpdateLayerRequest.builder().name(NEW_LAYER_NAME).layerId(layerId).attributes(attributes).build());

        describeLayersResult = opsWorks.describeLayers(DescribeLayersRequest.builder().layerIds(layerId).build());
        assertEquals(1, describeLayersResult.layers().size());
        assertEquals(NEW_LAYER_NAME, describeLayersResult.layers().get(0).name());
        assertEquals(LAYER_ATTRIBUTE_VALUE, describeLayersResult.layers().get(0).attributes().get(LAYER_ATTRIBUTE_KEY));


        // Attach the ELB
        opsWorks.attachElasticLoadBalancer(AttachElasticLoadBalancerRequest.builder()
                                                                           .layerId(layerId)
                                                                           .elasticLoadBalancerName(loadBalancerName).build());

        // Describe the ELB
        DescribeElasticLoadBalancersResult describeElasticLoadBalancersResult = opsWorks
                .describeElasticLoadBalancers(DescribeElasticLoadBalancersRequest.builder()
                                                                                 .layerIds(layerId).build());
        assertEquals(1, describeElasticLoadBalancersResult.elasticLoadBalancers().size());

        // Detach the ELB
        opsWorks.detachElasticLoadBalancer(DetachElasticLoadBalancerRequest.builder()
                                                                           .layerId(layerId)
                                                                           .elasticLoadBalancerName(loadBalancerName).build());

        // Now the stack should have zero ELB attached to it.
        describeElasticLoadBalancersResult = opsWorks.describeElasticLoadBalancers(
                DescribeElasticLoadBalancersRequest.builder()
                                                   .layerIds(layerId).build());
        assertEquals(0, describeElasticLoadBalancersResult.elasticLoadBalancers().size());


        // Create an instance
        CreateInstanceResult createInstacneResult = opsWorks
                .createInstance(CreateInstanceRequest.builder()
                                                     .stackId(stackId).layerIds(layerId)
                                                     .instanceType(INSTANCE_TYPE).amiId(AMI_ID)
                                                     .os("Custom").installUpdatesOnBoot(false).build());
        instanceId = createInstacneResult.instanceId();
        assertNotNull(instanceId);

        // Describe an instance
        DescribeInstancesResult describeInstancesResult =
                opsWorks.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceId).build());
        assertEquals(1, describeInstancesResult.instances().size());
        assertEquals(1, describeInstancesResult.instances().get(0).layerIds().size());
        assertEquals(instanceId, describeInstancesResult.instances().get(0).instanceId());
        assertEquals(INSTANCE_TYPE, describeInstancesResult.instances().get(0).instanceType());
        assertEquals(AMI_ID, describeInstancesResult.instances().get(0).amiId());
        assertFalse(describeInstancesResult.instances().get(0).installUpdatesOnBoot());


        // Assign the registered volume to this instance
        opsWorks.assignVolume(AssignVolumeRequest.builder().instanceId(
                instanceId).volumeId(registeredVolumeId).build());

        // Unassign the volume
        opsWorks.unassignVolume(UnassignVolumeRequest.builder()
                                                     .volumeId(registeredVolumeId).build());


        // Associate the registered EIP with this instance
        opsWorks.associateElasticIp(AssociateElasticIpRequest.builder()
                                                             .elasticIp(ELASTIC_IP_ADDRESS).instanceId(instanceId).build());

        // Disassociate the EIP
        opsWorks.disassociateElasticIp(DisassociateElasticIpRequest.builder().elasticIp(ELASTIC_IP_ADDRESS).build());


        // Update instance
        opsWorks.updateInstance(UpdateInstanceRequest.builder()
                                                     .instanceId(instanceId).instanceType(NEW_INSTANCE_TYPE)
                                                     .layerIds(layerId).build());

        // Check that the instance is really updated
        describeInstancesResult = opsWorks.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceId).build());
        assertEquals(NEW_INSTANCE_TYPE, describeInstancesResult.instances().get(0).instanceType());


        // Delete the instance
        opsWorks.deleteInstance(DeleteInstanceRequest.builder().instanceId(instanceId).build());

        // The instance should no longer exist
        try {
            describeInstancesResult =
                    opsWorks.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceId).build());
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getServiceName());
        }

        // Delete Layer
        opsWorks.deleteLayer(DeleteLayerRequest.builder().layerId(layerId).build());

        try {
            describeLayersResult = opsWorks.describeLayers(DescribeLayersRequest.builder().layerIds(layerId).build());
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getServiceName());
        }

        // Deregister the volume
        opsWorks.deregisterVolume(DeregisterVolumeRequest.builder().volumeId(registeredVolumeId).build());

        // Deregister the EIP
        opsWorks.deregisterElasticIp(DeregisterElasticIpRequest.builder().elasticIp(ELASTIC_IP_ADDRESS).build());

        // Delete the Stack
        opsWorks.deleteStack(DeleteStackRequest.builder().stackId(stackId).build());

        // get an unexisting stack
        try {
            describeStacksResult = opsWorks.describeStacks(DescribeStacksRequest.builder().stackIds(stackId).build());
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getServiceName());
        }
    }

    /** Tests creating, updating and deleting user profile **/
    @Test
    public void testUserProfile() {
        // Clear the existing profile
        try {
            opsWorks.deleteUserProfile(DeleteUserProfileRequest.builder().iamUserArn(IAM_ROLE).build());
        } catch (AmazonServiceException ase) {
            // Ignored or expected.
        }

        // Create user profile.
        CreateUserProfileResult createUserProfileResult = opsWorks
                .createUserProfile(CreateUserProfileRequest.builder()
                                                           .iamUserArn(IAM_ROLE).sshUsername(USER_NAME).allowSelfManagement(true)
                                                           .build());
        assertEquals(IAM_ROLE, createUserProfileResult.iamUserArn());

        // Describe user profile
        DescribeUserProfilesResult describeUserProfileResult = opsWorks
                .describeUserProfiles(DescribeUserProfilesRequest.builder()
                                                                 .iamUserArns(IAM_ROLE).build());
        assertEquals(1, describeUserProfileResult.userProfiles().size());
        assertEquals(IAM_ROLE, describeUserProfileResult.userProfiles().get(0).iamUserArn());
        assertEquals(USER_NAME, describeUserProfileResult.userProfiles().get(0).sshUsername());
        assertEquals(true, describeUserProfileResult.userProfiles().get(0).allowSelfManagement());

        // Update user profile
        opsWorks.updateUserProfile(UpdateUserProfileRequest.builder()
                                                           .iamUserArn(IAM_ROLE).sshPublicKey(SSH_PUBLIC_KEY).build());

        describeUserProfileResult =
                opsWorks.describeUserProfiles(DescribeUserProfilesRequest.builder().iamUserArns(IAM_ROLE).build());
        assertEquals(1, describeUserProfileResult.userProfiles().size());
        assertEquals(IAM_ROLE, describeUserProfileResult.userProfiles().get(0).iamUserArn());
        assertEquals(SSH_PUBLIC_KEY, describeUserProfileResult.userProfiles().get(0).sshPublicKey());

        opsWorks.updateMyUserProfile(UpdateMyUserProfileRequest.builder().sshPublicKey(SSH_PUBLIC_KEY + "new").build());

        // Delete user profile
        opsWorks.deleteUserProfile(DeleteUserProfileRequest.builder().iamUserArn(IAM_ROLE).build());

        // The user profile should no longer exist
        try {
            opsWorks.describeUserProfiles(DescribeUserProfilesRequest.builder().iamUserArns(IAM_ROLE).build());
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getServiceName());
            assertNotNull(e.getErrorType());
        }
    }

    @Test
    public void testStackSummary() {
        // Create Stack
        CreateStackResult createStackResult = opsWorks
                .createStack(CreateStackRequest.builder()
                                               .name(STACK_NAME)
                                               .region(STACK_REGION)
                                               .defaultInstanceProfileArn(
                                                       DEFAULT_IAM_INSTANCE_PROFILE)
                                               .serviceRoleArn(IAM_ROLE)
                                               .configurationManager(
                                                       StackConfigurationManager.builder().name(
                                                               STACK_CONFIG_MANAGER_NAME).version(
                                                               STACK_CONFIG_MANAGER_VERSION).build()).build());
        stackId = createStackResult.stackId();
        assertNotNull(stackId);
    }

    /**
     * Tests creating and deploying an app, using the a pre-defined layer
     * that has already had a running instance
     */
    @Test
    public void testAppDeployment() {
        // Create an App
        CreateAppResult createAppResult = opsWorks
                .createApp(CreateAppRequest.builder().stackId(APP_DEPLOYMENT_TEST_STACK_ID)
                                           .name(APP_NAME).type(AppType.Php)
                                           .shortname(APP_SHORT_NAME).build());
        appId = createAppResult.appId();
        assertNotNull(appId);

        DescribeAppsResult describeAppsResult = opsWorks.describeApps(DescribeAppsRequest.builder().appIds(appId).build());
        assertEquals(1, describeAppsResult.apps().size());
        assertEquals(appId, describeAppsResult.apps().get(0).appId());
        assertEquals(APP_NAME, describeAppsResult.apps().get(0).name());
        assertEquals(APP_SHORT_NAME, describeAppsResult.apps().get(0).shortname());
        assertEquals("Php".toLowerCase(), describeAppsResult.apps().get(0).type().toLowerCase());

        // Create a deployment
        DeploymentCommand command = DeploymentCommand.builder().name(DeploymentCommandName.Deploy).build();
        CreateDeploymentResult createDeploymentResult = opsWorks
                .createDeployment(CreateDeploymentRequest.builder()
                                                         .appId(appId).stackId(APP_DEPLOYMENT_TEST_STACK_ID)
                                                         .instanceIds(APP_DEPLOYMENT_TEST_RUNNING_INSTANCE_ID).command(command)
                                                         .build());
        deploymentId = createDeploymentResult.deploymentId();
        assertNotNull(deploymentId);

        // Describe a deployment
        DescribeDeploymentsResult describeDeploymentsResults = opsWorks
                .describeDeployments(DescribeDeploymentsRequest.builder()
                                                               .deploymentIds(deploymentId).build());
        assertEquals(1, describeDeploymentsResults.deployments().size());
        assertEquals(APP_DEPLOYMENT_TEST_STACK_ID, describeDeploymentsResults.deployments().get(0).stackId());
        assertEquals(appId, describeDeploymentsResults.deployments().get(0).appId());
        assertEquals(deploymentId, describeDeploymentsResults.deployments().get(0).deploymentId());

        // Delete the app
        opsWorks.deleteApp(DeleteAppRequest.builder().appId(appId).build());

        // The app should no longer exist.
        try {
            describeAppsResult = opsWorks.describeApps(DescribeAppsRequest.builder().appIds(appId).build());
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getServiceName());
        }
    }
}
