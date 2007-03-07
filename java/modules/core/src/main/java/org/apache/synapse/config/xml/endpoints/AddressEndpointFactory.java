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

import org.apache.synapse.mediators.builtin.send.endpoints.Endpoint;
import org.apache.synapse.mediators.builtin.send.endpoints.AddressEndpoint;
import org.apache.synapse.config.EndpointDefinition;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

public class AddressEndpointFactory implements EndpointFactory {

    private static Log log = LogFactory.getLog(AddressEndpointFactory.class);

    private static AddressEndpointFactory instance = new AddressEndpointFactory();

    private AddressEndpointFactory() {}

    public static AddressEndpointFactory getInstance() {
        return instance;
    }

    public Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        AddressEndpoint addressEndpoint = new AddressEndpoint();

        if (!anonymousEndpoint) {
            OMAttribute name = epConfig.getAttribute(new QName(
                    org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "name"));

            if (name != null) {
                addressEndpoint.setName(name.getAttributeValue());
            }
        }

        OMElement addressElement = epConfig.getFirstChildWithName
                (new QName(Constants.SYNAPSE_NAMESPACE, "address"));

        if (addressElement != null) {
            EndpointDefinition endpoint = createEndpointDefinition(addressElement);
            addressEndpoint.setEndpoint(endpoint);
        }

        return addressEndpoint;
    }

    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createEndpoint((OMElement) om, false);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    private EndpointDefinition createEndpointDefinition(OMElement elem) {
        
        OMAttribute address = elem.getAttribute(new QName(
                org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "uri"));
        OMAttribute force = elem.getAttribute(new QName(
                org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "force"));
        OMAttribute optimize = elem.getAttribute(new QName(
                org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "optimize"));

        EndpointDefinition endpoint = new EndpointDefinition();

        if (address != null) {
            endpoint.setAddress(address.getAttributeValue());
        } else {
            handleException("One of the 'address' or 'ref' attributes are required in an "
                    + "anonymous endpoint");
        }

        if (force != null)
        {
            String forceValue = force.getAttributeValue().trim().toLowerCase();
            if (forceValue.equals("pox")) {
                endpoint.setForcePOX(true);
            } else if (forceValue.equals("soap")) {
                endpoint.setForceSOAP(true);
            } else {
                handleException("force value -\""+forceValue+"\" not yet implemented");
            }
        }

        if (optimize != null && optimize.getAttributeValue().length() > 0) {
            String method = optimize.getAttributeValue().trim();
            if ("mtom".equalsIgnoreCase(method)) {
                endpoint.setUseMTOM(true);
            } else if ("swa".equalsIgnoreCase(method)) {
                endpoint.setUseSwa(true);
            }
        }

        OMElement wsAddr = elem.getFirstChildWithName(new QName(
                org.apache.synapse.config.xml.Constants.SYNAPSE_NAMESPACE, "enableAddressing"));
        if (wsAddr != null) {
            endpoint.setAddressingOn(true);
            String useSepList = wsAddr.getAttributeValue(new QName(
                    "separateListener"));
            if (useSepList != null) {
                if (useSepList.trim().toLowerCase().startsWith("tr")
                        || useSepList.trim().startsWith("1")) {
                    endpoint.setUseSeparateListener(true);
                }
            }
        }

        OMElement wsSec = elem.getFirstChildWithName(new QName(
                org.apache.synapse.config.xml.Constants.SYNAPSE_NAMESPACE, "enableSec"));
        if (wsSec != null) {
            endpoint.setSecurityOn(true);
            OMAttribute policy = wsSec.getAttribute(new QName(
                    org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                endpoint.setWsSecPolicyKey(policy.getAttributeValue());
            }
        }
        OMElement wsRm = elem.getFirstChildWithName(new QName(
                org.apache.synapse.config.xml.Constants.SYNAPSE_NAMESPACE, "enableRM"));
        if (wsRm != null) {
            endpoint.setReliableMessagingOn(true);
            OMAttribute policy = wsRm.getAttribute(new QName(
                    org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                endpoint.setWsRMPolicyKey(policy.getAttributeValue());
            }
        }

        return endpoint;
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
