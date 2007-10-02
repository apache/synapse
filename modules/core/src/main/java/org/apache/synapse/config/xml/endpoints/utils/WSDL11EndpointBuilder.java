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

package org.apache.synapse.config.xml.endpoints.utils;

import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.WSDLException;
import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Builds the EndpointDefinition containing the details for an epr using a WSDL 1.1 document.
 */
public class WSDL11EndpointBuilder {

    private static Log log = LogFactory.getLog(WSDL11EndpointBuilder.class);

    /**
     * Creates an EndpointDefinition for WSDL endpoint from an inline WSDL supplied in the WSDL
     * endpoint configuration.
     *
     * @param wsdl OMElement representing the inline WSDL
     * @param service Service of the endpoint
     * @param port Port of the endpoint
     *
     * @return EndpointDefinition containing the information retrieved from the WSDL
     */
    public EndpointDefinition createEndpointDefinitionFromWSDL
            (OMElement wsdl, String service, String port) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            wsdl.serialize(baos);
            InputStream in = new ByteArrayInputStream(baos.toByteArray());
            InputSource inputSource = new InputSource(in);
            WSDLFactory fac = WSDLFactory.newInstance();
            WSDLReader reader = fac.newWSDLReader();
            Definition definition = reader.readWSDL(null, inputSource);

            return createEndpointDefinitionFromWSDL(definition, service, port);

        } catch (XMLStreamException e) {
            handleException("Error retrieving the WSDL definition from the inline WSDL.");
        } catch (WSDLException e) {
            handleException("Error retrieving the WSDL definition from the inline WSDL.");
        }

        return null;
    }

    /**
     * Creates an EndpointDefinition for WSDL endpoint from a WSDL document residing in the given URI.
     *
     * @param wsdlURI URI of the WSDL document
     * @param service Service of the endpoint
     * @param port Port of the endpoint
     *
     * @return EndpointDefinition containing the information retrieved from the WSDL
     */
    public EndpointDefinition createEndpointDefinitionFromWSDL
            (String wsdlURI, String service, String port) {

        try {
            WSDLFactory fac = WSDLFactory.newInstance();
            WSDLReader reader = fac.newWSDLReader();
            Definition definition = reader.readWSDL(wsdlURI);

            return createEndpointDefinitionFromWSDL(definition, service, port);

        } catch (WSDLException e) {
            handleException("Error retrieving the WSDL definition from the WSDL URI.");
        }

        return null;
    }

    private EndpointDefinition createEndpointDefinitionFromWSDL
            (Definition definition, String serviceName, String portName) {

        if (definition == null) {
            handleException("WSDL is not specified.");
        }

        if (serviceName == null) {
            handleException("Service of the WSDL document is not specified.");
        }

        if (portName == null) {
            handleException("Port of the WSDL document is not specified.");
        }


        String serviceURL = null;
        // get soap version from wsdl port and update endpoint definition below
        // so that correct soap version is used when endpoint is called
        String format = null; 
        String tns = definition.getTargetNamespace();
        Service service = definition.getService(new QName(tns, serviceName));
        if (service != null) {
            Port port = service.getPort(portName);
            if (port != null) {
                List ext = port.getExtensibilityElements();
                for (int i = 0; i < ext.size(); i++) {
                    Object o = ext.get(i);
                    if (o instanceof SOAPAddress) {
                        SOAPAddress address = (SOAPAddress) o;
                        serviceURL = address.getLocationURI();
                        format = SynapseConstants.FORMAT_SOAP11;
                        break;
                    } else if (o instanceof SOAP12Address) {
                        SOAP12Address address = (SOAP12Address) o;
                        serviceURL = address.getLocationURI();
                        format = SynapseConstants.FORMAT_SOAP12;
                        break;
                    }
                }
            }
        }

        if (serviceURL != null) {
            EndpointDefinition endpointDefinition = new EndpointDefinition();
            endpointDefinition.setAddress(serviceURL);
            endpointDefinition.setFormat(format);
            
            // todo: determine this using wsdl and policy                                    

            return endpointDefinition;

        } else {
            handleException("Couldn't retrieve endpoint information from the WSDL.");
        }

        return null;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
