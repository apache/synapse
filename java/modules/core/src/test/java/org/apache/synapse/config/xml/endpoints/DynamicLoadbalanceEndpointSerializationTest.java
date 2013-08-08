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
import org.apache.synapse.endpoints.DynamicLoadbalanceEndpoint;

public class DynamicLoadbalanceEndpointSerializationTest extends AbstractTestCase {

    public void testDLBEndpointScenarioOne() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<dynamicLoadbalance algorithm=\"org.apache.synapse.endpoints.algorithms.RoundRobin\">" +
                "<membershipHandler class=\"org.apache.synapse.core.axis2.Axis2LoadBalanceMembershipHandler\">" +
                "<property name=\"applicationDomain\" value=\"foo\"/></membershipHandler></dynamicLoadbalance>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DynamicLoadbalanceEndpoint endpoint =
                (DynamicLoadbalanceEndpoint) DynamicLoadbalanceEndpointFactory.
                        getEndpointFromElement(inputElement, true, null);

        OMElement serializedOut = DynamicLoadbalanceEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testDLBEndpointScenarioTwo() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<session type=\"soap\"/>" +
                "<dynamicLoadbalance algorithm=\"org.apache.synapse.endpoints.algorithms.RoundRobin\">" +
                "<membershipHandler class=\"org.apache.synapse.core.axis2.Axis2LoadBalanceMembershipHandler\">" +
                "<property name=\"applicationDomain\" value=\"foo\"/></membershipHandler></dynamicLoadbalance>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DynamicLoadbalanceEndpoint endpoint =
                (DynamicLoadbalanceEndpoint) DynamicLoadbalanceEndpointFactory.
                        getEndpointFromElement(inputElement, true, null);

        OMElement serializedOut = DynamicLoadbalanceEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut,inputElement));
    }

    public void testDLBEndpointScenarioThree() throws Exception {
        String inputXML = "<endpoint name=\"ep\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<dynamicLoadbalance failover=\"false\" algorithm=\"org.apache.synapse.endpoints.algorithms.RoundRobin\">" +
                "<membershipHandler class=\"org.apache.synapse.core.axis2.Axis2LoadBalanceMembershipHandler\">" +
                "<property name=\"applicationDomain\" value=\"foo\"/></membershipHandler></dynamicLoadbalance>" +
                "</endpoint>" ;

        OMElement inputElement = createOMElement(inputXML);
        DynamicLoadbalanceEndpoint endpoint =
                (DynamicLoadbalanceEndpoint) DynamicLoadbalanceEndpointFactory.
                        getEndpointFromElement(inputElement, true, null);

        OMElement serializedOut = DynamicLoadbalanceEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue(compare(serializedOut,inputElement));
    }
}
