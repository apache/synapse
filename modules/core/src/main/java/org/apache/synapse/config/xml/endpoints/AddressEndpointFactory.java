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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

import javax.xml.namespace.QName;

/**
 * Creates {@link AddressEndpoint} using a XML configuration.
 * <p>
 * Configuration syntax:
 * <pre>
 * &lt;endpoint [name="<em>name</em>"] [trace="enable|disable"]>
 *   &lt;address uri="<em>url</em>" [format="soap11|soap12|pox|get"] [optimize="mtom|swa"]
 *      [encoding="<em>charset encoding</em>"] [statistics="enable|disable"]>
 *     .. extensibility ..
 *
 *     &lt;enableRM [policy="<em>key</em>"]/>?
 *     &lt;enableSec [policy="<em>key</em>"]/>?
 *     &lt;enableAddressing [version="final|submission"] [separateListener="true|false"]/>?
 *
 *     &lt;timeout>
 *       &lt;duration><em>timeout duration in seconds</em>&lt;/duration>
 *       &lt;action>discard|fault&lt;/action>
 *     &lt;/timeout>?
 *
 *     &lt;suspendDurationOnFailure&gt;
 *              <em>suspend duration in seconds</em>&lt;/suspendDurationOnFailure&gt;?
 *   &lt;/address>
 * &lt;/endpoint>
 * </pre>
 */
public class AddressEndpointFactory extends EndpointFactory {

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

        OMAttribute name
                = epConfig.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));

        if (name != null) {
            addressEndpoint.setName(name.getAttributeValue());
        }

        OMElement addressElement = epConfig.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "address"));

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
     * Creates an EndpointDefinition instance using the XML fragment specification. Configuration
     * for EndpointDefinition always resides inside a configuration of an AddressEndpoint. This
     * factory extracts the details related to the EPR provided for address endpoint.
     *
     * @param elem XML configuration element
     * @return EndpointDefinition object containing the endpoint details.
     */
    public EndpointDefinition createEndpointDefinition(OMElement elem) {

        OMAttribute address = elem.getAttribute(new QName("uri"));

        EndpointDefinition endpointDefinition = new EndpointDefinition();
        OMAttribute statistics = elem.getAttribute(
                new QName(org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE,
                        org.apache.synapse.config.xml.XMLConfigConstants.STATISTICS_ATTRIB_NAME));

        if (statistics != null) {
            String statisticsValue = statistics.getAttributeValue();
            if (statisticsValue != null) {

                if (org.apache.synapse.config.xml.XMLConfigConstants.
                        STATISTICS_ENABLE.equals(statisticsValue)) {
                    endpointDefinition.setStatisticsState(org.apache.synapse.SynapseConstants.STATISTICS_ON);
                } else if (org.apache.synapse.config.xml.XMLConfigConstants.
                        STATISTICS_DISABLE.equals(statisticsValue)) {
                    endpointDefinition.setStatisticsState(org.apache.synapse.SynapseConstants.STATISTICS_OFF);
                }
            }
        }

        if (address != null) {
            endpointDefinition.setAddress(address.getAttributeValue());
        }

        extractQOSInformation(endpointDefinition, elem);

        return endpointDefinition;
    }
}
