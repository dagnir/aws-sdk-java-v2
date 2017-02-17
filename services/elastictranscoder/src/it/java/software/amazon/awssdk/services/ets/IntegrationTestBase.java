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

package software.amazon.awssdk.services.ets;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.elastictranscoder.AmazonElasticTranscoderClient;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.sns.AmazonSNSClient;
import software.amazon.awssdk.test.AwsTestBase;

public class IntegrationTestBase extends AwsTestBase {

    protected static AmazonElasticTranscoderClient ets;
    protected static AmazonS3Client s3;
    protected static AmazonSNSClient sns;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
        ets = new AmazonElasticTranscoderClient(credentials);
        //  ets.setEndpoint("https://ets-beta.us-east-1.amazon.com", "elastictranscoder", "us-west-2");
        s3 = new AmazonS3Client(credentials);
        sns = new AmazonSNSClient(credentials);
    }
}
