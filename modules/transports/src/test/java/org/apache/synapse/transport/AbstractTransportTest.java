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

package org.apache.synapse.transport;

import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.transport.vfs.VFSTransportSender;

public class AbstractTransportTest extends TestCase {

    protected UtilsTransportServer server = null;

    protected void setUp() throws Exception {
        server.start();
    }

    protected void tearDown() throws Exception {
        server.stop();
    }

    /**
     * Create the payload for an echoOMElement request
     * @return
     */
    protected OMElement createPayload() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://localhost/axis2/services/EchoXMLService", "my");
        OMElement method = fac.createOMElement("echoOMElement", omNs);
        OMElement value = fac.createOMElement("myValue", omNs);
        value.addChild(fac.createOMText(value, "omTextValue"));
        method.addChild(value);
        return method;
    }

    /**
     * Get the default axis2 configuration context for a client
     * @return
     * @throws Exception
     */
    protected ConfigurationContext getClientCfgCtx() throws Exception {
        AxisConfiguration axisCfg = new AxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisCfg);
        return cfgCtx;
    }

}
