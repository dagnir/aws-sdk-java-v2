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
        CreateEnvironmentRequest request = new CreateEnvironmentRequest();
        request.setApplicationName(applicationName);
        request.setEnvironmentName(environmentName);
        request.setDescription("Work Role");
        request.setSolutionStackName(SOLUTION_STACK_NAME);

        EnvironmentTier tier = createTier();
        request.setTier(tier);

        ConfigurationOptionSetting optionSettings = createOptionSettings();
        request.setOptionSettings(Arrays.asList(optionSettings));

        CreateEnvironmentResult createEnvironmentResult = elasticbeanstalk.createEnvironment(request);

        assertEquals(applicationName, createEnvironmentResult.getApplicationName());
        assertEquals(environmentName, createEnvironmentResult.getEnvironmentName());
        assertEquals(SOLUTION_STACK_NAME, createEnvironmentResult.getSolutionStackName());
        assertEquals(tier, createEnvironmentResult.getTier());

        assertTrue(elasticbeanstalk.describeEnvironments(new DescribeEnvironmentsRequest()).getEnvironments().size() > 0);

        EnvironmentDescription environment = elasticbeanstalk
                .describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentNames(environmentName))
                .getEnvironments().get(0);

        assertEquals(tier, environment.getTier());

        waitForEnvironmentToTransitionToStateAndHealth(environmentName, EnvironmentStatus.Ready, null);
        UpdateEnvironmentResult updateEnvironmentResult = elasticbeanstalk
                .updateEnvironment(new UpdateEnvironmentRequest().withOptionSettings(optionSettings)
                                                                 .withEnvironmentName(environmentName));
        assertEquals(environmentName, updateEnvironmentResult.getEnvironmentName());
    }

    private void createApplication() {
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest()
                .withApplicationName(applicationName);
        elasticbeanstalk.createApplication(createApplicationRequest);
    }

    private EnvironmentTier createTier() {
        EnvironmentTier tier = new EnvironmentTier();
        tier.setName(tierName);
        tier.setType(tierType);
        tier.setVersion(tierVersion);
        return tier;
    }

    private ConfigurationOptionSetting createOptionSettings() {
        ConfigurationOptionSetting optionSettings = new ConfigurationOptionSetting();
        optionSettings.setNamespace("aws:elasticbeanstalk:sqsd");
        optionSettings.setOptionName("WorkerQueueURL");
        optionSettings.setValue("http://123");
        return optionSettings;
    }

}
