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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.FailoverEndpoint;
import org.apache.synapse.endpoints.IndirectEndpoint;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Creates {@link FailoverEndpoint} using a XML configuration.
 *
 * &lt;endpoint [name="name"]&gt;
 *    &lt;failover&gt;
 *       &lt;endpoint&gt;+
 *    &lt;/failover&gt;
 * &lt;/endpoint&gt;
 */
public class FailoverEndpointFactory extends EndpointFactory {

    private static FailoverEndpointFactory instance = new FailoverEndpointFactory();

    private FailoverEndpointFactory() {}

    public static FailoverEndpointFactory getInstance() {
        return instance;
    }

    protected Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        OMElement failoverElement = epConfig.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "failover"));
        if (failoverElement != null) {

            FailoverEndpoint failoverEndpoint = new FailoverEndpoint();
            // set endpoint name
            String name = epConfig.getAttributeValue(new QName("name"));
            if (name != null) {
                failoverEndpoint.setName(name);
            }
            // set endpoints and return
            failoverEndpoint.setEndpoints(getEndpoints(failoverElement, failoverEndpoint));
            return failoverEndpoint;
        }
        return null;
    }

}
