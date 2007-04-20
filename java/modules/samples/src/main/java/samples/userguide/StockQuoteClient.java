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
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.Constants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.rampart.RampartMessageData;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.Policy;
import samples.common.StockQuoteHandler;

import java.net.URL;

/**
 * ant stockquote
 *     [-Dsymbol=<sym>] [-Drepository=<repo>] [-Dpolicy=<policy>]
 *     [-Dmode=quote | customquote | fullquote | placeorder | marketactivity]
 *     [-Daddressingurl=<url>] [-Dtransporturl=<url> | -Dproxyurl=<url>]
 * 
 * StockQuoteClient <symbol> <quote | customquote | fullquote | placeorder | marketactivity>
 *      <addurl> <trpurl> <prxurl> <repo> <policy>
 */
public class StockQuoteClient {

    public static void main(String[] args) {

        // defaults
        String symbol    = "IBM";
        String mode      = "quote";
        String addUrl    = "http://localhost:9000/soap/SimpleStockQuoteService";
        String trpUrl    = "http://localhost:8080";
        String prxUrl    = null;
        String repo      = "client_repo";
        String svcPolicy = null;

        if (args.length > 0) symbol    = args[0];
        if (args.length > 1) mode      = args[1];
        if (args.length > 2) addUrl    = args[2];
        if (args.length > 3) trpUrl    = args[3];
        if (args.length > 4) prxUrl    = args[4];
        if (args.length > 5) repo      = args[5];
        if (args.length > 6) svcPolicy = args[6];

        double price = 0; int quantity = 0;

        try {
            Options options = new Options();
            OMElement payload = null;
            ServiceClient serviceClient = null;

            if (repo != null && !"null".equals(repo)) {
                ConfigurationContext configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo, null);
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
            } else if ("quote".equals(mode)) {
                payload = StockQuoteHandler.createStandardQuoteRequest(symbol);
                options.setAction("urn:getQuote");
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
                serviceClient.engageModule("addressing");
                serviceClient.engageModule("rampart");
                options.setProperty(
                    RampartMessageData.KEY_RAMPART_POLICY, loadPolicy(svcPolicy));
            }

            
            serviceClient.setOptions(options);

            if ("placeorder".equals(mode)) {
                serviceClient.fireAndForget(payload);
                Thread.sleep(5000);
                System.out.println("Order placed for " + quantity + " shares of stock " +
                symbol + " at a price of $ " + price);

            } else {
                OMElement result = serviceClient.sendReceive(payload);
                if("customquote".equals(mode)) {
                    System.out.println("Custom :: Stock price = $" +
                    StockQuoteHandler.parseCustomQuoteResponse(result));
                } else if ("quote".equals(mode)) {
                    System.out.println("Standard :: Stock price = $" +
                        StockQuoteHandler.parseStandardQuoteResponse(result));
                } else if ("fullquote".equals(mode)) {
                    System.out.println("Full :: Average price = $" +
                        StockQuoteHandler.parseFullQuoteResponse(result));
                } else if ("marketactivity".equals(mode)) {
                    System.out.println("Activity :: Average price = $" +
                        StockQuoteHandler.parseMarketActivityResponse(result));   
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Policy loadPolicy(String xmlPath) throws Exception {
        StAXOMBuilder builder = new StAXOMBuilder(xmlPath);
        return PolicyEngine.getPolicy(builder.getDocumentElement());
    }

    private static double getRandom(double base, double varience, boolean onlypositive) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * varience * base * rand))
            * (onlypositive ? 1 : (rand > 0.5 ? 1 : -1));
    }

}
