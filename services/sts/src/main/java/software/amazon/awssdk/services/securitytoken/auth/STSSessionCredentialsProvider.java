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

package software.amazon.awssdk.services.securitytoken.auth;

import java.util.concurrent.Callable;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.auth.AWSSessionCredentials;
import software.amazon.awssdk.auth.AWSSessionCredentialsProvider;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenService;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenServiceClient;
import software.amazon.awssdk.services.securitytoken.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.securitytoken.model.GetSessionTokenResult;

/**
 * AWSCredentialsProvider implementation that uses the AWS Security Token Service to create
 * temporary, short-lived sessions to use for authentication.
 */
@ThreadSafe
public class STSSessionCredentialsProvider implements AWSSessionCredentialsProvider {

    /**
     * Default duration for started sessions
     */
    public static final int DEFAULT_DURATION_SECONDS = 3600;

    /**
     * The client for starting STS sessions
     */
    private final AWSSecurityTokenService securityTokenService;

    private final Callable<SessionCredentialsHolder> refreshCallable = new Callable<SessionCredentialsHolder>() {
        @Override
        public SessionCredentialsHolder call() throws Exception {
            return newSession();
        }
    };

    /**
     * Handles the refreshing of sessions. Ideally this should be final but #setSTSClientEndpoint
     * forces us to create a new one.
     */
    private volatile RefreshableTask<SessionCredentialsHolder> refreshableTask;


    /**
     * Constructs a new STSSessionCredentialsProvider, which will use the specified long lived AWS
     * credentials to make a request to the AWS Security Token Service (STS) to request short lived
     * session credentials, which will then be returned by this class's {@link #getCredentials()}
     * method.
     *
     * @param longLivedCredentials The main AWS credentials for a user's account.
     */
    public STSSessionCredentialsProvider(AWSCredentials longLivedCredentials) {
        this(longLivedCredentials, new ClientConfiguration());
    }

    /**
     * Constructs a new STSSessionCredentialsProvider, which will use the specified long lived AWS
     * credentials to make a request to the AWS Security Token Service (STS) to request short lived
     * session credentials, which will then be returned by this class's {@link #getCredentials()}
     * method.
     *
     * @param longLivedCredentials The main AWS credentials for a user's account.
     * @param clientConfiguration  Client configuration connection parameters.
     */
    public STSSessionCredentialsProvider(AWSCredentials longLivedCredentials,
                                         ClientConfiguration clientConfiguration) {
        this(new AWSSecurityTokenServiceClient(longLivedCredentials, clientConfiguration));
    }

    /**
     * Constructs a new STSSessionCredentialsProvider, which will use the specified credentials
     * provider (which vends long lived AWS credentials) to make a request to the AWS Security Token
     * Service (STS) to request short lived session credentials, which will then be returned by this
     * class's {@link #getCredentials()} method.
     *
     * @param longLivedCredentialsProvider Credentials provider for the main AWS credentials for a
     *                                     user's account.
     */
    public STSSessionCredentialsProvider(AWSCredentialsProvider longLivedCredentialsProvider) {
        this(new AWSSecurityTokenServiceClient(longLivedCredentialsProvider));
    }

    /**
     * Constructs a new STSSessionCredentialsProvider, which will use the specified credentials
     * provider (which vends long lived AWS credentials) to make a request to the AWS Security Token
     * Service (STS) to request short lived session credentials, which will then be returned by this
     * class's {@link #getCredentials()} method.
     *
     * @param longLivedCredentialsProvider Credentials provider for the main AWS credentials for a
     *                                     user's account.
     * @param clientConfiguration          Client configuration connection parameters.
     */
    public STSSessionCredentialsProvider(AWSCredentialsProvider longLivedCredentialsProvider,
                                         ClientConfiguration clientConfiguration) {

        this(new AWSSecurityTokenServiceClient(longLivedCredentialsProvider, clientConfiguration));
    }

    /**
     * Constructs a new STSSessionCredentialsProvider with the alredy configured STS client.
     *
     * @param sts Preconfigured STS client to use for this provider
     */
    public STSSessionCredentialsProvider(AWSSecurityTokenService sts) {
        this.securityTokenService = sts;
        this.refreshableTask = createRefreshableTask();
    }

    private RefreshableTask<SessionCredentialsHolder> createRefreshableTask() {
        return new RefreshableTask.Builder<SessionCredentialsHolder>()
                .withRefreshCallable(refreshCallable)
                .withBlockingRefreshPredicate(new ShouldDoBlockingSessionRefresh())
                .withAsyncRefreshPredicate(new ShouldDoAsyncSessionRefresh()).build();
    }

    /**
     * Sets the AWS Security Token Service (STS) endpoint where session credentials are retrieved
     * from. <p> </p> The default AWS Security Token Service (STS) endpoint ("sts.amazonaws.com")
     * works for all accounts that are not for China (Beijing) region or GovCloud. You only need to
     * change the endpoint to "sts.cn-north-1.amazonaws.com.cn" when you are requesting session
     * credentials for services in China(Beijing) region or "sts.us-gov-west-1.amazonaws.com" for
     * GovCloud. <p> </p> Setting this invalidates existing session credentials. Calling this method
     * will temporarily cause getCredentials() to block until a new session is fetched from the STS
     * service.
     *
     * @deprecated This method may be removed in a future major version. Create multiple providers
     * if you need to work with multiple STS endpoints.
     */
    @Deprecated
    public synchronized void setSTSClientEndpoint(String endpoint) {
        securityTokenService.setEndpoint(endpoint);
        // Create a new task rather then trying to synchronize this in the refreshable task
        this.refreshableTask = createRefreshableTask();
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
    public AWSSessionCredentials getCredentials() {
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