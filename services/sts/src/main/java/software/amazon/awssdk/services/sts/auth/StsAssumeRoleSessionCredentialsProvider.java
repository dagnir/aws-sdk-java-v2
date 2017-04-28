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
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResult;

/**
 * AWSCredentialsProvider implementation that uses the AWS Security Token Service to assume a Role
 * and create temporary, short-lived sessions to use for authentication.
 */
@ThreadSafe
public class StsAssumeRoleSessionCredentialsProvider implements AwsSessionCredentialsProvider {

    /**
     * Default duration for started sessions.
     */
    public static final int DEFAULT_DURATION_SECONDS = 900;

    /**
     * The client for starting STS sessions.
     */
    private final STSClient securityTokenService;

    /**
     * The arn of the role to be assumed.
     */
    private final String roleArn;

    /**
     * An identifier for the assumed role session.
     */
    private final String roleSessionName;

    /**
     * An external Id parameter for the assumed role session
     */
    private final String roleExternalId;

    /**
     * The Duration for assume role sessions.
     */
    private final int roleSessionDurationSeconds;

    /**
     * Scope down policy to limit permissions from the assumed role.
     */
    private final String scopeDownPolicy;

    private final Callable<SessionCredentialsHolder> refreshCallable = this::newSession;

    /**
     * Handles the refreshing of sessions. Ideally this should be final but #setSTSClientEndpoint
     * forces us to create a new one.
     */
    private volatile RefreshableTask<SessionCredentialsHolder> refreshableTask;

    /**
     * The following private constructor reads state from the builder and sets the appropriate
     * parameters accordingly
     *
     * When public constructors are called, this constructors is deferred to with a null value for
     * roleExternalId and endpoint The inner Builder class can be used to construct an object that
     * actually has a value for roleExternalId and endpoint
     *
     * @throws IllegalArgumentException if both an AWSCredentials and AWSCredentialsProvider have
     *                                  been set on the builder
     */
    private StsAssumeRoleSessionCredentialsProvider(Builder builder) {
        this.securityTokenService = builder.sts;

        //required parameters are null checked in the builder constructor
        this.roleArn = builder.roleArn;
        this.roleSessionName = builder.roleSessionName;

        //roleExternalId may be null
        this.roleExternalId = builder.roleExternalId;

        //Assume Role Session duration may not be provided, in which case we fall back to default value of 15min
        if (builder.roleSessionDurationSeconds != 0) {
            this.roleSessionDurationSeconds = builder.roleSessionDurationSeconds;
        } else {
            this.roleSessionDurationSeconds = DEFAULT_DURATION_SECONDS;
        }

        this.refreshableTask = createRefreshableTask();
        this.scopeDownPolicy = builder.scopeDownPolicy;
    }

    private RefreshableTask<SessionCredentialsHolder> createRefreshableTask() {
        return new RefreshableTask.Builder<SessionCredentialsHolder>()
                .withRefreshCallable(refreshCallable)
                .withBlockingRefreshPredicate(new ShouldDoBlockingSessionRefresh())
                .withAsyncRefreshPredicate(new ShouldDoAsyncSessionRefresh()).build();
    }

    @Override
    public AwsSessionCredentials getCredentials() {
        return refreshableTask.getValue().getSessionCredentials();
    }

    @Override
    public void refresh() {
        refreshableTask.forceGetValue();
    }

    /**
     * Starts a new session by sending a request to the AWS Security Token Service (STS) to assume a
     * Role using the long lived AWS credentials. This class then vends the short lived session
     * credentials for the assumed Role sent back from STS.
     */
    private SessionCredentialsHolder newSession() {
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest().withRoleArn(roleArn)
                                                                     .withDurationSeconds(roleSessionDurationSeconds)
                                                                     .withRoleSessionName(roleSessionName)
                                                                     .withPolicy(scopeDownPolicy);
        if (roleExternalId != null) {
            assumeRoleRequest = assumeRoleRequest.withExternalId(roleExternalId);
        }

        AssumeRoleResult assumeRoleResult = securityTokenService.assumeRole(assumeRoleRequest);
        return new SessionCredentialsHolder(assumeRoleResult.getCredentials());
    }

    /**
     * Provides a builder pattern to avoid combinatorial explosion of the number of parameters that
     * are passed to constructors. The builder introspects which parameters have been set and calls
     * the appropriate constructor.
     */
    public static final class Builder {

        private final String roleArn;
        private final String roleSessionName;

        private String roleExternalId;
        private int roleSessionDurationSeconds;
        private String scopeDownPolicy;
        private STSClient sts;

        /**
         * @param roleArn         Required roleArn parameter used when starting a session
         * @param roleSessionName Required roleSessionName parameter used when starting a session
         */
        public Builder(String roleArn, String roleSessionName) {
            if (roleArn == null || roleSessionName == null) {
                throw new NullPointerException(
                        "You must specify a value for roleArn and roleSessionName");
            }
            this.roleArn = roleArn;
            this.roleSessionName = roleSessionName;
        }

        /**
         * Set the roleExternalId parameter that is used when retrieving session credentials under
         * an assumed role.
         *
         * @param roleExternalId An external id used in the service call used to retrieve session
         *                       credentials
         * @return the builder itself for chained calls
         */
        public Builder withExternalId(String roleExternalId) {
            this.roleExternalId = roleExternalId;
            return this;
        }

        /**
         * Set the roleSessionDurationSeconds that is used when creating a new assumed role
         * session.
         *
         * @param roleSessionDurationSeconds The duration for which we want to have an assumed role
         *                                   session to be active.
         * @return the itself for chained calls
         */
        public Builder withRoleSessionDurationSeconds(int roleSessionDurationSeconds) {
            if (roleSessionDurationSeconds < 900 || roleSessionDurationSeconds > 3600) {
                throw new IllegalArgumentException(
                        "Assume Role session duration should be in the range of 15min - 1Hr");
            }
            this.roleSessionDurationSeconds = roleSessionDurationSeconds;
            return this;
        }

        /**
         * <p>
         * An IAM policy in JSON format to scope down permissions granted from the assume role.
         * </p>
         * <p>
         * This parameter is optional. If you pass a policy, the temporary security
         * credentials that are returned by the operation have the permissions that
         * are allowed by both (the intersection of) the access policy of the role
         * that is being assumed, <i>and</i> the policy that you pass. This gives
         * you a way to further restrict the permissions for the resulting temporary
         * security credentials. You cannot use the passed policy to grant
         * permissions that are in excess of those allowed by the access policy of
         * the role that is being assumed. For more information, see <a href=
         * "http://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_temp_control-access_assumerole.html"
         * >Permissions for AssumeRole, AssumeRoleWithSAML, and
         * AssumeRoleWithWebIdentity</a> in the <i>IAM User Guide</i>.
         * </p>
         * <p>
         * The format for this parameter, as described by its regex pattern, is a
         * string of characters up to 2048 characters in length. The characters can
         * be any ASCII character from the space character to the end of the valid
         * character list ( -\u00FF). It can also include the tab ( ), linefeed ( ),
         * and carriage return ( ) characters.
         * </p>
         * <note>
         * <p>
         * The policy plain text must be 2048 bytes or shorter. However, an internal
         * conversion compresses it into a packed binary format with a separate
         * limit. The PackedPolicySize response element indicates by percentage how
         * close to the upper size limit the policy is, with 100% equaling the
         * maximum allowed size.
         * </p>
         * </note>
         *
         * @param scopeDownPolicy
         *        An IAM policy in JSON format.</p>
         *        <p>
         *        This parameter is optional. If you pass a policy, the temporary
         *        security credentials that are returned by the operation have the
         *        permissions that are allowed by both (the intersection of) the
         *        access policy of the role that is being assumed, <i>and</i> the
         *        policy that you pass. This gives you a way to further restrict the
         *        permissions for the resulting temporary security credentials. You
         *        cannot use the passed policy to grant permissions that are in
         *        excess of those allowed by the access policy of the role that is
         *        being assumed. For more information, see <a href=
         *        "http://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_temp_control-access_assumerole.html"
         *        >Permissions for AssumeRole, AssumeRoleWithSAML, and
         *        AssumeRoleWithWebIdentity</a> in the <i>IAM User Guide</i>.
         *        </p>
         *        <p>
         *        The format for this parameter, as described by its regex pattern,
         *        is a string of characters up to 2048 characters in length. The
         *        characters can be any ASCII character from the space character to
         *        the end of the valid character list ( -\u00FF). It can also
         *        include the tab ( ), linefeed ( ), and carriage return ( )
         *        characters.
         *        </p>
         *        <p>
         *        The policy plain text must be 2048 bytes or shorter. However, an
         *        internal conversion compresses it into a packed binary format with
         *        a separate limit. The PackedPolicySize response element indicates
         *        by percentage how close to the upper size limit the policy is,
         *        with 100% equaling the maximum allowed size.
         *        </p>
         * @return Returns a reference to this object so that method calls can be
         *         chained together.
         */
        public Builder withScopeDownPolicy(String scopeDownPolicy) {
            this.scopeDownPolicy = scopeDownPolicy;
            return this;
        }

        /**
         * Sets a preconfigured STS client to use for the credentials provider. See {@link
         * software.amazon.awssdk.services.sts.STSClientBuilder} for an easy
         * way to configure and create an STS client.
         *
         * @param sts Custom STS client to use.
         * @return This object for chained calls.
         */
        public Builder withStsClient(STSClient sts) {
            this.sts = sts;
            return this;
        }

        /**
         * Build the configured provider
         *
         * @return the configured StsAssumeRoleSessionCredentialsProvider
         */
        public StsAssumeRoleSessionCredentialsProvider build() {
            return new StsAssumeRoleSessionCredentialsProvider(this);
        }
    }

}
