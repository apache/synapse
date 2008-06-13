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
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.FailoverEndpoint;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;

import java.util.List;

/**
 * Serializes FailoverEndpoint to XML configuration.
 *
 * <endpoint [name="name"]>
 *    <failover>
 *       <endpoint>+
 *    </failover>
 * </endpoint>
 */
public class FailoverEndpointSerializer implements EndpointSerializer {

    private OMFactory fac = null;

    public OMElement serializeEndpoint(Endpoint endpoint) {

        if (!(endpoint instanceof FailoverEndpoint)) {
            throw new SynapseException("Invalid endpoint type.");
        }

        FailoverEndpoint failoverEndpoint = (FailoverEndpoint) endpoint;

        fac = OMAbstractFactory.getOMFactory();
        OMElement endpointElement = fac.createOMElement("endpoint", SynapseConstants.SYNAPSE_OMNAMESPACE);

        OMElement failoverElement = fac.createOMElement("failover", SynapseConstants.SYNAPSE_OMNAMESPACE);
        endpointElement.addChild(failoverElement);

        String name = failoverEndpoint.getName();
        if (name != null) {
            endpointElement.addAttribute("name", name, null);
        }

        List endpoints = failoverEndpoint.getEndpoints();
        for (int i = 0; i < endpoints.size(); i++) {
            Endpoint childEndpoint = (Endpoint) endpoints.get(i);
            EndpointSerializer serializer = EndpointAbstractSerializer.
                    getEndpointSerializer(childEndpoint);
            OMElement aeElement = serializer.serializeEndpoint(childEndpoint);
            failoverElement.addChild(aeElement);
        }

        return endpointElement;
    }
}
