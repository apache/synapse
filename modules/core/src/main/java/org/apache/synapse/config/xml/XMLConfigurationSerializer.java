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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Property;
import org.apache.synapse.core.axis2.ProxyService;

import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.util.Iterator;

public class XMLConfigurationSerializer {

    private static final Log log = LogFactory.getLog(XMLConfigurationSerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = fac.createOMNamespace(Constants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS = fac.createOMNamespace(Constants.NULL_NAMESPACE, "");


    public static void serializeConfiguration(SynapseConfiguration synCfg, OutputStream outputStream)
        throws XMLStreamException {

        OMElement synapse = fac.createOMElement("synapse", synNS);

        // process registries
        Iterator iter = synCfg.getRegistries().keySet().iterator();
        while (iter.hasNext()) {
            RegistrySerializer.serializeRegistry(synapse, synCfg.getRegistry((String) iter.next()));
        }

        OMElement definitions = fac.createOMElement("definitions", synNS);

        // process properties
        serializeProperties(definitions, synCfg);

        // process endpoints
        serializeEndpoints(definitions, synCfg);

        // process sequences
        iter = synCfg.getNamedSequences().keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            definitions.addChild(
                MediatorSerializerFinder.getInstance().getSerializer(synCfg.getNamedSequence(name))
                    .serializeMediator(null, synCfg.getNamedSequence(name)));
        }

        // add definitions
        synapse.addChild(definitions);

        // add proxy services
        OMElement proxies = fac.createOMElement("proxies", synNS);
        iter = synCfg.getProxyServices().iterator();
        while (iter.hasNext()) {
            ProxyService service = (ProxyService) iter.next();
            ProxyServiceSerializer.serializeProxy(proxies, service);
        }
        synapse.addChild(proxies);

        // process main mediator
        synapse.addChild(
            MediatorSerializerFinder.getInstance().getSerializer(synCfg.getMainMediator())
                .serializeMediator(null, synCfg.getMainMediator()));

        synapse.serialize(outputStream);
    }

    private static void serializeProperties(OMElement definitions, SynapseConfiguration synCfg) {
        Iterator iter = synCfg.getGlobalProps().keySet().iterator();
        while (iter.hasNext()) {
            String propertyName = (String) iter.next();
            PropertySerializer.serializeProperty(
                    synCfg.getPropertyObject(propertyName), definitions);
        }
    }

    private static void serializeEndpoints(OMElement definitions, SynapseConfiguration synCfg) {

        Iterator iter = synCfg.getNamedEndpoints().keySet().iterator();
        while (iter.hasNext()) {
            String endpointName = (String) iter.next();
            Object endpt = synCfg.getNamedEndpoint(endpointName);

            if (endpt instanceof Property) {
                OMElement endpoint = fac.createOMElement("endpoint", synNS);
                Property dp = (Property) endpt;
                endpoint.addAttribute(fac.createOMAttribute(
                        "name", nullNS, endpointName));
                endpoint.addAttribute(fac.createOMAttribute(
                        "key", nullNS, dp.getKey()));
                definitions.addChild(endpoint);

            } else if (endpt instanceof Endpoint) {
                EndpointSerializer.serializeEndpoint((Endpoint) endpt, definitions);
            } else {
                handleException("Invalid endpoint. Type : " + endpt.getClass());
            }
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
