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

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.testkit.server.Server;

public abstract class AbstractChannel<T extends ListenerTestSetup> implements Channel<T> {
    private final String name;
    private final Server<T> server;
    
    public AbstractChannel(String name, Server<T> server) {
        this.name = name;
        this.server = server;
    }

    public AbstractChannel(Server<T> server) {
        this(null, server);
    }
    
    public final T getSetup() {
        return server.getSetup();
    }

    public Server<T> getServer() {
        return server;
    }

    public void buildName(NameBuilder nameBuilder) {
        getSetup().buildName(nameBuilder);
        nameBuilder.addComponent("channel", name);
    }

    public TransportOutDescription createTransportOutDescription() throws Exception {
        throw new UnsupportedOperationException();
    }

    public void setupService(AxisService service) throws Exception {
    }
    
    public void setupRequestMessageContext(MessageContext msgContext) {
    }

    public EndpointReference createEndpointReference(String address) {
        return new EndpointReference(address);
    }

    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
    }
}
