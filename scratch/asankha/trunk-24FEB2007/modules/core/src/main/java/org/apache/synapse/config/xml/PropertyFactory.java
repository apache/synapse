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

import org.apache.synapse.config.XMLToObjectMapper;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.config.Property;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMText;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.net.URL;
import java.net.MalformedURLException;


public class PropertyFactory implements XMLToObjectMapper {
    private static Log log = LogFactory.getLog(PropertyFactory.class);

    private static final PropertyFactory instance = new PropertyFactory();

    private PropertyFactory() {}

    public static Property createProperty(OMElement elem) {

        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        if (name == null) {
            handleException("The 'name' attribute is required for a property definition");
            return null;
        } else {
            Property property = new Property();
            property.setName(name.getAttributeValue());
            String value = elem.getAttributeValue(new QName(Constants.NULL_NAMESPACE, "value"));
            String src = elem.getAttributeValue(new QName(Constants.NULL_NAMESPACE, "src"));
            String key = elem.getAttributeValue(new QName(Constants.NULL_NAMESPACE, "key"));
            OMElement content = elem.getFirstElement();
            if(value != null) {
                property.setType(Property.VALUE_TYPE);
                property.setValue(value);
            } else if(src != null) {
                property.setType(Property.SRC_TYPE);
                try {
                    property.setSrc(new URL(src));
                } catch (MalformedURLException e) {
                    handleException("Given src attribute " + src + "is not a propper URL.");
                }
            } else if(key != null) {
                property.setType(Property.DYNAMIC_TYPE);
                property.setKey(key);
            } else if(content != null) {
                if (content instanceof OMText) {
                    property.setType(Property.INLINE_STRING_TYPE);
                    property.setValue(content.getText());
                } else {
                    property.setType(Property.INLINE_XML_TYPE);
                    property.setValue(content);
                }
            }
            return property;
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

    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createProperty((OMElement) om);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    public static PropertyFactory getInstance() {
        return instance;
    }
}
