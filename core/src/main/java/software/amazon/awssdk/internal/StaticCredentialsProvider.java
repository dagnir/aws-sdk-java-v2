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

package software.amazon.awssdk.internal;

import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;

/**
 * Simple implementation of AwsCredentialsProvider that just wraps static AwsCredentials.
 *
 * @deprecated By {@link com.amazonaws.auth.AWSStaticCredentialsProvider}
 */
@Deprecated
public class StaticCredentialsProvider implements AwsCredentialsProvider {

    private final AwsCredentials credentials;

    public StaticCredentialsProvider(AwsCredentials credentials) {
        this.credentials = credentials;
    }

    public AwsCredentials getCredentials() {
        return credentials;
    }

    public void refresh() {
    }

}
