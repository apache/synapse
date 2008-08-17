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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.Channel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.name.NameComponent;
import org.apache.synapse.transport.testkit.name.NameUtils;
import org.apache.synapse.transport.testkit.server.Server;

public abstract class TransportTestCase extends TestCase {
    private final List<TestResource> resources = new LinkedList<TestResource>();
    private final TestEnvironment env;
    private final Channel channel;
    protected final ContentTypeMode contentTypeMode;
    protected final String contentType;
    
    private Map<String,String> nameComponents;
    
    private boolean manageServer = true;

    public TransportTestCase(TestEnvironment env, Channel channel, Server server, ContentTypeMode contentTypeMode, String contentType) {
        this.env = env;
        this.channel = channel;
        this.contentTypeMode = contentTypeMode;
        this.contentType = contentType;
        if (env != null) {
            addResource(env);
        }
        addResource(channel);
        if (server != null) {
            addResource(server);
        }
    }

    protected void addResource(Object resource) {
        resources.add(new TestResource(resources, resource));
    }
    
    public Map<String,String> getNameComponents() {
        if (nameComponents == null) {
            nameComponents = NameUtils.getNameComponents("test", this);
            nameComponents.put("contentTypeMode", contentTypeMode.toString().toLowerCase());
        }
        return nameComponents;
    }
    
    @Override
    public String getName() {
        String testName = super.getName();
        if (testName == null) {
            StringBuilder buffer = new StringBuilder();
            for (Map.Entry<String,String> entry : getNameComponents().entrySet()) {
                if (buffer.length() > 0) {
                    buffer.append(',');
                }
                buffer.append(entry.getKey());
                buffer.append('=');
                buffer.append(entry.getValue());
            }
            testName = buffer.toString();
            setName(testName);
        }
        return testName;
    }

    @NameComponent("env")
    public TestEnvironment getEnvironment() {
        return env;
    }
    
    @NameComponent("channel")
    public Channel getChannel() {
        return channel;
    }

//    public void setServer(ListenerTestServer server){
//        this.server = server;
//        manageServer = false;
//    }
    
    @Override
    protected void setUp() throws Exception {
        for (TestResource resource : resources) {
            resource.setUp();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        for (ListIterator<TestResource> it = resources.listIterator(resources.size()); it.hasPrevious(); ) {
            it.previous().tearDown();
        }
    }
}