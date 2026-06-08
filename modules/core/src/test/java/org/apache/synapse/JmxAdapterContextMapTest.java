/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
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
package org.apache.synapse;

import junit.framework.TestCase;
import org.apache.synapse.commons.jmx.JmxConfigurationConstants;
import org.apache.synapse.commons.jmx.JmxInformation;
import org.apache.synapse.commons.jmx.JmxSecretAuthenticator;
import org.apache.synapse.securevault.secret.SecretInformation;

import javax.management.remote.JMXConnectorServer;
import java.lang.reflect.Method;
import java.util.Map;

public class JmxAdapterContextMapTest extends TestCase {

    @SuppressWarnings("unchecked")
    private Map<String, Object> createContextMap(JmxInformation info) throws Exception {
        JmxAdapter adapter = new JmxAdapter(info);
        Method method = JmxAdapter.class.getDeclaredMethod("createContextMap");
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(adapter);
    }

    /**
     * Serial filter pattern is applied via {@code JmxSerializationFilterSupport}, not the standard
     * JMX connector env key {@code jmx.remote.rmi.server.serial.filter.pattern}.
     */
    public void testContextMapDoesNotPutSerialFilterPatternInEnv() throws Exception {
        String filterPattern = "maxdepth=5;java.lang.String;!*";
        JmxInformation info = new JmxInformation();
        info.setRemoteSerialFilterPattern(filterPattern);

        Map<String, Object> env = createContextMap(info);

        assertNull(env.get(JmxConfigurationConstants.JMX_REMOTE_SERIAL_FILTER_PATTERN));
    }

    public void testContextMapIncludesAuthAndSSLWhenEnabled() throws Exception {
        String filterPattern = "maxdepth=5;java.lang.String;!*";
        JmxInformation info = new JmxInformation();
        info.setRemoteSerialFilterPattern(filterPattern);
        info.setAuthenticate(true);
        info.setRemoteAccessFile("conf/jmxremote.access");
        info.setRemoteSSL(true);

        SecretInformation secretInformation = new SecretInformation();
        secretInformation.setUser("admin");
        secretInformation.setAliasSecret("admin");
        info.setSecretInformation(secretInformation);

        Map<String, Object> env = createContextMap(info);

        assertNull(env.get(JmxConfigurationConstants.JMX_REMOTE_SERIAL_FILTER_PATTERN));
        assertTrue(env.get(JMXConnectorServer.AUTHENTICATOR) instanceof JmxSecretAuthenticator);
        assertEquals("conf/jmxremote.access", env.get("jmx.remote.x.access.file"));
        assertNotNull(env.get("jmx.remote.rmi.client.socket.factory"));
        assertNotNull(env.get("jmx.remote.rmi.server.socket.factory"));
    }
}
