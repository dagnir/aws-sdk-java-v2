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

import java.util.Date;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsRefreshableSessionCredentials;
import software.amazon.awssdk.auth.AwsSessionCredentials;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicSessionCredentials;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResult;

/**
 * Session credentials periodically refreshed by AWS stsService.
 * <p>
 * Calls to {@link StsSessionCredentials#getAwsAccessKeyId()},
 * {@link StsSessionCredentials#getAwsSecretKey()}, and
 * {@link StsSessionCredentials#getSessionToken()} should be synchronized on
 * this object to prevent races on the boundary of session expiration.
 * Alternately, clients can call
 * {@link StsSessionCredentials#getImmutableCredentials()} to ensure a
 * consistent set of access key, secret key, and token.
 * <p>
 * This class is deprecated and should not be used anymore.
 * Instead, use {@link StsSessionCredentialsProvider}.
 */
@Deprecated
public class StsSessionCredentials implements AwsRefreshableSessionCredentials {

    public static final int DEFAULT_DURATION_SECONDS = 3600;
    private final STSClient stsService;
    private final int sessionDurationSeconds;
    private Credentials sessionCredentials;

    /**
     * Create a new credentials object that will periodically and automatically
     * obtain a session from STS.
     *
     * @param credentials
     *            Primary AWS account credentials.
     */
    public StsSessionCredentials(AwsCredentials credentials) {
        this(credentials, DEFAULT_DURATION_SECONDS);
    }

    /**
     * Create a new credentials object that will periodically and automatically
     * obtain a session from STS.
     *
     * @param credentials
     *            Primary AWS account credentials.
     * @param sessionDurationSeconds
     *            The duration, in seconds, for each session to last.
     */
    public StsSessionCredentials(AwsCredentials credentials, int sessionDurationSeconds) {
        this.stsService = STSClient.builder().credentialsProvider(new AwsStaticCredentialsProvider(credentials)).build();
        this.sessionDurationSeconds = sessionDurationSeconds;
    }

    /**
     * Create a new credentials object that will periodically and automatically
     * obtain a session from STS, using a preconfigured STS client.
     *
     * @param stsClient
     *            A pre-configured STS client from which to get credentials.
     */
    public StsSessionCredentials(STSClient stsClient) {
        this(stsClient, DEFAULT_DURATION_SECONDS);
    }

    /**
     * Create a new credentials object that will periodically and automatically
     * obtain a session from STS, using a preconfigured STS client.
     *
     * @param stsClient
     *            A pre-configured STS client from which to get credentials.
     * @param settings
     *            Session settings for all sessions created
     */
    public StsSessionCredentials(STSClient stsClient, int sessionDuratinSeconds) {
        this.stsService = stsClient;
        this.sessionDurationSeconds = sessionDuratinSeconds;
    }

    /**
     * Returns the AWS access key for the current STS session, beginning a new
     * one if necessary.
     * <p>
     * Clients are encouraged to call the atomic
     * {@link RenewableAWSSessionCredentials#getImmutableCredentials()} as a proxy to this method.
     */
    @Override
    public synchronized String getAwsAccessKeyId() {
        return getSessionCredentials().getAccessKeyId();
    }

    /**
     * Returns the AWS secret key for the current STS session, beginning a new
     * one if necessary.
     * <p>
     * Clients are encouraged to call the atomic
     * {@link RenewableAWSSessionCredentials#getImmutableCredentials()} as a proxy to this method.
     */
    @Override
    public synchronized String getAwsSecretKey() {
        return getSessionCredentials().getSecretAccessKey();
    }

    /**
     * Returns the session token for the current STS session, beginning a new
     * one if necessary.
     * <p>
     * Clients are encouraged to call the atomic
     * {@link RenewableAWSSessionCredentials#getImmutableCredentials()} as a proxy to this method.
     */
    @Override
    public synchronized String getSessionToken() {
        return getSessionCredentials().getSessionToken();
    }

    /**
     * Returns immutable session credentials for this session, beginning a new one if necessary.
     */
    public synchronized AwsSessionCredentials getImmutableCredentials() {
        Credentials creds = getSessionCredentials();
        return new BasicSessionCredentials(creds.getAccessKeyId(), creds.getSecretAccessKey(), creds.getSessionToken());
    }

    /**
     * Refreshes the session credentials from STS.
     */
    @Override
    public synchronized void refreshCredentials() {
        GetSessionTokenResult sessionTokenResult = stsService
                .getSessionToken(new GetSessionTokenRequest().withDurationSeconds(sessionDurationSeconds));
        sessionCredentials = sessionTokenResult.getCredentials();
    }

    /**
     * Gets a current session credentials object, reinitializing if necessary.
     */
    private synchronized Credentials getSessionCredentials() {
        if (needsNewSession()) {
            refreshCredentials();
        }
        return sessionCredentials;
    }

    private boolean needsNewSession() {
        if (sessionCredentials == null) {
            return true;
        }

        Date expiration = sessionCredentials.getExpiration();
        long timeRemaining = expiration.getTime() - System.currentTimeMillis();
        if (timeRemaining < (60 * 1000)) {
            return true;
        }

        return false;
    }
}
