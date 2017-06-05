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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateApplicationRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.CreateEnvironmentResult;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentStatus;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentTier;
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.UpdateEnvironmentResult;

public class WorkRoleIntegrationTest extends ElasticBeanstalkIntegrationTestBase {

    private static final String SOLUTION_STACK_NAME = "64bit Amazon Linux running Tomcat 6";

    private final String tierName = "Worker";
    private final String tierType = "SQS/HTTP";
    private final String tierVersion = "1.0";
    private final String environmentName = "work-role-" + System.currentTimeMillis();
    private final String applicationName = "work-role-" + System.currentTimeMillis();

    @Test
    public void testWorkRole() throws InterruptedException {

        createApplication();
        CreateEnvironmentRequest.Builder request = CreateEnvironmentRequest.builder();
        request.applicationName(applicationName);
        request.environmentName(environmentName);
        request.description("Work Role");
        request.solutionStackName(SOLUTION_STACK_NAME);

        EnvironmentTier tier = createTier();
        request.tier(tier);

        ConfigurationOptionSetting optionSettings = createOptionSettings();
        request.optionSettings(Arrays.asList(optionSettings));

        CreateEnvironmentResult createEnvironmentResult = elasticbeanstalk.createEnvironment(request.build());

        assertEquals(applicationName, createEnvironmentResult.applicationName());
        assertEquals(environmentName, createEnvironmentResult.environmentName());
        assertEquals(SOLUTION_STACK_NAME, createEnvironmentResult.solutionStackName());
        assertEquals(tier, createEnvironmentResult.tier());

        assertTrue(elasticbeanstalk.describeEnvironments(DescribeEnvironmentsRequest.builder().build()).environments().size() > 0);

        EnvironmentDescription environment = elasticbeanstalk
                .describeEnvironments(DescribeEnvironmentsRequest.builder().environmentNames(environmentName).build())
                .environments().get(0);

        assertEquals(tier, environment.tier());

        waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Ready, null);
        UpdateEnvironmentResult updateEnvironmentResult = elasticbeanstalk
                .updateEnvironment(UpdateEnvironmentRequest.builder().optionSettings(optionSettings)
                                                                 .environmentName(environmentName).build());
        assertEquals(environmentName, updateEnvironmentResult.environmentName());
    }

    private void createApplication() {
        CreateApplicationRequest createApplicationRequest = CreateApplicationRequest.builder()
                .applicationName(applicationName).build();
        elasticbeanstalk.createApplication(createApplicationRequest);
    }

    private EnvironmentTier createTier() {
        EnvironmentTier.Builder tier = EnvironmentTier.builder();
        tier.name(tierName);
        tier.type(tierType);
        tier.version(tierVersion);
        return tier.build();
    }

    private ConfigurationOptionSetting createOptionSettings() {
        ConfigurationOptionSetting.Builder optionSettings = ConfigurationOptionSetting.builder();
        optionSettings.namespace("aws:elasticbeanstalk:sqsd");
        optionSettings.optionName("WorkerQueueURL");
        optionSettings.value("http://123");
        return optionSettings.build();
    }

}
