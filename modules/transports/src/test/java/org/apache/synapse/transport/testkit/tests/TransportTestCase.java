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

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.synapse.transport.testkit.name.Key;
import org.apache.synapse.transport.testkit.name.NameUtils;

@Key("test")
public abstract class TransportTestCase extends TestCase {
    private final TestResourceSet resourceSet = new TestResourceSet();
    
    private Map<String,String> nameComponents;
    
    private boolean managed;

    public TransportTestCase(Object... resources) {
        resourceSet.addResources(resources);
    }

    protected void addResource(Object resource) {
        resourceSet.addResource(resource);
    }
    
    public Map<String,String> getNameComponents() {
        if (nameComponents == null) {
            nameComponents = new LinkedHashMap<String,String>();
            NameUtils.getNameComponents(nameComponents, this);
            resourceSet.resolve();
            for (Object resource : resourceSet.getResources()) {
                NameUtils.getNameComponents(nameComponents, resource);
            }
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

    public void setManaged(boolean managed) {
        this.managed = managed;
    }
    
    public TestResourceSet getResourceSet() {
        return resourceSet;
    }

    @Override
    protected void setUp() throws Exception {
        if (!managed) {
            resourceSet.setUp();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (!managed) {
            resourceSet.tearDown();
        }
    }
}