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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.SALoadbalanceEndpoint;
import org.apache.synapse.endpoints.dispatch.Dispatcher;
import org.apache.synapse.endpoints.dispatch.SoapSessionDispatcher;
import org.apache.synapse.endpoints.dispatch.SimpleClientSessionDispatcher;
import org.apache.synapse.endpoints.dispatch.HttpSessionDispatcher;
import org.apache.synapse.endpoints.algorithms.LoadbalanceAlgorithm;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.endpoints.utils.LoadbalanceAlgorithmFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNode;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Creates SALoadbalanceEndpoint from a XML configuration.
 *
 * <endpoint [name="name"]>
 *    <session type="soap | ..other session types.." />
 *    <loadbalance policy="policy">
 *       <endpoint>+
 *    </loadbalance>
 * </endpoint>
 */
public class SALoadbalanceEndpointFactory implements EndpointFactory {

    private static Log log = LogFactory.getLog(LoadbalanceEndpointFactory.class);

    private static SALoadbalanceEndpointFactory instance = new SALoadbalanceEndpointFactory();

    private SALoadbalanceEndpointFactory() {}

    public static SALoadbalanceEndpointFactory getInstance() {
        return instance;
    }

    public Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        // create the endpoint, manager and the algorithms
        SALoadbalanceEndpoint loadbalanceEndpoint = new SALoadbalanceEndpoint();

        // get the session for this endpoint
        OMElement sessionElement = epConfig.
                getFirstChildWithName(new QName(SynapseConstants.SYNAPSE_NAMESPACE, "session"));
        if (sessionElement != null) {

            String type = sessionElement.getAttributeValue(new QName("type"));

            if (type.equalsIgnoreCase("soap")) {
                Dispatcher soapDispatcher = new SoapSessionDispatcher();
                loadbalanceEndpoint.setDispatcher(soapDispatcher);

            } else if (type.equalsIgnoreCase("http")) {
                Dispatcher httpDispatcher = new HttpSessionDispatcher();
                loadbalanceEndpoint.setDispatcher(httpDispatcher);

            } else if (type.equalsIgnoreCase("simpleClientSession")) {
                Dispatcher csDispatcher = new SimpleClientSessionDispatcher();
                loadbalanceEndpoint.setDispatcher(csDispatcher);
            }
        } else {
            handleException("Session affinity endpoints should have a session element in the configuration.");
        }

        // set endpoint name
        OMAttribute name = epConfig.getAttribute(new QName(
                org.apache.synapse.config.xml.XMLConfigConstants.NULL_NAMESPACE, "name"));

        if (name != null) {
            loadbalanceEndpoint.setName(name.getAttributeValue());
        }

        OMElement loadbalanceElement =  null;
        loadbalanceElement = epConfig.getFirstChildWithName
                (new QName(SynapseConstants.SYNAPSE_NAMESPACE, "loadbalance"));

        if(loadbalanceElement != null) {

            // set endpoints
            ArrayList endpoints = getEndpoints(loadbalanceElement, loadbalanceEndpoint);
            loadbalanceEndpoint.setEndpoints(endpoints);

            // set load balance algorithm
            LoadbalanceAlgorithm algorithm = LoadbalanceAlgorithmFactory.
                    createLoadbalanceAlgorithm(loadbalanceElement, endpoints);
            loadbalanceEndpoint.setAlgorithm(algorithm);

            // set abandon time
            //long abandonTime = 0;
            //OMAttribute atAttribute = loadbalanceElement.getAttribute
            //        (new QName(null, org.apache.synapse.config.xml.Constants.RETRY_AFTER_FAILURE_TIME));
            //if(atAttribute != null) {
            //    String at = atAttribute.getAttributeValue();
            //    abandonTime = Long.parseLong(at);
            //    loadbalanceEndpoint.setAbandonTime(abandonTime);
            //}

            //long retryInterval = 30000;
            //OMAttribute riAttribute = loadbalanceElement.getAttribute
            //        (new QName(null, Constants.RETRY_INTERVAL));
            //
            //if(riAttribute != null) {
            //    String ri = riAttribute.getAttributeValue();
            //    retryInterval = Long.parseLong(ri);
            //}

            //int maximumRetries = 0;
            //OMAttribute mrAttribute = loadbalanceElement.getAttribute
            //        (new QName(null, Constants.MAXIMUM_RETRIES));
            //
            //if(mrAttribute != null) {
            //    String mr = mrAttribute.getAttributeValue();
            //    maximumRetries = Integer.parseInt(mr);
            //}

            return loadbalanceEndpoint;
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createEndpoint((OMElement) om, false);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    private ArrayList getEndpoints(OMElement loadbalanceElement, Endpoint parent) {

        ArrayList endpoints = new ArrayList();
        Iterator iter = loadbalanceElement.getChildrenWithName
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
