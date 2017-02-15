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

package software.amazon.awssdk.internal.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.http.conn.DnsResolver;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Implements the {@link org.apache.http.conn.DnsResolver} interface,
 * taking in a {@link DnsResolver} implementation and executing its
 * {@link DnsResolver#resolve(String)} method to perform the
 * actual DNS resolution.
 */
@SdkInternalApi
public class DelegatingDnsResolver implements DnsResolver {

    private final software.amazon.awssdk.DnsResolver delegate;

    public DelegatingDnsResolver(software.amazon.awssdk.DnsResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        return delegate.resolve(host);
    }
}
