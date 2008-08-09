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

import junit.framework.TestCase;

public abstract class ListenerTestCase<C extends Channel<?>,S extends MessageSender<? super C>> extends TestCase {
    private final String name;
    protected final C channel;
    protected final S sender;
    protected final ContentTypeMode contentTypeMode;
    protected final String contentType;
    
    private boolean manageServer = true;

    public ListenerTestCase(C channel, S sender, String name, ContentTypeMode contentTypeMode, String contentType) {
        this.channel = channel;
        this.sender = sender;
        this.name = name;
        this.contentTypeMode = contentTypeMode;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        String testName = super.getName();
        if (testName == null) {
            NameBuilder nameBuilder = new NameBuilder();
            nameBuilder.addComponent("test", name);
            channel.buildName(nameBuilder);
            buildName(nameBuilder);
            nameBuilder.addComponent("contentTypeMode", contentTypeMode.toString().toLowerCase());
            testName = nameBuilder.toString();
            setName(testName);
        }
        return testName;
    }

    protected void buildName(NameBuilder name) {
        sender.buildName(name);
    }
    
    public C getChannel() {
        return channel;
    }

    public ListenerTestSetup getSetup() {
        return channel.getSetup();
    }
    
//    public void setServer(ListenerTestServer server){
//        this.server = server;
//        manageServer = false;
//    }
    
    @Override
    protected void setUp() throws Exception {
        if (manageServer) {
            channel.getServer().start(channel);
        }
        sender.setUp(channel);
    }

    @Override
    protected void tearDown() throws Exception {
        sender.tearDown();
        if (manageServer) {
            channel.getServer().stop();
        }
    }
}