/**
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

package org.apache.synapse.transport.nhttp.endpoints.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.nhttp.endpoints.EndpointsConfiguration;
import org.apache.synapse.transport.nhttp.endpoints.Endpoint;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;

public class EndpointsConfigurationFactory {
    private static final Log log = LogFactory.getLog(EndpointsConfigurationFactory.class);

    public EndpointsConfiguration create(OMElement element) throws AxisFault {
        Iterator iterator = element.getChildrenWithName(new QName(EndpointsConfiguration.ENDPOINT));
        EndpointsConfiguration configuration = new EndpointsConfiguration();
        EndpointFactory fac = new EndpointFactory();
        while (iterator.hasNext()) {
            OMElement endpoint = (OMElement) iterator.next();

            Endpoint epr = fac.create(endpoint);
            configuration.addEndpoint(epr);
        }

        return configuration;
    }

    public EndpointsConfiguration create(String fileName) throws AxisFault {
        File synapseConfigLocation = new File(fileName);

        FileInputStream is = null;
        try {
            is = new FileInputStream(synapseConfigLocation);
        } catch (FileNotFoundException e) {
            handleException("Error reading file: " + fileName + "for creating the " +
                    EndpointsConfiguration.ENDPOINT + " configurations");
        }
        OMElement element = OMXMLBuilderFactory.createOMBuilder(is).getDocumentElement();
        element.build();

        Iterator iterator = element.getChildrenWithName(new QName(EndpointsConfiguration.ENDPOINT));
        EndpointsConfiguration configuration = new EndpointsConfiguration();
        EndpointFactory fac = new EndpointFactory();
        while (iterator.hasNext()) {
            OMElement endpoint = (OMElement) iterator.next();

            Endpoint epr = fac.create(endpoint);
            configuration.addEndpoint(epr);
        }

        return configuration;
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }
}
