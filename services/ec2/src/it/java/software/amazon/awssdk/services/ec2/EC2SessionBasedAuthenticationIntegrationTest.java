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
import software.amazon.awssdk.services.securitytoken.auth.STSSessionCredentialsProvider;

/** Tests of session-based auth. */
public class EC2SessionBasedAuthenticationIntegrationTest extends EC2IntegrationTestBase {

    @Test
    public void smokeTest() throws Exception {
        setUpCredentials();
        AmazonEC2 ec2 = new AmazonEC2Client(new STSSessionCredentialsProvider(getCredentials()));
        ec2.describeAvailabilityZones();
    }
}
