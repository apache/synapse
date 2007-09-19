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
import org.apache.synapse.endpoints.FailoverEndpoint;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Creates FailoverEndpoint using a XML configuration.
 *
 * <endpoint [name="name"]>
 *    <failover>
 *       <endpoint>+
 *    </failover>
 * </endpoint>
 */
public class FailoverEndpointFactory implements EndpointFactory {

    private static Log log = LogFactory.getLog(FailoverEndpointFactory.class);

    private static FailoverEndpointFactory instance = new FailoverEndpointFactory();

    private FailoverEndpointFactory() {}

    public static FailoverEndpointFactory getInstance() {
        return instance;
    }

    public Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        OMElement failoverElement = epConfig.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "failover"));
        if (failoverElement != null) {

            FailoverEndpoint failoverEndpoint = new FailoverEndpoint();

            // set endpoint name
            String name = epConfig.getAttributeValue(new QName("name"));
            if (name != null) {
                failoverEndpoint.setName(name);
            }

            // set endpoints
            ArrayList endpoints = getEndpoints(failoverElement, failoverEndpoint);
            failoverEndpoint.setEndpoints(endpoints);

            return failoverEndpoint;
        }

        return null;
    }

    public Object getObjectFromOMNode(OMNode om) {
         if (om instanceof OMElement) {
            return createEndpoint((OMElement) om, false);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    private ArrayList getEndpoints(OMElement failoverElement, Endpoint parent) {

        ArrayList endpoints = new ArrayList();
        Iterator iter = failoverElement.getChildrenWithName
                (org.apache.synapse.config.xml.XMLConfigConstants.ENDPOINT_ELT);
        while (iter.hasNext()) {

            OMElement endptElem = (OMElement) iter.next();

            EndpointFactory epFac = EndpointAbstractFactory.getEndpointFactroy(endptElem);
            Endpoint endpoint = epFac.createEndpoint(endptElem, true);
            endpoint.setParentEndpoint(parent);
            endpoints.add(endpoint);
        }

        return endpoints;
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
