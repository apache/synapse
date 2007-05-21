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

package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.SALoadbalanceEndpoint;
import org.apache.synapse.endpoints.dispatch.Dispatcher;
import org.apache.synapse.endpoints.dispatch.SoapSessionDispatcher;
import org.apache.synapse.endpoints.dispatch.SimpleClientSessionDispatcher;
import org.apache.synapse.endpoints.dispatch.HttpSessionDispatcher;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.endpoints.algorithms.RoundRobin;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class SALoadbalanceEndpointSerializer implements EndpointSerializer {

    private static final Log log = LogFactory.getLog(SALoadbalanceEndpointSerializer.class);

    private OMFactory fac = null;

    public OMElement serializeEndpoint(Endpoint endpoint) {

        if (!(endpoint instanceof SALoadbalanceEndpoint)) {
            handleException("Invalid endpoint type for serializing. " +
                    "Expected: SALoadbalanceEndpoint Found: " + endpoint.getClass().getName());
        }

        SALoadbalanceEndpoint loadbalanceEndpoint = (SALoadbalanceEndpoint) endpoint;

        fac = OMAbstractFactory.getOMFactory();
        OMElement endpointElement = fac.createOMElement("endpoint", Constants.SYNAPSE_OMNAMESPACE);

        String name = loadbalanceEndpoint.getName();
        if (name != null) {
            endpointElement.addAttribute("name", name, null);
        }

        Dispatcher dispatcher = loadbalanceEndpoint.getDispatcher();

        if (dispatcher instanceof SoapSessionDispatcher) {
            OMElement sessionElement = fac.createOMElement("session", Constants.SYNAPSE_OMNAMESPACE);
            sessionElement.addAttribute("type", "soap", null);
            endpointElement.addChild(sessionElement);

        } else if (dispatcher instanceof HttpSessionDispatcher) {
            OMElement sessionElement = fac.createOMElement("session", Constants.SYNAPSE_OMNAMESPACE);
            sessionElement.addAttribute("type", "http", null);
            endpointElement.addChild(sessionElement);

        } else if (dispatcher instanceof SimpleClientSessionDispatcher) {
            OMElement sessionElement = fac.createOMElement("session", Constants.SYNAPSE_OMNAMESPACE);
            sessionElement.addAttribute("type", "simpleClientSession", null);
            endpointElement.addChild(sessionElement);
        }

        OMElement loadbalanceElement = fac.createOMElement("loadbalance", Constants.SYNAPSE_OMNAMESPACE);
        endpointElement.addChild(loadbalanceElement);

        LoadbalanceAlgorithm algorithm = loadbalanceEndpoint.getAlgorithm();
        String algorithmName = "roundRobin";
        if (algorithm instanceof RoundRobin) {
             algorithmName = "roundRobin";
        }
        loadbalanceElement.addAttribute("algorithm", algorithmName, null);

        List endpoints = loadbalanceEndpoint.getEndpoints();
        for (int i = 0; i < endpoints.size(); i++) {
            Endpoint childEndpoint = (Endpoint) endpoints.get(i);
            EndpointSerializer serializer = EndpointAbstractSerializer.
                    getEndpointSerializer(childEndpoint);
            OMElement aeElement = serializer.serializeEndpoint(childEndpoint);
            loadbalanceElement.addChild(aeElement);
        }

        return endpointElement;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
