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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.config.Property;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import javax.xml.stream.XMLStreamConstants;

public class PropertySerializer {

    private static Log log = LogFactory.getLog(PropertySerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = fac.createOMNamespace(
            Constants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS = fac.createOMNamespace(Constants.NULL_NAMESPACE, "");

    /**
     * Serialize the Property object to an OMElement representing the property
     * @param property
     * @param parent
     * @return OMElement representing the property
     */
    public static OMElement serializeProperty(Property property, OMElement parent) {

        OMElement propertyElement = fac.createOMElement("set-property", synNS);
        propertyElement.addAttribute(fac.createOMAttribute(
                "name", nullNS, property.getName()));
//	    propertyElement.addAttribute(fac.createOMAttribute(
//                "type", nullNS, "" + property.getType()));

        if (property.getType() == Property.DYNAMIC_TYPE) {
            propertyElement.addAttribute(fac.createOMAttribute(
                    "key", nullNS, property.getKey()));
        } else if (property.getType() == Property.SRC_TYPE) {
            propertyElement.addAttribute(fac.createOMAttribute(
                    "src", nullNS, property.getSrc().toString()));
        } else if (property.getType() == Property.VALUE_TYPE) {
            propertyElement.addAttribute(fac.createOMAttribute(
                    "value", nullNS, (String) property.getValue()));
        } else if (property.getType() == Property.INLINE_XML_TYPE) {
            propertyElement.addChild((OMElement) property.getValue());
        } else if (property.getType() == Property.INLINE_STRING_TYPE) {
            OMTextImpl textData = (OMTextImpl) fac.createOMText((String)property.getValue());
            textData.setType(XMLStreamConstants.CDATA);
            propertyElement.addChild(textData);
        } else {
            handleException("Property type undefined");
        }
        if(parent != null) {
            parent.addChild(propertyElement);
        }
        return propertyElement;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
