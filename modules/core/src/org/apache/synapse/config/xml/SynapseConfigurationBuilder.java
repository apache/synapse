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
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.config.Configuration;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.base.SynapseMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;


/**
 * Builds a Synapse Configuration model from an XML input stream.
 */
public class SynapseConfigurationBuilder {

    private static Log log = LogFactory.getLog(SynapseConfigurationBuilder.class);
    private SynapseConfiguration config = new SynapseConfiguration();
    ConfigurationFactoryFinder configFacFinder = ConfigurationFactoryFinder.getInstance();

    public SynapseConfigurationBuilder() {}

    public SynapseConfiguration getConfig() {
        return config;
    }

    public void setConfiguration(InputStream is) {

        OMElement root = null;
        try {
            root = new StAXOMBuilder(is).getDocumentElement();
        } catch (XMLStreamException e) {
            handleException("Error parsing Synapse configuration : " + e.getMessage(), e);
        }
        root.build();

        OMContainer definitions = root.getFirstChildWithName(Constants.DEFINITIONS_ELT);
        if (definitions != null) {

            // digest defined Sequences
            Iterator iter = definitions.getChildrenWithName(Constants.SEQUENCE_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                defineSequence(elt);
            }

            // digest defined Endpoints
            iter = definitions.getChildrenWithName(Constants.ENDPOINT_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                defineEndpoint(elt);
            }

            // digest defined named Configurations
            iter = definitions.getChildrenWithName(Constants.CONFIG_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                defineConfiguration(elt);
            }

            // digest defined Global properties
            iter = definitions.getChildrenWithName(Constants.PROPERTY_ELT);
            while (iter.hasNext()) {
                OMElement elt = (OMElement) iter.next();
                defineProperty(elt);
            }
        }

        OMElement elem = root.getFirstChildWithName(Constants.RULES_ELT);
        if (elem == null) {
            handleException("A valid Synapse configuration MUST specify the main mediator using the <rules> element");
        } else {
            SynapseMediator sm = (SynapseMediator) MediatorFactoryFinder.getInstance().getMediator(elem);
            if (sm.getList().isEmpty()) {
                handleException("Invalid configuration, the main mediator specified by the <rules> element is empty");
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
            handleException("The 'name' and 'value' attributes are required");
        }
        config.addProperty(name.getAttributeValue(), value.getAttributeValue());
    }

    private void defineSequence(OMElement ele) {
        SequenceMediator seq = (SequenceMediator) MediatorFactoryFinder.getInstance().getMediator(ele);
        config.addNamedMediator(seq.getName(), seq);
    }

    /**
     * Create an endpoint definition digesting an XML fragment
     *
     * <endpoint name="string" [address="url"]>
     *    .. extensibility ..
     * </endpoint>
     * @param ele the <endpoint> element
     */
    private void defineEndpoint(OMElement ele) {

        OMAttribute name = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        if (name == null) {
            handleException("The 'name' attribute is required for a named endpoint definition");
        } else {
            Endpoint endpoint = new Endpoint();
            endpoint.setName(name.getAttributeValue());

            OMAttribute address = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "address"));
            if (address != null) {
                try {
                    endpoint.setAddress(new URL(address.getAttributeValue()));
                    config.addNamedEndpoint(endpoint.getName(), endpoint);
                } catch (MalformedURLException e) {
                    handleException("Invalid URL specified for 'address' : " + address.getAttributeValue(), e);
                }
            }
        }
    }

    /**
     * Digest a named configuratino definition and add it to the SynapseConfiguration
     *
     * <configuration name="string" type="string">
     *    <property name="string" value="string"/>*
     * </configuration>
     * @param elem the XML element defining the configuration
     */
    private void defineConfiguration(OMElement elem) {

        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute type = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "type"));

        if (name == null) {
            handleException("The 'name' attribute is required for a named configuration definition");
        } else if (type == null) {
            handleException("The 'type' attribute is required for a named configuration definition");
        } else {
            config.addNamedConfiguration(name.getAttributeValue(), configFacFinder.getConfiguration(elem));
        }
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }


}
