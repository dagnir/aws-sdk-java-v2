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

package software.amazon.awssdk.services.cloudtrail;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AWSCloudTrailClient cloudTrail;
    protected static AmazonS3Client s3;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
        cloudTrail = new AWSCloudTrailClient(credentials);
        s3 = new AmazonS3Client(credentials);
    }
}
