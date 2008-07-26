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

package org.apache.synapse.transport.testkit.listener;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DispatchPhase;
import org.apache.axis2.transport.TransportListener;
import org.apache.synapse.transport.UtilsTransportServer;
//import org.apache.synapse.transport.base.event.TransportErrorListener;
//import org.apache.synapse.transport.base.event.TransportErrorSource;

public class ListenerTestServer extends UtilsTransportServer {
    private static ListenerTestServer activeServer;
    
    private final Channel<?> channel;
    private TransportListener listener;
    
    public ListenerTestServer(Channel<?> channel) throws Exception {
        this.channel = channel;
    }
    
//    public void addErrorListener(TransportErrorListener listener) {
//        if (listener instanceof TransportErrorSource) {
//            ((TransportErrorSource)listener).addErrorListener(listener);
//        }
//    }
//    
//    public void removeErrorListener(TransportErrorListener listener) {
//        if (listener instanceof TransportErrorSource) {
//            ((TransportErrorSource)listener).removeErrorListener(listener);
//        }
//    }

    @Override
    public void start() throws Exception {
        if (activeServer != null) {
            throw new IllegalStateException();
        }
        activeServer = this;
        
        channel.getSetup().beforeStartup();
        
        TransportInDescription trpInDesc = channel.createTransportInDescription();
        listener = trpInDesc.getReceiver();
        addTransport(trpInDesc);
        
        AxisConfiguration axisConfiguration = getAxisConfiguration();
        
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
        
        channel.setUp();
        super.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        channel.tearDown();
        Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
        activeServer = null;
    }

    public String getEPR(AxisService service) throws AxisFault {
        EndpointReference[] endpointReferences =
            listener.getEPRsForService(service.getName(), "localhost");
        return endpointReferences != null && endpointReferences.length > 0
                            ? endpointReferences[0].getAddress() : null;
    }
}
