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

package software.amazon.awssdk.services.importexport;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for ImportExport integration tests; responsible for loading AWS
 * account info, instantiating clients, providing common helper methods, etc.
 *
 * @author fulghum@amazon.com
 */
public abstract class IntegrationTestBase extends AwsTestBase {

    protected static ImportExportClient ie;
    protected static AmazonS3 s3;

    /**
     * Loads the AWS account info for the integration tests and creates the
     * client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        ie = ImportExportClient.builder().withCredentials(CREDENTIALS_PROVIDER_CHAIN).build();
        s3 = new AmazonS3Client(credentials);
    }

}
