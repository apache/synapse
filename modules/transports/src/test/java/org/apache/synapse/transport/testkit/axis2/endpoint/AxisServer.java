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

package org.apache.synapse.transport.testkit.axis2.endpoint;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.TransportListener;
import org.apache.synapse.transport.UtilsTransportServer;
import org.apache.synapse.transport.testkit.axis2.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.server.Server;

public class AxisServer implements Server {
    public static final AxisServer INSTANCE = new AxisServer();
    
    private TransportListener listener;
    private UtilsTransportServer server;
    
    private AxisServer() {}
    
    @SuppressWarnings("unused")
    private void setUp(TransportDescriptionFactory tdf) throws Exception {
        
        server = new UtilsTransportServer();
        
        TransportOutDescription trpOutDesc = tdf.createTransportOutDescription();
        TransportInDescription trpInDesc = tdf.createTransportInDescription();
        listener = trpInDesc.getReceiver();
        server.addTransport(trpInDesc, trpOutDesc);
        
        ConfigurationContext cfgCtx = server.getConfigurationContext();
        
        cfgCtx.setContextRoot("/");
        cfgCtx.setServicePath("services");
        
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        
        server.start();
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        server.stop();
        listener = null;
        server = null;
    }
    
    public AxisConfiguration getAxisConfiguration() {
        return server.getAxisConfiguration();
    }

    public String getEPR(AxisService service) throws AxisFault {
        EndpointReference[] endpointReferences =
            listener.getEPRsForService(service.getName(), "localhost");
        return endpointReferences != null && endpointReferences.length > 0
                            ? endpointReferences[0].getAddress() : null;
    }
}
