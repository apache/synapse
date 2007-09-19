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
import org.apache.synapse.config.xml.PropertyHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
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
     * This will hold the log variable to be used for logging
     */
    private static final Log log = LogFactory.getLog(POJOCommandMediator.class);

    /**
     * This will hold the trace object to be used for tracing
     */
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /**
     * This will hold the command object to be executed
     */
    private Class command = null;

    private Object commandObject = null;

    /**
     * This will hold the properties of the relevent Command object as OMElements for the
     * use of serialization
     */
    private List properties = new ArrayList();

    /**
     * Implements the mediate method of the Mediator interface. This method will instantiate all
     * the properties and call the execute method of the Command object.
     *
     * @param synCtx - Synapse MessageContext to be mediated
     * @return boolean true since this will not stop exection chain
     */
    public boolean mediate(MessageContext synCtx) {

        // instantiate the command object
        try {
            commandObject = command.newInstance();
        } catch (InstantiationException e) {
            handleException("Unable to instantiate the Command object", e);
        } catch (IllegalAccessException e) {
            handleException("Unable to instantiate the Command object", e);
        }

        // then set the properties
        Iterator itr = properties.iterator();
        while(itr.hasNext()) {
            Object property = itr.next();
            if(property instanceof OMElement) {
                if(PropertyHelper.isStaticProperty((OMElement) property)) {
                    PropertyHelper.setStaticProperty((OMElement) property, commandObject);
                } else {
                    PropertyHelper.setDynamicProperty((OMElement) property, commandObject, synCtx);
                }
            }
        }

        // then call the execute method if the Command interface is implemented
        if(commandObject instanceof Command) {
            ((Command) commandObject).execute();
        } else {
            // use the reflection to find the execute method
            try {
                Method exeMethod = command.getMethod("execute", new Class[]{});
                try {
                    exeMethod.invoke(commandObject, new Object[]{});
                } catch (IllegalAccessException e) {
                    handleException("Unable to invoke the execute() method", e);
                } catch (InvocationTargetException e) {
                    handleException("Unable to invoke the execute() method", e);
                }
            } catch (NoSuchMethodException e) {
                // nothing to do in here (Command has no implementation)
            }
        }

        // continue the mediator execution
        return true;
    }

    public Class getCommand() {
        return command;
    }

    public void setCommand(Class command) {
        this.command = command;
    }

    public void addProperty(OMElement property) {
        this.properties.add(property);
    }

    public List getProperties() {
        return this.properties;
    }

    private void handleException(String message, Throwable e) {
        if (log.isDebugEnabled()) {
            log.debug(message + e.getMessage());
        }
        throw new SynapseException(message, e);
    }

    private void handleException(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        throw new SynapseException(message);
    }
}
