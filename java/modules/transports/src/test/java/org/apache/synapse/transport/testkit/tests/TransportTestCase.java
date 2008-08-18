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

import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.name.NameUtils;

public abstract class TransportTestCase extends TestCase {
    private final List<TestResource> resources = new LinkedList<TestResource>();
    protected final ContentTypeMode contentTypeMode;
    protected final String contentType;
    
    private Map<String,String> nameComponents;
    
    private boolean manageServer = true;

    public TransportTestCase(ContentTypeMode contentTypeMode, String contentType, Object... resources) {
        this.contentTypeMode = contentTypeMode;
        this.contentType = contentType;
        for (Object resource : resources) {
            addResource(resource);
        }
    }

    protected void addResource(Object resource) {
        // TODO: we should not allow null resources
        if (resource != null) {
            resources.add(new TestResource(resource));
        }
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

//    public void setServer(ListenerTestServer server){
//        this.server = server;
//        manageServer = false;
//    }
    
    @Override
    protected void setUp() throws Exception {
        for (TestResource resource : resources) {
            resource.resolve(resources);
        }
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