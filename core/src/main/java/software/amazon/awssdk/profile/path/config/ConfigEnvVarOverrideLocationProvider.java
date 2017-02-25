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

package software.amazon.awssdk.profile.path.config;

import java.io.File;
import software.amazon.awssdk.SdkGlobalConfiguration;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.profile.path.AwsProfileFileLocationProvider;

/**
 * If the {@value SdkGlobalConfiguration#AWS_CONFIG_FILE_ENV_VAR} environment variable is set then we source
 * the config file from the location specified.
 */
@SdkInternalApi
public class ConfigEnvVarOverrideLocationProvider implements AwsProfileFileLocationProvider {

    @Override
    public File getLocation() {
        String overrideLocation = System.getenv(SdkGlobalConfiguration.AWS_CONFIG_FILE_ENV_VAR);
        if (overrideLocation != null) {
            return new File(overrideLocation);
        }
        return null;
    }
}
