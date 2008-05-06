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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

/**
 * All endpoint serializers should implement this interface. Use EndpointAbstractSerializer to
 * obtain the correct EndpointSerializer implementation for a particular endpoint.
 * EndpointSerializer implementation may call other EndpointSerializer implementations to serialize
 * nested endpoints.
 */
public abstract class EndpointSerializer {

    private static Log log = LogFactory.getLog(EndpointSerializer.class);

    protected OMFactory fac;

    /**
     * Serializes the given endpoint implementation to an XML object.
     *
     * @param endpoint Endpoint implementation to be serialized.
     * @return OMElement containing XML configuration.
     */
    public abstract OMElement serializeEndpoint(Endpoint endpoint);

    /**
     * Serializes the QoS infomation of the endpoint to the XML element
     *
     * @param endpointDefinition specifies the QoS information of the endpoint
     * @param element to which the QoS information will be serialized
     */
    protected void serializeQOSInformation(
            EndpointDefinition endpointDefinition, OMElement element) {

        if (endpointDefinition.getStatisticsState() == SynapseConstants.STATISTICS_ON) {
            element.addAttribute(fac.createOMAttribute(XMLConfigConstants.STATISTICS_ATTRIB_NAME,
                    null, XMLConfigConstants.STATISTICS_ENABLE));
        } else if (endpointDefinition.getStatisticsState() == SynapseConstants.STATISTICS_OFF) {
            element.addAttribute(fac.createOMAttribute(XMLConfigConstants.STATISTICS_ATTRIB_NAME,
                    null, XMLConfigConstants.STATISTICS_DISABLE));
        }

        if (endpointDefinition.getTraceState() == SynapseConstants.TRACING_ON) {
            element.addAttribute(fac.createOMAttribute(XMLConfigConstants.TRACE_ATTRIB_NAME,
                    null, XMLConfigConstants.TRACE_ENABLE));
        } else if (endpointDefinition.getStatisticsState() == SynapseConstants.TRACING_OFF) {
            element.addAttribute(fac.createOMAttribute(XMLConfigConstants.TRACE_ATTRIB_NAME,
                    null, XMLConfigConstants.TRACE_DISABLE));
        }

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
            element.addAttribute(fac.createOMAttribute(
                    "encoding", null, endpointDefinition.getCharSetEncoding()));
        }
        
        if (endpointDefinition.isAddressingOn()) {
            OMElement addressing = fac.createOMElement(
                    "enableAddressing", SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (endpointDefinition.getAddressingVersion() != null) {
                addressing.addAttribute(fac.createOMAttribute(
                        "version", null, endpointDefinition.getAddressingVersion()));
            }
            
            if (endpointDefinition.isUseSeparateListener()) {
                addressing.addAttribute(fac.createOMAttribute("separateListener", null, "true"));
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

            OMElement timeout = fac.createOMElement(
                    "timeout", SynapseConstants.SYNAPSE_OMNAMESPACE);
            element.addChild(timeout);

            OMElement duration = fac.createOMElement(
                    "duration", SynapseConstants.SYNAPSE_OMNAMESPACE);
            duration.setText(Long.toString(endpointDefinition.getTimeoutDuration() / 1000));
            timeout.addChild(duration);

            OMElement action = fac.createOMElement("action", SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (endpointDefinition.getTimeoutAction() == SynapseConstants.DISCARD) {
                action.setText("discard");
            } else if (endpointDefinition.getTimeoutAction()
                    == SynapseConstants.DISCARD_AND_FAULT) {
                action.setText("fault");
            }
            timeout.addChild(action);
        }
    }

    protected void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }
}
