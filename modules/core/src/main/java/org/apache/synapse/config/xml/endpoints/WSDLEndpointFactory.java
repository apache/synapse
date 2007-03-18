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
import org.apache.synapse.endpoints.WSDLEndpoint;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.config.xml.endpoints.utils.WSDL11EndpointBuilder;
import org.apache.synapse.config.xml.endpoints.utils.WSDL20EndpointBuilder;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.wsdl.WSDLConstants;

import javax.xml.namespace.QName;
import java.net.URL;

/**
 * Creates an WSDL based endpoint from a XML configuration.
 *
 * <endpoint [name="name"]>
 *    <wsdl uri="wsdl uri" service="service name" port="port name">
 *       .. extensibility ..
 *    </wsdl>
 * </endpoint>
 */
public class WSDLEndpointFactory implements EndpointFactory {

    private static Log log = LogFactory.getLog(WSDLEndpointFactory.class);

    private static WSDLEndpointFactory instance = new WSDLEndpointFactory();

    private WSDLEndpointFactory() {}

    public static WSDLEndpointFactory getInstance() {
        return instance;
    }

    public Object getObjectFromOMNode(OMNode om) {
        if (om instanceof OMElement) {
            return createEndpoint((OMElement) om, false);
        } else {
            handleException("Invalid XML configuration for an Endpoint. OMElement expected");
        }
        return null;
    }

    public Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint) {

        WSDLEndpoint wsdlEndpoint = new WSDLEndpoint();

        if (!anonymousEndpoint) {
            OMAttribute name = epConfig.getAttribute(new QName(
                    org.apache.synapse.config.xml.Constants.NULL_NAMESPACE, "name"));

            if (name != null) {
                wsdlEndpoint.setName(name.getAttributeValue());
            }
        }

        OMElement wsdlElement = epConfig.getFirstChildWithName
                (new QName(Constants.SYNAPSE_NAMESPACE, "wsdl"));

        if (wsdlElement != null) {

            String wsdlURI = wsdlElement.getAttributeValue(new QName("uri"));
            try {
                EndpointDefinition endpoint = new EndpointDefinition();

                URL wsdlURL = new URL(wsdlURI);
                StAXOMBuilder OMBuilder = new StAXOMBuilder(wsdlURL.openConnection().getInputStream());
                OMElement docElement = OMBuilder.getDocumentElement();
                String ns = docElement.getNamespace().getNamespaceURI();

                if (org.apache.axis2.namespace.Constants.NS_URI_WSDL11.equals(ns)) {
                    endpoint = new WSDL11EndpointBuilder().
                            createEndpointDefinitionFromWSDL(wsdlElement);
                } else if (WSDLConstants.WSDL20_2006Constants.DEFAULT_NAMESPACE_URI.equals(ns)) {
                    //endpoint = new WSDL20EndpointBuilder().
                    //        createEndpointDefinitionFromWSDL(wsdlElement);
                    handleException("WSDL 2.0 Endpoints are currently not supported");
                }


                wsdlEndpoint.setEndpointDefinition(endpoint);
            } catch (Exception e1) {
                handleException("Unable to create endpoint from the given WSDL.", e1);
            }
        }

        return wsdlEndpoint;
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
