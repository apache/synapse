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
import org.apache.synapse.endpoints.IndirectEndpoint;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

/**
 * Creates an IndirectEndpoint using a XML configuration. Key can be a name of an endpoint defined
 * in the same Synapse configuration or a registry key pointing to an endpoint configuration in the
 * registry (e.g. &lt;endpoint key="registry/endpoint1.xml"/&gt;).
 *
 * &lt;endpoint key="key"/&gt;
 */
public class IndirectEndpointFactory extends EndpointFactory {

    private static IndirectEndpointFactory instance = new IndirectEndpointFactory();

    private IndirectEndpointFactory() {}

    public static IndirectEndpointFactory getInstance() {
        return instance;
    }

    protected Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        IndirectEndpoint indirectEndpoint = new IndirectEndpoint();
        String ref = epConfig.getAttributeValue(new QName("key"));
        String name = epConfig.getAttributeValue(new QName("name"));
        if (name != null) {
            indirectEndpoint.setName(name);
        }
        indirectEndpoint.setKey(ref);
        return indirectEndpoint;
    }
}
