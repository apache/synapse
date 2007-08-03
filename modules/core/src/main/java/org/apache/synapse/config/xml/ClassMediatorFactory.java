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

import java.lang.reflect.Method;
import java.util.Iterator;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.ext.ClassMediator;

import javax.xml.namespace.QName;

/**
 * Creates an instance of a Class mediator using XML configuration specified
 * <p/>
 * <pre>
 * &lt;class name=&quot;class-name&quot;&gt;
 *   &lt;property name=&quot;string&quot; value=&quot;literal&quot;&gt;
 *      either literal or XML child
 *   &lt;/property&gt;
 * &lt;/class&gt;
 * </pre>
 */
public class ClassMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(LogMediatorFactory.class);

    private static final QName CLASS_Q = new QName(Constants.SYNAPSE_NAMESPACE,
            "class");

    public Mediator createMediator(OMElement elem) {

        ClassMediator classMediator = new ClassMediator();

        OMAttribute name = elem.getAttribute(new QName(
                Constants.NULL_NAMESPACE, "name"));
        if (name == null) {
            String msg = "The name of the actual mediator class is a required attribute";
            log.error(msg);
            throw new SynapseException(msg);
        }
        Class clazz = null;
        Mediator m = null;
        try {
            clazz = getClass().getClassLoader().loadClass(
                    name.getAttributeValue());
            m = (Mediator) clazz.newInstance();
        } catch (Exception e) {
            String msg = "Error : " + name.getAttributeValue();
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }

        for (Iterator it = elem.getChildElements(); it.hasNext();) {
            OMElement child = (OMElement) it.next();
            if (child.getLocalName().toLowerCase().equals("property")) {

                String propertyName = child
                        .getAttributeValue(new QName("name"));
                String mName = "set"
                        + Character.toUpperCase(propertyName.charAt(0))
                        + propertyName.substring(1);

                // try to set String value first
                if (child.getAttributeValue(new QName("value")) != null) {
                    String value = child.getAttributeValue(new QName("value"));

                    try {
                        Method method = m.getClass().getMethod(mName,
                                new Class[]{String.class});
                        if (log.isDebugEnabled()) {
                            log.debug("Setting property :: invoking method "
                                    + mName + "(" + value + ")");
                        }
                        method.invoke(m, new Object[]{value});

                    } catch (Exception e) {
                        String msg = "Error setting property : " + propertyName
                                + " as a String property into class"
                                + " mediator : " + m.getClass() + " : "
                                + e.getMessage();
                        throw new SynapseException(msg, e);

                    }
                } else if (child.getAttributeValue(new QName("expression")) != null) {
                    // check whether there is an XPATH exp for the property value
                    // todo:
                } else {
                    // now try XML child
                    OMElement value = child.getFirstElement();
                    if (value != null) {

                        try {
                            Method method = m.getClass().getMethod(mName,
                                    new Class[]{OMElement.class});
                            if (log.isDebugEnabled()) {
                                log
                                        .debug("Setting property :: invoking method "
                                                + mName + "(" + value + ")");
                            }
                            method.invoke(m, new Object[]{value});

                        } catch (Exception e) {
                            String msg = "Error setting property : "
                                    + propertyName
                                    + " as an OMElement property into class"
                                    + " mediator : " + m.getClass() + " : "
                                    + e.getMessage();
                            throw new SynapseException(msg, e);

                        }

                    }

                    // classMediator.addAllProperties(MediatorPropertyFactory.getMediatorProperties(elem));

                }
            }
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        classMediator.setMediator(m);
        initMediator(classMediator, elem);

        return classMediator;
    }

    public QName getTagQName() {
        return CLASS_Q;
    }
}
