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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.XMLToObjectMapper;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

import javax.xml.namespace.QName;

/**
 * All endpoint factories should implement this interface. Use EndpointAbstractFactory to obtain the
 * correct endpoint factory for particular endpoint configuration. As endpoints can be nested inside
 * each other, EndpointFactory implementations may call other EndpointFactory implementations
 * recursively to obtain the required endpoint hierarchy.
 *
 * This also serves as the XMLToObjactMapper implementation for specific endpoint implementations.
 * If the endpoint type is not known use XMLToEndpointMapper as the generic XMLToObjectMapper for
 * all endpoints.
 */
public abstract class EndpointFactory implements XMLToObjectMapper {

    private static Log log = LogFactory.getLog(EndpointFactory.class);

    /**
     * Creates the Endpoint implementation for the given XML endpoint configuration. If the endpoint
     * configuration is an inline one, it should be an anonymous endpoint. If it is defined as an
     * immediate child element of the definitions tag it should have a name, which is used as the
     * key in local registry.
     *
     * @param epConfig OMElement conatining the endpoint configuration.
     * @param anonymousEndpoint false if the endpoint has a name. true otherwise.
     * @return Endpoint implementation for the given configuration.
     */
    public abstract Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint);
    
    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createEndpoint((OMElement) om, false);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    /**
     * Extracts the QoS information from the XML which represents a WSDL/Address/Default endpoints
     *
     * @param endpointDefinition to be filled with the extracted information
     * @param elem XML which represents the endpoint with QoS information
     */
    protected void extractQOSInformation(EndpointDefinition endpointDefinition, OMElement elem) {

        OMAttribute format
                = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "format"));
        OMAttribute optimize
                = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "optimize"));
        OMAttribute encoding
                = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "encoding"));

        OMAttribute statistics = elem.getAttribute(new QName(
                XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.STATISTICS_ATTRIB_NAME));
        if (statistics != null && statistics.getAttributeValue() != null) {
            String statisticsValue = statistics.getAttributeValue();
            if (XMLConfigConstants.STATISTICS_ENABLE.equals(statisticsValue)) {
                endpointDefinition.setStatisticsState(SynapseConstants.STATISTICS_ON);
            } else if (XMLConfigConstants.STATISTICS_DISABLE.equals(statisticsValue)) {
                endpointDefinition.setStatisticsState(SynapseConstants.STATISTICS_OFF);
            }
        } else {
            endpointDefinition.setStatisticsState(SynapseConstants.STATISTICS_UNSET);
        }

        OMAttribute trace = elem.getAttribute(new QName(
                XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.TRACE_ATTRIB_NAME));
        if (trace != null && trace.getAttributeValue() != null) {
            String traceValue = trace.getAttributeValue();
            if (XMLConfigConstants.TRACE_ENABLE.equals(traceValue)) {
                endpointDefinition.setTraceState(SynapseConstants.TRACING_ON);
            } else if (XMLConfigConstants.TRACE_DISABLE.equals(traceValue)) {
                endpointDefinition.setTraceState(SynapseConstants.TRACING_OFF);
            }
        } else {
            endpointDefinition.setTraceState(SynapseConstants.TRACING_UNSET);
        }

        if (format != null) {
            String forceValue = format.getAttributeValue().trim().toLowerCase();
            if (SynapseConstants.FORMAT_POX.equals(forceValue)) {
                endpointDefinition.setForcePOX(true);
                endpointDefinition.setFormat(SynapseConstants.FORMAT_POX);

            } else if (SynapseConstants.FORMAT_GET.equals(forceValue)) {
            	endpointDefinition.setForceGET(true);
            	endpointDefinition.setFormat(SynapseConstants.FORMAT_GET);

            } else if (SynapseConstants.FORMAT_SOAP11.equals(forceValue)) {
                endpointDefinition.setForceSOAP11(true);
                endpointDefinition.setFormat(SynapseConstants.FORMAT_SOAP11);

            } else if (SynapseConstants.FORMAT_SOAP12.equals(forceValue)) {
                endpointDefinition.setForceSOAP12(true);
                endpointDefinition.setFormat(SynapseConstants.FORMAT_SOAP12);

            } else {
                handleException("force value -\""+forceValue+"\" not yet implemented");
            }
        }

        if (optimize != null && optimize.getAttributeValue().length() > 0) {
            String method = optimize.getAttributeValue().trim();
            if ("mtom".equalsIgnoreCase(method)) {
                endpointDefinition.setUseMTOM(true);
            } else if ("swa".equalsIgnoreCase(method)) {
                endpointDefinition.setUseSwa(true);
            }
        }

        if (encoding != null && encoding.getAttributeValue() != null) {
            endpointDefinition.setCharSetEncoding(encoding.getAttributeValue());
        }

        OMElement wsAddr = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableAddressing"));
        if (wsAddr != null) {
            
            endpointDefinition.setAddressingOn(true);

            OMAttribute version = wsAddr.getAttribute(new QName("version"));
            if (version != null && version.getAttributeValue() != null) {
                String versionValue = version.getAttributeValue().trim().toLowerCase();
                if (SynapseConstants.ADDRESSING_VERSION_FINAL.equals(versionValue) ||
                        SynapseConstants.ADDRESSING_VERSION_SUBMISSION.equals(versionValue)) {
                    endpointDefinition.setAddressingVersion(version.getAttributeValue());
                } else {
                    handleException("Unknown value for the addressing version. Possible values " +
                            "for the addressing version are 'final' and 'submission' only.");
                }
            }

            String useSepList = wsAddr.getAttributeValue(new QName("separateListener"));
            if (useSepList != null) {
                if ("true".equals(useSepList.trim().toLowerCase())) {
                    endpointDefinition.setUseSeparateListener(true);
                }
            }
        }

        OMElement wsSec = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableSec"));
        if (wsSec != null) {

            endpointDefinition.setSecurityOn(true);

            OMAttribute policy
                    = wsSec.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                endpointDefinition.setWsSecPolicyKey(policy.getAttributeValue());
            }
        }
        
        OMElement wsRm = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableRM"));
        if (wsRm != null) {

            endpointDefinition.setReliableMessagingOn(true);

            OMAttribute policy
                    = wsRm.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                endpointDefinition.setWsRMPolicyKey(policy.getAttributeValue());
            }
        }

        // set the timeout configuration
        OMElement timeout = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "timeout"));
        if (timeout != null) {
            OMElement duration = timeout.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "duration"));

            if (duration != null) {
                String d = duration.getText();
                if (d != null) {
                    try {
                        long timeoutSeconds = Long.parseLong(d.trim());
                        endpointDefinition.setTimeoutDuration(timeoutSeconds * 1000);
                    } catch (NumberFormatException e) {
                        handleException("Endpoint timeout duration expected as a " +
                                "number but was not a number");
                    }
                }
            }

            OMElement action = timeout.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "action"));
            if (action != null && action.getText() != null) {
                String actionString = action.getText();
                if ("discard".equalsIgnoreCase(actionString.trim())) {
                        
                    endpointDefinition.setTimeoutAction(SynapseConstants.DISCARD);

                    // set timeout duration to 30 seconds, if it is not set explicitly
                    if (endpointDefinition.getTimeoutDuration() == 0) {
                        endpointDefinition.setTimeoutDuration(30000);
                    }
                } else if ("fault".equalsIgnoreCase(actionString.trim())) {
                        
                    endpointDefinition.setTimeoutAction(SynapseConstants.DISCARD_AND_FAULT);

                    // set timeout duration to 30 seconds, if it is not set explicitly
                    if (endpointDefinition.getTimeoutDuration() == 0) {
                        endpointDefinition.setTimeoutDuration(30000);
                    }
                } else {
                    handleException("Invalid timeout action, action : "
                            + actionString + " is not supported");
                }
            }
        }
    }

    protected static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    protected static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
