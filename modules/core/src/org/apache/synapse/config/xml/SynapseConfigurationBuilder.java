/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.base.SynapseMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;


/**
 * Builds a Synapse Configuration model from an XML input stream.
 */
public class SynapseConfigurationBuilder {

    private static Log log = LogFactory.getLog(SynapseConfigurationBuilder.class);
    private SynapseConfiguration config = new SynapseConfiguration();

    public SynapseConfigurationBuilder() {}

    public SynapseConfiguration getConfig() {
        return config;
    }

    public void setConfiguration(InputStream is) {

        OMElement root = null;
        try {
            root = new StAXOMBuilder(is).getDocumentElement();
        } catch (XMLStreamException e) {
            String msg = "Error parsing Synapse configuration : " + e.getMessage();
            log.error(msg);
            throw new SynapseException(msg);
        }
        root.build();

        // digest defined Sequences
        OMContainer definitions = root.getFirstChildWithName(Constants.DEFINITIONS_ELT);
        if (definitions != null) {
            Iterator iter = definitions.getChildrenWithName(Constants.SEQUENCE_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                defineSequence(elt);
            }
        }

        // digest defined Endpoints
        OMContainer endpoints = root.getFirstChildWithName(Constants.ENDPOINT_ELT);
        if (endpoints != null) {
            Iterator iter = endpoints.getChildrenWithName(Constants.ENDPOINT_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                //defineEndpoint(synCfg, elt); //TODO process Endpoints
            }
        }

        // digest defined Global properties
        OMContainer properties = root.getFirstChildWithName(Constants.PROPERTY_ELT);
        if (properties != null) {
            Iterator iter = properties.getChildrenWithName(Constants.PROPERTY_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                defineProperty(elt);
            }
        }

        OMElement elem = root.getFirstChildWithName(Constants.RULES_ELT);
        if (elem == null) {
            String msg = "A valid Synapse configuration MUST specify the main mediator using the <rules> element";
            log.error(msg);
            throw new SynapseException(msg);
        } else {
            SynapseMediator sm = (SynapseMediator) MediatorFactoryFinder.getInstance().getMediator(elem);
            if (sm.getList().isEmpty()) {
                String msg = "Invalid configuration, the main mediator specified by the <rules> element is empty";
                log.error(msg);
                throw new SynapseException(msg);
            } else {
                config.setMainMediator(sm);
            }
        }

        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {}
        }
    }

    /**
     * <set-property name="string" value="string"/>
     * @param elem
     */
    private void defineProperty(OMElement elem) {
        OMAttribute name  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute value = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
        if (name == null || value == null) {
            String msg = "The 'name' and 'value' attributes are required";
            log.error(msg);
            throw new SynapseException(msg);
        }
        config.addProperty(name.getAttributeValue(), value.getAttributeValue());
    }

    private void defineSequence(OMElement ele) {
        SequenceMediator seq = (SequenceMediator) MediatorFactoryFinder.getInstance().getMediator(ele);
        config.addNamedMediator(seq.getName(), seq);
    }

}
