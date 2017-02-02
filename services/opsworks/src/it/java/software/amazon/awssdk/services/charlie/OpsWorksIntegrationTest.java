package software.amazon.awssdk.services.charlie;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    /** Configurations for the new stack to be created. **/
    private final String STACK_NAME = "java-stack" + System.currentTimeMillis();
    private final String STACK_REGION = "us-east-1";
    private final String STACK_CONFIG_MANAGER_NAME = "Chef";
    private final String STACK_CONFIG_MANAGER_VERSION = "11.4";
    private final String IAM_ROLE = "arn:aws:iam::599169622985:role/aws-opsworks-service-role";
    private final String DEFAULT_IAM_INSTANCE_PROFILE = "arn:aws:iam::599169622985:instance-profile/aws-opsworks-ec2-role.1381193218933";
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

    private static String stackId = null;
    private static String registeredVolumeId = null;
    private static String layerId = null;
    private static String deploymentId = null;
    private static String instanceId = null;
    private static String appId = null;

    @AfterClass
    public static void teardown() throws FileNotFoundException, IOException, InterruptedException {
        try {
            opsWorks.deleteStack(new DeleteStackRequest().withStackId(stackId));
        } catch (AmazonServiceException e) {

        }

        try {
            elb.deleteLoadBalancer(new DeleteLoadBalancerRequest().withLoadBalancerName(loadBalancerName));
        } catch (Exception e) {

        }
    }

    @Test
    public void testServiceOperations() throws InterruptedException {
        // Create Stack
        CreateStackResult createStackResult = opsWorks
                .createStack(new CreateStackRequest()
                        .withName(STACK_NAME)
                        .withRegion(STACK_REGION)
                        .withDefaultInstanceProfileArn(
                                DEFAULT_IAM_INSTANCE_PROFILE)
                        .withServiceRoleArn(IAM_ROLE)
                        .withConfigurationManager(
                                new StackConfigurationManager().withName(
                                        STACK_CONFIG_MANAGER_NAME).withVersion(
                                        STACK_CONFIG_MANAGER_VERSION)));
        stackId = createStackResult.getStackId();
        assertNotNull(stackId);

        DescribeStacksResult describeStacksResult = opsWorks.describeStacks(new DescribeStacksRequest().withStackIds(stackId));
        assertEquals(describeStacksResult.getStacks().size(), 1);
        assertEquals(describeStacksResult.getStacks().get(0).getName(), STACK_NAME);
        assertEquals(describeStacksResult.getStacks().get(0).getStackId(), stackId);
        assertEquals(describeStacksResult.getStacks().get(0).getRegion(), STACK_REGION);
        assertEquals(describeStacksResult.getStacks().get(0).getServiceRoleArn(), IAM_ROLE);
        assertEquals(describeStacksResult.getStacks().get(0).getConfigurationManager().getName(), STACK_CONFIG_MANAGER_NAME);
        assertEquals(describeStacksResult.getStacks().get(0).getConfigurationManager().getVersion(), STACK_CONFIG_MANAGER_VERSION);

        // Update the stack
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(STACK_ATTRIBUTE_KEY, STACK_ATTRIBUTE_VALUE);
        opsWorks.updateStack(new UpdateStackRequest().withStackId(stackId).withAttributes(attributes).withServiceRoleArn(IAM_ROLE));

        // Describe the updated stack
        describeStacksResult = opsWorks.describeStacks(new DescribeStacksRequest().withStackIds(stackId));
        assertEquals(STACK_ATTRIBUTE_VALUE, describeStacksResult.getStacks().get(0).getAttributes().get(STACK_ATTRIBUTE_KEY));

        // List all the stacks
        opsWorks.describeStacks(new DescribeStacksRequest());
        assertTrue(describeStacksResult.getStacks().size() > 0);


        // Register a EBS volume with the stack
        RegisterVolumeResult registerVolumeResult = opsWorks
                .registerVolume(new RegisterVolumeRequest()
                        .withStackId(stackId).withEc2VolumeId(EBS_VOLUME_ID));
        registeredVolumeId = registerVolumeResult.getVolumeId();

        // Update the volume
        opsWorks.updateVolume(new UpdateVolumeRequest().withVolumeId(
                registeredVolumeId).withMountPoint("/vol/mountpoint"));


        // Register an EIP with the stack
        RegisterElasticIpResult registerElasticIpResult = opsWorks
                .registerElasticIp(new RegisterElasticIpRequest().withStackId(
                        stackId).withElasticIp(ELASTIC_IP_ADDRESS));
        assertEquals(ELASTIC_IP_ADDRESS, registerElasticIpResult.getElasticIp());

        // Update the EIP and add a new name for it.
        opsWorks.updateElasticIp(new UpdateElasticIpRequest().withElasticIp(
                ELASTIC_IP_ADDRESS).withName("test-eip"));


        // Create the layer
        CreateLayerResult createLayerResult = opsWorks
                .createLayer(new CreateLayerRequest().withStackId(stackId)
                        .withType(LAYER_TYPE).withName(LAYER_NAME)
                        .withInstallUpdatesOnBoot(false));
        layerId = createLayerResult.getLayerId();
        assertNotNull(layerId);

        // Describe the layer
        DescribeLayersResult describeLayersResult = opsWorks.describeLayers(new DescribeLayersRequest().withLayerIds(layerId));
        assertEquals(1, describeLayersResult.getLayers().size());
        assertEquals(LAYER_NAME, describeLayersResult.getLayers().get(0).getName());
        assertEquals(LAYER_TYPE, describeLayersResult.getLayers().get(0).getType());
        assertEquals(LAYER_SHORT_NAME, describeLayersResult.getLayers().get(0).getShortname());

        // Update the layer
        attributes.clear();
        attributes.put(LAYER_ATTRIBUTE_KEY, LAYER_ATTRIBUTE_VALUE);
        opsWorks.updateLayer(new UpdateLayerRequest().withName(NEW_LAYER_NAME).withLayerId(layerId).withAttributes(attributes));

        describeLayersResult = opsWorks.describeLayers(new DescribeLayersRequest().withLayerIds(layerId));
        assertEquals(1, describeLayersResult.getLayers().size());
        assertEquals(NEW_LAYER_NAME, describeLayersResult.getLayers().get(0).getName());
        assertEquals(LAYER_ATTRIBUTE_VALUE, describeLayersResult.getLayers().get(0).getAttributes().get(LAYER_ATTRIBUTE_KEY));


        // Attach the ELB
        opsWorks.attachElasticLoadBalancer(new AttachElasticLoadBalancerRequest()
                                           .withLayerId(layerId)
                                           .withElasticLoadBalancerName(loadBalancerName));

        // Describe the ELB
        DescribeElasticLoadBalancersResult describeElasticLoadBalancersResult = opsWorks
                .describeElasticLoadBalancers(new DescribeElasticLoadBalancersRequest()
                        .withLayerIds(layerId));
        assertEquals(1, describeElasticLoadBalancersResult.getElasticLoadBalancers().size());

        // Detach the ELB
        opsWorks.detachElasticLoadBalancer(new DetachElasticLoadBalancerRequest()
                                                .withLayerId(layerId)
                                                .withElasticLoadBalancerName(loadBalancerName));

        // Now the stack should have zero ELB attached to it.
        describeElasticLoadBalancersResult = opsWorks.describeElasticLoadBalancers(
                               new DescribeElasticLoadBalancersRequest()
                                     .withLayerIds(layerId));
        assertEquals(0, describeElasticLoadBalancersResult.getElasticLoadBalancers().size());


        // Create an instance
        CreateInstanceResult createInstacneResult = opsWorks
                .createInstance(new CreateInstanceRequest()
                        .withStackId(stackId).withLayerIds(layerId)
                        .withInstanceType(INSTANCE_TYPE).withAmiId(AMI_ID)
                        .withOs("Custom").withInstallUpdatesOnBoot(false));
        instanceId = createInstacneResult.getInstanceId();
        assertNotNull(instanceId);

        // Describe an instance
        DescribeInstancesResult describeInstancesResult = opsWorks.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        assertEquals(1, describeInstancesResult.getInstances().size());
        assertEquals(1, describeInstancesResult.getInstances().get(0).getLayerIds().size());
        assertEquals(instanceId, describeInstancesResult.getInstances().get(0).getInstanceId());
        assertEquals(INSTANCE_TYPE, describeInstancesResult.getInstances().get(0).getInstanceType());
        assertEquals(AMI_ID, describeInstancesResult.getInstances().get(0).getAmiId());
        assertFalse(describeInstancesResult.getInstances().get(0).getInstallUpdatesOnBoot());


        // Assign the registered volume to this instance
        opsWorks.assignVolume(new AssignVolumeRequest().withInstanceId(
                instanceId).withVolumeId(registeredVolumeId));

        // Unassign the volume
        opsWorks.unassignVolume(new UnassignVolumeRequest()
                .withVolumeId(registeredVolumeId));


        // Associate the registered EIP with this instance
        opsWorks.associateElasticIp(new AssociateElasticIpRequest()
                .withElasticIp(ELASTIC_IP_ADDRESS).withInstanceId(instanceId));

        // Disassociate the EIP
        opsWorks.disassociateElasticIp(new DisassociateElasticIpRequest().withElasticIp(ELASTIC_IP_ADDRESS));


        // Update instance
        opsWorks.updateInstance(new UpdateInstanceRequest()
                .withInstanceId(instanceId).withInstanceType(NEW_INSTANCE_TYPE)
                .withLayerIds(layerId));

        // Check that the instance is really updated
        describeInstancesResult = opsWorks.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
        assertEquals(NEW_INSTANCE_TYPE, describeInstancesResult.getInstances().get(0).getInstanceType());


        // Delete the instance
        opsWorks.deleteInstance(new DeleteInstanceRequest().withInstanceId(instanceId));

        // The instance should no longer exist
        try {
            describeInstancesResult = opsWorks.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getServiceName());
        }

        // Delete Layer
        opsWorks.deleteLayer(new DeleteLayerRequest().withLayerId(layerId));

        try {
            describeLayersResult = opsWorks.describeLayers(new DescribeLayersRequest().withLayerIds(layerId));
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getServiceName());
        }

        // Deregister the volume
        opsWorks.deregisterVolume(new DeregisterVolumeRequest().withVolumeId(registeredVolumeId));

        // Deregister the EIP
        opsWorks.deregisterElasticIp(new DeregisterElasticIpRequest().withElasticIp(ELASTIC_IP_ADDRESS));

        // Delete the Stack
        opsWorks.deleteStack(new DeleteStackRequest().withStackId(stackId));

        // get an unexisting stack
        try {
            describeStacksResult = opsWorks.describeStacks(new DescribeStacksRequest().withStackIds(stackId));
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
            opsWorks.deleteUserProfile(new DeleteUserProfileRequest().withIamUserArn(IAM_ROLE));
        } catch (AmazonServiceException ase) {}

        // Create user profile.
        CreateUserProfileResult createUserProfileResult = opsWorks
                .createUserProfile(new CreateUserProfileRequest()
                        .withIamUserArn(IAM_ROLE).withSshUsername(USER_NAME).withAllowSelfManagement(true));
        assertEquals(IAM_ROLE, createUserProfileResult.getIamUserArn());

        // Describe user profile
        DescribeUserProfilesResult describeUserProfileResult = opsWorks
                .describeUserProfiles(new DescribeUserProfilesRequest()
                        .withIamUserArns(IAM_ROLE));
        assertEquals(1, describeUserProfileResult.getUserProfiles().size());
        assertEquals(IAM_ROLE, describeUserProfileResult.getUserProfiles().get(0).getIamUserArn());
        assertEquals(USER_NAME, describeUserProfileResult.getUserProfiles().get(0).getSshUsername());
        assertEquals(true, describeUserProfileResult.getUserProfiles().get(0).getAllowSelfManagement());

        // Update user profile
        opsWorks.updateUserProfile(new UpdateUserProfileRequest()
                .withIamUserArn(IAM_ROLE).withSshPublicKey(SSH_PUBLIC_KEY));

        describeUserProfileResult = opsWorks.describeUserProfiles(new DescribeUserProfilesRequest().withIamUserArns(IAM_ROLE));
        assertEquals(1, describeUserProfileResult.getUserProfiles().size());
        assertEquals(IAM_ROLE, describeUserProfileResult.getUserProfiles().get(0).getIamUserArn());
        assertEquals(SSH_PUBLIC_KEY, describeUserProfileResult.getUserProfiles().get(0).getSshPublicKey());

        opsWorks.updateMyUserProfile(new UpdateMyUserProfileRequest().withSshPublicKey(SSH_PUBLIC_KEY + "new"));

        // Delete user profile
        opsWorks.deleteUserProfile(new DeleteUserProfileRequest().withIamUserArn(IAM_ROLE));

        // The user profile should no longer exist
        try {
            opsWorks.describeUserProfiles(new DescribeUserProfilesRequest().withIamUserArns(IAM_ROLE));
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
                .createStack(new CreateStackRequest()
                        .withName(STACK_NAME)
                        .withRegion(STACK_REGION)
                        .withDefaultInstanceProfileArn(
                                DEFAULT_IAM_INSTANCE_PROFILE)
                        .withServiceRoleArn(IAM_ROLE)
                        .withConfigurationManager(
                                new StackConfigurationManager().withName(
                                        STACK_CONFIG_MANAGER_NAME).withVersion(
                                        STACK_CONFIG_MANAGER_VERSION)));
        stackId = createStackResult.getStackId();
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
                .createApp(new CreateAppRequest().withStackId(APP_DEPLOYMENT_TEST_STACK_ID)
                        .withName(APP_NAME).withType(AppType.Php)
                        .withShortname(APP_SHORT_NAME));
        appId = createAppResult.getAppId();
        assertNotNull(appId);

        DescribeAppsResult describeAppsResult = opsWorks.describeApps(new DescribeAppsRequest().withAppIds(appId));
        assertEquals(1, describeAppsResult.getApps().size());
        assertEquals(appId, describeAppsResult.getApps().get(0).getAppId());
        assertEquals(APP_NAME, describeAppsResult.getApps().get(0).getName());
        assertEquals(APP_SHORT_NAME, describeAppsResult.getApps().get(0).getShortname());
        assertEquals("Php".toLowerCase(), describeAppsResult.getApps().get(0).getType().toLowerCase());

        // Create a deployment
        DeploymentCommand command = new DeploymentCommand();
        command.setName(DeploymentCommandName.Deploy);
        CreateDeploymentResult createDeploymentResult = opsWorks
                .createDeployment(new CreateDeploymentRequest()
                        .withAppId(appId).withStackId(APP_DEPLOYMENT_TEST_STACK_ID)
                        .withInstanceIds(APP_DEPLOYMENT_TEST_RUNNING_INSTANCE_ID).withCommand(command));
        deploymentId = createDeploymentResult.getDeploymentId();
        assertNotNull(deploymentId);

        // Describe a deployment
        DescribeDeploymentsResult describeDeploymentsResults = opsWorks
                .describeDeployments(new DescribeDeploymentsRequest()
                        .withDeploymentIds(deploymentId));
        assertEquals(1, describeDeploymentsResults.getDeployments().size());
        assertEquals(APP_DEPLOYMENT_TEST_STACK_ID, describeDeploymentsResults.getDeployments().get(0).getStackId());
        assertEquals(appId, describeDeploymentsResults.getDeployments().get(0).getAppId());
        assertEquals(deploymentId, describeDeploymentsResults.getDeployments().get(0).getDeploymentId());

        // Delete the app
        opsWorks.deleteApp(new DeleteAppRequest().withAppId(appId));

        // The app should no longer exist.
        try {
            describeAppsResult = opsWorks.describeApps(new DescribeAppsRequest().withAppIds(appId));
            fail();
        } catch (AmazonServiceException e) {
            assertNotNull(e.getMessage());
            assertNotNull(e.getErrorType());
            assertNotNull(e.getServiceName());
        }
    }
}
