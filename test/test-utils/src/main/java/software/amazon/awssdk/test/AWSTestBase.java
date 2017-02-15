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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.AWSCredentialsProviderChain;
import software.amazon.awssdk.auth.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.PropertiesFileCredentialsProvider;
import software.amazon.awssdk.auth.SystemPropertiesCredentialsProvider;
import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.test.util.InputStreamUtils;
import software.amazon.awssdk.test.util.SdkAsserts;
import software.amazon.awssdk.util.IOUtils;

public abstract class AWSTestBase {

    /** Default Properties Credentials file path */
    private static final String propertiesFilePath = System.getProperty("user.home")
                                                     + "/.aws/awsTestAccount.properties";
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-java-sdk-test";
    private static final AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(
            new PropertiesFileCredentialsProvider(propertiesFilePath),
            new ProfileCredentialsProvider(TEST_CREDENTIALS_PROFILE_NAME), new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider());
    /**
     * Shared AWS credentials, loaded from a properties file. Direct access to this field is
     * deprecated
     *
     * @deprecated Extend from {@link AWSIntegrationTestBase} to access credentials
     */
    @Deprecated
    public static AWSCredentials credentials;

    /**
     * @deprecated Extend from {@link AWSIntegrationTestBase} to access credentials
     */
    @Deprecated
    public static void setUpCredentials() {
        if (credentials == null) {
            try {
                credentials = chain.getCredentials();
            } catch (Exception ignored) {
            }
        }
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
            String resourceAsString = IOUtils.toString(resourceStream);
            resourceStream.close();
            return resourceAsString;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertNotEmpty(String str) {
        SdkAsserts.assertNotEmpty(str);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertFileEqualsStream(File expected, InputStream actual) {
        SdkAsserts.assertFileEqualsStream(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertFileEqualsStream(String errmsg, File expected, InputStream actual) {
        SdkAsserts.assertFileEqualsStream(errmsg, expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertStreamEqualsStream(InputStream expected, InputStream actual) {
        SdkAsserts.assertStreamEqualsStream(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertStreamEqualsStream(String errmsg, InputStream expectedInputStream, InputStream inputStream) {
        assertStreamEqualsStream(errmsg, expectedInputStream, inputStream);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertFileEqualsFile(File expected, File actual) {
        SdkAsserts.assertFileEqualsFile(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertStringEqualsStream(String expected, InputStream actual) {
        SdkAsserts.assertStringEqualsStream(expected, actual);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected boolean doesStreamEqualStream(InputStream expected, InputStream actual) throws IOException {
        return SdkAsserts.doesStreamEqualStream(expected, actual);
    }

    /**
     * @deprecated Use {@link InputStreamUtils#drainInputStream(InputStream)}
     */
    @Deprecated
    protected byte[] drainInputStream(InputStream inputStream) {
        return InputStreamUtils.drainInputStream(inputStream);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected boolean doesFileEqualStream(File expectedFile, InputStream inputStream) throws IOException {
        return SdkAsserts.doesFileEqualStream(expectedFile, inputStream);
    }

    /**
     * @deprecated Use static imports for custom asserts in {@link SdkAsserts} instead
     */
    @Deprecated
    protected void assertValidException(AmazonServiceException e) {
        SdkAsserts.assertValidException(e);
    }
}
