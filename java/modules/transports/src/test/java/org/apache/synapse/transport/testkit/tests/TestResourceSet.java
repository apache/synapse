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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestResourceSet {
    private enum Status { UNRESOLVED, RESOLVED, SETUP, RECYCLED };
    
    private static Log log = LogFactory.getLog(TestResourceSet.class);
    
    private final TestResourceSet parent;
    private final List<TestResource> resources = new LinkedList<TestResource>();
    private Status status = Status.UNRESOLVED;
    
    public TestResourceSet(TestResourceSet parent) {
        this.parent = parent;
    }
    
    public TestResourceSet() {
        this(null);
    }

    public void addResource(Object resource) {
        if (status != Status.UNRESOLVED) {
            throw new IllegalStateException();
        }
        resources.add(new TestResource(resource));
    }
    
    public void addResources(Object... resources) {
        for (Object resource : resources) {
            addResource(resource);
        }
    }
    
    public void resolve() {
        if (status == Status.UNRESOLVED) {
            List<TestResource> availableResources = new LinkedList<TestResource>();
            if (parent != null) {
                availableResources.addAll(parent.resources);
            }
            availableResources.addAll(resources);
            for (TestResource resource : resources) {
                resource.resolve(availableResources);
            }
            status = Status.RESOLVED;
        }
    }
    
    public void setUp() throws Exception {
        resolve();
        if (status != Status.RESOLVED) {
            throw new IllegalStateException();
        }
        setUp(resources);
        status = Status.SETUP;
    }
    
    private static void setUp(List<TestResource> resources) throws Exception {
        if (!resources.isEmpty()) {
            log.info("Setting up: " + resources);
            for (TestResource resource : resources) {
                resource.setUp();
            }
        }
    }
    
    private TestResource lookup(Object instance) {
        for (TestResource resource : resources) {
            if (resource.getInstance() == instance) {
                return resource;
            }
        }
        return null;
    }
    
    public void setUp(TestResourceSet old) throws Exception {
        resolve();
        if (status != Status.RESOLVED) {
            throw new IllegalStateException();
        }
        if (old.status != Status.SETUP) {
            throw new IllegalStateException();
        }
        List<TestResource> oldResourcesToTearDown = new LinkedList<TestResource>();
        List<TestResource> resourcesToSetUp = new LinkedList<TestResource>(resources);
        List<TestResource> resourcesToKeep = new LinkedList<TestResource>();
        for (TestResource oldResource : old.resources) {
            boolean keep;
            TestResource resource = lookup(oldResource.getInstance());
            if (resource == null) {
                keep = false;
            } else {
                keep = true;
                for (Object instance : oldResource.getAllDependencies()) {
                    if (lookup(instance) == null) {
                        keep = false;
                        break;
                    }
                }
            }
            if (keep) {
                resourcesToSetUp.remove(resource);
                resourcesToKeep.add(resource);
            } else {
                oldResourcesToTearDown.add(oldResource);
            }
        }
        tearDown(oldResourcesToTearDown);
        log.debug("Keeping: " + resourcesToKeep);
        setUp(resourcesToSetUp);
        status = Status.SETUP;
        old.status = Status.RECYCLED;
    }
    
    public void tearDown() throws Exception {
        if (status != Status.SETUP) {
            throw new IllegalStateException();
        }
        tearDown(resources);
    }
    
    private static void tearDown(List<TestResource> resources) throws Exception {
        if (!resources.isEmpty()) {
            log.info("Tearing down: " + resources);
            for (ListIterator<TestResource> it = resources.listIterator(resources.size()); it.hasPrevious(); ) {
                it.previous().tearDown();
            }
        }
    }

    @Override
    public String toString() {
        return resources.toString();
    }
}
