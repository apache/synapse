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

package org.apache.synapse.transport.nhttp;

import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.testkit.listener.AbstractChannel;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.server.axis2.AxisServer;

public class HttpChannel extends AbstractChannel<ListenerTestSetup> implements AsyncChannel<ListenerTestSetup>, RequestResponseChannel<ListenerTestSetup> {
    public HttpChannel() {
        super(AxisServer.DEFAULT);
    }

    public TransportInDescription createTransportInDescription() {
        TransportInDescription trpInDesc = new TransportInDescription("http");
        trpInDesc.setReceiver(new HttpCoreNIOListener());
        return trpInDesc;
    }

    @Override
    public TransportOutDescription createTransportOutDescription() throws Exception {
        TransportOutDescription trpOutDesc = new TransportOutDescription("http");
        trpOutDesc.setSender(new HttpCoreNIOSender());
        return trpOutDesc;
    }
}