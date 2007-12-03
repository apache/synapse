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

package org.apache.synapse.mediators.ext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.Command;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.annotations.Namespaces;
import org.apache.synapse.mediators.annotations.ReadAndUpdate;
import org.apache.synapse.mediators.annotations.ReadFromMessage;
import org.apache.synapse.mediators.annotations.UpdateMessage;
import org.jaxen.JaxenException;

/**
 */
public class AnnotatedCommandMediator extends POJOCommandMediator {

    protected Map<Field, AXIOMXPath> beforeFields;
    protected Map<Method, AXIOMXPath> beforeMethods;
    protected Map<Field, AXIOMXPath> afterFields;
    protected Map<Method, AXIOMXPath> afterMethods;
    
    @Override
    public boolean mediate(MessageContext synCtx) {
        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : POJOCommand mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Creating a new instance of POJO class : " + getCommand().getClass());
        }

        Object commandObject = null;
        try {
            // instantiate a new command object each time
            commandObject = getCommand().newInstance();
        } catch (Exception e) {
            handleException("Error creating an instance of the POJO command class : " +
                            getCommand().getClass(), e, synCtx);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Instance created, setting static and dynamic properties");
        }

        // then set the static/constant properties first
        for (Iterator iter = getStaticProps().keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            setInstanceProperty(name, (String) getStaticProps().get(name), commandObject, synCtx);
        }
        
        
        for (Field f : beforeFields.keySet()) {
            AXIOMXPath xpath = beforeFields.get(f);
            Object v;
            if (f.getType().equals(String.class)) {
                v = Axis2MessageContext.getStringValue(xpath, synCtx);
            } else {
                throw new UnsupportedOperationException("non-String types not supportted yet");
            }
            try {
                f.set(commandObject, v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Method m : beforeMethods.keySet()) {
            AXIOMXPath xpath = beforeMethods.get(m);
            Object v;
            if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0].equals(String.class)) {
                v = Axis2MessageContext.getStringValue(xpath, synCtx);
            } else {
                throw new UnsupportedOperationException("non-String types not supportted yet");
            }
            try {
                m.invoke(commandObject, v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "POJO initialized successfully, invoking the execute() method");
        }

        // then call the execute method if the Command interface is implemented
        if (commandObject instanceof Command) {
            try {
                ((Command) commandObject).execute();
            } catch (Exception e) {
                handleException("Error invoking POJO command class : "
                    + getCommand().getClass(), e, synCtx);
            }

        } else {

            Method exeMethod = null;
            try {
                exeMethod = getCommand().getMethod("execute", new Class[]{});
                exeMethod.invoke(commandObject, new Object[]{});
            } catch (NoSuchMethodException e) {
                handleException("Cannot locate an execute() method on POJO class : " +
                                getCommand().getClass(), e, synCtx);
            } catch (Exception e) {
                handleException("Error invoking the execute() method on POJO class : " +
                                getCommand().getClass(), e, synCtx);
            }
        }

        // TODO: now update the MessageContext from the commandObject
        
        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : POJOCommand mediator");
        }
        return true;
    }
    
    @Override
    public void setCommand(Class commandClass) {
        super.setCommand(commandClass);
        introspectClass(commandClass);
    }

    /**
     * Introspect the command class annotations
     */
    protected void introspectClass(Class<?> commandClass) {

        beforeFields = new HashMap<Field, AXIOMXPath>();
        afterFields = new HashMap<Field, AXIOMXPath>();
        beforeMethods = new HashMap<Method, AXIOMXPath>();
        afterMethods = new HashMap<Method, AXIOMXPath>();

        for (Field f : commandClass.getDeclaredFields()) {

            ReadFromMessage readFromMessage = f.getAnnotation(ReadFromMessage.class);
            if (readFromMessage != null) {
                AXIOMXPath axiomXpath = createAxiomXPATH(readFromMessage.value(), f.getAnnotation(Namespaces.class));
                beforeFields.put(f, axiomXpath);
            }

            UpdateMessage updateMessage = f.getAnnotation(UpdateMessage.class);
            if (updateMessage != null) {
                AXIOMXPath axiomXpath = createAxiomXPATH(updateMessage.value(), f.getAnnotation(Namespaces.class));
                afterFields.put(f, axiomXpath);
            }

            ReadAndUpdate readAndUpdate = f.getAnnotation(ReadAndUpdate.class);
            if (readAndUpdate != null) {
                AXIOMXPath axiomXpath = createAxiomXPATH(readAndUpdate.value(), f.getAnnotation(Namespaces.class));
                beforeFields.put(f, axiomXpath);
                afterFields.put(f, axiomXpath);
            }
        }

        for (Method m : commandClass.getDeclaredMethods()) {

            ReadFromMessage readFromMessage = m.getAnnotation(ReadFromMessage.class);
            if (readFromMessage != null) {
                AXIOMXPath axiomXpath = createAxiomXPATH(readFromMessage.value(), m.getAnnotation(Namespaces.class));
                beforeMethods.put(m, axiomXpath);
            }

            UpdateMessage updateMessage = m.getAnnotation(UpdateMessage.class);
            if (updateMessage != null) {
                AXIOMXPath axiomXpath = createAxiomXPATH(updateMessage.value(), m.getAnnotation(Namespaces.class));
                afterMethods.put(m, axiomXpath);
            }

        }
    }

    /**
     * Create an AXIOMXPath from an xpath string 
     */
    protected AXIOMXPath createAxiomXPATH(String xpath, Namespaces nsAnnotation) {
        
        Map<String, String> namespaces = getNamespaces(nsAnnotation);     
        try {

            AXIOMXPath axiomXPath = new AXIOMXPath(xpath);
            
            for (String prefix : namespaces.keySet()) {
                axiomXPath.addNamespace(prefix, namespaces.get(prefix));
            }
            
            return axiomXPath;

        } catch (JaxenException e) {
            throw new RuntimeException("Error creating AXIOMXPath: " + xpath, e);
        }
    }

    /**
     * Creates a Map of namespace prefixes and namespaces from a Namespace annotation
     * and the default Namespace annotation on the command class.
     */
    protected Map<String, String> getNamespaces(Namespaces namespaces) {
        Map<String, String> allNamespaces = new HashMap<String, String>();
        
        Namespaces defaultNamespaces = ((Class<?>)getCommand()).getAnnotation(Namespaces.class);

        // First add any default namespaces
        if (defaultNamespaces != null) {
            if (defaultNamespaces.value()[0].length()>0) {
                allNamespaces.put(defaultNamespaces.value()[0], defaultNamespaces.value()[1]);
            }
            if (defaultNamespaces.ns() != null && defaultNamespaces.ns().length() > 0) {
                allNamespaces.put("ns", defaultNamespaces.ns());
            }
            if (defaultNamespaces.ns1() != null && defaultNamespaces.ns1().length() > 0) {
                allNamespaces.put("ns1", defaultNamespaces.ns());
            }
            if (defaultNamespaces.ns2() != null && defaultNamespaces.ns2().length() > 0) {
                allNamespaces.put("ns2", defaultNamespaces.ns());
            }
            if (defaultNamespaces.ns3() != null && defaultNamespaces.ns3().length() > 0) {
                allNamespaces.put("ns3", defaultNamespaces.ns());
            }
            if (defaultNamespaces.ns4() != null && defaultNamespaces.ns4().length() > 0) {
                allNamespaces.put("ns4", defaultNamespaces.ns());
            }
            if (defaultNamespaces.ns5() != null && defaultNamespaces.ns5().length() > 0) {
                allNamespaces.put("ns5", defaultNamespaces.ns());
            }
        }

        // add any namespaces which will overwrite any previously added default namespaces
        if (namespaces != null) {
            if (namespaces.value()[0].length()>0) {
                allNamespaces.put(namespaces.value()[0], namespaces.value()[1]);
            }
            if (namespaces.ns() != null && namespaces.ns().length() > 0) {
                allNamespaces.put("ns", namespaces.ns());
            }
            if (namespaces.ns1() != null && namespaces.ns1().length() > 0) {
                allNamespaces.put("ns1", namespaces.ns());
            }
            if (namespaces.ns2() != null && namespaces.ns2().length() > 0) {
                allNamespaces.put("ns2", namespaces.ns());
            }
            if (namespaces.ns3() != null && namespaces.ns3().length() > 0) {
                allNamespaces.put("ns3", namespaces.ns());
            }
            if (namespaces.ns4() != null && namespaces.ns4().length() > 0) {
                allNamespaces.put("ns4", namespaces.ns());
            }
            if (namespaces.ns5() != null && namespaces.ns5().length() > 0) {
                allNamespaces.put("ns5", namespaces.ns());
            }
        }
        return allNamespaces;
    }

}
