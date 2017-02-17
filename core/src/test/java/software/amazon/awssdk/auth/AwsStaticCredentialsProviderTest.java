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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AwsStaticCredentialsProviderTest {

    @Test
    public void getBasicAwsCredentials_ReturnsSameCredentials() throws Exception {
        final BasicAwsCredentials credentials = new BasicAwsCredentials("akid", "skid");
        final AwsCredentials actualCredentials =
                new AwsStaticCredentialsProvider(credentials).getCredentials();
        assertEquals(credentials, actualCredentials);
    }

    @Test
    public void getSessionAwsCredentials_ReturnsSameCredentials() throws Exception {
        final BasicSessionCredentials credentials =
                new BasicSessionCredentials("akid", "skid", "token");
        final AwsCredentials actualCredentials =
                new AwsStaticCredentialsProvider(credentials).getCredentials();
        assertEquals(credentials, actualCredentials);
    }

    @Test
    public void refreshCalled_DoesNotChangeCredentials() throws Exception {
        final BasicAwsCredentials credentials = new BasicAwsCredentials("akid", "skid");
        final AwsStaticCredentialsProvider credentialsProvider =
                new AwsStaticCredentialsProvider(credentials);
        final AwsCredentials actualCredentials = credentialsProvider.getCredentials();

        // Should not affect the credentials served by getCredentials
        credentialsProvider.refresh();

        assertEquals(credentials, actualCredentials);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCredentials_ThrowsIllegalArgumentException() {
        new AwsStaticCredentialsProvider(null);
    }

}