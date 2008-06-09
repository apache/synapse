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
import org.apache.synapse.endpoints.IndirectEndpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * All endpoint factories should implement this interface. Use EndpointFactory to obtain the
 * correct endpoint for particular endpoint configuration. As endpoints can be nested inside
 * each other, EndpointFactory implementations may call other EndpointFactory implementations
 * recursively to obtain the required endpoint hierarchy.
 * <p/>
 * This also serves as the XMLToObjactMapper implementation for specific endpoint implementations.
 * If the endpoint type is not known use XMLToEndpointMapper as the generic XMLToObjectMapper for
 * all endpoints.
 */
public abstract class EndpointFactory implements XMLToObjectMapper {

    private static Log log = LogFactory.getLog(EndpointFactory.class);

    /**
     * Core method which is exposed for the external use, and this will find the proper
     * {@link EndpointFactory} and create the endpoint which is of the format {@link Endpoint}.
     *
     * @param elem        XML from which the endpoint will be built
     * @param isAnonymous whether this is an anonymous endpoint or not
     * @return created endpoint
     */
    public static Endpoint getEndpointFromElement(OMElement elem, boolean isAnonymous) {
        return getEndpointFactory(elem).createEndpoint(elem, isAnonymous);
    }

    /**
     * Creates the {@link Endpoint} object from the provided {@link OMNode}
     *
     * @param om XML node from which the endpoint will be built
     * @return created endpoint as an {@link Object}
     */
    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createEndpoint((OMElement) om, false);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    /**
     * Creates the Endpoint implementation for the given XML endpoint configuration. If the endpoint
     * configuration is an inline one, it should be an anonymous endpoint. If it is defined as an
     * immediate child element of the definitions tag it should have a name, which is used as the
     * key in local registry.
     *
     * @param epConfig          OMElement conatining the endpoint configuration.
     * @param anonymousEndpoint false if the endpoint has a name. true otherwise.
     * @return Endpoint implementation for the given configuration.
     */
    protected abstract Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint);

    /**
     * Extracts the QoS information from the XML which represents a WSDL/Address/Default endpoints
     *
     * @param definition to be filled with the extracted information
     * @param elem       XML which represents the endpoint with QoS information
     */
    protected void extractCommonEndpointProperties(EndpointDefinition definition, OMElement elem) {

        OMAttribute optimize
                = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "optimize"));
        OMAttribute encoding
                = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "encoding"));

        OMAttribute statistics = elem.getAttribute(new QName(
                XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.STATISTICS_ATTRIB_NAME));
        if (statistics != null && statistics.getAttributeValue() != null) {
            String statisticsValue = statistics.getAttributeValue();
            if (XMLConfigConstants.STATISTICS_ENABLE.equals(statisticsValue)) {
                definition.setStatisticsState(SynapseConstants.STATISTICS_ON);
            } else if (XMLConfigConstants.STATISTICS_DISABLE.equals(statisticsValue)) {
                definition.setStatisticsState(SynapseConstants.STATISTICS_OFF);
            }
        } else {
            definition.setStatisticsState(SynapseConstants.STATISTICS_UNSET);
        }

        OMAttribute trace = elem.getAttribute(new QName(
                XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.TRACE_ATTRIB_NAME));
        if (trace != null && trace.getAttributeValue() != null) {
            String traceValue = trace.getAttributeValue();
            if (XMLConfigConstants.TRACE_ENABLE.equals(traceValue)) {
                definition.setTraceState(SynapseConstants.TRACING_ON);
            } else if (XMLConfigConstants.TRACE_DISABLE.equals(traceValue)) {
                definition.setTraceState(SynapseConstants.TRACING_OFF);
            }
        } else {
            definition.setTraceState(SynapseConstants.TRACING_UNSET);
        }


        if (optimize != null && optimize.getAttributeValue().length() > 0) {
            String method = optimize.getAttributeValue().trim();
            if ("mtom".equalsIgnoreCase(method)) {
                definition.setUseMTOM(true);
            } else if ("swa".equalsIgnoreCase(method)) {
                definition.setUseSwa(true);
            }
        }

        if (encoding != null && encoding.getAttributeValue() != null) {
            definition.setCharSetEncoding(encoding.getAttributeValue());
        }

        OMElement wsAddr = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableAddressing"));
        if (wsAddr != null) {

            definition.setAddressingOn(true);

            OMAttribute version = wsAddr.getAttribute(new QName("version"));
            if (version != null && version.getAttributeValue() != null) {
                String versionValue = version.getAttributeValue().trim().toLowerCase();
                if (SynapseConstants.ADDRESSING_VERSION_FINAL.equals(versionValue) ||
                        SynapseConstants.ADDRESSING_VERSION_SUBMISSION.equals(versionValue)) {
                    definition.setAddressingVersion(version.getAttributeValue());
                } else {
                    handleException("Unknown value for the addressing version. Possible values " +
                            "for the addressing version are 'final' and 'submission' only.");
                }
            }

            String useSepList = wsAddr.getAttributeValue(new QName("separateListener"));
            if (useSepList != null) {
                if ("true".equals(useSepList.trim().toLowerCase())) {
                    definition.setUseSeparateListener(true);
                }
            }
        }

        OMElement wsSec = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableSec"));
        if (wsSec != null) {

            definition.setSecurityOn(true);

            OMAttribute policy
                    = wsSec.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                definition.setWsSecPolicyKey(policy.getAttributeValue());
            }
        }

        OMElement wsRm = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "enableRM"));
        if (wsRm != null) {

            definition.setReliableMessagingOn(true);

            OMAttribute policy
                    = wsRm.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                definition.setWsRMPolicyKey(policy.getAttributeValue());
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
                        definition.setTimeoutDuration(timeoutSeconds * 1000);
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

                    definition.setTimeoutAction(SynapseConstants.DISCARD);

                    // set timeout duration to 30 seconds, if it is not set explicitly
                    if (definition.getTimeoutDuration() == 0) {
                        definition.setTimeoutDuration(30000);
                    }
                } else if ("fault".equalsIgnoreCase(actionString.trim())) {

                    definition.setTimeoutAction(SynapseConstants.DISCARD_AND_FAULT);

                    // set timeout duration to 30 seconds, if it is not set explicitly
                    if (definition.getTimeoutDuration() == 0) {
                        definition.setTimeoutDuration(30000);
                    }
                } else {
                    handleException("Invalid timeout action, action : "
                            + actionString + " is not supported");
                }
            }
        }

        // set the suspend on fail duration.
        OMElement suspendElement = elem.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE,
                XMLConfigConstants.SUSPEND_DURATION_ON_FAILURE));
        if (suspendElement != null && suspendElement.getText() != null) {
            try {
                long suspendDuration = Long.parseLong(suspendElement.getText().trim());
                definition.setSuspendOnFailDuration(suspendDuration * 1000);
            } catch (NumberFormatException e) {
                handleException("The suspend duration should be specified as a valid number :: "
                        + e.getMessage(), e);
            }
        }
    }

    protected void extractSpecificEndpointProperties(EndpointDefinition definition, OMElement elem){

    }

    /**
     * Returns the EndpointFactory implementation for given endpoint configuration. Throws a
     * SynapseException, if there is no EndpointFactory for given configuration.
     *
     * @param configElement Endpoint configuration.
     * @return EndpointFactory implementation.
     */
    private static EndpointFactory getEndpointFactory(OMElement configElement) {

        if (configElement.getAttribute(new QName("key")) != null) {
            return IndirectEndpointFactory.getInstance();
        }

        OMElement addressElement = configElement.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "address"));
        if (addressElement != null) {
            return AddressEndpointFactory.getInstance();
        }

        OMElement wsdlElement = configElement.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "wsdl"));
        if (wsdlElement != null) {
            return WSDLEndpointFactory.getInstance();
        }

        OMElement defaultElement = configElement.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "default"));
        if (defaultElement != null) {
            return DefaultEndpointFactory.getInstance();
        }

        OMElement lbElement = configElement.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "loadbalance"));
        if (lbElement != null) {
            OMElement sessionElement = configElement.
                    getFirstChildWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, "session"));
            if (sessionElement != null) {
                return SALoadbalanceEndpointFactory.getInstance();
            } else {
                return LoadbalanceEndpointFactory.getInstance();
            }
        }

        OMElement foElement = configElement.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "failover"));
        if (foElement != null) {
            return FailoverEndpointFactory.getInstance();
        }

        handleException("Invalid endpoint configuration.");
        // just to make the compiler happy : never executes
        return null;
    }

    /**
     * Helper method to construct children endpoints
     *
     * @param listEndpointElement OMElement representing  the children endpoints
     * @param parent              Parent endpoint
     * @return List of children endpoints
     */
    protected ArrayList<Endpoint> getEndpoints(OMElement listEndpointElement, Endpoint parent) {

        ArrayList<Endpoint> endpoints = new ArrayList<Endpoint>();
        ArrayList<String> keys = new ArrayList<String>();
        Iterator iter = listEndpointElement.getChildrenWithName(XMLConfigConstants.ENDPOINT_ELT);
        while (iter.hasNext()) {
            OMElement endptElem = (OMElement) iter.next();
            Endpoint endpoint = EndpointFactory.getEndpointFromElement(endptElem, true);
            if (endpoint instanceof IndirectEndpoint) {
                String key = ((IndirectEndpoint) endpoint).getKey();
                if (!keys.contains(key)) {
                    keys.add(key);
                } else {
                    handleException("Same endpoint definition cannot be used with in the siblings");
                }
            }
            endpoint.setParentEndpoint(parent);
            endpoints.add(endpoint);
        }

        return endpoints;
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
