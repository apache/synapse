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

package samples.common;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

import javax.xml.namespace.QName;

public class Util {

    public static void testStandardQuote(
        String symbol, String soapAction, String xurl, String turl, String repo) {

        try {
            OMElement getQuote = StockQuoteHandler.createStandardRequestPayload(symbol);

            Options options = new Options();
            if (xurl != null)
                options.setTo(new EndpointReference(xurl));
            if (turl != null)
                options.setProperty(Constants.Configuration.TRANSPORT_URL, turl);
            options.setAction(soapAction);

            ServiceClient serviceClient = null;
            if (repo != null) {
                ConfigurationContext configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo, null);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }
            serviceClient.engageModule(new QName("addressing"));
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote).getFirstElement();
            System.out.println("Standard :: Stock price = $" +
                StockQuoteHandler.parseStandardResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testCustomQuote(
        String symbol, String soapAction, String xurl, String turl, String repo) {

        try {
            OMElement getQuote = StockQuoteHandler.createCustomRequestPayload(symbol);

            Options options = new Options();
            if (xurl != null)
                options.setTo(new EndpointReference(xurl));
            if (turl != null)
                options.setProperty(Constants.Configuration.TRANSPORT_URL, turl);
            options.setAction(soapAction);

            ServiceClient serviceClient = null;
            if (repo != null) {
                ConfigurationContext configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo, null);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }
            serviceClient.engageModule(new QName("addressing"));
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote);
            System.out.println("Custom :: Stock price = $" +
                StockQuoteHandler.parseCustomResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testAdvancedQuote(
        String symbol, String soapAction, String xurl, String turl, String repo) {
        try {
            OMElement getQuote = StockQuoteHandler.createCustomRequestPayload(symbol);

            Options options = new Options();
            if (xurl != null)
                options.setTo(new EndpointReference(xurl));
            if (turl != null)
                options.setProperty(Constants.Configuration.TRANSPORT_URL, turl);
            options.setAction(soapAction);

            ServiceClient serviceClient = null;
            if (repo != null) {
                ConfigurationContext configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo, null);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }
            serviceClient.engageModule(new QName("addressing"));
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote);
            System.out.println("Custom :: Stock price = $" +
                StockQuoteHandler.parseCustomResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testErroneousQuote(
        String symbol, String soapAction, String xurl, String turl, String repo) {
        try {
            OMElement getQuote = StockQuoteHandler.createErroneousCustomRequestPayload(symbol);

            Options options = new Options();
            if (xurl != null)
                options.setTo(new EndpointReference(xurl));
            if (turl != null)
                options.setProperty(Constants.Configuration.TRANSPORT_URL, turl);
            options.setAction(soapAction);

            ServiceClient serviceClient = null;
            if (repo != null) {
                ConfigurationContext configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo, null);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }
            serviceClient.engageModule(new QName("addressing"));
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote);
            System.out.println("Error :: Stock price = $" +
                StockQuoteHandler.parseCustomResponsePayload(result));

        } catch (Exception e) {
            if (e instanceof AxisFault) {
                System.out.println("Fault : " + ((AxisFault)e).getFaultElements());
            } else {
                e.printStackTrace();
            }
        }
    }

}
