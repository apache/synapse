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
import org.apache.synapse.Constants;
import org.apache.synapse.config.EndpointDefinition;

public class AddressEndpointSerializer implements EndpointSerializer {

    private OMFactory fac = null;

    public OMElement serializeEndpoint(Endpoint endpoint) {

        if (!(endpoint instanceof AddressEndpoint)) {
            throw new SynapseException("Invalid endpoint type.");
        }

        fac = OMAbstractFactory.getOMFactory();
        OMElement endpointElement = fac.createOMElement("endpoint", Constants.SYNAPSE_OMNAMESPACE);

        AddressEndpoint addressEndpoint = (AddressEndpoint) endpoint;
        String name = addressEndpoint.getName();
        if (name != null) {
            endpointElement.addAttribute("name", name, null);
        }

        EndpointDefinition epAddress = addressEndpoint.getEndpoint();
        OMElement addressElement = serializeAddress(epAddress);
        endpointElement.addChild(addressElement);

        return endpointElement;
    }

    public OMElement serializeAddress(EndpointDefinition address) {

        OMElement addressElement = fac.createOMElement("address", Constants.SYNAPSE_OMNAMESPACE);
        String uri = address.getAddress();
        if (uri != null) {
            addressElement.addAttribute("uri", uri, null);
        }

        if (address.isAddressingOn()) {
            addressElement.addChild(fac.createOMElement("enableAddressing", Constants.SYNAPSE_OMNAMESPACE));
        }

        return addressElement;
    }
}
