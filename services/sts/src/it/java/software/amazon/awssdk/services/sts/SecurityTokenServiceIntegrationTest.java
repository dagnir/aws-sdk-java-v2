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

package software.amazon.awssdk.services.sts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.services.sts.model.GetFederationTokenResult;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResult;


public class SecurityTokenServiceIntegrationTest extends IntegrationTestBase {

    private static final int SESSION_DURATION = 60 * 60;

    /** Tests that we can call GetSession to start a session. */
    @Test
    public void testGetSessionToken() throws Exception {
        GetSessionTokenRequest request = new GetSessionTokenRequest().withDurationSeconds(SESSION_DURATION);
        GetSessionTokenResult result = sts.getSessionToken(request);

        assertNotNull(result.getCredentials().getAccessKeyId());
        assertNotNull(result.getCredentials().getExpiration());
        assertNotNull(result.getCredentials().getSecretAccessKey());
        assertNotNull(result.getCredentials().getSessionToken());
    }

    /** Tests that we can call GetFederatedSession to start a federated session. */
    @Test
    public void testGetFederatedSessionToken() throws Exception {
        GetFederationTokenRequest request = new GetFederationTokenRequest()
                .withDurationSeconds(SESSION_DURATION)
                .withName("Name");
        GetFederationTokenResult result = sts.getFederationToken(request);

        assertNotNull(result.getCredentials().getAccessKeyId());
        assertNotNull(result.getCredentials().getExpiration());
        assertNotNull(result.getCredentials().getSecretAccessKey());
        assertNotNull(result.getCredentials().getSessionToken());

        assertNotNull(result.getFederatedUser().getArn());
        assertNotNull(result.getFederatedUser().getFederatedUserId());


    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SDKGlobalConfiguration.setGlobalTimeOffset(3600);
        assertTrue(SDKGlobalConfiguration.getGlobalTimeOffset() == 3600);
        sts = STSClient.builder().withCredentials(CREDENTIALS_PROVIDER_CHAIN).build();
        sts.getSessionToken(new GetSessionTokenRequest());
        assertTrue("Clockskew is fixed!", SDKGlobalConfiguration.getGlobalTimeOffset() < 3600);
        // subsequent changes to the global time offset won't affect existing client
        SDKGlobalConfiguration.setGlobalTimeOffset(3600);
        sts.getSessionToken(new GetSessionTokenRequest());
        assertTrue(SDKGlobalConfiguration.getGlobalTimeOffset() == 3600);
    }
}
