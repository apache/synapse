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
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;

import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.WSDLException;
import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * Builds the EndpointDefinition containing the details for an epr using a WSDL 1.1 document.
 */
public class WSDL11EndpointBuilder {

    public EndpointDefinition createEndpointDefinitionFromWSDL(OMElement wsdlElement) {

        EndpointDefinition endpointDefinition = null;
        String serviceURL = null;

        String wsdlURI = wsdlElement.getAttributeValue(new QName("uri"));
        String serviceName = wsdlElement.getAttributeValue(new QName("service"));
        String portName = wsdlElement.getAttributeValue(new QName("port"));

        if (wsdlURI == null) {
            throw new SynapseException("WSDL is not specified.");
        }

        if (serviceName == null) {
            throw new SynapseException("Service is not specified.");
        }

        if (portName == null) {
            throw new SynapseException("Port is not specified.");
        }

        try {
            WSDLFactory fac = WSDLFactory.newInstance();
            WSDLReader reader = fac.newWSDLReader();
            Definition definition = reader.readWSDL(wsdlURI);
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
                            break;
                        }
                    }
                }
            }

        } catch (WSDLException e) {
            throw new SynapseException("Unable create endpoint definition from WSDL.");
        }

        if (serviceURL != null) {
            endpointDefinition = new EndpointDefinition();
            endpointDefinition.setAddress(serviceURL);

            // todo: determine this using wsdl and policy
            endpointDefinition.setAddressingOn(true);
        }

        return endpointDefinition;
    }
}
