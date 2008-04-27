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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.synapse.Command;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This mediator will use the specified command object and execute the command after setting
 * the properties specified to it through the configuraiton. The specified command object may or
 * may not implement the Command interface. If the Command object has not implemented the Command
 * interface then this will use reflection to find a method called execute() and execute it.
 *
 * @see org.apache.synapse.Command interface
 */
public class POJOCommandMediator extends AbstractMediator {

    /**
     * This will hold the command object to be executed
     */
    private Class command = null;

    /**
     * 'static' properties whose values are constant and does not depend
     * on the current message (i.e. and XPath over it)
     */
    private Map<String, Object> staticSetterProperties = new HashMap<String, Object>();

    /**
     * 'dynamic' properties whose values are dynamically evaluated before each
     * invocation of the command, by evaluating an XPath against the current message
     */
    private Map<String, SynapseXPath> messageSetterProperties = new HashMap<String, SynapseXPath>();

    /**
     * 'dynamic' properties whose values are dynamically evaluated before each
     * invocation of the command, by getting a property from the message context
     */
    private Map<String, String> contextSetterProperties = new HashMap<String, String>();

    /**
     * 'context' properties whose values are set back to the message context as message
     * context properties
     */
    private Map<String, String> contextGetterProperties = new HashMap<String, String>();

    /**
     * 'messsage' properties whose values are set back to the current message, from the command
     * and as specified by the XPATH
     */
    private Map<String, SynapseXPath> messageGetterProperties = new HashMap<String, SynapseXPath>();

    /**
     * Implements the mediate method of the Mediator interface. This method will instantiate
     * a new instance of the POJO class, set all specified properties from the current runtime
     * state (and message context) and call the execute method of the Command object.
     *
     * @param synCtx - Synapse MessageContext to be mediated
     * @return boolean true since this will not stop exection chain
     */
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
            traceOrDebug(traceOn, "Creating a new instance of POJO class : " + command.getClass());
        }

        Object commandObject = null;
        try {
            // instantiate a new command object each time
            commandObject = command.newInstance();
        } catch (Exception e) {
            handleException("Error creating an instance of the POJO command class : " +
                command.getClass(), e, synCtx);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Instance created, setting static and dynamic properties");
        }

        // then set the static/constant properties first
        for (String name : staticSetterProperties.keySet()) {
            setInstanceProperty(name, staticSetterProperties.get(name), commandObject, synCtx);
        }

        // now set the any dynamic properties from the message context properties
        for (String name : contextSetterProperties.keySet()) {
            setInstanceProperty(name, synCtx.getProperty(contextSetterProperties.get(name)),
                commandObject, synCtx);
        }

        // now set the any dynamic properties evaluating XPath's on the current message
        for (String name : messageSetterProperties.keySet()) {
            SynapseXPath xpath = messageSetterProperties.get(name);
            String value = xpath.stringValueOf(synCtx);

            setInstanceProperty(name, value, commandObject, synCtx);
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
                    + command.getClass(), e, synCtx);
            }

        } else {

            Method exeMethod = null;
            try {
                exeMethod = command.getMethod("execute", new Class[]{});
                exeMethod.invoke(commandObject, new Object[]{});
            } catch (NoSuchMethodException e) {
                handleException("Cannot locate an execute() method on POJO class : " +
                    command.getClass(), e, synCtx);
            } catch (Exception e) {
                handleException("Error invoking the execute() method on POJO class : " +
                    command.getClass(), e, synCtx);
            }
        }

        // then set the context properties back to the messageContext from the command
        for (String name : contextGetterProperties.keySet()) {
            synCtx.setProperty(contextGetterProperties.get(name),
                getInstanceProperty(name, commandObject, synCtx));
        }

        // now set the any message properties evaluating XPath's on the current message back
        // to the message from the command
        for (String name : messageGetterProperties.keySet()) {
            SynapseXPath xpath = messageGetterProperties.get(name);

            Object resultValue = getInstanceProperty(name, commandObject, synCtx);

            try {
                List list = EIPUtils.getMatchingElements(synCtx.getEnvelope(), xpath);
                if (list.size() > 0) {
                    Object o = list.get(0);
                    if (resultValue instanceof String) {
                        OMAbstractFactory.getOMFactory().createOMText(
                            ((OMNode) o).getParent(), (String) resultValue);
                        ((OMNode) o).detach();
                    } else if (resultValue instanceof OMNode) {
                        ((OMNode) o).insertSiblingAfter((OMNode) resultValue);
                        ((OMNode) o).detach();
                    }

                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "Unable to set the message property " + resultValue
                            + "back to the message : Specified element by the xpath "
                            + xpath + " can not be found");
                    }
                }
            } catch (JaxenException e) {
                handleException("Unable to set the command property "
                    + name + " back to the message", e, synCtx);
            }
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : POJOCommand mediator");
        }
        return true;
    }

    /**
     * Find and invoke the getter method with the name of form getXXX and returns the value given
     * on the POJO object
     *
     * @param name name of the getter field
     * @param obj POJO instance
     * @param synCtx current message
     * @return object representing the value of the getter method
     */
    private Object getInstanceProperty(String name, Object obj, MessageContext synCtx) {

        String mName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            Method[] methods = obj.getClass().getMethods();

            for (Method method : methods) {
                if (mName.equals(method.getName())) {
                    return method.invoke(obj);
                }
            }
        } catch(InvocationTargetException e) {
            handleException("Unable to get the command property '"
                + name + "' back to the message", e, synCtx);
        } catch(IllegalAccessException e){
            handleException("Unable to get the command property '"
                + name + "' back to the message", e, synCtx);
        }

        return null;
    }

    /**
     * Find and invoke the setter method with the name of form setXXX passing in the value given
     * on the POJO object
     *
     * @param name name of the setter field
     * @param value value to be set
     * @param obj POJO instance
     * @param synCtx current message
     */
    protected void setInstanceProperty(
        String name, Object value, Object obj, MessageContext synCtx) {

        String mName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method method = null;

        try {
            Method[] methods = obj.getClass().getMethods();
            boolean invoked = false;

            for (int i=0; i<methods.length; i++) {
                if (mName.equals(methods[i].getName())) {
                    Class[] params = methods[i].getParameterTypes();
                    if (params.length != 1) {
                        handleException("Did not find a setter method named : " + mName +
                            "() that takes a single String, int, long, float, double " +
                            "or boolean or OMElement parameter", synCtx);
                    } else if (value instanceof String) {
                        if (params[0].equals(String.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{String.class});
                            method.invoke(obj, new String[]{(String) value});
                        } else if (params[0].equals(int.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{int.class});
                            method.invoke(obj, new Integer[]{new Integer((String) value)});
                        } else if (params[0].equals(long.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{long.class});
                            method.invoke(obj, new Long[]{new Long((String) value)});
                        } else if (params[0].equals(float.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{float.class});
                            method.invoke(obj, new Float[]{new Float((String) value)});
                        } else if (params[0].equals(double.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{double.class});
                            method.invoke(obj, new Double[]{new Double((String) value)});
                        } else if (params[0].equals(boolean.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{boolean.class});
                            method.invoke(obj, new Boolean[]{new Boolean((String) value)});
                        } else {
                            handleException("Did not find a setter method named : " + mName +
                                "() that takes a single String, int, long, float, double " +
                                "or boolean parameter", synCtx);
                        }
                    } else if (value instanceof OMElement) {
                        if (params[0].equals(OMElement.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{OMElement.class});
                            method.invoke(obj, new OMElement[]{(OMElement) value});                            
                        } else {
                            handleException("Did not find a setter method named : " + mName
                                + "() that takes an OMElement as the parameter", synCtx);
                        }
                    } else {
                        handleException("Can not handle the value type : "
                            + value.getClass(), synCtx);
                    }
                    invoked = true;
                }
            }

            if (!invoked) {
                handleException("Did not find a setter method named : " + mName +
                    "() that takes a single String, int, long, float, double " +
                    "or boolean parameter", synCtx);
            }

        } catch (Exception e) {
            handleException("Error invoking setter method named : " + mName +
                "() that takes a single String, int, long, float, double " +
                "or boolean parameter", e, synCtx);
        }
    }

    public Class getCommand() {
        return command;
    }

    public void setCommand(Class command) {
        this.command = command;
    }

    public void addStaticSetterProperty(String name, Object value) {
        this.staticSetterProperties.put(name, value);
    }

    public void addMessageSetterProperty(String name, SynapseXPath xpath) {
        this.messageSetterProperties.put(name, xpath);
    }
    
    public void addContextSetterProperty(String name, String ctxName) {
        this.contextSetterProperties.put(name, ctxName);
    }

    public void addContextGetterProperty(String name, String value) {
        this.contextGetterProperties.put(name, value);
    }

    public void addMessageGetterProperty(String name, SynapseXPath xpath) {
        this.messageGetterProperties.put(name, xpath);
    }

    public Map<String, Object> getStaticSetterProperties() {
        return this.staticSetterProperties;
    }

    public Map<String, SynapseXPath> getMessageSetterProperties() {
        return this.messageSetterProperties;
    }

    public Map<String, String> getContextSetterProperties() {
        return this.contextSetterProperties;
    }

    public Map<String, String> getContextGetterProperties() {
        return this.contextGetterProperties;
    }

    public Map<String, SynapseXPath> getMessageGetterProperties() {
        return this.messageGetterProperties;
    }
}
