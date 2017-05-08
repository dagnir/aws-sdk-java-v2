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
import software.amazon.awssdk.auth.AwsSessionCredentials;
import software.amazon.awssdk.auth.AwsSessionCredentialsProvider;
import software.amazon.awssdk.auth.BasicSessionCredentials;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResult;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * AWSCredentialsProvider implementation that uses the AWS Security Token
 * Service to create temporary, short-lived sessions to use for authentication.
 */
public class WebIdentityFederationSessionCredentialsProvider implements AwsSessionCredentialsProvider {

    /** Default duration for started sessions. */
    public static final int DEFAULT_DURATION_SECONDS = 3600;

    /** Default threshold for refreshing session credentials. */
    public static final int DEFAULT_THRESHOLD_SECONDS = 500;

    /** The client for starting STS sessions. */
    private final STSClient securityTokenService;
    private final String wifToken;
    private final String wifProvider;
    private final String roleArn;
    /** The current session credentials. */
    private AwsSessionCredentials sessionCredentials;
    /** The expiration time for the current session credentials. */
    private Date sessionCredentialsExpiration;
    private int sessionDuration;
    private int refreshThreshold;
    private String subjectFromWif;

    /**
     * Constructs a new WebIdentityFederationSessionCredentialsProvider, which will use the
     * specified 3rd-party web identity provider to make a request to the AWS
     * Security Token Service (STS) using the provided client to request short 
     * lived session credentials, which will then be returned by this class's 
     * {@link #getCredentials()} method.
     *
     * @param wifToken
     *            The OAuth/OpenID token from the the Identity Provider
     * @param wifProvider
     *            The name of the Identity Provider (null for OpenID providers)
     * @param roleArn
     *            The ARN of the IAM Role that will be assumed
     * @param stsClient
     *            Preconfigured STS client to make requests with
     */
    public WebIdentityFederationSessionCredentialsProvider(String wifToken, String wifProvider, String roleArn,
                                                           STSClient stsClient) {
        this.securityTokenService = stsClient;
        this.wifProvider = wifProvider;
        this.wifToken = wifToken;
        this.roleArn = roleArn;
        this.sessionDuration = DEFAULT_DURATION_SECONDS;
        this.refreshThreshold = DEFAULT_THRESHOLD_SECONDS;
    }

    @Override
    public AwsSessionCredentials getCredentials() {
        if (needsNewSession()) {
            startSession();
        }

        return sessionCredentials;
    }

    @Override
    public void refresh() {
        startSession();
    }

    /**
     * Set the duration of the session credentials created by this client in
     * seconds. Values must be supported by AssumeRoleWithWebIdentityRequest.
     * Returns refreence to object so methods can be chained together.
     *
     * @see software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest
     *
     * @param sessionDuration
     *              The new duration for session credentials created by this
     *              provider
     *
     * @return A reference to this updated object so that method calls
     *          can be chained together.
     *
     */
    public WebIdentityFederationSessionCredentialsProvider withSessionDuration(int sessionDuration) {
        this.setSessionDuration(sessionDuration);
        return this;
    }

    /**
     * Get the duration of the session credentials created by this client in
     * seconds. Values must be supported by AssumeRoleWithWebIdentityRequest.
     *
     * @see software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest
     *
     * @return The duration for session credentials created by this provider
     */
    public int getSessionDuration() {
        return this.sessionDuration;
    }

    /**
     * Set the duration of the session credentials created by this client in
     * seconds. Values must be supported by AssumeRoleWithWebIdentityRequest.
     *
     * @see software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest
     *
     * @param sessionDuration
     *              The new duration for session credentials created by this
     *              provider
     */
    public void setSessionDuration(int sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    /**
     * Set the refresh threshold for the session credentials created by this client in
     * seconds. This value will be used internally to determine if new
     * credentials should be fetched from STS. Returns a refrence to the object
     * so methods can be chained.
     *
     * @see software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest
     *
     * @param refreshThreshold
     *              The new refresh threshold for session credentials created by this
     *              provider
     *
     * @return A reference to this updated object so that method calls
     *          can be chained together.
     *
     */
    public WebIdentityFederationSessionCredentialsProvider withRefreshThreshold(int refreshThreshold) {
        this.setRefreshThreshold(refreshThreshold);
        return this;
    }

    /**
     * Get the refresh threshold for the session credentials created by this client in
     * seconds. This value will be used internally to determine if new
     * credentials should be fetched from STS.
     *
     * @see software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest
     *
     * @return The refresh threshold for session credentials created by this provider
     */
    public int getRefreshThreshold() {
        return this.refreshThreshold;
    }

    /**
     * Set the refresh threshold for the session credentials created by this client in
     * seconds. This value will be used internally to determine if new
     * credentials should be fetched from STS.
     *
     * @see software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest
     *
     * @param refreshThreshold
     *              The new refresh threshold for session credentials created by this
     *              provider
     */
    public void setRefreshThreshold(int refreshThreshold) {
        this.refreshThreshold = refreshThreshold;
    }

    /**
     * Get the identifier returned from the Identity Provider for the
     * authenticated user.  This value is returned as part of the
     * AssumeRoleWithIdentityResult
     *
     * @see software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResult
     *
     * @return The identifier returned from Identity Provider
     */
    public String getSubjectFromWif() {
        return this.subjectFromWif;
    }

    /**
     * Starts a new session by sending a request to the AWS Security Token
     * Service (STS) with the long lived AWS credentials. This class then vends
     * the short lived session credentials sent back from STS.
     */
    private void startSession() {
        AssumeRoleWithWebIdentityResult sessionTokenResult = securityTokenService
                .assumeRoleWithWebIdentity(new AssumeRoleWithWebIdentityRequest().withWebIdentityToken(wifToken)
                                                                                 .withProviderId(wifProvider)
                                                                                 .withRoleArn(roleArn)
                                                                                 .withRoleSessionName("ProviderSession")
                                                                                 .withDurationSeconds(this.sessionDuration));
        Credentials stsCredentials = sessionTokenResult.getCredentials();

        subjectFromWif = sessionTokenResult.getSubjectFromWebIdentityToken();

        sessionCredentials = new BasicSessionCredentials(
                stsCredentials.getAccessKeyId(),
                stsCredentials.getSecretAccessKey(),
                stsCredentials.getSessionToken());
        sessionCredentialsExpiration = stsCredentials.getExpiration();
    }

    /**
     * Returns true if a new STS session needs to be started. A new STS session
     * is needed when no session has been started yet, or if the last session is
     * within the configured refresh threshold.
     *
     * @return True if a new STS session needs to be started.
     */
    private boolean needsNewSession() {
        if (sessionCredentials == null) {
            return true;
        }

        long timeRemaining = sessionCredentialsExpiration.getTime() - System.currentTimeMillis();
        return timeRemaining < (this.refreshThreshold * 1000);
    }

}
