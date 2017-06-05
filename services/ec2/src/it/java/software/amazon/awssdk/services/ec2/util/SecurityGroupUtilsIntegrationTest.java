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

package software.amazon.awssdk.services.ec2.util;

import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;

public class SecurityGroupUtilsIntegrationTest {

    private static EC2Client ec2;
    private static String GROUP_NAME = UUID.randomUUID().toString();
    private static String NON_EXISTENT_GROUP_NAME = UUID.randomUUID().toString();

    @BeforeClass
    public static void setup() {
        ec2 = EC2Client.builder().build();

        ec2.createSecurityGroup(
                CreateSecurityGroupRequest.builder().groupName(GROUP_NAME).description("my security group").build());
    }

    @AfterClass
    public static void teardown() {
        ec2.deleteSecurityGroup(DeleteSecurityGroupRequest.builder().groupName(GROUP_NAME).build());
    }

    @Test
    public void testSecurityGroupExist() {
        Assert.assertTrue(SecurityGroupUtils.doesSecurityGroupExist(ec2, GROUP_NAME));
    }

    @Test
    public void testSecurityGroupNotExist() {
        Assert.assertFalse(SecurityGroupUtils.doesSecurityGroupExist(ec2, NON_EXISTENT_GROUP_NAME));
    }

    /**
     * Make sure the API does not swallow service-side exceptions unrelated to
     * the existence of the security group
     */
    @Test(expected = AmazonServiceException.class)
    public void testServiceSideException() {
        StaticCredentialsProvider credentialsProvider = new StaticCredentialsProvider(new AwsCredentials("key", "key"));
        EC2Client clientWithWrongKeys = EC2Client.builder().credentialsProvider(credentialsProvider).build();
        SecurityGroupUtils.doesSecurityGroupExist(clientWithWrongKeys, GROUP_NAME);
    }
}
