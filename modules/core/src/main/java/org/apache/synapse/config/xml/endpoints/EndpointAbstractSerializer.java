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

import org.apache.synapse.endpoints.*;
import org.apache.synapse.SynapseException;

/**
 * Abstract serialier for endpoint serializers. Use this class to obtain the EndpointSerializer
 * implementation for particular endpoint type.
 */
public class EndpointAbstractSerializer {

    /**
     * Returns the EndpointSerializer implementation for the given endpoint. Throws a SynapseException,
     * if there is no serializer for the given endpoint type.
     *
     * @param endpoint Endpoint implementaion.
     * @return EndpointSerializer implementation.
     */
    public static EndpointSerializer getEndpointSerializer(Endpoint endpoint) {

        if (endpoint instanceof AddressEndpoint) {
            return new AddressEndpointSerializer();
        } else if (endpoint instanceof WSDLEndpoint) {
            return new WSDLEndpointSerializer();
        } else if (endpoint instanceof IndirectEndpoint) {
            return new IndirectEndpointSerializer();
        } else if (endpoint instanceof LoadbalanceEndpoint) {
            return new LoadbalanceEndpointSerializer();
        } else if (endpoint instanceof SALoadbalanceEndpoint) {
            return new SALoadbalanceEndpointSerializer();
        } else if (endpoint instanceof FailoverEndpoint) {
            return new FailoverEndpointSerializer();
        }

        throw new SynapseException("Serializer for endpoint " +
                endpoint.getClass().toString() + " is not defined.");
    }
}
