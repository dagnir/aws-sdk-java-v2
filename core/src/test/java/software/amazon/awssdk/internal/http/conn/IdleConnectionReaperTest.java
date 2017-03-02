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

package software.amazon.awssdk.internal.http.conn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class IdleConnectionReaperTest {
    @Before
    public void init() {
        IdleConnectionReaper.shutdown();
    }

    @AfterClass
    public static void shutdown() {
        IdleConnectionReaper.shutdown();
    }

    @Test
    public void forceShutdown() throws Exception {
        assertEquals(0, IdleConnectionReaper.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(IdleConnectionReaper.registerConnectionManager(new TestClientConnectionManager(), Duration.ofSeconds(60)));
            assertEquals(1, IdleConnectionReaper.size());
            assertTrue(IdleConnectionReaper.shutdown());
            assertEquals(0, IdleConnectionReaper.size());
            assertFalse(IdleConnectionReaper.shutdown());
        }
    }

    @Test
    public void autoShutdown() throws Exception {
        assertEquals(0, IdleConnectionReaper.size());
        for (int i = 0; i < 3; i++) {
            HttpClientConnectionManager m = new TestClientConnectionManager();
            HttpClientConnectionManager m2 = new TestClientConnectionManager();
            assertTrue(IdleConnectionReaper.registerConnectionManager(m, Duration.ofSeconds(60)));
            assertEquals(1, IdleConnectionReaper.size());
            assertTrue(IdleConnectionReaper.registerConnectionManager(m2, Duration.ofSeconds(60)));
            assertEquals(2, IdleConnectionReaper.size());
            assertTrue(IdleConnectionReaper.removeConnectionManager(m));
            assertEquals(1, IdleConnectionReaper.size());
            assertTrue(IdleConnectionReaper.removeConnectionManager(m2));
            assertEquals(0, IdleConnectionReaper.size());
            assertFalse(IdleConnectionReaper.shutdown());
        }
    }

    @Test
    public void maxIdle_HonoredOnClose() throws InterruptedException {
        HttpClientConnectionManager connectionManager = mock(HttpClientConnectionManager.class);

        final Duration idleTime = Duration.ofSeconds(5);
        IdleConnectionReaper.setPeriod(Duration.ofSeconds(1));
        IdleConnectionReaper.registerConnectionManager(connectionManager, idleTime);
        verify(connectionManager, timeout(10_000)).closeIdleConnections(eq(idleTime.toMillis()), eq(TimeUnit.MILLISECONDS));

    }

    @Test(expected = IllegalArgumentException.class)
    public void negativePeriodDuration_ThrowsException() {
        IdleConnectionReaper.setPeriod(Duration.ofMillis(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeConnectionDuration_ThrowsException() {
        IdleConnectionReaper.registerConnectionManager(mock(HttpClientConnectionManager.class), Duration.ofMillis(-1));
    }

    private static class TestClientConnectionManager implements HttpClientConnectionManager {
        @Override
        public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
        }

        @Override
        public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context)
                throws IOException {
        }

        @Override
        public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
        }

        @Override
        public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public void closeIdleConnections(long idletime, TimeUnit tunit) {
        }

        @Override
        public void closeExpiredConnections() {
        }

        @Override
        public ConnectionRequest requestConnection(HttpRoute route, Object state) {
            return null;
        }
    }
}
