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

package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.model.CreateLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateLoginProfileResult;
import software.amazon.awssdk.services.identitymanagement.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.identitymanagement.model.GetLoginProfileRequest;
import software.amazon.awssdk.services.identitymanagement.model.GetLoginProfileResult;
import software.amazon.awssdk.services.identitymanagement.model.NoSuchEntityException;

/**
 * Integration tests of the login profile APIs of IAM.
 *
 * Adapted from jimfl@'s C# tests:
 *
 * https://brazil-subversion.amazon.com/brazil/src/appgroup/awsdr/sdk/
 * AWSCSharpSDKFactory
 * /mainline/Beta.NET.SDK/AWSSDKTests/IntegrationTests/IAM/LoginProfileTests.cs
 *
 * @author zachmu
 */
public class LoginProfileIntegrationTest extends IntegrationTestBase {

    @Before
    public void TestSetup() {
        IAMUtil.deleteUsersAndGroupsInTestNameSpace();
    }

    @Test
    public void TestCreateGetLoginProfile() throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            CreateLoginProfileResult createRes = iam
                    .createLoginProfile(new CreateLoginProfileRequest()
                                                .withUserName(username).withPassword(password));

            Thread.sleep(3 * 3600);

            assertEquals(username, createRes.getLoginProfile().getUserName());

            GetLoginProfileResult getRes = iam
                    .getLoginProfile(new GetLoginProfileRequest()
                                             .withUserName(username));

            assertEquals(username, getRes.getLoginProfile().getUserName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = EntityAlreadyExistsException.class)
    public void TestCreateLoginProfileTwiceException()
            throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            iam.createLoginProfile(new CreateLoginProfileRequest()
                                           .withUserName(username).withPassword(password));
            Thread.sleep(3 * 3600);
            iam.createLoginProfile(new CreateLoginProfileRequest()
                                           .withUserName(username).withPassword(password));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

    @Test(expected = NoSuchEntityException.class)
    public void TestDeleteLoginProfile() throws InterruptedException {
        String username = IAMUtil.createTestUser();
        String password = IAMUtil.uniqueName();

        try {
            iam.createLoginProfile(new CreateLoginProfileRequest()
                                           .withUserName(username).withPassword(password));
            Thread.sleep(3 * 3600);
            iam.deleteLoginProfile(new DeleteLoginProfileRequest()
                                           .withUserName(username));
            Thread.sleep(3 * 3600);
            iam.getLoginProfile(new GetLoginProfileRequest()
                                        .withUserName(username));
        } finally {
            IAMUtil.deleteTestUsers(username);
        }
    }

}
