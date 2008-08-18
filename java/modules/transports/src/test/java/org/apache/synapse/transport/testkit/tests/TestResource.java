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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import org.apache.synapse.transport.testkit.Adapter;

public class TestResource {
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
    private final LinkedList<Initializer> initializers = new LinkedList<Initializer>();
    private final List<Finalizer> finalizers = new LinkedList<Finalizer>();
    private boolean resolved;
    
    public TestResource(Object instance) {
        this.instance = instance;
    }
    
    public void resolve(List<TestResource> resources) {
        if (resolved) {
            return;
        }
        Object target = instance;
        while (target instanceof Adapter) {
            target = ((Adapter)target).getTarget();
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
                        TestResource res = null;
                        for (TestResource resource : resources) {
                            if (resource != this) {
                                Object obj = resource.getInstance();
                                if (parameterClass.isInstance(obj)) {
                                    if (res != null) {
                                        throw new Error();
                                    }
                                    res = resource;
                                }
                            }
                        }
                        if (res == null) {
                            throw new Error(target.getClass().getName() + " depends on " + parameterClass.getName() + ", but none found");
                        }
                        args[i] = res.getInstance();
                    }
                    method.setAccessible(true);
                    initializers.addFirst(new Initializer(method, target, args));
                } else if (name.equals("tearDown") && method.getParameterTypes().length == 0) {
                    method.setAccessible(true);
                    finalizers.add(new Finalizer(method, target));
                }
            }
        }
        resolved = true;
    }

    public Object getInstance() {
        return instance;
    }
    
    public void setUp() throws Exception {
        for (Initializer initializer : initializers) {
            initializer.execute();
        }
    }
    
    public void tearDown() throws Exception {
        for (Finalizer finalizer : finalizers) {
            finalizer.execute();
        }
    }
}
