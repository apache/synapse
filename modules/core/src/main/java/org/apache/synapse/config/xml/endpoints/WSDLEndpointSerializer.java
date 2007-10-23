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
import org.apache.synapse.endpoints.WSDLEndpoint;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Serializes an WSDL based endpoint to an XML configuration.
 *
 * <endpoint [name="name"]>
 *    <suspendDurationOnFailue>suspend-duration</suspendDurationOnFailue>
 *    <wsdl uri="wsdl uri" service="service name" port="port name">
 *       .. extensibility ..
 *    </wsdl>
 * </endpoint>
 */
public class WSDLEndpointSerializer implements EndpointSerializer {

    private static Log log = LogFactory.getLog(WSDLEndpointSerializer.class);

    private OMFactory fac = null;

    public OMElement serializeEndpoint(Endpoint endpoint) {

        if (!(endpoint instanceof WSDLEndpoint)) {
            throw new SynapseException("Invalid endpoint type.");
        }

        fac = OMAbstractFactory.getOMFactory();
        OMElement endpointElement = fac.createOMElement("endpoint", SynapseConstants.SYNAPSE_OMNAMESPACE);

        WSDLEndpoint wsdlEndpoint = (WSDLEndpoint) endpoint;
        String name = wsdlEndpoint.getName();
        if (name != null) {
            endpointElement.addAttribute("name", name, null);
        }

        OMElement wsdlElement = fac.createOMElement("wsdl", SynapseConstants.SYNAPSE_OMNAMESPACE);
        String serviceName = wsdlEndpoint.getServiceName();
        if (serviceName != null) {
            wsdlElement.addAttribute("service", serviceName, null);
        }

        String portName = wsdlEndpoint.getPortName();
        if (portName != null) {
            wsdlElement.addAttribute("port", portName, null);
        }

        String uri = wsdlEndpoint.getWsdlURI();
        if (uri != null) {
            wsdlElement.addAttribute("uri", uri, null);
        }

        OMElement wsdlDoc = wsdlEndpoint.getWsdlDoc();
        if (wsdlDoc != null) {
            wsdlElement.addChild(wsdlDoc);
        }

        long suspendDuration = wsdlEndpoint.getSuspendOnFailDuration();
        if (suspendDuration != -1) {
            // user has set some value for this. let's serialize it.

            OMElement suspendElement = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_DURATION_ON_FAILURE,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);

            suspendElement.setText(Long.toString(suspendDuration / 1000));
            wsdlElement.addChild(suspendElement);
        }

        // currently, we have to get QOS information from the endpoint definition and set them as
        // special elements under the wsdl element. in future, these information should be
        // extracted from the wsdl.
        EndpointDefinition epAddress = wsdlEndpoint.getEndpoint();
        serializeQOSInformation(epAddress, wsdlElement);

        endpointElement.addChild(wsdlElement);

        return endpointElement;
    }

    public void serializeQOSInformation
            (EndpointDefinition endpointDefinition, OMElement wsdlElement) {

        if (SynapseConstants.FORMAT_POX.equals(endpointDefinition.getFormat())) {
            wsdlElement.addAttribute(fac.createOMAttribute("format", null, "pox"));
            
        } else if (SynapseConstants.FORMAT_SOAP11.equals(endpointDefinition.getFormat())) {
            wsdlElement.addAttribute(fac.createOMAttribute("format", null, "soap11"));
            
        } else if (SynapseConstants.FORMAT_SOAP12.equals(endpointDefinition.getFormat())) {
            wsdlElement.addAttribute(fac.createOMAttribute("format", null, "soap12"));
        
            // following two kept for backward compatibility
        } else if (endpointDefinition.isForcePOX()) {
            wsdlElement.addAttribute(fac.createOMAttribute("format", null, "pox"));
            
        } else if (endpointDefinition.isForceSOAP11()) {
            wsdlElement.addAttribute(fac.createOMAttribute("format", null, "soap11"));
        } else if (endpointDefinition.isForceSOAP12()) {
            wsdlElement.addAttribute(fac.createOMAttribute("format", null, "soap12"));
        }
        
        int isEnableStatistics = endpointDefinition.getStatisticsState();
        String statisticsValue = null;
        if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_ON) {
            statisticsValue = org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ENABLE;
        } else if (isEnableStatistics == org.apache.synapse.SynapseConstants.STATISTICS_OFF) {
            statisticsValue = org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_DISABLE;
        }
        if (statisticsValue != null) {
            wsdlElement.addAttribute(fac.createOMAttribute(
                    org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ATTRIB_NAME, null, statisticsValue));
        }
        if (endpointDefinition.isAddressingOn()) {
            OMElement addressing = fac.createOMElement("enableAddressing", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.isUseSeparateListener()) {
                addressing.addAttribute(fac.createOMAttribute(
                        "separateListener", null, "true"));
            }
            wsdlElement.addChild(addressing);
        }

        if (endpointDefinition.isReliableMessagingOn()) {
            OMElement rm = fac.createOMElement("enableRM", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getWsRMPolicyKey() != null) {
                rm.addAttribute(fac.createOMAttribute(
                        "policy", null, endpointDefinition.getWsRMPolicyKey()));
            }
            wsdlElement.addChild(rm);
        }

        if (endpointDefinition.isSecurityOn()) {
            OMElement sec = fac.createOMElement("enableSec", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getWsSecPolicyKey() != null) {
                sec.addAttribute(fac.createOMAttribute(
                        "policy", null, endpointDefinition.getWsSecPolicyKey()));
            }
            wsdlElement.addChild(sec);
        }

        if (endpointDefinition.getTimeoutAction() != SynapseConstants.NONE) {
            OMElement timeout = fac.createOMElement("timeout", SynapseConstants.SYNAPSE_OMNAMESPACE);
            wsdlElement.addChild(timeout);

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
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
