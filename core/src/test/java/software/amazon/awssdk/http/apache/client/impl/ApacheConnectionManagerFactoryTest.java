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
package software.amazon.awssdk.http.apache.client.impl;


import static org.junit.Assert.assertEquals;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.internal.http.apache.client.impl.ApacheConnectionManagerFactory;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;

public class ApacheConnectionManagerFactoryTest {


    private final ApacheConnectionManagerFactory factory = new ApacheConnectionManagerFactory();

    @Test
    public void validateAfterInactivityMillis_RespectedInConnectionManager() {
        final int validateAfterInactivity = 1234;
        final HttpClientSettings httpClientSettings =
                HttpClientSettings.adapt(new LegacyClientConfiguration()
                                                 .withValidateAfterInactivityMillis(validateAfterInactivity));

        final PoolingHttpClientConnectionManager connectionManager =
                (PoolingHttpClientConnectionManager) factory.create(httpClientSettings);
        assertEquals(validateAfterInactivity, connectionManager.getValidateAfterInactivity());
    }
}
