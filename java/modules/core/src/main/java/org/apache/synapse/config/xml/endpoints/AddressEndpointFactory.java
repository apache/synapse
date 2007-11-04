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

import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

/**
 * Creates AddressEndpoint using a XML configuration.
 *
 * <endpoint [name="name"] [trace="enable|disable"]>
 *   <suspendDurationOnFailue>suspend-duration</suspendDurationOnFailue>
 *   <address uri="url" [format="soap11|soap12|pox"] [optimize="mtom|swa"]>
 *      .. extensibility ..
 *
 *      <timeout>
 *          <duration>duration in milliseconds</duration>
 *          <action>discard | fault</action>
 *      </timeout>?
 *
 *      <enableRM [policy="key"]/>?
 *      <enableSec [policy="key"]/>?
 *      <enableAddressing/>?
 *      <suspendDurationOnFailure>suspend-duration</suspendDurationOnFailure>?
 *   </address>
 * </endpoint>
 */
public class AddressEndpointFactory implements EndpointFactory {

    private static Log log = LogFactory.getLog(AddressEndpointFactory.class);

    private static AddressEndpointFactory instance = new AddressEndpointFactory();

    /**
     * To decide to whether statistics should have collected or not
     */
    private int statisticsState = SynapseConstants.STATISTICS_UNSET;
    /**
     * The variable that indicate tracing on or off for the current mediator
     */
    protected int traceState = SynapseConstants.TRACING_UNSET;

    private AddressEndpointFactory() {}

    public static AddressEndpointFactory getInstance() {
        return instance;
    }

    public Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        AddressEndpoint addressEndpoint = new AddressEndpoint();

        if (!anonymousEndpoint) {
            OMAttribute name = epConfig.getAttribute(new QName(
                    org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "name"));

            if (name != null) {
                addressEndpoint.setName(name.getAttributeValue());
            }
        }

        OMElement addressElement = epConfig.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "address"));

        if (addressElement != null) {
            EndpointDefinition endpoint = createEndpointDefinition(addressElement);
            addressEndpoint.setEndpoint(endpoint);

            // set the suspend on fail duration.
            OMElement suspendElement = addressElement.getFirstChildWithName(new QName(
                    SynapseConstants.SYNAPSE_NAMESPACE,
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_DURATION_ON_FAILURE));

            if (suspendElement != null) {
                String suspend = suspendElement.getText();

                try {
                    if (suspend != null) {
                        long suspendDuration = Long.parseLong(suspend.trim());
                        addressEndpoint.setSuspendOnFailDuration(suspendDuration * 1000);
                    }

                } catch (NumberFormatException e) {
                    handleException("The suspend duration should be specified as a valid number :: "
                        + e.getMessage(), e);
                }
            }
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

    /**
     * Creates an EndpointDefinition instance using the XML fragment specification. Configuration for
     * EndpointDefinition always resides inside a configuration of an AddressEndpoint. This factory
     * extracts the details related to the EPR provided for address endpoint.
     *
     * @param elem XML configuration element
     * @return EndpointDefinition object containing the endpoint details.
     */
    public EndpointDefinition createEndpointDefinition(OMElement elem) {

        OMAttribute address = elem.getAttribute(new QName(
                org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "uri"));
        OMAttribute format = elem.getAttribute(new QName(
                org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "format"));
        OMAttribute optimize = elem.getAttribute(new QName(
                org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "optimize"));

        EndpointDefinition endpoint = new EndpointDefinition();
        OMAttribute statistics = elem.getAttribute(
                new QName(org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE,
                        org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ATTRIB_NAME));
        if (statistics != null) {
            String statisticsValue = statistics.getAttributeValue();
            if (statisticsValue != null) {
                if (org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ENABLE.equals(
                        statisticsValue)) {
                    endpoint.setStatisticsState(org.apache.synapse.SynapseConstants.STATISTICS_ON);
                } else if (org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_DISABLE.equals(
                        statisticsValue)) {
                    endpoint.setStatisticsState(org.apache.synapse.SynapseConstants.STATISTICS_OFF);
                }
            }
        }
        if (address != null) {
            endpoint.setAddress(address.getAttributeValue());
//        } else {
//            handleException("One of the 'address' or 'ref' attributes are required in an "
//                    + "anonymous endpoint");
        }
        if (format != null)
        {
            String forceValue = format.getAttributeValue().trim().toLowerCase();
            if (forceValue.equals(SynapseConstants.FORMAT_POX)) {
                endpoint.setForcePOX(true);
                endpoint.setFormat(SynapseConstants.FORMAT_POX);
                
            } else if (forceValue.equals(SynapseConstants.FORMAT_SOAP11)) {
            	endpoint.setForceSOAP11(true);
            	endpoint.setFormat(SynapseConstants.FORMAT_SOAP11);
                
            } else if (forceValue.equals(SynapseConstants.FORMAT_SOAP12)) {
            	endpoint.setForceSOAP12(true);
                endpoint.setFormat(SynapseConstants.FORMAT_SOAP12);
                
            } else {
                handleException("unknown value -\""+forceValue+"\". Attribute 'format' accepts only 'pox','soap11','soap12'");
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
                org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "enableAddressing"));
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
                org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "enableSec"));
        if (wsSec != null) {
            endpoint.setSecurityOn(true);
            OMAttribute policy = wsSec.getAttribute(new QName(
                    org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                endpoint.setWsSecPolicyKey(policy.getAttributeValue());
            }
        }
        OMElement wsRm = elem.getFirstChildWithName(new QName(
                org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "enableRM"));
        if (wsRm != null) {
            endpoint.setReliableMessagingOn(true);
            OMAttribute policy = wsRm.getAttribute(new QName(
                    org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "policy"));
            if (policy != null) {
                endpoint.setWsRMPolicyKey(policy.getAttributeValue());
            }
        }
        // set the timeout configuration
        OMElement timeout = elem.getFirstChildWithName(new QName(
                org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "timeout"));
        if (timeout != null) {
            OMElement duration = timeout.getFirstChildWithName(new QName(
                    org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "duration"));
            if (duration != null) {
                String d = duration.getText();
                if (d != null) {
                    try {
                        long timeoutSeconds = new Long(d.trim()).longValue();
                        endpoint.setTimeoutDuration(timeoutSeconds * 1000);

                    } catch (NumberFormatException e) {
                        handleException(
                            "The timeout seconds should be specified as a valid number :: "
                            + e.getMessage(), e);
                    }
                }
            }

            OMElement action = timeout.getFirstChildWithName(new QName(
                    org.apache.synapse.config.xml.XMLConfigConstants.SYNAPSE_NAMESPACE, "action"));
            if (action != null) {
                String a = action.getText();
                if (a != null) {
                    if ((a.trim()).equalsIgnoreCase("discard")) {
                        endpoint.setTimeoutAction(SynapseConstants.DISCARD);

                        // set timeout duration to 30 seconds, if it is not set explicitly
                        if (endpoint.getTimeoutDuration() == 0) {
                            endpoint.setTimeoutDuration(30000);
                        }
                    } else if ((a.trim()).equalsIgnoreCase("fault")) {
                        endpoint.setTimeoutAction(SynapseConstants.DISCARD_AND_FAULT);

                        // set timeout duration to 30 seconds, if it is not set explicitly
                        if (endpoint.getTimeoutDuration() == 0) {
                            endpoint.setTimeoutDuration(30000);
                        }
                    }
                }
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
