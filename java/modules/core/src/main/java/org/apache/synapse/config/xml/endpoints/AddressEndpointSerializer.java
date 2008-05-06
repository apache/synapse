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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Serializes {@link AddressEndpoint} to XML.
 * 
 * @see AddressEndpointFactory
 */
public class AddressEndpointSerializer extends EndpointSerializer {

    public OMElement serializeEndpoint(Endpoint endpoint) {

        if (!(endpoint instanceof AddressEndpoint)) {
            throw new SynapseException("Invalid endpoint type.");
        }

        fac = OMAbstractFactory.getOMFactory();
        OMElement endpointElement = fac.createOMElement("endpoint", SynapseConstants.SYNAPSE_OMNAMESPACE);

        AddressEndpoint addressEndpoint = (AddressEndpoint) endpoint;
        String name = addressEndpoint.getName();
        if (name != null) {
            endpointElement.addAttribute("name", name, null);
        }

        EndpointDefinition epAddress = addressEndpoint.getEndpoint();
        OMElement addressElement = serializeEndpointDefinition(epAddress);
        endpointElement.addChild(addressElement);

        long suspendDuration = addressEndpoint.getSuspendOnFailDuration();
        if (suspendDuration != -1) {
            // user has set some value for this. let's serialize it.

            OMElement suspendElement = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_DURATION_ON_FAILURE,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);

            suspendElement.setText(Long.toString(suspendDuration / 1000));
            addressElement.addChild(suspendElement);
        }

        return endpointElement;
    }

    public OMElement serializeEndpointDefinition(EndpointDefinition endpointDefinition) {

        OMElement element = fac.createOMElement("address", SynapseConstants.SYNAPSE_OMNAMESPACE);

        if (SynapseConstants.FORMAT_POX.equals(endpointDefinition.getFormat())) {
            element.addAttribute(fac.createOMAttribute("format", null, "pox"));
        } else if (SynapseConstants.FORMAT_GET.equals(endpointDefinition.getFormat())) {
            element.addAttribute(fac.createOMAttribute("format", null, "get"));
        } else if (SynapseConstants.FORMAT_SOAP11.equals(endpointDefinition.getFormat())) {
            element.addAttribute(fac.createOMAttribute("format", null, "soap11"));
        } else if (SynapseConstants.FORMAT_SOAP12.equals(endpointDefinition.getFormat())) {
            element.addAttribute(fac.createOMAttribute("format", null, "soap12"));
        
            // following two kept for backward compatibility
        } else if (endpointDefinition.isForcePOX()) {
            element.addAttribute(fac.createOMAttribute("format", null, "pox"));
        } else if (endpointDefinition.isForceGET()) {
            element.addAttribute(fac.createOMAttribute("format", null, "get"));
        } else if (endpointDefinition.isForceSOAP11()) {
            element.addAttribute(fac.createOMAttribute("format", null, "soap11"));
        } else if (endpointDefinition.isForceSOAP12()) {
            element.addAttribute(fac.createOMAttribute("format", null, "soap12"));
        }
        
        if (endpointDefinition.isUseSwa()) {
            element.addAttribute(fac.createOMAttribute("optimize", null, "swa"));
        } else if (endpointDefinition.isUseMTOM()) {
            element.addAttribute(fac.createOMAttribute("optimize", null, "mtom"));
        }
        
        if (endpointDefinition.getCharSetEncoding() != null) {
            element.addAttribute(fac.createOMAttribute("encoding", null, endpointDefinition.getCharSetEncoding()));
        }
        
        if (endpointDefinition.getAddress() != null) {
            element.addAttribute(fac.createOMAttribute(
                    "uri", null, endpointDefinition.getAddress()));
//        } else {
//            handleException("Invalid Endpoint. Address is required");
        }

        int isEnableStatistics = endpointDefinition.getStatisticsState();
        String statisticsValue = null;
        if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_ON) {
            statisticsValue = org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ENABLE;
        } else if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_OFF) {
            statisticsValue = org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_DISABLE;
        }
        if (statisticsValue != null) {
            element.addAttribute(fac.createOMAttribute(
                    org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ATTRIB_NAME, null, statisticsValue));
        }
        if (endpointDefinition.isAddressingOn()) {
            OMElement addressing = fac.createOMElement("enableAddressing", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getAddressingVersion() != null) {
                addressing.addAttribute(fac.createOMAttribute(
                        "version", null, endpointDefinition.getAddressingVersion()));
            }
            if (endpointDefinition.isUseSeparateListener()) {
                addressing.addAttribute(fac.createOMAttribute(
                        "separateListener", null, "true"));
            }
            element.addChild(addressing);
        }

        if (endpointDefinition.isReliableMessagingOn()) {
            OMElement rm = fac.createOMElement("enableRM", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getWsRMPolicyKey() != null) {
                rm.addAttribute(fac.createOMAttribute(
                        "policy", null, endpointDefinition.getWsRMPolicyKey()));
            }
            element.addChild(rm);
        }

        if (endpointDefinition.isSecurityOn()) {
            OMElement sec = fac.createOMElement("enableSec", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getWsSecPolicyKey() != null) {
                sec.addAttribute(fac.createOMAttribute(
                        "policy", null, endpointDefinition.getWsSecPolicyKey()));
            }
            element.addChild(sec);
        }

        if (endpointDefinition.getTimeoutAction() != SynapseConstants.NONE) {
            OMElement timeout = fac.createOMElement("timeout", SynapseConstants.SYNAPSE_OMNAMESPACE);
            element.addChild(timeout);

            OMElement duration = fac.createOMElement("duration", SynapseConstants.SYNAPSE_OMNAMESPACE);
            duration.setText(Long.toString(endpointDefinition.getTimeoutDuration() / 1000));
            timeout.addChild(duration);

            OMElement action = fac.createOMElement("action", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getTimeoutAction() == SynapseConstants.DISCARD) {
                action.setText("discard");
            } else if (endpointDefinition.getTimeoutAction() == SynapseConstants.DISCARD_AND_FAULT) {
                action.setText("fault");
            }
            timeout.addChild(action);
        }

        return element;
    }
}
