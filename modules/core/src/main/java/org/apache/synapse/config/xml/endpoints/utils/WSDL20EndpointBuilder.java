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
//import org.apache.woden.WSDLFactory;
//import org.apache.woden.WSDLReader;
//import org.apache.woden.WSDLException;
//import org.apache.woden.types.NCName;
//import org.apache.woden.wsdl20.xml.DescriptionElement;
//import org.apache.woden.wsdl20.Description;
//import org.apache.woden.wsdl20.Service;
//import org.apache.woden.wsdl20.Endpoint;

import javax.xml.namespace.QName;

public class WSDL20EndpointBuilder {

/*
    public EndpointDefinition createEndpointDefinitionFromWSDL(OMElement wsdlElement) {

        EndpointDefinition endpointDefinition = null;

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
            reader.setFeature(WSDLReader.FEATURE_VALIDATION, true);

            DescriptionElement descriptionElement = reader.readWSDL(wsdlURI);
            Description description = descriptionElement.toComponent();
            String tns = descriptionElement.getTargetNamespace().toString();
            Service service = description.getService(new QName(tns, serviceName));
            if (service != null) {
                Endpoint wsdlEndpoint = service.getEndpoint(new NCName(portName));
                String serviceURL = wsdlEndpoint.getAddress().toString();
                endpointDefinition = new EndpointDefinition();
                endpointDefinition.setAddress(serviceURL);
            }

        } catch (WSDLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return endpointDefinition;
    }
*/
}
