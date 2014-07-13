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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.registry.Registry;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * Create an instance of the given registry, and sets properties on it.
 *
 * &lt;registry [name="string"] provider="provider.class"&gt;
 *   &lt;property name="string" value="string"&gt;
 * &lt;/registry&gt;
 */
public class RegistryFactory {

    private static final Log log = LogFactory.getLog(RegistryFactory.class);

    public static final QName PROVIDER_Q = new QName(Constants.NULL_NAMESPACE, "provider");
    public static final QName PROPERTY_Q = new QName(Constants.NULL_NAMESPACE, "property");
    public static final QName NAME_Q     = new QName(Constants.NULL_NAMESPACE, "name");
    public static final QName VALUE_Q    = new QName(Constants.NULL_NAMESPACE, "value");

    public static Registry createRegistry(OMElement elem) {

        OMAttribute prov = elem.getAttribute(PROVIDER_Q);
        if (prov != null) {
            try {
                Class provider = Class.forName(prov.getAttributeValue());
                Registry registry = (Registry) provider.newInstance();
                setProperties(registry, elem);

                OMAttribute name = elem.getAttribute(NAME_Q);
                if (name != null) {
                    registry.setRegistryName(name.getAttributeValue());
                }
                return registry;

            } catch (ClassNotFoundException e) {
                handleException("Cannot locate registry provider class : " +
                    prov.getAttributeValue(), e);
            } catch (IllegalAccessException e) {
                handleException("Error instantiating registry provider : " +
                    prov.getAttributeValue(), e);
            } catch (InstantiationException e) {
                handleException("Error instantiating registry provider : " +
                    prov.getAttributeValue(), e);
            }
        } else {
            handleException("The registry 'provider' attribute is required for a registry definition");
        }

        return null;
    }

    private static void setProperties(Registry reg, OMElement elem) {
        Iterator iter = elem.getChildrenWithName(PROPERTY_Q);
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof OMElement) {
                OMElement propEle = (OMElement) o;
                OMAttribute nAtt = propEle.getAttribute(NAME_Q);
                OMAttribute vAtt = propEle.getAttribute(VALUE_Q);
                if (nAtt != null && vAtt !=null) {
                    reg.addConfigProperty(nAtt.getAttributeValue(), vAtt.getAttributeValue());
                } else {
                    handleException("The 'name' and 'value' attributes are required for a " +
                        "registry property definition");
                }
            } else {
                handleException("Invalid 'property' definition for registry.");
            }
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
