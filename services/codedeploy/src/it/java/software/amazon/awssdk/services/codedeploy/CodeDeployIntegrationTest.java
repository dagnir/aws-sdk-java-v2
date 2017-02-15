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

package software.amazon.awssdk.services.codedeploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.codedeploy.model.ApplicationInfo;
import software.amazon.awssdk.services.codedeploy.model.CreateApplicationRequest;
import software.amazon.awssdk.services.codedeploy.model.CreateApplicationResult;
import software.amazon.awssdk.services.codedeploy.model.CreateDeploymentGroupRequest;
import software.amazon.awssdk.services.codedeploy.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.codedeploy.model.GetApplicationRequest;
import software.amazon.awssdk.services.codedeploy.model.GetApplicationResult;
import software.amazon.awssdk.services.codedeploy.model.ListApplicationsResult;

/**
 * Performs basic integration tests for AWS Code Deploy service.
 *
 */
public class CodeDeployIntegrationTest extends IntegrationTestBase {

    /**
     * The name of the application being created.
     */
    private static final String APP_NAME = "java-sdk-appln-"
                                           + System.currentTimeMillis();
    /** The name of the deployment group being created, */
    private static final String DEPLOYMENT_GROUP_NAME = "java-sdk-deploy-"
                                                        + System.currentTimeMillis();

    /**
     * The id of the application created in AWS Code Deploy service.
     */
    private static String applicationId = null;

    /**
     * Creates an application and asserts the result for the application id
     * returned from code deploy service.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        IntegrationTestBase.setUp();
        CreateApplicationRequest createRequest = new CreateApplicationRequest()
                .withApplicationName(APP_NAME);
        CreateApplicationResult createResult = codeDeploy
                .createApplication(createRequest);
        applicationId = createResult.getApplicationId();
        assertNotNull(applicationId);
    }

    /**
     * Delete the application from code deploy service created for this testing.
     */
    @AfterClass
    public static void tearDown() {
        if (applicationId != null) {
            codeDeploy.deleteApplication(new DeleteApplicationRequest()
                                                 .withApplicationName(APP_NAME));
        }
        IntegrationTestBase.tearDown();
    }

    /**
     * Performs a list application operation. Asserts that the result should
     * have atleast one application
     */
    @Test
    public void testListApplication() {
        ListApplicationsResult listResult = codeDeploy.listApplications();
        List<String> applicationList = listResult.getApplications();
        assertTrue(applicationList.size() >= 1);
        assertTrue(applicationList.contains(APP_NAME));
    }

    /**
     * Performs a get application operation. Asserts that the application name
     * and id retrieved matches the one created for the testing.
     */
    @Test
    public void testGetApplication() {
        GetApplicationResult getResult = codeDeploy
                .getApplication(new GetApplicationRequest()
                                        .withApplicationName(APP_NAME));
        ApplicationInfo applicationInfo = getResult.getApplication();
        assertEquals(applicationId, applicationInfo.getApplicationId());
        assertEquals(APP_NAME, applicationInfo.getApplicationName());
    }

    /**
     * Tries to create a deployment group. The operation should fail as the
     * service role arn is not mentioned as part of the request.
     * TODO: Re work on this test case to use the IAM role ARN when the code supports it.
     */
    @Test
    public void testCreateDeploymentGroup() {
        try {
            codeDeploy.createDeploymentGroup(new CreateDeploymentGroupRequest()
                                                     .withApplicationName(APP_NAME).withDeploymentGroupName(
                            DEPLOYMENT_GROUP_NAME));
            fail("Create Deployment group should fail as it requires a service role ARN to be specified");
        } catch (Exception ace) {
            assertTrue(ace instanceof AmazonServiceException);
        }
    }
}
