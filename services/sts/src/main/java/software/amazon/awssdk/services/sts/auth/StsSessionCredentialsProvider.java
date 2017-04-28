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

package software.amazon.awssdk.services.sts.auth;

import java.util.concurrent.Callable;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.auth.AwsSessionCredentials;
import software.amazon.awssdk.auth.AwsSessionCredentialsProvider;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResult;

/**
 * AWSCredentialsProvider implementation that uses the AWS Security Token Service to create
 * temporary, short-lived sessions to use for authentication.
 */
@ThreadSafe
public class StsSessionCredentialsProvider implements AwsSessionCredentialsProvider {

    /**
     * Default duration for started sessions
     */
    public static final int DEFAULT_DURATION_SECONDS = 3600;

    /**
     * The client for starting STS sessions
     */
    private final STSClient securityTokenService;

    private final Callable<SessionCredentialsHolder> refreshCallable = this::newSession;

    /**
     * Handles the refreshing of sessions. Ideally this should be final but #setSTSClientEndpoint
     * forces us to create a new one.
     */
    private volatile RefreshableTask<SessionCredentialsHolder> refreshableTask;


    /**
     * Constructs a new StsSessionCredentialsProvider, which will use the specified long-lived AWS
     * STS client to make a request to the AWS Security Token Service (STS) to request short lived
     * session credentials, which will then be returned by this class's {@link #getCredentials()}
     * method.
     *
     * @param securityTokenService The STS client to use for loading short-lived STS credentials.
     */
    public StsSessionCredentialsProvider(STSClient securityTokenService) {
        this.securityTokenService = securityTokenService;
        this.refreshableTask = createRefreshableTask();
    }

    private RefreshableTask<SessionCredentialsHolder> createRefreshableTask() {
        return new RefreshableTask.Builder<SessionCredentialsHolder>()
                .withRefreshCallable(refreshCallable)
                .withBlockingRefreshPredicate(new ShouldDoBlockingSessionRefresh())
                .withAsyncRefreshPredicate(new ShouldDoAsyncSessionRefresh()).build();
    }

    /**
     * Method will return valid session credentials or throw an AmazonClientException due to STS
     * service time-out or thread interruption. The first call will block until valid session
     * credentials are fetched. Subsequent calls will re-use fetched credentials that are still
     * valid. Expiring credentials are automatically refreshed via a background thread. Multiple
     * threads may call this method concurrently without causing simultaneous network calls to the
     * STS service. Care has been taken to resist Throttling exceptions.
     */
    @Override
    public AwsSessionCredentials getCredentials() {
        return refreshableTask.getValue().getSessionCredentials();
    }

    /**
     * Force refresh of session credentials. A decision to use this method should be made
     * judiciously since this class automatically manages refreshing expiring credentials limiting
     * its usefulness. Calling this method may temporarily cause getCredentials() to block until a
     * new session is fetched from the STS service.
     */
    @Override
    public void refresh() {
        refreshableTask.forceGetValue();
    }

    private SessionCredentialsHolder newSession() {
        GetSessionTokenResult sessionTokenResult = securityTokenService.getSessionToken(
                new GetSessionTokenRequest().withDurationSeconds(DEFAULT_DURATION_SECONDS));
        return new SessionCredentialsHolder(sessionTokenResult.getCredentials());
    }


}
