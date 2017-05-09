/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResult;
import software.amazon.awssdk.services.sts.model.Credentials;


@RunWith(MockitoJUnitRunner.class)
public class ScopeDownPolicyCredentialsProviderTest {

    @Mock
    private STSClient sts;

    @Test
    public void scopeDownPolicyInCredentialsProvider_SetsPolicyOnAssumeRoleRequest() {
        when(sts.assumeRole(any(AssumeRoleRequest.class)))
                .thenReturn(new AssumeRoleResult().credentials(
                        new Credentials("akid", "skid", "sessionToken", new Date())));
        ArgumentCaptor<AssumeRoleRequest> argumentCaptor = ArgumentCaptor
                .forClass(AssumeRoleRequest.class);

        new StsAssumeRoleSessionCredentialsProvider.Builder(
                "some-role-arn", "role-session-name")
                .withScopeDownPolicy("{...}")
                .withStsClient(this.sts)
                .build()
                .getCredentials();

        verify(sts).assumeRole(argumentCaptor.capture());
        assertEquals("{...}", argumentCaptor.getValue().getPolicy());
    }
}
