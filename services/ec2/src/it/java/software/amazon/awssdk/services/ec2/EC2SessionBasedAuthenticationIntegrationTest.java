/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.services.ec2;

import org.junit.Test;

import software.amazon.awssdk.auth.STSSessionCredentialsProvider;

/** Tests of session-based auth. */
public class EC2SessionBasedAuthenticationIntegrationTest extends EC2IntegrationTestBase {

    @Test
    public void smokeTest() throws Exception {
        setUpCredentials();
        AmazonEC2 ec2 = new AmazonEC2Client(new STSSessionCredentialsProvider(getCredentials()));
        ec2.describeAvailabilityZones();
    }
}
