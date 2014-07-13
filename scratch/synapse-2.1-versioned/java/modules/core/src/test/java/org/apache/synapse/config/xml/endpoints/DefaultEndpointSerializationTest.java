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
import org.apache.synapse.config.xml.AbstractTestCase;
import org.apache.synapse.endpoints.DefaultEndpoint;

public class DefaultEndpointSerializationTest extends AbstractTestCase {

    public void testDefaultEndpointScenarioOne() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<default/>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DefaultEndpoint endpoint = (DefaultEndpoint) DefaultEndpointFactory.getEndpointFromElement(
                inputElement,true,null);

        OMElement serializedOut = DefaultEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testDefaultEndpointScenarioTwo() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<default format=\"soap11\"/>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DefaultEndpoint endpoint = (DefaultEndpoint) DefaultEndpointFactory.getEndpointFromElement(
                inputElement,true,null);

        OMElement serializedOut = DefaultEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testDefaultEndpointScenarioThree() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<default><enableSec policy=\"foo\"/></default>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DefaultEndpoint endpoint = (DefaultEndpoint) DefaultEndpointFactory.getEndpointFromElement(
                inputElement,true,null);

        OMElement serializedOut = DefaultEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testDefaultEndpointScenarioFour() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<default><timeout><duration>10000</duration></timeout></default>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DefaultEndpoint endpoint = (DefaultEndpoint) DefaultEndpointFactory.getEndpointFromElement(
                inputElement,true,null);

        OMElement serializedOut = DefaultEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testDefaultEndpointScenarioFive() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<default><timeout><duration>10000</duration></timeout>" +
                "<suspendOnFailure><initialDuration>60000</initialDuration>" +
                "<progressionFactor>1.0</progressionFactor></suspendOnFailure></default>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DefaultEndpoint endpoint = (DefaultEndpoint) DefaultEndpointFactory.getEndpointFromElement(
                inputElement,true,null);

        OMElement serializedOut = DefaultEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut, inputElement));
    }
}
