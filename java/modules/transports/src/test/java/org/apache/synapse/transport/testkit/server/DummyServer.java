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

package org.apache.synapse.transport.testkit.server;

import org.apache.synapse.transport.testkit.listener.Channel;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;

public class DummyServer<T extends ListenerTestSetup> extends Server<T> {
    private Channel<?> channel;
    
    public DummyServer(T setup) {
        super(setup);
    }

    @Override
    public void start(Channel<?> channel) throws Exception {
        this.channel = channel;
        channel.getSetup().setUp();
        channel.setUp();
    }

    @Override
    public void stop() throws Exception {
        channel.tearDown();
        channel.getSetup().tearDown();
    }

    @Override
    public Endpoint createEchoEndpoint(String contentType) throws Exception {
        throw new UnsupportedOperationException();
    }
}
