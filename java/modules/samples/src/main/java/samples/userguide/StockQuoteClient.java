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
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.synapse.util.UUIDGenerator;
import samples.common.StockQuoteHandler;

import java.net.URL;
import java.io.File;

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

        // defaults
        String symbol    = getProperty("symbol", "IBM");
        String mode      = getProperty("mode", "quote");
        String addUrl    = getProperty("addurl", null);
        String trpUrl    = getProperty("trpurl", null);
        String prxUrl    = getProperty("prxurl", null);
        String repo      = getProperty("repository", "client_repo");
        String svcPolicy = getProperty("policy", null);
        String rest      = getProperty("rest", null);
        String wsrm      = getProperty("wsrm", null);

        double price = 0; int quantity = 0;
		ConfigurationContext configContext = null;

        try {
            Options options = new Options();
            OMElement payload = null;
            ServiceClient serviceClient = null;

            if (repo != null && !"null".equals(repo)) {
                configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo,
                            repo+ File.separator + "conf" + File.separator + "axis2.xml");
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
            if (Boolean.parseBoolean(wsrm)) {
                System.out.println("Using WS-RM");
                serviceClient.engageModule("sandesha2");
                options.setProperty("Sandesha2LastMessage", "true");
                options.setProperty(SandeshaClientConstants.OFFERED_SEQUENCE_ID, UUIDGenerator.getUUID());
            }

            serviceClient.setOptions(options);

            if ("placeorder".equals(mode)) {
                serviceClient.fireAndForget(payload);
                Thread.sleep(5000);
                System.out.println("Order placed for " + quantity + " shares of stock " +
                symbol + " at a price of $ " + price);

            } else {
                OMElement result = serviceClient.sendReceive(payload);

                if (Boolean.parseBoolean(wsrm)) {
                    // give some time for RM to terminate normally
                    Thread.sleep(5000);
                }

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
                if (Boolean.parseBoolean(wsrm)) {
                    configContext.getListenerManager().stop();
					serviceClient.cleanup();
					System.exit(0);
                }
            }

            try {
                configContext.terminate();
            } catch (Exception ignore) {}

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
