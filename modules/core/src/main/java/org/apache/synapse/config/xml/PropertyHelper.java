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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.SynapseException;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * This class will be used as a Helper class to get the properties loaded while building the
 * Synapse Configuration from the XML
 */
public class PropertyHelper {

    /**
     * Log variable for the logging purposes
     */
    private static final Log log = LogFactory.getLog(PropertyHelper.class);

    /**
     * This method will set the static property discribed in the OMElement to the specified object.
     * This Object should have the setter method for the specified property name
     * 
     * @param property - OMElement specifying the property to be built in to the object
     * @param o - Object to which the specified property will be set.
     */
    public static void setStaticProperty(OMElement property, Object o) {

        if (property.getLocalName().toLowerCase().equals("property")) {

            String propertyName = property.getAttributeValue(new QName("name"));
            String mName = "set"
                    + Character.toUpperCase(propertyName.charAt(0))
                    + propertyName.substring(1);

            // try to set String value first
            if (property.getAttributeValue(new QName("value")) != null) {
                String value = property.getAttributeValue(new QName("value"));

                try {
                    Method method = o.getClass().getMethod(mName, new Class[]{String.class});
                    if (log.isDebugEnabled()) {
                        log.debug("Setting property :: invoking method "
                                + mName + "(" + value + ")");
                    }
                    method.invoke(o, new Object[]{value});

                } catch (Exception e) {
                    handleException("Error setting property : " + propertyName
                            + " as a String property into class mediator : " + o.getClass() + " : "
                            + e.getMessage(), e);
                }
                
            } else {
                // now try XML child
                OMElement value = property.getFirstElement();
                if (value != null) {

                    try {
                        Method method = o.getClass().getMethod(mName, new Class[]{OMElement.class});
                        if (log.isDebugEnabled()) {
                            log.debug("Setting property :: invoking method "
                                    + mName + "(" + value + ")");
                        }
                        method.invoke(o, new Object[]{value});

                    } catch (Exception e) {
                        handleException("Error setting property : " + propertyName
                                + " as an OMElement property into class mediator : "
                                + o.getClass() + " : " + e.getMessage(), e);
                    }

                }

            }
        }
    }

    /**
     * This method will be called in the mediation time to set the dynamic properties specified by
     * XPATH functions over the message context to the specified object. In this case the setter
     * method should be present for the specified property name
     * 
     * @param property - OMElement specifying the property to get the XPATH expression
     * @param o - Object to which the executed XPATH function value over the MC will be set
     * @param synCtx - MessageContext containg the message over which the XPATH function will
     *                 be executed
     */
    public static void setDynamicProperty(OMElement property, Object o, MessageContext synCtx) {

        if (property.getLocalName().toLowerCase().equals("property")) {

            String propertyName = property.getAttributeValue(new QName("name"));
            String mName = "set"
                    + Character.toUpperCase(propertyName.charAt(0))
                    + propertyName.substring(1);

            // try to set String value first
            if (property.getAttributeValue(new QName("expression")) != null) {
                String expression = property.getAttributeValue(new QName("expression"));

                try {
                    Method method = o.getClass().getMethod(mName, new Class[]{String.class});

                    AXIOMXPath xp = new AXIOMXPath(expression);
                    OMElementUtils.addNameSpaces(xp, property, log);
                    String value = Axis2MessageContext.getStringValue(xp, synCtx);

                    if (log.isDebugEnabled()) {
                        log.debug("Setting property :: invoking method "
                                + mName + "(" + expression + ")");
                    }

                    method.invoke(o, new Object[]{value});

                } catch(NoSuchMethodException e) {
                    handleException("Unable to set the dynamic property value to the class. " +
                            "No setter method for the property " + propertyName + " in class "
                            + o.getClass().getName(), e);
                } catch (JaxenException e) {
                    handleException("Unable to evaluate the XPATH " + expression
                            + " to set the property " + propertyName, e);
                } catch (IllegalAccessException e) {
                    handleException("Unable to set the dynamic property using the method "
                            + mName + " of the class " + o.getClass().getName(), e);
                } catch (InvocationTargetException e) {
                    handleException("Unable to set the dynamic property using the method "
                            + mName + " of the class " + o.getClass().getName(), e);
                }
            }
        }
    }

    /**
     * This method will check the given OMElement represent either a static property or not
     * 
     * @param property - OMElement to be checked for the static property
     * @return boolean true if the elemet represents a static property element false otherwise
     */
    public static boolean isStaticProperty(OMElement property) {

        return "property".equals(property.getLocalName().toLowerCase())
                && (property.getAttributeValue(new QName("expression")) == null);

    }

    private static void handleException(String message, Throwable e) {
        if(log.isDebugEnabled()) {
            log.debug(message + e.getMessage());
        }
        throw new SynapseException(message, e);
    }

    private static void handleException(String message) {
        if(log.isDebugEnabled()) {
            log.debug(message);
        }
        throw new SynapseException(message);
    }
}
