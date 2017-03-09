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

package software.amazon.awssdk.test;

import java.io.InputStream;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.PropertiesFileCredentialsProvider;
import software.amazon.awssdk.auth.SystemPropertiesCredentialsProvider;
import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.util.IoUtils;

public abstract class AwsIntegrationTestBase {

    /** Default Properties Credentials file path. */
    private static final String PROPERTIES_FILE_PATH = System.getProperty("user.home")
                                                       + "/.aws/awsTestAccount.properties";
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-java-sdk-test";
    private static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN = new AwsCredentialsProviderChain(
            new PropertiesFileCredentialsProvider(PROPERTIES_FILE_PATH),
            new ProfileCredentialsProvider(TEST_CREDENTIALS_PROFILE_NAME), new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider());
    /**
     * Shared AWS credentials, loaded from a properties file.
     */
    private static AwsCredentials credentials;

    /**
     * Before of super class is guaranteed to be called before that of a subclass so the following
     * is safe. http://junit-team.github.io/junit/javadoc/latest/org/junit/Before.html
     */
    @BeforeClass
    public static void setUpCredentials() {
        if (credentials == null) {
            try {
                credentials = CREDENTIALS_PROVIDER_CHAIN.getCredentials();
            } catch (Exception ignored) {
                // Ignored.
            }
        }
    }

    /**
     * @return AWSCredentials to use during tests. Setup by base fixture
     */
    protected static AwsCredentials getCredentials() {
        return credentials;
    }

    /**
     * Reads a system resource fully into a String
     *
     * @param location
     *            Relative or absolute location of system resource.
     * @return String contents of resource file
     * @throws RuntimeException
     *             if any error occurs
     */
    protected String getResourceAsString(String location) {
        try {
            InputStream resourceStream = getClass().getResourceAsStream(location);
            String resourceAsString = IoUtils.toString(resourceStream);
            resourceStream.close();
            return resourceAsString;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
