/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.config.XMLToObjectMapper;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Creates an Endpoint instance using the XML fragment specification
 *
 * <endpoint name="string" address="url">
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
 * </endpoint>
 */
public class EndpointFactory implements XMLToObjectMapper {

    private static Log log = LogFactory.getLog(EndpointFactory.class);

    private static final EndpointFactory instance = new EndpointFactory();

    private EndpointFactory() {}

    public static Endpoint createEndpoint(OMElement elem) {

        OMAttribute name = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "name"));
        if (name == null) {
            handleException("The 'name' attribute is required for a named endpoint definition");
        } else {
            Endpoint endpoint = new Endpoint();
            endpoint.setName(name.getAttributeValue());

            OMAttribute address = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "address"));
            if (address != null) {
                try {
                    endpoint.setAddress(new URL(address.getAttributeValue()));
                } catch (MalformedURLException e) {
                    handleException("Invalid URL specified for 'address' : " +
                        address.getAttributeValue(), e);
                }
            } else {
                // right now an address is *required*
                handleException("The 'address' attribute is required for an endpoint");
            }

            OMElement wsAddr = elem.getFirstChildWithName(
                new QName(Constants.NULL_NAMESPACE, "enableAddressing"));
            if (wsAddr != null) {
                endpoint.setAddressingOn(true);
            }
            OMElement wsSec = elem.getFirstChildWithName(
                new QName(Constants.NULL_NAMESPACE, "enableSec"));
            if (wsSec != null) {
                endpoint.setSecurityOn(true);
            }
            OMElement wsRm = elem.getFirstChildWithName(
                new QName(Constants.NULL_NAMESPACE, "enableRM"));
            if (wsRm != null) {
                endpoint.setReliableMessagingOn(true);
            }

            // if a Rampart OutflowSecurity parameter is specified, digest it
            endpoint.setOutflowSecurity(
                RampartSecurityBuilder.getSecurityParameter(elem, Constants.OUTFLOW_SECURITY));

            // if a Rampart InflowSecurity parameter is specified, digest it
            endpoint.setInflowSecurity(
                RampartSecurityBuilder.getSecurityParameter(elem, Constants.INFLOW_SECURITY));

            // if WS-RM is enabled, set it as requested
            endpoint.setReliableMessagingOn(OutflowRMPolicyBuilder.isRMEnabled(elem));
            endpoint.setWsRMPolicy(OutflowRMPolicyBuilder.getRMPolicy(elem));

            return endpoint;
        }
        return null;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createEndpoint((OMElement) om);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    public static EndpointFactory getInstance() {
        return instance;
    }
}
