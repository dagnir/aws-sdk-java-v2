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

package software.amazon.awssdk.services.securitytoken.internal;

import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.profile.internal.securitytoken.ProfileCredentialsService;
import software.amazon.awssdk.auth.profile.internal.securitytoken.RoleInfo;
import software.amazon.awssdk.services.securitytoken.auth.StsAssumeRoleSessionCredentialsProvider;

/**
 * Loaded via reflection by the core module when role assumption is configured in a
 * credentials profile.
 */
public class StsProfileCredentialsService implements ProfileCredentialsService {
    @Override
    public AwsCredentialsProvider getAssumeRoleCredentialsProvider(RoleInfo targetRoleInfo) {
        return new StsAssumeRoleSessionCredentialsProvider.Builder(targetRoleInfo.getRoleArn(),
                                                                   targetRoleInfo.getRoleSessionName())
                .withLongLivedCredentialsProvider(targetRoleInfo.getLongLivedCredentialsProvider())
                .withExternalId(targetRoleInfo.getExternalId())
                .build();
    }
}
