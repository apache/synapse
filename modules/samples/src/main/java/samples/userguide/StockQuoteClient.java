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

package samples.userguide;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import samples.common.StockQuoteHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * See build.xml for options
 */
public class StockQuoteClient {

    private static String getProperty(String name, String def) {
        String result = System.getProperty(name);
        if (result == null || result.length() == 0) {
            result = def;
        }
        return result;
    }

    public static void main(String[] args) {

        try {
            executeClient();           
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void printResult() throws Exception {
        
        if ("placeorder".equals(InnerStruct.MODE)) {
            System.out.println("Order placed for " + InnerStruct.QUANTITY
                    + " shares of stock " + InnerStruct.SYMBOL
                    + " at a price of $ " + InnerStruct.PRICE);
        } else {
            if ("customquote".equals(InnerStruct.MODE)) {
                System.out.println("Custom :: Stock price = $" +
                        StockQuoteHandler.parseCustomQuoteResponse(InnerStruct.RESULT));
            } else if ("quote".equals(InnerStruct.MODE)) {
                System.out.println("Standard :: Stock price = $" +
                        StockQuoteHandler.parseStandardQuoteResponse(InnerStruct.RESULT));
            } else if ("dualquote".equals(InnerStruct.MODE)) {
                while (true) {
                    if (InnerStruct.COMPLETED) {
                        System.out.println("Standard dual channel :: Stock price = $" +
                                StockQuoteHandler.parseStandardQuoteResponse(InnerStruct.RESULT));
                        System.exit(0);
                    } else {
                        Thread.sleep(100);
                    }
                }
            } else if ("fullquote".equals(InnerStruct.MODE)) {
                System.out.println("Full :: Average price = $" +
                        StockQuoteHandler.parseFullQuoteResponse(InnerStruct.RESULT));
            } else if ("marketactivity".equals(InnerStruct.MODE)) {
                System.out.println("Activity :: Average price = $" +
                        StockQuoteHandler.parseMarketActivityResponse(InnerStruct.RESULT));
            }
        }
    }
    
    public static OMElement executeTestClient() throws Exception {
        executeClient();
        return InnerStruct.RESULT;
    }

    public static void executeClient() throws Exception {

        // defaults
        String symbol = getProperty("symbol", "IBM");
        String soapVer = getProperty("soapver", "soap11");
        String mode = getProperty("mode", "quote");
        String addUrl = getProperty("addurl", null);
        String trpUrl = getProperty("trpurl", null);
        String prxUrl = getProperty("prxurl", null);
        String repo = getProperty("repository", "client_repo");
        String svcPolicy = getProperty("policy", null);
        String rest = getProperty("rest", null);
        String itr = getProperty("itr", "1");
        int iterations = 1;
        boolean infinite = false;

        String pIterations = getProperty("i", null);


        if (pIterations != null) {
            try {
                iterations = Integer.parseInt(pIterations);
                if (iterations != -1) {
                    infinite = false;
                }
            } catch (NumberFormatException e) {
                // run with default values
            }
        }

        double price = 0;
        int quantity = 0;
        ConfigurationContext configContext;

        Options options = new Options();
        OMElement payload = null;
        ServiceClient serviceClient;

        if (repo != null && !"null".equals(repo)) {
            configContext =
                    ConfigurationContextFactory.
                            createConfigurationContextFromFileSystem(repo,
                                    repo + File.separator + "conf" + File.separator + "axis2.xml");
            serviceClient = new ServiceClient(configContext, null);
        } else {
            serviceClient = new ServiceClient();
        }

        if ("customquote".equals(mode)) {
            payload = StockQuoteHandler.createCustomQuoteRequest(symbol);
            options.setAction("urn:getQuote");
        } else if ("fullquote".equals(mode)) {
            payload = StockQuoteHandler.createFullQuoteRequest(symbol);
            options.setAction("urn:getFullQuote");
        } else if ("placeorder".equals(mode)) {
            price = getRandom(100, 0.9, true);
            quantity = (int) getRandom(10000, 1.0, true);
            payload = StockQuoteHandler.createPlaceOrderRequest(price, quantity, symbol);
            options.setAction("urn:placeOrder");
        } else if ("marketactivity".equals(mode)) {
            payload = StockQuoteHandler.createMarketActivityRequest();
            options.setAction("urn:getMarketActivity");
        } else if ("quote".equals(mode) || "dualquote".equals(mode)) {
            payload = StockQuoteHandler.createStandardQuoteRequest(
                    symbol, Integer.parseInt(itr));
            options.setAction("urn:getQuote");
            if ("dualquote".equals(mode)) {
                serviceClient.engageModule("addressing");
                options.setUseSeparateListener(true);
            }
        }

        // set addressing, transport and proxy url
        if (addUrl != null && !"null".equals(addUrl)) {
            serviceClient.engageModule("addressing");
            options.setTo(new EndpointReference(addUrl));
        }
        if (trpUrl != null && !"null".equals(trpUrl)) {
            options.setProperty(Constants.Configuration.TRANSPORT_URL, trpUrl);
        }
        if (prxUrl != null && !"null".equals(prxUrl)) {
            HttpTransportProperties.ProxyProperties proxyProperties =
                    new HttpTransportProperties.ProxyProperties();
            URL url = new URL(prxUrl);
            proxyProperties.setProxyName(url.getHost());
            proxyProperties.setProxyPort(url.getPort());
            proxyProperties.setUserName("");
            proxyProperties.setPassWord("");
            proxyProperties.setDomain("");
            options.setProperty(HTTPConstants.PROXY, proxyProperties);
        }

        // apply any service policies if any
        if (svcPolicy != null && !"null".equals(svcPolicy) && svcPolicy.length() > 0) {
            System.out.println("Using WS-Security");
            serviceClient.engageModule("addressing");
            serviceClient.engageModule("rampart");
            options.setProperty(
                    RampartMessageData.KEY_RAMPART_POLICY, loadPolicy(svcPolicy));
        }

        if (Boolean.parseBoolean(rest)) {
            System.out.println("Sending as REST");
            options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
        }

        if ("soap12".equals(soapVer)) {
            options.setSoapVersionURI(SOAP12Constants. SOAP_ENVELOPE_NAMESPACE_URI);
        }

        serviceClient.setOptions(options);

        InnerStruct.MODE = mode;
        InnerStruct.SYMBOL = symbol;
        InnerStruct.PRICE = price;
        InnerStruct.QUANTITY = quantity;

        if ("placeorder".equals(mode)) {
            serviceClient.fireAndForget(payload);
            Thread.sleep(5000);

        } else if ("dualquote".equals(mode)) {
            serviceClient.sendReceiveNonBlocking(payload, new StockQuoteCallback());
            printResult();
        } else {
            long i = 0;
            while (i < iterations || infinite) {
                InnerStruct.RESULT = serviceClient.sendReceive(payload);
                i++;
                printResult();
            }
        }
    }

    public static class InnerStruct {
        static String MODE = null;
        static String SYMBOL = null;
        static int QUANTITY = 0;
        static double PRICE = 0;
        static boolean COMPLETED = false;
        static OMElement RESULT = null;
    }

    private static Policy loadPolicy(String xmlPath) throws Exception {
        InputStream in = new FileInputStream(xmlPath);
        try {
            return PolicyEngine.getPolicy(
                    OMXMLBuilderFactory.createOMBuilder(in).getDocumentElement());
        } finally {
            in.close();
        }
    }

    private static double getRandom(double base, double variance, boolean onlyPositive) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * variance * base * rand))
                * (onlyPositive ? 1 : (rand > 0.5 ? 1 : -1));
    }

}
