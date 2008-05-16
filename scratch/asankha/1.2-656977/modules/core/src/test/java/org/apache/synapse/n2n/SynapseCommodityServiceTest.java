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
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.transport.TransportListener;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.ServerManager;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.utils.Services;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;


public class SynapseCommodityServiceTest extends TestCase {

    protected void setUp() throws java.lang.Exception {
        ServerManager.getInstance().setSynapseHome(".");
        // Initializing Synapse repository
        ServerManager.getInstance().setSynapseXMLPath(
                           "./../../repository/conf/sample/resources/misc/synapse.xml");
        ServerManager.getInstance().setAxis2Xml(
                           "./../../repository/conf/axis2.xml");

        findAndReplace(
            new File("./../../repository/conf/axis2.xml"),
            new File("./target/test_repos/axis2.xml"),
            "lib/", "src/main/resources/");

        ConfigurationContext synapseConfigCtx = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(
                        "./target/test_repos/synapse",
                        "./target/test_repos/axis2.xml");

        // Initializing Bussiness Endpoint
        ConfigurationContext businessConfigCtx = ConfigurationContextFactory
                .createConfigurationContextFromFileSystem(
                        "./target/test_repos/synapse",
                        "./target/test_repos/axis2.xml");

        HashMap messageReciverMap = new HashMap();
        Class inOnlyMessageReceiver = org.apache.axis2.util.Loader.loadClass(
                "org.apache.axis2.rpc.receivers.RPCInOnlyMessageReceiver");
        org.apache.axis2.engine.MessageReceiver messageReceiver =
                (org.apache.axis2.engine.MessageReceiver) inOnlyMessageReceiver.newInstance();
        messageReciverMap.put(
                org.apache.axis2.description.WSDL2Constants.MEP_URI_IN_ONLY,
                messageReceiver);
        Class inoutMessageReceiver = org.apache.axis2.util.Loader.loadClass(
                "org.apache.axis2.rpc.receivers.RPCMessageReceiver");
        MessageReceiver inOutmessageReceiver =
                (MessageReceiver) inoutMessageReceiver.newInstance();
        messageReciverMap.put(
                org.apache.axis2.description.WSDL2Constants.MEP_URI_IN_OUT,
                inOutmessageReceiver);
        messageReciverMap.put(org.apache.axis2.description.WSDL2Constants.MEP_URI_ROBUST_IN_ONLY,
            inOutmessageReceiver);

        AxisService businessService =
                AxisService.createService(Services.class.getName(),
                                          businessConfigCtx.getAxisConfiguration(),
                                          messageReciverMap,
                                          "http://business.org", "http://business.org",
                                          Services.class.getClassLoader());
        businessConfigCtx.getAxisConfiguration().addService(businessService);

        TransportInDescription synTrsIn = (TransportInDescription)
            synapseConfigCtx.getAxisConfiguration().getTransportsIn().get("http");
        synTrsIn.getParameter("port").setValue("10100");
        synTrsIn = (TransportInDescription)
            synapseConfigCtx.getAxisConfiguration().getTransportsIn().get("https");
        synTrsIn.getParameter("port").setValue("12100");
        startServer(synapseConfigCtx);

        TransportInDescription busTrsIn = (TransportInDescription)
            businessConfigCtx.getAxisConfiguration().getTransportsIn().get("http");
        busTrsIn.getParameter("port").setValue("10101");
        busTrsIn = (TransportInDescription)
            businessConfigCtx.getAxisConfiguration().getTransportsIn().get("https");
        busTrsIn.getParameter("port").setValue("12101");
        startServer(businessConfigCtx);
    }

    protected void tearDown() throws java.lang.Exception {
    }

    private void startServer(ConfigurationContext configctx) throws AxisFault {
        ListenerManager listenerManager = configctx.getListenerManager();
        if (listenerManager == null) {
            listenerManager = new ListenerManager();
            listenerManager.init(configctx);
        }

        Iterator iter = configctx.getAxisConfiguration().
            getTransportsIn().keySet().iterator();
        while (iter.hasNext()) {
            String trp = (String) iter.next();
            TransportInDescription trsIn = (TransportInDescription)
                configctx.getAxisConfiguration().getTransportsIn().get(trp);
            listenerManager.addListener(trsIn, false);
        }
    }

    public void testN2N() throws Exception {
        try {
            Thread.sleep(4000);
            // ensure that the servers are up before we actually use them
        } catch (InterruptedException ignore) {}
        
        // Creating the Simple Commodity Client
        System.getProperties().remove(org.apache.axis2.Constants.AXIS2_CONF);
        ServiceClient businessClient = new ServiceClient(null, null);
        Options options = new Options();
        options.setTo(
                new EndpointReference("http://127.0.0.1:10100/CommodityQuote"));
        businessClient.setOptions(options);

        OMElement response = businessClient.sendReceive(commodityPayload());

        assertNotNull(response);

        SynapseXPath xPath = new SynapseXPath("//return");
        xPath.addNamespace("ns","http://services.samples/xsd");
        OMElement returnEle = (OMElement) xPath.selectSingleNode(response);

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

    private void findAndReplace(File srcFile, File tgtFile, String search, String replace) throws Exception {

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            StringBuffer buffer = new StringBuffer(1024);
            reader = new BufferedReader(new FileReader(srcFile));
            String line = null;
            while( (line = reader.readLine()) != null){
                line = line.trim();
                buffer.append(line + "\n");
            }

            String result = buffer.toString().replace(search, replace);

            writer = new BufferedWriter(new FileWriter((tgtFile != null ? tgtFile : srcFile)));
            writer.write(result);

        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ignore) {}
        }
    }
}
