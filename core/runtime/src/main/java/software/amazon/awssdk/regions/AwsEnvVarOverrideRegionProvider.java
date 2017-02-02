/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.regions;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.SDKGlobalConfiguration;

import static software.amazon.awssdk.SDKGlobalConfiguration.AWS_REGION_ENV_VAR;

/**
 * Loads region information from the '{@value SDKGlobalConfiguration#AWS_REGION_ENV_VAR}'
 * environment variable.
 */
public class AwsEnvVarOverrideRegionProvider extends AwsRegionProvider {

    @Override
    public String getRegion() throws SdkClientException {
        return System.getenv(SDKGlobalConfiguration.AWS_REGION_ENV_VAR);
    }
}
