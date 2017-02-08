/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.ec2;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.BasicSessionCredentials;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DryRunResult;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenServiceClient;
import software.amazon.awssdk.services.securitytoken.model.Credentials;
import software.amazon.awssdk.services.securitytoken.model.GetFederationTokenRequest;

public class EC2DryrunOperationsIntegrationTest extends EC2IntegrationTestBase {

    private static final String DENY_ALL_POLICY =
            "{\"Statement\":[{" +
                    "\"Effect\":\"Deny\"," +
                    "\"Action\":\"*\"," +
                    "\"Resource\":\"*\"" +
                    "}]" +
            "}";
    private static final String FAKE_INSTANCE_ID = "i-2b12f844";

    private static AmazonEC2Client ec2WithNoPermission;

    @BeforeClass
    public static void setup() {
        ec2WithNoPermission = setupEc2ClientWithNoPermission();
    }

    /**
     * Tests that dryRun method is handling the service response properly, and
     * returns the appropriate boolean.
     */
    @Test
    public void testDryrunOperations() {

        /* This guy doesn't have the permission for this operation, */
        DryRunResult<?> result = ec2WithNoPermission.dryRun(
                new DescribeInstancesRequest()
                        .withInstanceIds(FAKE_INSTANCE_ID)
                );
        assertFalse(result.isSuccessful());
        assertThat(result.getOriginalRequest(), instanceOf((DescribeInstancesRequest.class)));
        assertNotNull(result.getMessage());
        assertNotNull(result.getDryRunResponse());
        assertEquals(403, result.getDryRunResponse().getStatusCode());

        /* but this guy does. */
        result = ec2.dryRun(new DescribeInstancesRequest()
                .withInstanceIds(FAKE_INSTANCE_ID));
        assertTrue(result.isSuccessful());
        assertThat(result.getOriginalRequest(), instanceOf((DescribeInstancesRequest.class)));

        assertNotNull(result.getMessage());
        assertNotNull(result.getDryRunResponse());
        assertEquals(412, result.getDryRunResponse().getStatusCode());
    }

    private static AmazonEC2Client setupEc2ClientWithNoPermission() {

        AWSSecurityTokenServiceClient sts = new AWSSecurityTokenServiceClient();
        Credentials temporaryCredentials =
                sts.getFederationToken(new GetFederationTokenRequest()
                    .withName("java-sdk-ec2-dryrun")
                    .withPolicy(DENY_ALL_POLICY)
                ).getCredentials();

        String accessKey = temporaryCredentials.getAccessKeyId();
        String secretKey = temporaryCredentials.getSecretAccessKey();
        String sessionToken = temporaryCredentials.getSessionToken();

        return new AmazonEC2Client(new BasicSessionCredentials(
                accessKey, secretKey, sessionToken));
    }
}
