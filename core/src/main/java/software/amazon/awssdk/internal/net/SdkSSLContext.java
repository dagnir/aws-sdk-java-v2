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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLInitializationException;

public class SdkSSLContext {

    /**
     * @see SSLContexts#createDefault()
     */
    public static final SSLContext getPreferredSSLContext(final SecureRandom secureRandom) {
        try {
            final SSLContext sslcontext = SSLContext.getInstance("TLS");
            // http://download.java.net/jdk9/docs/technotes/guides/security/jsse/JSSERefGuide.html
            sslcontext.init(null, null, secureRandom);
            return sslcontext;
        } catch (final NoSuchAlgorithmException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        } catch (final KeyManagementException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        }
    }
}
