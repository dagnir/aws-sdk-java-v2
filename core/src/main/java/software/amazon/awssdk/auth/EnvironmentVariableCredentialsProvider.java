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

package software.amazon.awssdk.auth;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.SdkGlobalConfiguration;
import software.amazon.awssdk.util.StringUtils;

/**
 * {@link AwsCredentialsProvider} implementation that provides credentials
 * by looking at the: <code>AWS_ACCESS_KEY_ID</code> (or <code>AWS_ACCESS_KEY</code>) and
 * <code>AWS_SECRET_KEY</code> (or <code>AWS_SECRET_ACCESS_KEY</code>) environment variables.
 */
public class EnvironmentVariableCredentialsProvider implements AwsCredentialsProvider {
    @Override
    public AwsCredentials getCredentials() {
        String accessKey = System.getenv(SdkGlobalConfiguration.ACCESS_KEY_ID_ENV_VAR);
        String secretKey = System.getenv(SdkGlobalConfiguration.SECRET_KEY_ENV_VAR);

        accessKey = StringUtils.trim(accessKey);
        secretKey = StringUtils.trim(secretKey);
        String sessionToken =
                StringUtils.trim(System.getenv(SdkGlobalConfiguration.AWS_SESSION_TOKEN_ENV_VAR));

        if (StringUtils.isNullOrEmpty(accessKey)
            || StringUtils.isNullOrEmpty(secretKey)) {

            throw new SdkClientException("Unable to load AWS credentials from environment variables (" +
                                         SdkGlobalConfiguration.ACCESS_KEY_ID_ENV_VAR + "  and " +
                                         SdkGlobalConfiguration.SECRET_KEY_ENV_VAR + ")");
        }

        return sessionToken == null ?
               new BasicAwsCredentials(accessKey, secretKey)
                                    :
               new BasicSessionCredentials(accessKey, secretKey, sessionToken);
    }

    @Override
    public void refresh() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
