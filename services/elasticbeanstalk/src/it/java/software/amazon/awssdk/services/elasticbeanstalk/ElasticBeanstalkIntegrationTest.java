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
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateStorageLocationRequest;
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
import software.amazon.awssdk.services.elasticbeanstalk.model.ListAvailableSolutionStacksRequest;
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
            elasticbeanstalk.terminateEnvironment(TerminateEnvironmentRequest.builder().environmentName(environmentName).build());
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Terminated, null);
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            elasticbeanstalk.deleteConfigurationTemplate(DeleteConfigurationTemplateRequest.builder()
                                                                                           .applicationName(APPLICATION_NAME)
                                                                                           .templateName(templateName).build());
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            elasticbeanstalk.deleteApplicationVersion(DeleteApplicationVersionRequest.builder()
                                                                                     .applicationName(APPLICATION_NAME)
                                                                                     .versionLabel(versionLabel).build());
        } catch (Exception e) {
            // Ignored or expected.
        }

        try {
            elasticbeanstalk.deleteApplication(DeleteApplicationRequest.builder().applicationName(APPLICATION_NAME).build());
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
        List<String> solutionStacks =
                elasticbeanstalk.listAvailableSolutionStacks(ListAvailableSolutionStacksRequest.builder().build())
                                .solutionStacks();
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
                TerminateEnvironmentRequest.builder().environmentName(environmentName).build());
        assertEquals(environmentName, terminateEnvironmentResult.environmentName());
        assertEquals(APPLICATION_NAME, terminateEnvironmentResult.applicationName());
        assertNotEmpty(terminateEnvironmentResult.cname());
        assertRecent(terminateEnvironmentResult.dateCreated());
        assertRecent(terminateEnvironmentResult.dateUpdated());
        assertNotEmpty(terminateEnvironmentResult.description());
        assertNotEmpty(terminateEnvironmentResult.endpointURL());
        assertNotEmpty(terminateEnvironmentResult.environmentId());
        assertNotEmpty(terminateEnvironmentResult.health());
        assertNotEmpty(terminateEnvironmentResult.solutionStackName());
        assertNotEmpty(terminateEnvironmentResult.status());


        // Clean up all resources... (env, versions, s3, etc)
        waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Terminated, null);
        elasticbeanstalk.deleteApplicationVersion(DeleteApplicationVersionRequest.builder().applicationName(APPLICATION_NAME)
                                                                                 .versionLabel(versionLabel).build());
        elasticbeanstalk.deleteApplication(DeleteApplicationRequest.builder().applicationName(APPLICATION_NAME).build());
        s3.deleteObject(bucketName, versionLabel);
        s3.deleteBucket(bucketName);
    }


    /*
     * Private Interface
     */

    /** Tests that we can request and retrieve logs for an environment. */
    private void testEnvironmentInfoOperations() {
        // Create a storage location
        assertNotEmpty(elasticbeanstalk.createStorageLocation(CreateStorageLocationRequest.builder().build()).s3Bucket());

        // Request the log tails for our environment
        elasticbeanstalk.requestEnvironmentInfo(RequestEnvironmentInfoRequest.builder()
                                                                             .environmentId("tail")
                                                                             .environmentName(environmentName).build());

        // Wait a few seconds for ElasticBeanstalk to collect the logs
        try {
            Thread.sleep(1000 * 20);
        } catch (Exception e) {
            // Ignored or expected.
        }

        // Pull the log tails
        List<EnvironmentInfoDescription> environmentInfoList = elasticbeanstalk.retrieveEnvironmentInfo(
                RetrieveEnvironmentInfoRequest.builder().environmentId("tail").environmentName(environmentName).build())
                                                                               .environmentInfo();
        assertTrue(environmentInfoList.size() > 0);
        for (EnvironmentInfoDescription environmentInfo : environmentInfoList) {
            assertNotEmpty(environmentInfo.ec2InstanceId());
            assertNotEmpty(environmentInfo.infoType());
            assertNotEmpty(environmentInfo.message());
            assertRecent(environmentInfo.sampleTimestamp());
        }
    }

    /** Tests that we can create, update and describe applications. */
    private void testApplicationOperations() {
        // Create an application
        ApplicationDescription createdApplication = elasticbeanstalk.createApplication(
                CreateApplicationRequest.builder().applicationName(APPLICATION_NAME)
                                        .description(APPLICATION_DESCRIPTION).build()).application();
        assertEquals(APPLICATION_NAME, createdApplication.applicationName());
        assertEquals(APPLICATION_DESCRIPTION, createdApplication.description());
        assertRecent(createdApplication.dateCreated());

        // Update it
        createdApplication = elasticbeanstalk.updateApplication(
                UpdateApplicationRequest.builder().applicationName(APPLICATION_NAME)
                                        .description("New " + APPLICATION_DESCRIPTION).build()).application();
        assertEquals(APPLICATION_NAME, createdApplication.applicationName());
        assertEquals("New " + APPLICATION_DESCRIPTION, createdApplication.description());
        assertRecent(createdApplication.dateUpdated());

        // Describe it
        List<ApplicationDescription> describedApplications = elasticbeanstalk.describeApplications(
                DescribeApplicationsRequest.builder()
                                           .applicationNames(APPLICATION_NAME).build()).applications();
        assertEquals(1, describedApplications.size());
        assertEquals(APPLICATION_NAME, describedApplications.get(0).applicationName());
    }

    /** Tests that we can create, update and describe application versions. */
    private void testApplicationVersionOperations() {
        s3.createBucket(bucketName);
        s3.putObject(bucketName, versionLabel, getClass().getResourceAsStream("/HelloWorldWeb.war"), new ObjectMetadata());

        // Create an application version
        ApplicationVersionDescription createdApplicationVersion = elasticbeanstalk.createApplicationVersion(
                CreateApplicationVersionRequest.builder().applicationName(APPLICATION_NAME)
                                               .versionLabel(versionLabel)
                                               .description(APPLICATION_VERSION_DESCRIPTION)
                                               .autoCreateApplication(true)
                                               .sourceBundle(
                                                       S3Location.builder().s3Bucket(bucketName).s3Key(versionLabel).build())
                                               .build())
                                                                                  .applicationVersion();
        assertEquals(APPLICATION_NAME, createdApplicationVersion.applicationName());
        assertEquals(APPLICATION_VERSION_DESCRIPTION, createdApplicationVersion.description());
        assertRecent(createdApplicationVersion.dateCreated());
        assertEquals(versionLabel, createdApplicationVersion.versionLabel());
        assertNotEmpty(createdApplicationVersion.sourceBundle().s3Bucket());
        assertNotEmpty(createdApplicationVersion.sourceBundle().s3Key());

        // Update it
        ApplicationVersionDescription applicationVersion = elasticbeanstalk.updateApplicationVersion(
                UpdateApplicationVersionRequest.builder()
                                               .applicationName(APPLICATION_NAME)
                                               .versionLabel(versionLabel)
                                               .description("New Application Version Description").build()
                                                                                                    ).applicationVersion();
        assertRecent(applicationVersion.dateUpdated());

        // Describe it
        List<ApplicationVersionDescription> describedApplicationVersions = elasticbeanstalk.describeApplicationVersions(
                DescribeApplicationVersionsRequest.builder()
                                                  .applicationName(APPLICATION_NAME)
                                                  .versionLabels(versionLabel).build()).applicationVersions();
        assertEquals(1, describedApplicationVersions.size());
        assertEquals(APPLICATION_NAME, describedApplicationVersions.get(0).applicationName());
    }

    /**
     * Tests that we can create an environment, describe it, wait for it to
     * start up and then update it and describe the resources it contains.
     */
    private void testEnvironmentOpeartions() throws Exception {
        // Create a new environment
        CreateEnvironmentResult createEnvironmentResult = elasticbeanstalk.createEnvironment(
                CreateEnvironmentRequest.builder()
                                        .applicationName(APPLICATION_NAME)
                                        .environmentName(environmentName)
                                        .cnamePrefix(environmentName)
                                        .description("Environment Description")
                                        .versionLabel(versionLabel)
                                        .solutionStackName(SOLUTION_STACK_NAME)
                                        .tags(Tag.builder().key(tagKey).value(tagValue).build()).build());
        assertEquals(APPLICATION_NAME, createEnvironmentResult.applicationName());
        assertNotEmpty(createEnvironmentResult.cname());
        assertRecent(createEnvironmentResult.dateCreated());
        assertNotEmpty(createEnvironmentResult.description());
        assertEquals(versionLabel, createEnvironmentResult.versionLabel());
        assertNotEmpty(createEnvironmentResult.environmentId());
        assertNotEmpty(createEnvironmentResult.environmentName());
        assertNotEmpty(createEnvironmentResult.health());
        assertNotEmpty(createEnvironmentResult.solutionStackName());
        assertNotEmpty(createEnvironmentResult.status());


        // Describe it
        List<EnvironmentDescription> describedEnvironments = elasticbeanstalk.describeEnvironments(
                DescribeEnvironmentsRequest.builder()
                                           .applicationName(APPLICATION_NAME)
                                           .environmentNames(environmentName)
                                           .includeDeleted(false)
                                           .versionLabel(versionLabel).build()).environments();
        assertEquals(1, describedEnvironments.size());
        assertEquals(APPLICATION_NAME, describedEnvironments.get(0).applicationName());


        waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Ready, EnvironmentHealth.Green);


        // Update the environment once it's started
        UpdateEnvironmentResult updateEnvironmentResult = elasticbeanstalk.updateEnvironment(
                UpdateEnvironmentRequest.builder()
                                        .environmentName(environmentName)
                                        .description("New Environment Description").build());
        assertNotEmpty(updateEnvironmentResult.description());
        assertRecent(updateEnvironmentResult.dateUpdated());


        // Describe the resources that make up the environment
        EnvironmentResourceDescription environmentResources = elasticbeanstalk.describeEnvironmentResources(
                DescribeEnvironmentResourcesRequest.builder()
                                                   .environmentName(environmentName).build()).environmentResources();
        assertEquals(1, environmentResources.autoScalingGroups().size());
        assertEquals(environmentName, environmentResources.environmentName());
        assertTrue(environmentResources.instances().size() > 0);
        assertTrue(environmentResources.launchConfigurations().size() > 0);
        assertTrue(environmentResources.loadBalancers().size() > 0);
    }

    /** Tests that we can describe an environment's events. */
    private void testEventOperations() {
        List<EventDescription> describedEvents = elasticbeanstalk.describeEvents(
                DescribeEventsRequest.builder()
                                     .applicationName(APPLICATION_NAME)
                                     .environmentName(environmentName).build()).events();
        assertTrue(describedEvents.size() > 0);
        for (EventDescription event : describedEvents) {
            assertEquals(APPLICATION_NAME, event.applicationName());
            assertEquals(environmentName, event.environmentName());
            assertRecent(event.eventDate());
            assertNotEmpty(event.message());
            assertNotEmpty(event.severity());
        }
    }

    /** Tests that we can call the DNS operations. */
    private void testDnsOperations() {
        CheckDNSAvailabilityResult checkDNSAvailabilityResult = elasticbeanstalk.checkDNSAvailability(
                CheckDNSAvailabilityRequest.builder().cnamePrefix(environmentName).build());
        assertFalse(checkDNSAvailabilityResult.available());
    }

    /** Tests that we can create/describe/delete/update configuration templates/settings/options. */
    private void testEnvironmentConfigurationOperations() {
        // Describe the available configuration options
        DescribeConfigurationOptionsResult describeConfigurationOptionsResult = elasticbeanstalk.describeConfigurationOptions(
                DescribeConfigurationOptionsRequest.builder().environmentName(environmentName).build());
        assertTrue(describeConfigurationOptionsResult.options().size() > 0);
        for (ConfigurationOptionDescription optionDescription : describeConfigurationOptionsResult.options()) {
            assertNotEmpty(optionDescription.changeSeverity());
            assertNotEmpty(optionDescription.name());
            assertNotEmpty(optionDescription.namespace());
            assertNotEmpty(optionDescription.valueType());
        }

        // Describe current configuration settings for our environment
        List<ConfigurationSettingsDescription> configurationSettings = elasticbeanstalk.describeConfigurationSettings(
                DescribeConfigurationSettingsRequest.builder()
                                                    .applicationName(APPLICATION_NAME)
                                                    .environmentName(environmentName).build()).configurationSettings();
        assertTrue(configurationSettings.size() > 0);
        for (ConfigurationSettingsDescription configurationSetting : configurationSettings) {
            assertEquals(APPLICATION_NAME, configurationSetting.applicationName());
            assertRecent(configurationSetting.dateCreated());
            assertEquals(environmentName, configurationSetting.environmentName());
            for (ConfigurationOptionSetting optionSetting : configurationSetting.optionSettings()) {
                assertNotEmpty(optionSetting.namespace());
                assertNotEmpty(optionSetting.optionName());
            }
        }

        // Validate a configuration change
        ArrayList<ConfigurationOptionSetting> optionSettings = new ArrayList<ConfigurationOptionSetting>();
        optionSettings.add(ConfigurationOptionSetting.builder().namespace("aws:elasticbeanstalk:application:environment")
                                                     .optionName("PARAM3").value("newValue").build());
        elasticbeanstalk.validateConfigurationSettings(ValidateConfigurationSettingsRequest.builder()
                                                                                           .applicationName(APPLICATION_NAME)
                                                                                           .optionSettings(optionSettings)
                                                                                           .environmentName(environmentName)
                                                                                           .build());

        // Create a new template
        CreateConfigurationTemplateResult createConfigurationTemplateResult = elasticbeanstalk.createConfigurationTemplate(
                CreateConfigurationTemplateRequest.builder()
                                                  .templateName(templateName)
                                                  .solutionStackName(SOLUTION_STACK_NAME)
                                                  .applicationName(APPLICATION_NAME).build());
        assertEquals(APPLICATION_NAME, createConfigurationTemplateResult.applicationName());
        assertRecent(createConfigurationTemplateResult.dateCreated());

        // Update the template
        elasticbeanstalk.updateConfigurationTemplate(UpdateConfigurationTemplateRequest.builder()
                                                                                       .applicationName(APPLICATION_NAME)
                                                                                       .templateName(templateName)
                                                                                       .description("New Template Description")
                                                                                       .build());

        // Delete the template
        elasticbeanstalk.deleteConfigurationTemplate(DeleteConfigurationTemplateRequest.builder()
                                                                                       .applicationName(APPLICATION_NAME)
                                                                                       .templateName(templateName)
                                                                                       .build());
    }

}
