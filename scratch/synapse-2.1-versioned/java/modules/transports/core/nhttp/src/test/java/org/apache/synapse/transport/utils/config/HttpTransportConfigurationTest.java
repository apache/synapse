/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.utils.config;

import junit.framework.TestCase;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import java.nio.charset.CodingErrorAction;

public class HttpTransportConfigurationTest extends TestCase {

    public void testDefaults() {
        HttpTransportConfiguration config = new SimpleHttpTransportConfiguration("bogus");
        assertEquals(10, (int) config.getIntProperty("foo", 10));
        assertEquals("barValue", config.getStringProperty("bar", "barValue"));
        assertEquals(true, (boolean) config.getBooleanProperty("baz", true));

        IOReactorConfig reactorConfig = config.getListeningReactorConfig();
        assertEquals(2, reactorConfig.getIoThreadCount());
        assertEquals(0, reactorConfig.getConnectTimeout());
        assertEquals(60000, reactorConfig.getSoTimeout());
        assertEquals(true, reactorConfig.isTcpNoDelay());
        assertEquals(false, reactorConfig.isInterestOpQueued());

        reactorConfig = config.getConnectingReactorConfig();
        assertEquals(2, reactorConfig.getIoThreadCount());
        assertEquals(0, reactorConfig.getConnectTimeout());
        assertEquals(60000, reactorConfig.getSoTimeout());
        assertEquals(true, reactorConfig.isTcpNoDelay());
        assertEquals(false, reactorConfig.isInterestOpQueued());

        ConnectionConfig connConfig = config.getConnectionConfig();
        assertEquals(1024 * 8, connConfig.getBufferSize());
        assertEquals(CodingErrorAction.REPORT, connConfig.getMalformedInputAction());
        assertEquals(CodingErrorAction.REPORT, connConfig.getUnmappableInputAction());
    }

    public void testNHttp() {
        HttpTransportConfiguration config = new SimpleHttpTransportConfiguration("nhttp");
        assertEquals(1000, (int) config.getIntProperty("test.foo", -1));
        assertEquals("Testing", config.getStringProperty("test.bar", "NotTesting"));
        assertEquals(true, (boolean) config.getBooleanProperty("test.baz", false));

        IOReactorConfig reactorConfig = config.getListeningReactorConfig();
        assertEquals(true, reactorConfig.isSoReuseAddress());
        assertEquals(2, reactorConfig.getIoThreadCount());
        assertEquals(0, reactorConfig.getConnectTimeout());
        assertEquals(60000, reactorConfig.getSoTimeout());
        assertEquals(true, reactorConfig.isTcpNoDelay());
        assertEquals(false, reactorConfig.isInterestOpQueued());

        ConnectionConfig connConfig = config.getConnectionConfig();
        assertEquals(1024 * 8, connConfig.getBufferSize());
        assertEquals(CodingErrorAction.REPORT, connConfig.getMalformedInputAction());
        assertEquals(CodingErrorAction.REPORT, connConfig.getUnmappableInputAction());
    }

    public void testTimeoutAndBufferSizeConfig() {
        HttpTransportConfiguration config = new SimpleHttpTransportConfiguration("test-http");

        IOReactorConfig reactorConfig = config.getListeningReactorConfig();
        assertEquals(30000, reactorConfig.getSoTimeout());
        assertEquals(2, reactorConfig.getIoThreadCount());
        assertEquals(0, reactorConfig.getConnectTimeout());
        // Should be able to override platform default buffer sizes if needed
        assertEquals(1024 * 8, reactorConfig.getRcvBufSize());
        assertEquals(1024 * 8, reactorConfig.getSndBufSize());
        assertEquals(true, reactorConfig.isTcpNoDelay());
        assertEquals(false, reactorConfig.isInterestOpQueued());

        reactorConfig = config.getConnectingReactorConfig();
        assertEquals(20000, reactorConfig.getSoTimeout());
        assertEquals(2, reactorConfig.getIoThreadCount());
        assertEquals(0, reactorConfig.getConnectTimeout());
        // Should be able to override platform default buffer sizes if needed
        assertEquals(1024 * 8, reactorConfig.getRcvBufSize());
        assertEquals(1024 * 8, reactorConfig.getSndBufSize());
        assertEquals(true, reactorConfig.isTcpNoDelay());
        assertEquals(false, reactorConfig.isInterestOpQueued());
    }

    class SimpleHttpTransportConfiguration extends HttpTransportConfiguration {
        public SimpleHttpTransportConfiguration(String fileName) {
            super(fileName);
        }

        @Override
        protected int getThreadsPerReactor() {
            return 2;
        }
    }
}
