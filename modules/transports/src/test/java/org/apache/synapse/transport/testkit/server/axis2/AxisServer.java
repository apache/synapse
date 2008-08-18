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

package org.apache.synapse.transport.testkit.server.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DispatchPhase;
import org.apache.axis2.transport.TransportListener;
import org.apache.synapse.transport.UtilsTransportServer;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.listener.Channel;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.server.Server;

public class AxisServer implements Server {
    private static AxisServer activeServer;
    
    private TransportListener listener;
    private UtilsTransportServer server;
    
    @SuppressWarnings("unused")
    private void setUp(Channel channel, TransportDescriptionFactory tdf) throws Exception {
        server = new UtilsTransportServer();
        
        if (activeServer != null) {
            throw new IllegalStateException();
        }
        activeServer = this;
        
        TransportOutDescription trpOutDesc;
        if (channel instanceof RequestResponseChannel) {
            trpOutDesc = tdf.createTransportOutDescription();
        } else {
            trpOutDesc = null;
        }
        
        TransportInDescription trpInDesc = tdf.createTransportInDescription();
        listener = trpInDesc.getReceiver();
        server.addTransport(trpInDesc, trpOutDesc);
        
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        
        // Add a DefaultOperationDispatcher to the InFlow phase. This is necessary because
        // we want to receive all messages through the same operation.
        DispatchPhase dispatchPhase = null;
        for (Object phase : axisConfiguration.getInFlowPhases()) {
            if (phase instanceof DispatchPhase) {
                dispatchPhase = (DispatchPhase)phase;
                break;
            }
        }
        DefaultOperationDispatcher dispatcher = new DefaultOperationDispatcher();
        dispatcher.initDispatcher();
        dispatchPhase.addHandler(dispatcher);
        
        server.start();
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        server.stop();
        Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
        server = null;
        activeServer = null;
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
