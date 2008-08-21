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

import java.net.URI;
import java.util.Iterator;
import java.util.UUID;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.transport.TransportListener;
import org.apache.synapse.transport.UtilsTransportServer;
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.listener.Channel;
import org.apache.synapse.transport.testkit.server.Endpoint;
import org.apache.synapse.transport.testkit.server.Server;

public class AxisServer implements Server {
    private static AxisServer activeServer;
    
    private TransportListener listener;
    private UtilsTransportServer server;
    private TestEnvironment env;
    
    @SuppressWarnings("unused")
    private void setUp(TestEnvironment env, TransportDescriptionFactory tdf) throws Exception {
        this.env = env;
        
        server = new UtilsTransportServer();
        
        if (activeServer != null) {
            throw new IllegalStateException();
        }
        activeServer = this;
        
        TransportOutDescription trpOutDesc = tdf.createTransportOutDescription();
        TransportInDescription trpInDesc = tdf.createTransportInDescription();
        listener = trpInDesc.getReceiver();
        server.addTransport(trpInDesc, trpOutDesc);
        
        ConfigurationContext cfgCtx = server.getConfigurationContext();
        
        cfgCtx.setContextRoot("/");
        cfgCtx.setServicePath("services");
        
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        
        // Add a DefaultOperationDispatcher to the InFlow phase. This is necessary because
        // we want to receive all messages through the same operation.
        DefaultOperationDispatcher operationDispatcher = new DefaultOperationDispatcher();
        operationDispatcher.initDispatcher();
        getInFlowPhase(axisConfiguration, "Dispatch").addHandler(operationDispatcher);
        
        server.start();
    }
    
    private static Phase getInFlowPhase(AxisConfiguration axisConfiguration, String name) {
        for (Iterator<?> it = axisConfiguration.getInFlowPhases().iterator(); it.hasNext(); ) {
            Phase phase = (Phase)it.next();
            if (phase.getName().equals(name)) {
                return phase;
            }
        }
        return null;
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        server.stop();
        listener = null;
        server = null;
        env = null;
        activeServer = null;
    }
    
    public AxisConfiguration getAxisConfiguration() {
        return server.getAxisConfiguration();
    }

    public AxisService deployService(Channel channel, AxisOperation operation, String contentType) throws Exception {
        String path = new URI(channel.getEndpointReference().getAddress()).getPath();
        String serviceName;
        if (path != null && path.startsWith(Channel.CONTEXT_PATH + "/")) {
            serviceName = path.substring(Channel.CONTEXT_PATH.length()+1);
        } else {
            serviceName = "TestService-" + UUID.randomUUID();
        }
        AxisService service = new AxisService(serviceName);
        service.addOperation(operation);
        channel.setupService(service);
        if (contentType != null) {
            env.setupContentType(service, contentType);
        }
        server.getAxisConfiguration().addService(service);
        return service;
    }
    
    public Endpoint createAsyncEndpoint(Channel channel, MessageReceiver messageReceiver, String contentType) throws Exception {
        AxisOperation operation = new InOnlyAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        operation.setMessageReceiver(messageReceiver);
        return new EndpointImpl(this, deployService(channel, operation, contentType));
    }

    public String getEPR(AxisService service) throws AxisFault {
        EndpointReference[] endpointReferences =
            listener.getEPRsForService(service.getName(), "localhost");
        return endpointReferences != null && endpointReferences.length > 0
                            ? endpointReferences[0].getAddress() : null;
    }
}
