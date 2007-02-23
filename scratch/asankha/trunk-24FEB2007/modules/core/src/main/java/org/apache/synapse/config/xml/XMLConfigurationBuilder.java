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

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Property;
import org.apache.synapse.config.Util;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.base.SynapseMediator;
import org.apache.synapse.registry.Registry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;


/**
 * Builds a Synapse Configuration model from an XML input stream.
 */
public class XMLConfigurationBuilder {

    private static Log log = LogFactory.getLog(XMLConfigurationBuilder.class);

    public static SynapseConfiguration getConfiguration(InputStream is) {

        log.info("Generating the Synapse configuration model by parsing the XML configuration");
        SynapseConfiguration config = new SynapseConfiguration();

        OMElement root = null;
        try {
            root = new StAXOMBuilder(is).getDocumentElement();
        } catch (XMLStreamException e) {
            handleException("Error parsing Synapse configuration : " + e.getMessage(), e);
        }
        root.build();

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
                        handleException("Unexpected element : " + elt);
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
            if (regs == null) {
                handleException("A valid Synapse configuration MUST specify the main mediator using the <rules> element");
            } else {
                // this is a fully dynamic configuration. look for synapse.xml at thr registry root
                OMNode cfg = config.getRegistry(null).lookup("synapse.xml");
                return getConfiguration(Util.getStreamSource(cfg).getInputStream());
            }

        } else {
            OMAttribute key = rules.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
            if (key != null) {
                Property dp = new Property();
                dp.setName("namespace");
                dp.setType(Property.DYNAMIC_TYPE);
                dp.setKey(key.getAttributeValue());
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

        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
            }
        }

        return config;
    }

    /**
     * <pre>
     * &lt;set-property name="string" value="string"/&gt;
     * </pre>
     *
     * @param elem
     */
    public static void defineProperty(SynapseConfiguration config, OMElement elem) {
        Property prop = PropertyFactory.createProperty(elem);
        if(prop.getType() == Property.SRC_TYPE) {
            try {
                prop.setValue(org.apache.synapse.config.Util.getObject(
                        new URL(prop.getSrc().toString())));
            } catch (MalformedURLException e) {
                handleException("Source URL is not valid");
            }
        }
        config.addProperty(prop.getName(), prop);
    }

    /**
     * <pre>
     * &lt;sequence name="string" [key="string"]&gt;
     *    Mediator*
     * &lt;/sequence&gt;
     * </pre>
     *
     * @param ele
     */
    public static void defineSequence(SynapseConfiguration config, OMElement ele) {
        OMAttribute name = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute key = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        if (name != null && key != null) {
            Property dp = new Property();
            dp.setType(Property.DYNAMIC_TYPE);
            dp.setKey(key.getAttributeValue());
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
     * <p/>
     * <pre>
     * &lt;endpoint name="string" [key="string"] [address="url"]&gt;
     *    .. extensibility ..
     * &lt;/endpoint&gt;
     * </pre>
     *
     * @param ele the &lt;endpoint&gt; element
     */
    public static void defineEndpoint(SynapseConfiguration config, OMElement ele) {

        OMAttribute name = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        OMAttribute key = ele.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        if (name != null && key != null) {
            Property dp = new Property();
            dp.setType(Property.DYNAMIC_TYPE);
            dp.setKey(key.getAttributeValue());
            dp.setMapper(EndpointFactory.getInstance());
            config.addNamedEndpoint(name.getAttributeValue(), dp);
        } else {
            Endpoint endpoint = EndpointFactory.createEndpoint(ele, false);
            // add this endpoint to the configuration
            config.addNamedEndpoint(endpoint.getName(), endpoint);
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
