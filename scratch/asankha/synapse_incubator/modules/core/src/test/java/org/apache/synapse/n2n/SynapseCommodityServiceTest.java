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

package org.apache.synapse.n2n;

import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.rpc.receivers.RPCMessageReceiver;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.synapse.Constants;
import org.apache.synapse.utils.Services;

import javax.xml.namespace.QName;


public class SynapseCommodityServiceTest extends TestCase {

    private SimpleHTTPServer synapseServer = null;
    private SimpleHTTPServer businessServer = null;

    protected void setUp() throws java.lang.Exception {
        // Initializing Synapse repository
        System.setProperty(Constants.SYNAPSE_XML,
                           "./../../repository/conf/sample/resources/misc/synapse.xml");
        System.setProperty(org.apache.axis2.Constants.AXIS2_CONF,
                           "./../../repository/conf/axis2.xml");

        ConfigurationContext synapseConfigCtx = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(
                        "./target/test_repos/synapse",
                        "./../../repository/conf/axis2.xml");

        // Initializing Bussiness Endpoint
        ConfigurationContext businessConfigCtx = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(
                        "./target/test_repos/synapse",
                        "./../../repository/conf/axis2.xml");

        AxisService businessService =
                AxisService.createService(Services.class.getName(),
                                          businessConfigCtx.getAxisConfiguration(),
                                          RPCMessageReceiver.class,
                                          "http://business.org", "http://business.org");
        businessConfigCtx.getAxisConfiguration().addService(businessService);

        synapseServer = new SimpleHTTPServer(synapseConfigCtx, 10000);
        businessServer = new SimpleHTTPServer(businessConfigCtx, 10001);

        //starting servers
        synapseServer.start();
        businessServer.start();
    }

    protected void tearDown() throws java.lang.Exception {
        businessServer.stop();
        synapseServer.stop();
    }

    public void testN2N() throws Exception {
        // Creating the Simple Commodity Client
        System.getProperties().remove(org.apache.axis2.Constants.AXIS2_CONF);
        ServiceClient businessClient = new ServiceClient(null, null);
        Options options = new Options();
        options.setTo(
                new EndpointReference("http://127.0.0.1:10000/CommodityQuote"));
        businessClient.setOptions(options);

        OMElement response = businessClient.sendReceive(commodityPayload());

        assertNotNull(response);

        OMElement returnEle = response.getFirstChildWithName(new QName("return"));

        assertNotNull(returnEle);

        assertEquals(returnEle.getText().trim(),"100");


    }

    private static OMElement commodityPayload() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace businessNS =
                fac.createOMNamespace("http://business.org", "ns");
        OMNamespace emptyNS = fac.createOMNamespace("", "");
        OMElement commodityEle = fac.createOMElement("commodity", businessNS);

        OMElement realCommodity = fac.createOMElement("commodity", emptyNS);
        realCommodity.setText("W");

        commodityEle.addChild(realCommodity);

        return commodityEle;
    }
}
