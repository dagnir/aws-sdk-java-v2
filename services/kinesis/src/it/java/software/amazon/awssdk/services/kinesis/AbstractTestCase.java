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

package software.amazon.awssdk.services.kinesis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.junit.BeforeClass;
import software.amazon.awssdk.test.AwsTestBase;

public class AbstractTestCase extends AwsTestBase {
    protected static AmazonKinesisClient client;
    private static final String DEFAULT_ENDPOINT = "https://kinesis.us-east-1.amazonaws.com";

    @BeforeClass
    public static void init() throws FileNotFoundException, IOException {
        setUpCredentials();
        client = new AmazonKinesisClient(credentials);
        setEndpoint();
    }

    private static void setEndpoint() throws IOException {
        File endpointOverrides = new File(
                new File(System.getProperty("user.home")),
                ".aws/awsEndpointOverrides.properties"
        );

        if (endpointOverrides.exists()) {
            Properties properties = new Properties();
            properties.load(new FileInputStream(endpointOverrides));

            String endpoint = properties.getProperty("kinesis.endpoint");

            if (endpoint != null) {
                client.setEndpoint(endpoint);
                return;
            }

        }

        client.setEndpoint(DEFAULT_ENDPOINT);
    }
}