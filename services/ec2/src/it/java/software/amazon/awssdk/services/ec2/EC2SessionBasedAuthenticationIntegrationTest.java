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

package software.amazon.awssdk.services.ec2;

import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider;

/** Tests of session-based auth. */
public class EC2SessionBasedAuthenticationIntegrationTest extends EC2IntegrationTestBase {

    @Test
    public void smokeTest() throws Exception {
        setUpCredentials();
        STSClient stsClient = STSClient.builder().credentialsProvider(new StaticCredentialsProvider(getCredentials())).build();
        AwsCredentialsProvider stsCredentialsProvider = StsGetSessionTokenCredentialsProvider.builder().stsClient(stsClient).build();
        EC2Client ec2 = EC2Client.builder().credentialsProvider(stsCredentialsProvider).build();
        ec2.describeAvailabilityZones(DescribeAvailabilityZonesRequest.builder().build());
    }
}
