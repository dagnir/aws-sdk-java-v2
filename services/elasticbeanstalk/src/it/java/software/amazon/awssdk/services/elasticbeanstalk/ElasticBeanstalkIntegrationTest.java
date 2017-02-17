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

package software.amazon.awssdk.services.elasticbeanstalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.elasticbeanstalk.model.ApplicationDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.ApplicationVersionDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;
import software.amazon.awssdk.services.elasticbeanstalk.model.ConfigurationOptionDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import software.amazon.awssdk.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateApplicationRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateConfigurationTemplateResult;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateEnvironmentResult;
import software.amazon.awssdk.services.elasticbeanstalk.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DeleteApplicationVersionRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DeleteConfigurationTemplateRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEventsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentHealth;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentInfoDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentStatus;
import software.amazon.awssdk.services.elasticbeanstalk.model.EventDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.RequestEnvironmentInfoRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.RetrieveEnvironmentInfoRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.S3Location;
import software.amazon.awssdk.services.elasticbeanstalk.model.Tag;
import software.amazon.awssdk.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.TerminateEnvironmentResult;
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateApplicationRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateApplicationVersionRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateConfigurationTemplateRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateEnvironmentResult;
import software.amazon.awssdk.services.elasticbeanstalk.model.ValidateConfigurationSettingsRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;

/**
 * Integration test to bring up a new ElasticBeanstalk environment and run through as
 * many operations as possible.
 */
public class ElasticBeanstalkIntegrationTest extends ElasticBeanstalkIntegrationTestBase {

    private static final String APPLICATION_VERSION_DESCRIPTION = "Application Version Description";
    private static final String APPLICATION_DESCRIPTION = "Application Description";
    private static final String APPLICATION_NAME = "Application-" + System.currentTimeMillis();
    private static final String SOLUTION_STACK_NAME = "64bit Amazon Linux running Tomcat 6";

    private final String bucketName = "java-sdk-elasticbeanstalk-test-" + System.currentTimeMillis();
    private final String environmentName = "java-sdk-" + System.currentTimeMillis();
    private final String versionLabel = "java-sdk-test-" + System.currentTimeMillis();
    private final String templateName = "java-sdk-template-" + System.currentTimeMillis();

    /** Tag Key and Value used to tag the newly created environment.*/
    private final String tagKey = "java-sdk-env-tag-key" + System.currentTimeMillis();
    private final String tagValue = "java-sdk-env-tag-value" + System.currentTimeMillis();

    /** Releases all resources created by this test. */
    @After
    public void tearDown() throws Exception {
        try {
            elasticbeanstalk.terminateEnvironment(new TerminateEnvironmentRequest().withEnvironmentName(environmentName));
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Terminated, null);
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            elasticbeanstalk.deleteConfigurationTemplate(new DeleteConfigurationTemplateRequest(APPLICATION_NAME, templateName));
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            elasticbeanstalk.deleteApplicationVersion(new DeleteApplicationVersionRequest(APPLICATION_NAME, versionLabel));
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            elasticbeanstalk.deleteApplication(new DeleteApplicationRequest(APPLICATION_NAME));
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            s3.deleteObject(bucketName, versionLabel);
            s3.deleteBucket(bucketName);
        } catch (Exception e) {
            // Ignored or expected.
        }
    }


    /** Tests that we can describe the available solution stacks. */
    @Test
    public void testListAvailableSolutionStacks() throws Exception {
        List<String> solutionStacks = elasticbeanstalk.listAvailableSolutionStacks().getSolutionStacks();
        assertNotNull(solutionStacks);
        assertTrue(solutionStacks.size() > 1);
        for (String stack : solutionStacks) {
            assertNotEmpty(stack);
        }
    }

    /** Tests that we can launch a new environment and walk through the ElasticBeanstalk operations. */
    @Test
    public void testEnvironmentLaunch() throws Exception {
        testApplicationOperations();
        testApplicationVersionOperations();
        testEnvironmentOpeartions();
        testEnvironmentInfoOperations();
        testEnvironmentConfigurationOperations();
        testEventOperations();
        testDnsOperations();

        TerminateEnvironmentResult terminateEnvironmentResult = elasticbeanstalk.terminateEnvironment(
                new TerminateEnvironmentRequest().withEnvironmentName(environmentName));
        assertEquals(environmentName, terminateEnvironmentResult.getEnvironmentName());
        assertEquals(APPLICATION_NAME, terminateEnvironmentResult.getApplicationName());
        assertNotEmpty(terminateEnvironmentResult.getCNAME());
        assertRecent(terminateEnvironmentResult.getDateCreated());
        assertRecent(terminateEnvironmentResult.getDateUpdated());
        assertNotEmpty(terminateEnvironmentResult.getDescription());
        assertNotEmpty(terminateEnvironmentResult.getEndpointURL());
        assertNotEmpty(terminateEnvironmentResult.getEnvironmentId());
        assertNotEmpty(terminateEnvironmentResult.getHealth());
        assertNotEmpty(terminateEnvironmentResult.getSolutionStackName());
        assertNotEmpty(terminateEnvironmentResult.getStatus());


        // Clean up all resources... (env, versions, s3, etc)
        waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Terminated, null);
        elasticbeanstalk.deleteApplicationVersion(new DeleteApplicationVersionRequest(APPLICATION_NAME, versionLabel));
        elasticbeanstalk.deleteApplication(new DeleteApplicationRequest(APPLICATION_NAME));
        s3.deleteObject(bucketName, versionLabel);
        s3.deleteBucket(bucketName);
    }


    /*
     * Private Interface
     */

    /** Tests that we can request and retrieve logs for an environment. */
    private void testEnvironmentInfoOperations() {
        // Create a storage location
        assertNotEmpty(elasticbeanstalk.createStorageLocation().getS3Bucket());

        // Request the log tails for our environment
        elasticbeanstalk.requestEnvironmentInfo(new RequestEnvironmentInfoRequest("tail").withEnvironmentName(environmentName));

        // Wait a few seconds for ElasticBeanstalk to collect the logs
        try {
            Thread.sleep(1000 * 20);
        } catch (Exception e) {
            // Ignored or expected.
        }

        // Pull the log tails
        List<EnvironmentInfoDescription> environmentInfoList = elasticbeanstalk.retrieveEnvironmentInfo(
                new RetrieveEnvironmentInfoRequest("tail")
                        .withEnvironmentName(environmentName)).getEnvironmentInfo();
        assertTrue(environmentInfoList.size() > 0);
        for (EnvironmentInfoDescription environmentInfo : environmentInfoList) {
            assertNotEmpty(environmentInfo.getEc2InstanceId());
            assertNotEmpty(environmentInfo.getInfoType());
            assertNotEmpty(environmentInfo.getMessage());
            assertRecent(environmentInfo.getSampleTimestamp());
        }
    }

    /** Tests that we can create, update and describe applications. */
    private void testApplicationOperations() {
        // Create an application
        ApplicationDescription createdApplication = elasticbeanstalk.createApplication(
                new CreateApplicationRequest(APPLICATION_NAME)
                        .withDescription(APPLICATION_DESCRIPTION)).getApplication();
        assertEquals(APPLICATION_NAME, createdApplication.getApplicationName());
        assertEquals(APPLICATION_DESCRIPTION, createdApplication.getDescription());
        assertRecent(createdApplication.getDateCreated());

        // Update it
        createdApplication = elasticbeanstalk.updateApplication(
                new UpdateApplicationRequest(APPLICATION_NAME)
                        .withDescription("New " + APPLICATION_DESCRIPTION)).getApplication();
        assertEquals(APPLICATION_NAME, createdApplication.getApplicationName());
        assertEquals("New " + APPLICATION_DESCRIPTION, createdApplication.getDescription());
        assertRecent(createdApplication.getDateUpdated());

        // Describe it
        List<ApplicationDescription> describedApplications = elasticbeanstalk.describeApplications(
                new DescribeApplicationsRequest()
                        .withApplicationNames(APPLICATION_NAME)).getApplications();
        assertEquals(1, describedApplications.size());
        assertEquals(APPLICATION_NAME, describedApplications.get(0).getApplicationName());
    }

    /** Tests that we can create, update and describe application versions. */
    private void testApplicationVersionOperations() {
        s3.createBucket(bucketName);
        s3.putObject(bucketName, versionLabel, getClass().getResourceAsStream("/HelloWorldWeb.war"), new ObjectMetadata());

        // Create an application version
        ApplicationVersionDescription createdApplicationVersion = elasticbeanstalk.createApplicationVersion(
                new CreateApplicationVersionRequest(APPLICATION_NAME, versionLabel)
                        .withDescription(APPLICATION_VERSION_DESCRIPTION)
                        .withAutoCreateApplication(true)
                        .withSourceBundle(new S3Location(bucketName, versionLabel))).getApplicationVersion();
        assertEquals(APPLICATION_NAME, createdApplicationVersion.getApplicationName());
        assertEquals(APPLICATION_VERSION_DESCRIPTION, createdApplicationVersion.getDescription());
        assertRecent(createdApplicationVersion.getDateCreated());
        assertEquals(versionLabel, createdApplicationVersion.getVersionLabel());
        assertNotEmpty(createdApplicationVersion.getSourceBundle().getS3Bucket());
        assertNotEmpty(createdApplicationVersion.getSourceBundle().getS3Key());

        // Update it
        ApplicationVersionDescription applicationVersion = elasticbeanstalk.updateApplicationVersion(
                new UpdateApplicationVersionRequest(APPLICATION_NAME, versionLabel)
                        .withDescription("New Application Version Description")).getApplicationVersion();
        assertRecent(applicationVersion.getDateUpdated());

        // Describe it
        List<ApplicationVersionDescription> describedApplicationVersions = elasticbeanstalk.describeApplicationVersions(
                new DescribeApplicationVersionsRequest()
                        .withApplicationName(APPLICATION_NAME)
                        .withVersionLabels(versionLabel)).getApplicationVersions();
        assertEquals(1, describedApplicationVersions.size());
        assertEquals(APPLICATION_NAME, describedApplicationVersions.get(0).getApplicationName());
    }

    /**
     * Tests that we can create an environment, describe it, wait for it to
     * start up and then update it and describe the resources it contains.
     */
    private void testEnvironmentOpeartions() throws Exception {
        // Create a new environment
        CreateEnvironmentResult createEnvironmentResult = elasticbeanstalk.createEnvironment(
                new CreateEnvironmentRequest(APPLICATION_NAME, environmentName)
                        .withCNAMEPrefix(environmentName)
                        .withDescription("Environment Description")
                        .withVersionLabel(versionLabel)
                        .withSolutionStackName(SOLUTION_STACK_NAME)
                        .withTags(new Tag().withKey(tagKey).withValue(tagValue)));
        assertEquals(APPLICATION_NAME, createEnvironmentResult.getApplicationName());
        assertNotEmpty(createEnvironmentResult.getCNAME());
        assertRecent(createEnvironmentResult.getDateCreated());
        assertNotEmpty(createEnvironmentResult.getDescription());
        assertEquals(versionLabel, createEnvironmentResult.getVersionLabel());
        assertNotEmpty(createEnvironmentResult.getEnvironmentId());
        assertNotEmpty(createEnvironmentResult.getEnvironmentName());
        assertNotEmpty(createEnvironmentResult.getHealth());
        assertNotEmpty(createEnvironmentResult.getSolutionStackName());
        assertNotEmpty(createEnvironmentResult.getStatus());


        // Describe it
        List<EnvironmentDescription> describedEnvironments = elasticbeanstalk.describeEnvironments(
                new DescribeEnvironmentsRequest()
                        .withApplicationName(APPLICATION_NAME)
                        .withEnvironmentNames(environmentName)
                        .withIncludeDeleted(false)
                        .withVersionLabel(versionLabel)).getEnvironments();
        assertEquals(1, describedEnvironments.size());
        assertEquals(APPLICATION_NAME, describedEnvironments.get(0).getApplicationName());


        waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Ready, EnvironmentHealth.Green);


        // Update the environment once it's started
        UpdateEnvironmentResult updateEnvironmentResult = elasticbeanstalk.updateEnvironment(
                new UpdateEnvironmentRequest()
                        .withEnvironmentName(environmentName)
                        .withDescription("New Environment Description"));
        assertNotEmpty(updateEnvironmentResult.getDescription());
        assertRecent(updateEnvironmentResult.getDateUpdated());


        // Describe the resources that make up the environment
        EnvironmentResourceDescription environmentResources = elasticbeanstalk.describeEnvironmentResources(
                new DescribeEnvironmentResourcesRequest()
                        .withEnvironmentName(environmentName)).getEnvironmentResources();
        assertEquals(1, environmentResources.getAutoScalingGroups().size());
        assertEquals(environmentName, environmentResources.getEnvironmentName());
        assertTrue(environmentResources.getInstances().size() > 0);
        assertTrue(environmentResources.getLaunchConfigurations().size() > 0);
        assertTrue(environmentResources.getLoadBalancers().size() > 0);
    }

    /** Tests that we can describe an environment's events. */
    private void testEventOperations() {
        List<EventDescription> describedEvents = elasticbeanstalk.describeEvents(
                new DescribeEventsRequest()
                        .withApplicationName(APPLICATION_NAME)
                        .withEnvironmentName(environmentName)).getEvents();
        assertTrue(describedEvents.size() > 0);
        for (EventDescription event : describedEvents) {
            assertEquals(APPLICATION_NAME, event.getApplicationName());
            assertEquals(environmentName, event.getEnvironmentName());
            assertRecent(event.getEventDate());
            assertNotEmpty(event.getMessage());
            assertNotEmpty(event.getSeverity());
        }
    }

    /** Tests that we can call the DNS operations. */
    private void testDnsOperations() {
        CheckDNSAvailabilityResult checkDNSAvailabilityResult = elasticbeanstalk.checkDNSAvailability(
                new CheckDNSAvailabilityRequest(environmentName));
        assertFalse(checkDNSAvailabilityResult.getAvailable());
    }

    /** Tests that we can create/describe/delete/update configuration templates/settings/options. */
    private void testEnvironmentConfigurationOperations() {
        // Describe the available configuration options
        DescribeConfigurationOptionsResult describeConfigurationOptionsResult = elasticbeanstalk.describeConfigurationOptions(
                new DescribeConfigurationOptionsRequest().withEnvironmentName(environmentName));
        assertTrue(describeConfigurationOptionsResult.getOptions().size() > 0);
        for (ConfigurationOptionDescription optionDescription : describeConfigurationOptionsResult.getOptions()) {
            assertNotEmpty(optionDescription.getChangeSeverity());
            assertNotEmpty(optionDescription.getName());
            assertNotEmpty(optionDescription.getNamespace());
            assertNotEmpty(optionDescription.getValueType());
        }

        // Describe current configuration settings for our environment
        List<ConfigurationSettingsDescription> configurationSettings = elasticbeanstalk.describeConfigurationSettings(
                new DescribeConfigurationSettingsRequest()
                        .withApplicationName(APPLICATION_NAME)
                        .withEnvironmentName(environmentName)).getConfigurationSettings();
        assertTrue(configurationSettings.size() > 0);
        for (ConfigurationSettingsDescription configurationSetting : configurationSettings) {
            assertEquals(APPLICATION_NAME, configurationSetting.getApplicationName());
            assertRecent(configurationSetting.getDateCreated());
            assertEquals(environmentName, configurationSetting.getEnvironmentName());
            for (ConfigurationOptionSetting optionSetting : configurationSetting.getOptionSettings()) {
                assertNotEmpty(optionSetting.getNamespace());
                assertNotEmpty(optionSetting.getOptionName());
            }
        }

        // Validate a configuration change
        ArrayList<ConfigurationOptionSetting> optionSettings = new ArrayList<ConfigurationOptionSetting>();
        optionSettings.add(new ConfigurationOptionSetting("aws:elasticbeanstalk:application:environment", "PARAM3", "newValue"));
        elasticbeanstalk.validateConfigurationSettings(new ValidateConfigurationSettingsRequest(APPLICATION_NAME, optionSettings)
                                                               .withEnvironmentName(environmentName));

        // Create a new template
        CreateConfigurationTemplateResult createConfigurationTemplateResult = elasticbeanstalk.createConfigurationTemplate(
                new CreateConfigurationTemplateRequest()
                        .withTemplateName(templateName)
                        .withSolutionStackName(SOLUTION_STACK_NAME)
                        .withApplicationName(APPLICATION_NAME));
        assertEquals(APPLICATION_NAME, createConfigurationTemplateResult.getApplicationName());
        assertRecent(createConfigurationTemplateResult.getDateCreated());

        // Update the template
        elasticbeanstalk.updateConfigurationTemplate(new UpdateConfigurationTemplateRequest(
                APPLICATION_NAME, templateName).withDescription("New Template Description"));

        // Delete the template
        elasticbeanstalk.deleteConfigurationTemplate(new DeleteConfigurationTemplateRequest(
                APPLICATION_NAME, templateName));
    }

}
