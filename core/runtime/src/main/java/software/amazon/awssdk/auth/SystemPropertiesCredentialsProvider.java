/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.auth;

import static software.amazon.awssdk.SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY;
import static software.amazon.awssdk.SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.util.StringUtils;

/**
 * {@link AWSCredentialsProvider} implementation that provides credentials by
 * looking at the <code>aws.accessKeyId</code> and <code>aws.secretKey</code>
 * Java system properties.
 */
public class SystemPropertiesCredentialsProvider implements AWSCredentialsProvider {

    @Override
    public AWSCredentials getCredentials() {
        String accessKey =
            StringUtils.trim(System.getProperty(ACCESS_KEY_SYSTEM_PROPERTY));

        String secretKey =
            StringUtils.trim(System.getProperty(SECRET_KEY_SYSTEM_PROPERTY));

        if (StringUtils.isNullOrEmpty(accessKey)
                || StringUtils.isNullOrEmpty(secretKey)) {

            throw new SdkClientException(
                    "Unable to load AWS credentials from Java system "
                    + "properties (" + ACCESS_KEY_SYSTEM_PROPERTY + " and "
                    + SECRET_KEY_SYSTEM_PROPERTY + ")");
        }

        return new BasicAWSCredentials(accessKey, secretKey);
    }

    @Override
    public void refresh() {}

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}