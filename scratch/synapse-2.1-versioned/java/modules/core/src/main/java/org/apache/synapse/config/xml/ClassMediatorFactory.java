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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.util.PropertyHelper;
import org.apache.synapse.mediators.ext.ClassMediator;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Properties;

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

    private static final QName CLASS_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "class");

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        ClassMediator classMediator = new ClassMediator();

        OMAttribute name = elem.getAttribute(ATT_NAME);
        if (name == null) {
            String msg = "The name of the actual mediator class is a required attribute";
            log.error(msg);
            throw new SynapseException(msg);
        }
        Class clazz = null;
        Mediator m = null;
        try {
            ClassLoader classMediatorLoader = null;
            if(properties != null){
                classMediatorLoader = (ClassLoader) properties.get(SynapseConstants.SYNAPSE_LIB_LOADER);
            }
            classMediatorLoader = classMediatorLoader != null ? classMediatorLoader :
                                  getClass().getClassLoader();
            clazz = classMediatorLoader.loadClass(
                    name.getAttributeValue());
            m = (Mediator) clazz.newInstance();
        } catch (Exception e) {
            String msg = "Error : " + name.getAttributeValue();
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }

        for (Iterator it = elem.getChildrenWithName(PROP_Q); it.hasNext();) {
            OMElement child = (OMElement) it.next();

            String propName = child.getAttribute(ATT_NAME).getAttributeValue();
            if (propName == null) {
                handleException(
                    "A Class mediator property must specify the name attribute");
            } else {
                if (child.getAttribute(ATT_VALUE) != null) {
                    String value = child.getAttribute(ATT_VALUE).getAttributeValue();
                    classMediator.addProperty(propName, value);
                    PropertyHelper.setInstanceProperty(propName, value, m);
                } else {
                    OMNode omElt = child.getFirstElement();
                    if (omElt != null) {
                        classMediator.addProperty(propName, omElt);
                        PropertyHelper.setInstanceProperty(propName, omElt, m);
                    } else {
                        handleException("A Class mediator property must specify " +
                            "name and value attributes, or a name and a child XML fragment");
                    }
                }
            }
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        classMediator.setMediator(m);
        processAuditStatus(classMediator, elem);

        return classMediator;
    }

    public QName getTagQName() {
        return CLASS_Q;
    }
}
