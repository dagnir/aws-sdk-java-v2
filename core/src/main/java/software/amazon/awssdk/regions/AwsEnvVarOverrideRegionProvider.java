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

package software.amazon.awssdk.regions;

import static software.amazon.awssdk.SDKGlobalConfiguration.AWS_REGION_ENV_VAR;

import software.amazon.awssdk.SdkClientException;

/**
 * Loads region information from the '{@value software.amazon.awssdk.SDKGlobalConfiguration#AWS_REGION_ENV_VAR}'
 * environment variable.
 */
public class AwsEnvVarOverrideRegionProvider extends AwsRegionProvider {

    @Override
    public String getRegion() throws SdkClientException {
        return System.getenv(AWS_REGION_ENV_VAR);
    }
}
