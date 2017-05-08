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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentDescription;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentHealth;
import software.amazon.awssdk.services.elasticbeanstalk.model.EnvironmentStatus;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for ElasticBeanstalk integration tests; responsible for loading AWS account info for
 * running the tests, and instantiating clients for tests to use.
 */
public abstract class ElasticBeanstalkIntegrationTestBase extends AwsTestBase {

    protected static ElasticBeanstalkClient elasticbeanstalk;
    protected static AmazonS3Client s3;

    /**
     * Loads the AWS account info for the integration tests and creates an clients for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();

        elasticbeanstalk = ElasticBeanstalkClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .withRegion(Regions.US_EAST_1)
                .build();
        s3 = new AmazonS3Client(credentials);

    }

    /**
     * Asserts that the specified date is not null, and is recent (within 24 hours of the current
     * date).
     *
     * @param date
     *            The date to test.
     */
    protected void assertRecent(Date date) {
        assertNotNull(date);

        long dateOffset = new Date().getTime() - date.getTime();
        assertTrue(dateOffset < 1000 * 60 * 60 * 24);
    }

    protected void waitForEnvironmentToTransitionToStateAndHealth(String environmentName,
                                                                  EnvironmentStatus state,
                                                                  EnvironmentHealth health) throws InterruptedException {
        System.out.println("Waiting for instance " + environmentName + " to transition to " + state + "/" + health);

        int count = 0;
        while (true) {
            Thread.sleep(1000 * 30);
            if (count++ > 100) {
                throw new RuntimeException("Environment " + environmentName + " never transitioned to " + state + "/"
                                           + health);
            }

            List<EnvironmentDescription> environments = elasticbeanstalk.describeEnvironments(
                    new DescribeEnvironmentsRequest().withEnvironmentNames(environmentName)).getEnvironments();

            if (environments.size() == 0) {
                throw new RuntimeException("Unable to find an environment with name " + environmentName);
            }

            EnvironmentDescription environment = environments.get(0);
            System.out.println(" - " + environment.getStatus() + "/" + environment.getHealth());
            if (environment.getStatus().equalsIgnoreCase(state.toString()) == false) {
                continue;
            }
            if (health != null && environment.getHealth().equalsIgnoreCase(health.toString()) == false) {
                continue;
            }
            return;
        }
    }
}
