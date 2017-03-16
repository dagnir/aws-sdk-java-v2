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
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.function.SdkPredicate;

/**
 * Predicate to determine when we do a blocking, synchronous refresh of session credentials in
 * StsSessionCredentialsProvider and StsAssumeRoleSessionCredentialsProvider.
 */
@SdkInternalApi
@ThreadSafe
class ShouldDoBlockingSessionRefresh extends SdkPredicate<SessionCredentialsHolder> {

    /**
     * Time before expiry within which credentials will be renewed synchronously.
     */
    private static final int EXPIRY_TIME_MILLIS = 60 * 1000;

    /**
     * Session credentials that expire in less than a minute are considered expiring.
     *
     * @param expiry expiration time of a session
     */
    private static boolean expiring(Date expiry) {
        long timeRemaining = expiry.getTime() - System.currentTimeMillis();
        return timeRemaining < EXPIRY_TIME_MILLIS;
    }

    @Override
    public boolean test(SessionCredentialsHolder sessionCredentialsHolder) {
        return sessionCredentialsHolder == null ||
               expiring(sessionCredentialsHolder.getSessionCredentialsExpiration());
    }
}
