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

package software.amazon.awssdk.auth.profile.internal.securitytoken;

import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.AWSCredentialsProvider;

@ThreadSafe
public class STSProfileCredentialsServiceProvider implements AWSCredentialsProvider {
    private static final String CLASS_NAME = "software.amazon.awssdk.services.securitytoken.internal.STSProfileCredentialsService";
    private static volatile ProfileCredentialsService STS_CREDENTIALS_SERVICE;

    private final RoleInfo roleInfo;
    private volatile AWSCredentialsProvider profileCredentialsProvider;

    public STSProfileCredentialsServiceProvider(RoleInfo roleInfo) {
        this.roleInfo = roleInfo;
    }

    /**
     * Only called once per creation of each profile credential provider so we don't bother with any
     * double checked locking.
     */
    private static synchronized ProfileCredentialsService getProfileCredentialService() {
        if (STS_CREDENTIALS_SERVICE == null) {
            try {
                STS_CREDENTIALS_SERVICE = (ProfileCredentialsService) Class.forName(CLASS_NAME)
                                                                           .newInstance();
            } catch (ClassNotFoundException ex) {
                throw new SdkClientException(
                        "To use assume role profiles the aws-java-sdk-sts module must be on the class path.",
                        ex);
            } catch (InstantiationException ex) {
                throw new SdkClientException("Failed to instantiate " + CLASS_NAME, ex);
            } catch (IllegalAccessException ex) {
                throw new SdkClientException("Failed to instantiate " + CLASS_NAME, ex);
            }
        }
        return STS_CREDENTIALS_SERVICE;
    }

    private AWSCredentialsProvider getProfileCredentialsProvider() {
        if (this.profileCredentialsProvider == null) {
            synchronized (STSProfileCredentialsServiceProvider.class) {
                if (this.profileCredentialsProvider == null) {
                    this.profileCredentialsProvider = getProfileCredentialService()
                            .getAssumeRoleCredentialsProvider(roleInfo);
                }
            }
        }
        return this.profileCredentialsProvider;
    }

    @Override
    public AWSCredentials getCredentials() {
        return getProfileCredentialsProvider().getCredentials();
    }

    @Override
    public void refresh() {
        getProfileCredentialsProvider().refresh();
    }
}
