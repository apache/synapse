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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Endpoint;

/**
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
public class EndpointSerializer {

    private static Log log = LogFactory.getLog(EndpointSerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = fac.createOMNamespace(Constants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS = fac.createOMNamespace(Constants.NULL_NAMESPACE, "");

    public static OMElement serializeEndpoint(Endpoint endpt, OMElement parent) {

        OMElement endpoint = fac.createOMElement("endpoint", synNS);
        if (endpt.getAddress() != null) {
            endpoint.addAttribute(fac.createOMAttribute(
                "address", nullNS, endpt.getAddress().toString()));
        } else {
            handleException("Invalid Endpoint. Address is required");
        }

        // TODO handle advanced options

        if (parent != null) {
            parent.addChild(endpoint);
        }
        return endpoint;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
