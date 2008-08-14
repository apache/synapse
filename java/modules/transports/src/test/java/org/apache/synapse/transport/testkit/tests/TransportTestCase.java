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

package org.apache.synapse.transport.testkit.tests;

import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.Channel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageSender;
import org.apache.synapse.transport.testkit.listener.NameBuilder;
import org.apache.synapse.transport.testkit.name.NameComponent;
import org.apache.synapse.transport.testkit.name.NameUtils;
import org.apache.synapse.transport.testkit.server.Server;

import junit.framework.TestCase;

public abstract class TransportTestCase<E extends TestEnvironment,C extends Channel<? super E>,S extends MessageSender<? super E,? super C>> extends TestCase {
    protected final E env;
    protected final C channel;
    protected final S sender;
    private final Server<? super E> server;
    protected final ContentTypeMode contentTypeMode;
    protected final String contentType;
    
    private boolean manageServer = true;

    public TransportTestCase(E env, C channel, S sender, Server<? super E> server, ContentTypeMode contentTypeMode, String contentType) {
        this.env = env;
        this.channel = channel;
        this.sender = sender;
        this.server = server;
        this.contentTypeMode = contentTypeMode;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        String testName = super.getName();
        if (testName == null) {
            NameBuilder nameBuilder = new NameBuilder();
            nameBuilder.addComponent("test", NameUtils.getName(this));
            NameUtils.getNameComponents(nameBuilder, this);
            nameBuilder.addComponent("contentTypeMode", contentTypeMode.toString().toLowerCase());
            testName = nameBuilder.toString();
            setName(testName);
        }
        return testName;
    }

    @NameComponent("channel")
    public C getChannel() {
        return channel;
    }

    @NameComponent("sender")
    public S getSender() {
        return sender;
    }

    @NameComponent("env")
    public E getEnvironment() {
        return env;
    }
    
//    public void setServer(ListenerTestServer server){
//        this.server = server;
//        manageServer = false;
//    }
    
    @Override
    protected void setUp() throws Exception {
        if (env != null) {
            env.setUp();
        }
        channel.setUp(env);
        if (server != null && manageServer) {
            server.start(env, channel);
        }
        sender.setUp(env, channel);
    }

    @Override
    protected void tearDown() throws Exception {
        sender.tearDown();
        if (server != null && manageServer) {
            server.stop();
        }
        channel.tearDown();
        if (env != null) {
            env.tearDown();
        }
    }
}