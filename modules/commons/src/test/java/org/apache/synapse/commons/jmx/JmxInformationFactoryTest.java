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
package org.apache.synapse.commons.jmx;

import junit.framework.TestCase;

import java.util.Properties;

public class JmxInformationFactoryTest extends TestCase {

    public void testSerialFilterPatternNotAppliedWhenUnset() {
        Properties properties = new Properties();

        JmxInformation info = JmxInformationFactory.createJmxInformation(properties, "localhost");

        assertEquals(-1, info.getJndiPort());
        assertNull(info.getRemoteSerialFilterPattern());
    }

    public void testConfiguredJndiPortIsKept() {
        Properties properties = new Properties();
        properties.setProperty("synapse.jmx.jndiPort", "0");

        JmxInformation info = JmxInformationFactory.createJmxInformation(properties, "localhost");

        assertEquals(0, info.getJndiPort());
    }

    public void testSynapseSerialFilterOverrideUsed() {
        String customPattern = "maxdepth=10;java.lang.*;!*";
        Properties properties = new Properties();
        properties.setProperty("synapse.jmx.remote.serial.filter.pattern", customPattern);

        JmxInformation info = JmxInformationFactory.createJmxInformation(properties, "localhost");

        assertEquals(customPattern, info.getRemoteSerialFilterPattern());
    }

    public void testBlankSerialFilterPatternIsIgnored() {
        Properties properties = new Properties();
        properties.setProperty("synapse.jmx.remote.serial.filter.pattern", "   ");

        JmxInformation info = JmxInformationFactory.createJmxInformation(properties, "localhost");

        assertNull(info.getRemoteSerialFilterPattern());
    }
}
