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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.synapse.transport.testkit.Adapter;

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
    
    public static Map<String,String> getNameComponents(String rootKey, Object object) {
        Map<String,String> result = new LinkedHashMap<String,String>();
        collectNameComponents(result, rootKey, object);
        return result;
    }
    
    private static void collectNameComponents(Map<String,String> map, String key, Object component) {
        while (component instanceof Adapter) {
            component = ((Adapter)component).getTarget();
        }
        if (component instanceof String) {
            map.put(key, (String)component);
        } else {
            DisplayName displayName = component.getClass().getAnnotation(DisplayName.class);
            if (displayName != null) {
                map.put(key, displayName.value());
            }
            collectNameComponents(map, component);
        }
    }
    
    private static void collectNameComponents(Map<String,String> map, Object object) {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getMethods()) {
            NameComponent ann = method.getAnnotation(NameComponent.class);
            if (ann != null) {
                Object component;
                try {
                    method.setAccessible(true);
                    component = method.invoke(object);
                } catch (Throwable ex) {
                    throw new Error("Error invoking " + method, ex);
                }
                if (component != null) {
                    collectNameComponents(map, ann.value(), component);
                }
            }
        }
    }
}
