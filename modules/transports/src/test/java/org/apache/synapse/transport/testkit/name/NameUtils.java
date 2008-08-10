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

package org.apache.synapse.transport.testkit.name;

import java.lang.reflect.Method;

import org.apache.synapse.transport.testkit.Adapter;
import org.apache.synapse.transport.testkit.listener.NameBuilder;

public class NameUtils {
    public static String getName(Object object) {
        Class<?> clazz = object.getClass();
        DisplayName displayName = clazz.getAnnotation(DisplayName.class);
        if (displayName != null) {
            return displayName.value();
        } else {
            String className = clazz.getName();
            return className.substring(className.lastIndexOf('.') + 1);
        }
    }
    
    public static void getNameComponents(NameBuilder nameBuilder, Object object) {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getMethods()) {
            NameComponent ann = method.getAnnotation(NameComponent.class);
            if (ann != null) {
                Object component;
                try {
                    component = method.invoke(object);
                } catch (Throwable ex) {
                    throw new Error(ex);
                }
                while (component instanceof Adapter) {
                    component = ((Adapter)component).getTarget();
                }
                if (component instanceof String) {
                    nameBuilder.addComponent(ann.value(), (String)component);
                } else {
                    DisplayName displayName = component.getClass().getAnnotation(DisplayName.class);
                    if (displayName != null) {
                        nameBuilder.addComponent(ann.value(), displayName.value());
                    }
                    getNameComponents(nameBuilder, component);
                }
            }
        }
    }
}
