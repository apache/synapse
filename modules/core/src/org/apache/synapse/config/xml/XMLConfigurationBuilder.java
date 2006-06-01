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
public class XMLConfigurationBuilder {

    private static Log log = LogFactory.getLog(XMLConfigurationBuilder.class);
    ExtensionFactoryFinder extensionFacFinder = ExtensionFactoryFinder.getInstance();

    public SynapseConfiguration getConfiguration(InputStream is) {

        log.info("Generating the Synapse configuration model by parsing the XML configuration");
        SynapseConfiguration config = new SynapseConfiguration();

        OMElement root = null;
        try {
            root = new StAXOMBuilder(is).getDocumentElement();
        } catch (XMLStreamException e) {
            handleException("Error parsing Synapse configuration : " + e.getMessage(), e);
        }
        root.build();

        OMContainer definitions = root.getFirstChildWithName(Constants.DEFINITIONS_ELT);
        if (definitions != null) {

            Iterator iter = definitions.getChildren();
            while (iter.hasNext()) {
                Object o = iter.next();
                if (o instanceof OMElement) {
                    OMElement elt = (OMElement) o;
                    if (Constants.SEQUENCE_ELT.equals(elt.getQName())) {
                        defineSequence(config, elt);
                    } else if (Constants.ENDPOINT_ELT.equals(elt.getQName())) {
                        defineEndpoint(config, elt);
                    } else if (Constants.PROPERTY_ELT.equals(elt.getQName())) {
                        defineProperty(config, elt);
                    } else {
                        defineExtension(config, elt);
                    }
                }
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

        return config;
    }

    /**
     * <pre>
     * &lt;set-property name="string" value="string"/&gt;
     * </pre>
     * @param elem
     */
    private void defineProperty(SynapseConfiguration config, OMElement elem) {
        OMAttribute name  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute value = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
        if (name == null || value == null) {
            handleException("The 'name' and 'value' attributes are required");
        }
        config.addProperty(name.getAttributeValue(), value.getAttributeValue());
    }

    /**
     * <pre>
     * &lt;sequence name="string&gt;
     *    Mediator+
     * &lt;/sequence&gt;
     * </pre>
     * @param ele
     */
    private void defineSequence(SynapseConfiguration config, OMElement ele) {
        SequenceMediator seq = (SequenceMediator) MediatorFactoryFinder.getInstance().getMediator(ele);
        config.addNamedMediator(seq.getName(), seq);
    }

    /**
     * Create an endpoint definition digesting an XML fragment
     *
     * <pre>
     * &lt;endpoint name="string" [address="url"]&gt;
     *    .. extensibility ..
     * &lt;/endpoint&gt;
     * </pre>
     * @param ele the &lt;endpoint&gt; element
     */
    private void defineEndpoint(SynapseConfiguration config, OMElement ele) {

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
     * Digest extensions into Synapse configuration definitions
     *
     * An extension *must* have a unique 'name' attribute. The instance
     * created through the ExtensionFactoryFinder will be set as a
     * global property into the SynapseConfiguration with this name as
     * the key.
     *
     * e.g. The Spring configuration extension is as follows
     * <pre>
     * &lt;configuration name="string" src="string"/&gt;
     * </pre>
     *
     * @param elem the XML element defining the configuration
     */
    private void defineExtension(SynapseConfiguration config, OMElement elem) {

        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));

        if (name == null) {
            handleException("The 'name' attribute is required for an extension configuration definition");
        } else {
            config.addProperty(name.getAttributeValue(), extensionFacFinder.getExtension(elem));
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
