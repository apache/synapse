/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * A utility class for serializing instances of MediatorProperty objects by reading
 * through a given XML configuration
 *
 * <pre>
 * &lt;element&gt;
 *    &lt;property name="string" (value="literal" | expression="xpath")/&gt;*
 * &lt;/element&gt;
 * </pre>
 */
public class MediatorPropertySerializer {
    private static final Log log = LogFactory.getLog(MediatorPropertySerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = SynapseConstants.SYNAPSE_OMNAMESPACE;
    protected static final OMNamespace nullNS
            = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");

    protected static final QName PROP_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");

    /**
     * Serialize all the properties to the given paren element. For each and every
     * property ther will be a seperate property element created inside the parent element.
     * 
     * @param parent element to which property elements should be added
     * @param props <code>Collection</code> of propertis
     */
    public static void serializeMediatorProperties(OMElement parent,
                                               Collection<MediatorProperty> props) {
            serializeMediatorProperties(parent, props, PROP_Q);
    }

    /**
     * Serialize all the properties to the given paren element. For each and every
     * property ther will be a seperate element with the given name created inside the
     * parent element.
     *
     * @param parent element to which property elements should be added
     * @param props <code>Collection</code> of propertis
     * @param childElementName <code>QNmae</code> of the property element to be created
     */
    public static void serializeMediatorProperties(OMElement parent,
                                               Collection<MediatorProperty> props,
                                               QName childElementName) {
        for (MediatorProperty mp : props) {
            serializeMediatorProperty(parent, mp, childElementName);
        }
    }

    /**
     * Serialize the property to the given paren element. There will be a element created with
     * the name property inside the parent element.
     *
     * @param parent element to which property elements should be added
     * @param mp a property to be serialized
     */
    public static void serializeMediatorProperty(OMElement parent,
                                             MediatorProperty mp) {
        serializeMediatorProperty(parent, mp, PROP_Q);
    }

    /**
     * Serialize the property to the given paren element. There will be a element created with
     * given name inside the parent element.
     *
     * @param parent element to which property elements should be added
     * @param mp a property to be serialized
     * @param childElementName <code>QName</code> of the element to be created
     */
    public static void serializeMediatorProperty(OMElement parent,
                                             MediatorProperty mp,
                                             QName childElementName) {
        OMElement prop = fac.createOMElement(childElementName, parent);
        if (mp.getName() != null) {
            prop.addAttribute(fac.createOMAttribute("name", nullNS, mp.getName()));
        } else {
            String msg = "Mediator property name missing";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (mp.getValue() != null) {
            prop.addAttribute(fac.createOMAttribute("value", nullNS, mp.getValue()));

        } else if (mp.getExpression() != null) {
            SynapseXPathSerializer.serializeXPath(mp.getExpression(), prop, "expression");

        } else {
            String msg = "Mediator property must have a literal value or be an expression";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (mp.getScope() != null && !XMLConfigConstants.SCOPE_DEFAULT.equals(mp.getScope())) {
            prop.addAttribute(fac.createOMAttribute("scope", nullNS, mp.getScope()));
        }
    }
}
