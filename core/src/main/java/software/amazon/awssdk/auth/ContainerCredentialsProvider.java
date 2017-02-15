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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.retry.internal.CredentialsEndpointRetryPolicy;

/**
 * <p>
 * {@link AWSCredentialsProvider} implementation that loads credentials
 * from an Amazon Elastic Container.
 * </p>
 * <p>
 * By default, the URI path is retrieved from the environment variable
 * "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" in the container's environment.
 * </p>
 */
public class ContainerCredentialsProvider implements AWSCredentialsProvider {

    /** Environment variable to get the Amazon ECS credentials resource path. */
    static final String ECS_CONTAINER_CREDENTIALS_PATH = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";

    /** Default endpoint to retreive the Amazon ECS Credentials. */
    private static final String ECS_CREDENTIALS_ENDPOINT = "http://169.254.170.2";

    private final EC2CredentialsFetcher credentialsFetcher;

    public ContainerCredentialsProvider() {
        this(new ECSCredentialsEndpointProvider());
    }

    @SdkInternalApi
    public ContainerCredentialsProvider(CredentialsEndpointProvider credentailsEndpointProvider) {
        this.credentialsFetcher = new EC2CredentialsFetcher(credentailsEndpointProvider);
    }

    @Override
    public AWSCredentials getCredentials() {
        return credentialsFetcher.getCredentials();
    }

    @Override
    public void refresh() {
        credentialsFetcher.refresh();
    }

    public Date getCredentialsExpiration() {
        return credentialsFetcher.getCredentialsExpiration();
    }


    private static class ECSCredentialsEndpointProvider extends CredentialsEndpointProvider {
        @Override
        public URI getCredentialsEndpoint() throws URISyntaxException {
            String path = System.getenv(ECS_CONTAINER_CREDENTIALS_PATH);
            if (path == null) {
                throw new SdkClientException(
                        "The environment variable " + ECS_CONTAINER_CREDENTIALS_PATH + " is empty");
            }

            return new URI(ECS_CREDENTIALS_ENDPOINT + path);
        }

        @Override
        public CredentialsEndpointRetryPolicy getRetryPolicy() {
            return ContainerCredentialsRetryPolicy.getInstance();
        }
    }

}
