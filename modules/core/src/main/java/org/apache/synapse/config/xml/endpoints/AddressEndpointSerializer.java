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
 * Serializes AddressEndpoint to XML.
 *
 * <endpoint [name="name"]>
 *  <suspendDurationOnFailue>suspend-duration</suspendDurationOnFailue>
 *  <address uri="url">
 *
 *    .. extensibility ..
 *
 *    <!-- Axis2 Rampart configurations : may be obsolete soon -->
 *    <parameter name="OutflowSecurity">
 *      ...
 *    </parameter>+
 *
 *    <!-- Apache Sandesha configurations : may be obsolete soon -->
 *    <wsp:Policy xmlns:wsp="http://schemas.xmlsoap.org/ws/2004/09/policy"..
 *      xmlns:wsrm="http://ws.apache.org/sandesha2/policy" wsu:Id="RMPolicy">
 *      ...
 *    </Policy>+
 *
 *    <enableRM/>+
 *    <enableSec/>+
 *    <enableAddressing/>+
 *
 *    <timeout>
 *      <duration>duration in milliseconds</duration>
 *      <action>discard | fault</action>
 *    </timeout>
 *
 *  </address>
 * </endpoint>
 */
public class AddressEndpointSerializer implements EndpointSerializer {

    private static Log log = LogFactory.getLog(AddressEndpointSerializer.class);

    private OMFactory fac = null;

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

    public OMElement serializeEndpointDefinition(EndpointDefinition endpt) {

        OMElement address = fac.createOMElement("address", SynapseConstants.SYNAPSE_OMNAMESPACE);

        if (SynapseConstants.FORMAT_POX.equals(endpt.getFormat())) {
        	address.addAttribute(fac.createOMAttribute("format", null, "pox"));
        	
        } else if (SynapseConstants.FORMAT_SOAP11.equals(endpt.getFormat())) {
            address.addAttribute(fac.createOMAttribute("format", null, "soap11"));
        	
        } else if (SynapseConstants.FORMAT_SOAP12.equals(endpt.getFormat())) {
            address.addAttribute(fac.createOMAttribute("format", null, "soap12"));
        
        	// following two kept for backward compatibility
        } else if (endpt.isForcePOX()) {
            address.addAttribute(fac.createOMAttribute("format", null, "pox"));
            
        } else if (endpt.isForceSOAP11()) {
            address.addAttribute(fac.createOMAttribute("format", null, "soap11"));
        } else if (endpt.isForceSOAP12()) {
            address.addAttribute(fac.createOMAttribute("format", null, "soap12"));
        }
        
        if (endpt.isUseSwa()) {
            address.addAttribute(fac.createOMAttribute("optimize", null, "swa"));
        } else if (endpt.isUseMTOM()) {
            address.addAttribute(fac.createOMAttribute("optimize", null, "mtom"));
        }
        if (endpt.getAddress() != null) {
            address.addAttribute(fac.createOMAttribute(
                    "uri", null, endpt.getAddress()));
//        } else {
//            handleException("Invalid Endpoint. Address is required");
        }

        int isEnableStatistics = endpt.getStatisticsState();
        String statisticsValue = null;
        if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_ON) {
            statisticsValue = org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ENABLE;
        } else if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_OFF) {
            statisticsValue = org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_DISABLE;
        }
        if (statisticsValue != null) {
            address.addAttribute(fac.createOMAttribute(
                    org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ATTRIB_NAME, null, statisticsValue));
        }
        if (endpt.isAddressingOn()) {
            OMElement addressing = fac.createOMElement("enableAddressing", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpt.isUseSeparateListener()) {
                addressing.addAttribute(fac.createOMAttribute(
                        "separateListener", null, "true"));
            }
            address.addChild(addressing);
        }

        if (endpt.isReliableMessagingOn()) {
            OMElement rm = fac.createOMElement("enableRM", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpt.getWsRMPolicyKey() != null) {
                rm.addAttribute(fac.createOMAttribute(
                        "policy", null, endpt.getWsRMPolicyKey()));
            }
            address.addChild(rm);
        }

        if (endpt.isSecurityOn()) {
            OMElement sec = fac.createOMElement("enableSec", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpt.getWsSecPolicyKey() != null) {
                sec.addAttribute(fac.createOMAttribute(
                        "policy", null, endpt.getWsSecPolicyKey()));
            }
            address.addChild(sec);
        }

        if (endpt.getTimeoutAction() != SynapseConstants.NONE) {
            OMElement timeout = fac.createOMElement("timeout", SynapseConstants.SYNAPSE_OMNAMESPACE);
            address.addChild(timeout);

            OMElement duration = fac.createOMElement("duration", SynapseConstants.SYNAPSE_OMNAMESPACE);
            duration.setText(Long.toString(endpt.getTimeoutDuration() / 1000));
            timeout.addChild(duration);

            OMElement action = fac.createOMElement("action", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpt.getTimeoutAction() == SynapseConstants.DISCARD) {
                action.setText("discard");
            } else if (endpt.getTimeoutAction() == SynapseConstants.DISCARD_AND_FAULT) {
                action.setText("fault");
            }
            timeout.addChild(action);
        }

        return address;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
