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

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.*;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.axiom.om.xpath.AXIOMXPath;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

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
    private Map staticProps = new HashMap();
    /**
     * 'dynamic' properties whose values are dynamically evaluated before each
     * invocation of the command, by evaluating an XPath against the current message
     */
    private Map dynamicProps = new HashMap();

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
        for (Iterator iter = staticProps.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            setInstanceProperty(name, (String) staticProps.get(name), commandObject, synCtx);
        }

        // now set the any dynamic properties evaluating XPath's on the current message
        for (Iterator iter = dynamicProps.keySet().iterator(); iter.hasNext(); ) {

            String name = (String) iter.next();
            AXIOMXPath xpath = (AXIOMXPath) dynamicProps.get(name);
            String value = Axis2MessageContext.getStringValue(xpath, synCtx);

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

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : POJOCommand mediator");
        }
        return true;
    }

    /**
     * Find and invoke the setter method with the name of form setXXX passing in the value given
     * on the POJO object
     * @param name name of the setter field
     * @param value value to be set
     * @param obj POJO instance
     * @param synCtx current message
     */
    private void setInstanceProperty(String name, String value, Object obj, MessageContext synCtx) {

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
                            "or boolean parameter", synCtx);
                    } else {
                        if (params[0].equals(String.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{String.class});
                            method.invoke(obj, new String[]{value});
                        } else if (params[0].equals(int.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{int.class});
                            method.invoke(obj, new Integer[]{new Integer(value)});
                        } else if (params[0].equals(long.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{long.class});
                            method.invoke(obj, new Long[]{new Long(value)});
                        } else if (params[0].equals(float.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{float.class});
                            method.invoke(obj, new Float[]{new Float(value)});
                        } else if (params[0].equals(double.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{double.class});
                            method.invoke(obj, new Double[]{new Double(value)});
                        } else if (params[0].equals(boolean.class)) {
                            method = obj.getClass().getMethod(mName, new Class[]{boolean.class});
                            method.invoke(obj, new Boolean[]{new Boolean(value)});
                        } else {
                            handleException("Did not find a setter method named : " + mName +
                                "() that takes a single String, int, long, float, double " +
                                "or boolean parameter", synCtx);
                        }
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

    public void addStaticProperty(String name, String value) {
        this.staticProps.put(name, value);
    }

    public void addDynamicProperty(String name, Object value) {
        this.dynamicProps.put(name, value);
    }

    public Map getStaticProps() {
        return this.staticProps;
    }

    public Map getDynamicProps() {
        return this.dynamicProps;
    }
}
