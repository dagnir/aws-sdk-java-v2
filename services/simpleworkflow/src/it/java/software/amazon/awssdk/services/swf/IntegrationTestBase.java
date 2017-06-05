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

package software.amazon.awssdk.services.swf;

import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class IntegrationTestBase extends AwsIntegrationTestBase {

    protected static SWFClient swf;

    /**
     * Loads the AWS account info for the integration tests and creates a client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        swf = SWFClient.builder()
                       .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                       .region(Region.US_EAST_1)
                       .build();
    }

}
