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
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DispatchPhase;
import org.apache.axis2.receivers.AbstractInOutMessageReceiver;
import org.apache.axis2.transport.TransportListener;
import org.apache.synapse.transport.UtilsTransportServer;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.Channel;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.apache.synapse.transport.testkit.listener.MockMessageReceiver;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.message.MessageData;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.Endpoint;
import org.apache.synapse.transport.testkit.server.Server;

public class AxisServer<T extends ListenerTestSetup> extends Server<T> implements AsyncEndpointFactory<AsyncChannel<?>,MessageData> {
    public static final AxisServer<ListenerTestSetup> DEFAULT = new AxisServer<ListenerTestSetup>(ListenerTestSetup.DEFAULT);
    
    private static Server<?> activeServer;
    
    private Channel<?> channel;
    private TransportListener listener;
    UtilsTransportServer server;
    
    public AxisServer(T setup) {
        super(setup);
    }

    @Override
    public void start(Channel<?> channel) throws Exception {
        server = new UtilsTransportServer();
        this.channel = channel;
        
        if (activeServer != null) {
            throw new IllegalStateException();
        }
        activeServer = this;
        
        channel.getSetup().setUp();
        
        TransportOutDescription trpOutDesc;
        if (channel instanceof RequestResponseChannel) {
            trpOutDesc = ((RequestResponseChannel<?>)channel).createTransportOutDescription();
        } else {
            trpOutDesc = null;
        }
        
        TransportInDescription trpInDesc = channel.createTransportInDescription();
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
        
        channel.setUp();
        server.start();
    }
    
    @Override
    public void stop() throws Exception {
        server.stop();
        channel.tearDown();
        channel.getSetup().tearDown();
        Thread.sleep(100); // TODO: this is required for the NIO transport; check whether this is a bug
        server = null;
        activeServer = null;
    }
    
    public AxisConfiguration getAxisConfiguration() {
        return server.getAxisConfiguration();
    }

    String getEPR(AxisService service) throws AxisFault {
        EndpointReference[] endpointReferences =
            listener.getEPRsForService(service.getName(), "localhost");
        return endpointReferences != null && endpointReferences.length > 0
                            ? endpointReferences[0].getAddress() : null;
    }
    
    public AsyncEndpoint<MessageData> createAsyncEndpoint(AsyncChannel<?> channel, String contentType) throws Exception {
        // Set up a test service with a default operation backed by a mock message
        // receiver. The service is configured using the parameters specified by the
        // implementation.
        AxisService service = new AxisService("TestService");
        AxisOperation operation = new InOnlyAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        MockMessageReceiver messageReceiver = new MockMessageReceiver();
        operation.setMessageReceiver(messageReceiver);
        service.addOperation(operation);
        channel.setupService(service);
        if (contentType != null) {
            channel.getSetup().setupContentType(service, contentType);
        }
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
//        server.addErrorListener(messageReceiver);
        return new AsyncEndpointImpl(this, service, messageReceiver);
    }
    
    @Override
    public Endpoint createEchoEndpoint(String contentType) throws Exception {
        AxisService service = new AxisService("EchoService");
        AxisOperation operation = new InOutAxisOperation(DefaultOperationDispatcher.DEFAULT_OPERATION_NAME);
        operation.setMessageReceiver(new AbstractInOutMessageReceiver() {
            @Override
            public void invokeBusinessLogic(MessageContext inMessage, MessageContext outMessage) throws AxisFault {
                System.out.println(inMessage.getProperty(Constants.OUT_TRANSPORT_INFO));
                System.out.println(inMessage.getEnvelope());
                outMessage.setEnvelope(inMessage.getEnvelope());
            }
        });
        service.addOperation(operation);
        channel.setupService(service);
        if (contentType != null) {
            channel.getSetup().setupContentType(service, contentType);
        }
        
        AxisConfiguration axisConfiguration = server.getAxisConfiguration();
        axisConfiguration.addService(service);
        return new EndpointImpl(this, service);
    }
}
