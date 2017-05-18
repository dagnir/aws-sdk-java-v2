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

import java.util.Optional;
import software.amazon.awssdk.SdkClientException;

/**
 * Interface for loading {@link AwsCredentials} that are used for authentication.
 *
 * <p>Commonly-used implementations include {@link StaticCredentialsProvider} for a fixed set of credentials and the
 * {@link DefaultCredentialsProvider} for discovering credentials from the host's environment. The AWS Security Token
 * Service (STS) client also provides implementations of this interface for loading temporary, limited-privilege credentials from
 * AWS STS.</p>
 */
@FunctionalInterface
public interface AwsCredentialsProvider {
    /**
     * Returns {@link AwsCredentials} that can be used to authorize an AWS request. Each implementation of AWSCredentialsProvider
     * can chose its own strategy for loading credentials. For example, an implementation might load credentials from an existing
     * key management system, or load new credentials when credentials are rotated.
     *
     * <p>If an error occurs during the loading of credentials, a runtime exception will be raised. If there was no error
     * attempting to load credentials, but no credentials were found, {@link Optional#empty()} will be returned.</p>
     *
     * @return AwsCredentials which the caller can use to authorize an AWS request, or {@link Optional#empty()} if no credentials
     *         are found.
     */
    Optional<AwsCredentials> getCredentials();

    /**
     * Returns {@link AwsCredentials} that can be used to authorize an AWS request. By default, this delegates to
     * {@link #getCredentials()}.
     *
     * <p>If an error occurs during the loading of credentials, a runtime exception will be raised. If there was no error
     * attempting to load credentials, but no credentials were found, An {@link SdkClientException} will be raised.</p>
     *
     * @return AwsCredentials which the caller can use to authorize an AWS request.
     * @throws SdkClientException if no credentials can be found by this provider.
     */
    default AwsCredentials getCredentialsOrThrow() throws SdkClientException {
        return getCredentials().orElseThrow(() ->
                new SdkClientException("Credentials from the credentials provider '" + toString() + "' were invalid. The "
                                       + "credential provider successfully loaded the credentials, but the credentials that were "
                                       + "loaded were null. Please check to make sure you are using the correct credential "
                                       + "provider and that your credentials are not set to null."));
    }
}
