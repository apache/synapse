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

package org.apache.synapse.config.xml;

import junit.framework.TestCase;

import java.net.URL;

import org.apache.synapse.config.SynapseConfiguration;

import javax.xml.stream.XMLStreamException;

public class MultiXMLConfigurationBuilderTest extends TestCase {

    public void testConfigurationBuilder() {
        URL u = this.getClass().getClassLoader().getResource("synapse-config");
        String root = u.getPath();

        System.out.println("Using SYNAPSE_CONFIG_HOME=" + root);
        try {
            SynapseConfiguration synapseConfig =
                    MultiXMLConfigurationBuilder.getConfiguration(root);

            assertNotNull(synapseConfig.getDefinedSequences().get("main"));
            assertNotNull(synapseConfig.getDefinedSequences().get("fault"));
            assertNotNull(synapseConfig.getDefinedSequences().get("foo"));
            assertNull(synapseConfig.getDefinedSequences().get("bar"));

            assertNotNull(synapseConfig.getDefinedEndpoints().get("epr1"));

            assertNotNull(synapseConfig.getProxyService("proxy1"));

            assertNotNull(synapseConfig.getStartup("task1"));
            
        } catch (XMLStreamException e) {
            fail("Error while parsing a configuration file: " + e.getMessage());
        }
    }
}
