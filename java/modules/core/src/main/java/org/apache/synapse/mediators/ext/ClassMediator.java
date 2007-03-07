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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The class mediator delegates the mediation to a new instance of a specified class. The specified class
 * must implement the Mediator interface
 *
 * @see Mediator
 */
public class ClassMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(ClassMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    private Class clazz = null;
    private List properties = new ArrayList();

    /**
     * Delegate mediation to a new instance of the specified class
     *
     * @param synCtx the message context
     * @return as per standard semantics
     */
    public boolean mediate(MessageContext synCtx) {

        log.debug("Class mediator <" + clazz.getName() + ">:: mediate()");
        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        if (shouldTrace) {
            trace.trace("Start : Class mediator");
        }
        Mediator m ;
        try {
            try {
                m = (Mediator) clazz.newInstance();
            } catch (Exception e) {
                String msg = "Error while creating an instance of the specified mediator class : " + clazz.getName();
                if (shouldTrace)
                    trace.trace(msg);
                log.error(msg, e);
                throw new SynapseException(msg, e);
            }

            setProperties(m, synCtx, shouldTrace);
            if (shouldTrace) {
                trace.trace("Executing an instance of the specified class : " + clazz.getName());
            }
            return m.mediate(synCtx);
        } finally {
            if (shouldTrace) {
                trace.trace("End : Class mediator");
            }
        }
    }

    /**
     * Only String properties are supported
     *
     * @param m the mediator
     */
    private void setProperties(Mediator m, MessageContext synCtx, boolean shouldTrace) {

        Iterator iter = properties.iterator();
        while (iter.hasNext()) {

            MediatorProperty mProp = (MediatorProperty) iter.next();

            String mName = "set" + Character.toUpperCase(mProp.getName().charAt(0)) + mProp.getName().substring(1);
            String value = (mProp.getValue() != null ?
                mProp.getValue() :
                Axis2MessageContext.getStringValue(mProp.getExpression(), synCtx));

            try {
                if (value != null) {
                    Method method = m.getClass().getMethod(mName, new Class[]{String.class});
                    log.debug("Setting property :: invoking method " + mName + "(" + value + ")");
                    if (shouldTrace) {
                        trace.trace("Setting property :: invoking method " + mName + "(" + value + ")");
                    }
                    method.invoke(m, new Object[]{value});
                }
            } catch (Exception e) {
                String msg = "Error setting property : " + mProp.getName() + " as a String property into class" +
                    " mediator : " + m.getClass() + " : " + e.getMessage();
                log.error(msg);
                if (shouldTrace) {
                    trace.trace(msg);
                }
                throw new SynapseException(msg, e);
            }
        }
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }

    public void addProperty(MediatorProperty p) {
        properties.add(p);
    }

    public void addAllProperties(List list) {
        properties.addAll(list);
    }

    public List getProperties() {
        return properties;
    }

}
