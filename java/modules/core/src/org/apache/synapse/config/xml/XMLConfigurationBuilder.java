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
import org.apache.synapse.registry.Registry;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.config.DynamicProperty;
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

        OMElement proxies = root.getFirstChildWithName(Constants.PROXIES_ELT);
        if (proxies != null) {
            Iterator iter = proxies.getChildren();
            while (iter.hasNext()) {
                Object o = iter.next();
                if (o instanceof OMElement) {
                    OMElement elt = (OMElement) o;
                    if (Constants.PROXY_ELT.equals(elt.getQName())) {
                        ProxyService proxy = ProxyServiceFactory.createProxy(elt);
                        config.addProxyService(proxy.getName(), proxy);
                    }
                }
            }
        }

        OMElement rules = root.getFirstChildWithName(Constants.RULES_ELT);

        if (rules == null) {
            handleException("A valid Synapse configuration MUST specify the main mediator using the <rules> element");
        } else {
            OMAttribute key  = rules.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
            if (key != null) {
                DynamicProperty dp = new DynamicProperty(key.getAttributeValue());
                dp.setMapper(MediatorFactoryFinder.getInstance());
                config.setMainMediator(dp);
            } else {
                SynapseMediator sm = (SynapseMediator)
                    MediatorFactoryFinder.getInstance().getMediator(rules);
                if (sm.getList().isEmpty()) {
                    handleException("Invalid configuration, the main mediator specified by the <rules> element is empty");
                } else {
                    config.setMainMediator(sm);
                }
            }
        }

        Iterator regs = root.getChildrenWithName(Constants.REGISTRY_ELT);
        if (regs != null) {
            while (regs.hasNext()) {
                Object o = regs.next();
                if (o instanceof OMElement) {
                    Registry reg = RegistryFactory.createRegistry((OMElement) o);
                    config.addRegistry(reg.getRegistryName(), reg);
                } else {
                    handleException("Invalid registry declaration in configuration");
                }
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
    public void defineProperty(SynapseConfiguration config, OMElement elem) {
        OMAttribute name  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute value = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "value"));
        OMAttribute src   = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "src"));
        OMAttribute key   = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        if (name == null) {
            handleException("The 'name' attribute is required for a property definition");
        } else if (
            (value != null && src != null) ||
            (value != null && key != null) ||
            (src != null && key != null)) {
            // if more than one attribute of (value|src|key) is specified
            handleException("A property must use exactly one of 'value', " +
                "'src' or 'key' attributes");

        } else if (value == null && src == null && key == null) {
            // if no attribute of (value|src|key) is specified, check if this is a TextNode
            if (elem.getFirstOMChild() != null) {
                config.addProperty(name.getAttributeValue(), elem.getFirstOMChild());
            } else {
                handleException("A property must use exactly one of 'value', " +
                    "'src' or 'key' attributes");
            }
        }

        // a simple string literal property
        if (value != null) {
            config.addProperty(name.getAttributeValue(), value.getAttributeValue());

        // a property (XML) loaded once through the given URL as an OMNode
        } else if (src != null) {
            try {
                config.addProperty(name.getAttributeValue(),
                    org.apache.synapse.config.Util.getObject(new URL(src.getAttributeValue())));
            } catch (MalformedURLException e) {
                handleException("Invalid URL specified by 'src' attribute : " +
                    src.getAttributeValue(), e);
            }

        // a DynamicProperty which refers to an XML resource on a remote registry
        } else if (key != null) {
            config.addProperty(name.getAttributeValue(),
                new DynamicProperty(key.getAttributeValue()));

        // an inline XML fragment
        } else {
            config.addProperty(name.getAttributeValue(), elem.getFirstElement());
        }
    }

    /**
     * <pre>
     * &lt;sequence name="string" [key="string"]&gt;
     *    Mediator*
     * &lt;/sequence&gt;
     * </pre>
     * @param ele
     */
    public void defineSequence(SynapseConfiguration config, OMElement ele) {
        OMAttribute name = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute key  = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        if (name != null && key != null) {
            DynamicProperty dp = new DynamicProperty(key.getAttributeValue());
            dp.setMapper(MediatorFactoryFinder.getInstance());
            config.addNamedSequence(name.getAttributeValue(), dp);
        } else {
            SequenceMediator seq = (SequenceMediator)
                MediatorFactoryFinder.getInstance().getMediator(ele);
            config.addNamedSequence(seq.getName(), seq);
        }
    }

    /**
     * Create an endpoint definition digesting an XML fragment
     *
     * <pre>
     * &lt;endpoint name="string" [key="string"] [address="url"]&gt;
     *    .. extensibility ..
     * &lt;/endpoint&gt;
     * </pre>
     * @param ele the &lt;endpoint&gt; element
     */
    public void defineEndpoint(SynapseConfiguration config, OMElement ele) {

        OMAttribute name = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute key  = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        if (name != null && key != null) {
            DynamicProperty dp = new DynamicProperty(key.getAttributeValue());
            dp.setMapper(EndpointFactory.getInstance());
            config.addNamedEndpoint(name.getAttributeValue(), dp);
        } else {
            Endpoint endpoint = EndpointFactory.createEndpoint(ele);
            // add this endpoint to the configuration
            config.addNamedEndpoint(endpoint.getName(), endpoint);
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
    public void defineExtension(SynapseConfiguration config, OMElement elem) {

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
