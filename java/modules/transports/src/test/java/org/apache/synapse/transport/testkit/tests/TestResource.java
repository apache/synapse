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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.synapse.transport.testkit.Adapter;

public class TestResource {
    private enum Status { UNRESOLVED, RESOLVED, SETUP, RECYCLED };
    
    private static class Initializer {
        private final Method method;
        private final Object object;
        private final Object[] args;
        
        public Initializer(Method method, Object object, Object[] args) {
            this.method = method;
            this.object = object;
            this.args = args;
        }
        
        public void execute() throws Exception {
            method.invoke(object, args);
        }
    }
    
    private static class Finalizer {
        private final Method method;
        private final Object object;
        
        public Finalizer(Method method, Object object) {
            this.method = method;
            this.object = object;
        }
        
        public void execute() throws Exception {
            method.invoke(object);
        }
    }
    
    private final Object instance;
    private final Object target;
    private final Set<TestResource> directDependencies = new HashSet<TestResource>();
    private final LinkedList<Initializer> initializers = new LinkedList<Initializer>();
    private final List<Finalizer> finalizers = new LinkedList<Finalizer>();
    private Status status = Status.UNRESOLVED;
    private boolean hasHashCode;
    private int hashCode;
    
    public TestResource(Object instance) {
        this.instance = instance;
        Object target = instance;
        while (target instanceof Adapter) {
            target = ((Adapter)target).getTarget();
        }
        this.target = target;
    }
    
    public void resolve(TestResourceSet resourceSet) {
        if (status != Status.UNRESOLVED) {
            return;
        }
        for (Class<?> clazz = target.getClass(); !clazz.equals(Object.class);
                clazz = clazz.getSuperclass()) {
            for (Method method : clazz.getDeclaredMethods()) {
                String name = method.getName();
                if (name.equals("setUp")) {
                    Type[] parameterTypes = method.getGenericParameterTypes();
                    Object[] args = new Object[parameterTypes.length];
                    for (int i=0; i<parameterTypes.length; i++) {
                        Type parameterType = parameterTypes[i];
                        if (!(parameterType instanceof Class)) {
                            throw new Error("Generic parameters not supported in " + method);
                        }
                        Class<?> parameterClass = (Class<?>)parameterType;
                        if (parameterClass.isArray()) {
                            Class<?> componentType = parameterClass.getComponentType();
                            TestResource[] resources = resourceSet.findResources(componentType, true);
                            Object[] arg = (Object[])Array.newInstance(componentType, resources.length);
                            for (int j=0; j<resources.length; j++) {
                                TestResource resource = resources[j];
                                directDependencies.add(resource);
                                arg[j] = resource.getInstance();
                            }
                            args[i] = arg;
                        } else {
                            TestResource[] resources = resourceSet.findResources(parameterClass, true);
                            if (resources.length == 0) {
                                throw new Error(target.getClass().getName() + " depends on " +
                                        parameterClass.getName() + ", but none found");
                            } else if (resources.length > 1) {
                                throw new Error(target.getClass().getName() + " depends on " +
                                        parameterClass.getName() + ", but multiple candidates found");
                                
                            }
                            TestResource resource = resources[0];
                            directDependencies.add(resource);
                            args[i] = resource.getInstance();
                        }
                    }
                    method.setAccessible(true);
                    initializers.addFirst(new Initializer(method, target, args));
                } else if (name.equals("tearDown") && method.getParameterTypes().length == 0) {
                    method.setAccessible(true);
                    finalizers.add(new Finalizer(method, target));
                }
            }
        }
        status = Status.RESOLVED;
    }

    public Object getInstance() {
        return instance;
    }
    
    public Object getTarget() {
        return target;
    }

    public boolean hasLifecycle() {
        return !(initializers.isEmpty() && finalizers.isEmpty());
    }

    public void setUp() throws Exception {
        if (status != Status.RESOLVED) {
            throw new IllegalStateException();
        }
        for (Initializer initializer : initializers) {
            initializer.execute();
        }
        status = Status.SETUP;
    }
    
    public void recycle(TestResource resource) {
        if (status != Status.RESOLVED || resource.status != Status.SETUP || !equals(resource)) {
            throw new IllegalStateException();
        }
        status = Status.SETUP;
        resource.status = Status.RECYCLED;
    }
    
    public void tearDown() throws Exception {
        if (status != Status.SETUP) {
            throw new IllegalStateException();
        }
        for (Finalizer finalizer : finalizers) {
            finalizer.execute();
        }
        status = Status.RESOLVED;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TestResource) {
            TestResource other = (TestResource)obj;
            return target == other.target && directDependencies.equals(other.directDependencies);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode;
        if (hasHashCode) {
            hashCode = this.hashCode;
        } else {
            hashCode = new HashCodeBuilder().append(target).append(directDependencies).toHashCode();
            if (status != Status.UNRESOLVED) {
                this.hashCode = hashCode;
            }
        }
        return hashCode;
    }

    @Override
    public String toString() {
        Class<?> clazz = target.getClass();
        String simpleName = clazz.getSimpleName();
        if (simpleName.length() > 0) {
            return simpleName;
        } else {
            return "<anonymous " + clazz.getSuperclass().getSimpleName() + ">";
        }
    }
}
