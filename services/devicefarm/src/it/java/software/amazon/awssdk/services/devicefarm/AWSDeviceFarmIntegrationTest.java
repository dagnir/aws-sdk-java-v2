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

package software.amazon.awssdk.services.devicefarm;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.devicefarm.model.CreateProjectRequest;
import software.amazon.awssdk.services.devicefarm.model.CreateProjectResult;
import software.amazon.awssdk.services.devicefarm.model.ListDevicePoolsRequest;
import software.amazon.awssdk.services.devicefarm.model.Project;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Smoke tests for device farm service.
 */
public class AWSDeviceFarmIntegrationTest extends AWSTestBase {

    private static final String PROJECT_NAME = "df-java-project-"
                                               + System.currentTimeMillis();
    private static AWSDeviceFarm client;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        client = new AWSDeviceFarmClient(credentials);
    }

    @AfterClass
    public static void tearDown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    public void testCreateProject() {
        CreateProjectResult result = client
                .createProject(new CreateProjectRequest()
                                       .withName(PROJECT_NAME));
        final Project project = result.getProject();
        assertNotNull(project);
        assertNotNull(project.getArn());
    }

    @Test(expected = AmazonServiceException.class)
    public void testExceptionHandling() {
        client.listDevicePools(new ListDevicePoolsRequest()
                                       .withNextToken("fake-token"));
    }
}
