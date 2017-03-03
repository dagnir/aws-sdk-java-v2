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

package software.amazon.awssdk.internal.net;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import javax.net.ssl.SSLSocket;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

public class SdkSSLMetricsSocketTest {

    @Test
    public void testSetMetrics() throws IOException {
        SSLSocket sock = mock(SSLSocket.class);
        InputStream inputStream = mock(InputStream.class);

        when(sock.getInputStream()).thenReturn(inputStream);

        SdkSslMetricsSocket sdkSSLMetricsSocket = new SdkSslMetricsSocket(sock);
        sdkSSLMetricsSocket.setMetrics(new AwsRequestMetrics());

        Assert.assertThat(sdkSSLMetricsSocket.getMetricsInputStream(), not(nullValue()));
    }

}
