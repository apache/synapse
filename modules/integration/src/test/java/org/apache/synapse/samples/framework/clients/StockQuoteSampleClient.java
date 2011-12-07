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
package org.apache.synapse.samples.framework.clients;

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SampleConfiguration;

import javax.xml.namespace.QName;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StockQuoteSampleClient {

    private static final Log log = LogFactory.getLog(StockQuoteSampleClient.class);

    private final static String COOKIE = "Cookie";
    private final static String SET_COOKIE = "Set-Cookie";
    private ConfigurationContext configContext = null;

    private Options options;
    private ServiceClient serviceClient;
    private SampleClientResult clientResult;
    private OMElement payload;
    private OMElement response;
    private boolean completed;
    private SampleConfiguration.ClientSampleConfiguration configuration;

    public StockQuoteSampleClient(SampleConfiguration.ClientSampleConfiguration configuration) {
        this.configuration = configuration;
    }

    private void initializeClient(String addUrl, String trpUrl, String prxUrl,
                                  String svcPolicy, long timeout) throws Exception {
        log.info("initialing client config...");
        options = new Options();
        clientResult = new SampleClientResult();
        clientResult.setGotResponse(false);
        payload = null;

        log.info("creating axis2 configuration context using the repo: " + configuration.getClientRepo());

        configContext = ConfigurationContextFactory.
                createConfigurationContextFromFileSystem(configuration.getClientRepo(),
                        configuration.getAxis2Xml());
        serviceClient = new ServiceClient(configContext, null);

        log.info("setting address, transport, proxy urls where applicable");
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
            log.info("Using WS-Security");
            serviceClient.engageModule("addressing");
            serviceClient.engageModule("rampart");
            StAXOMBuilder builder = new StAXOMBuilder(svcPolicy);
            Policy policy = PolicyEngine.getPolicy(builder.getDocumentElement());
            options.setProperty(
                    RampartMessageData.KEY_RAMPART_POLICY, policy);
        }

        if (timeout > 0) {
            log.info("setting client timeout to: " + timeout);
            options.setTimeOutInMilliSeconds(timeout);
        }

        serviceClient.setOptions(options);
    }

    private void deInitializeClient() {
        if (serviceClient != null) {
            try {
                log.info("cleaning up client");
                serviceClient.cleanup();
                configContext.terminate();
            } catch (AxisFault axisFault) {
                log.error("Error terminating client", axisFault);
            }
        }
    }

    public SampleClientResult requestStandardQuote(String addUrl, String trpUrl, String prxUrl,
                                                   String symbol, String svcPolicy) {
        log.info("sending standard quote request");
        try {
            initializeClient(addUrl, trpUrl, prxUrl, svcPolicy, 10000);

            payload = StockQuoteHandler.createStandardQuoteRequest(
                    symbol, 1);
            options.setAction("urn:getQuote");
            OMElement resultElement = serviceClient.sendReceive(payload);
            log.info("Standard :: Stock price = $" +
                    StockQuoteHandler.parseStandardQuoteResponse(resultElement));
            clientResult.setGotResponse(true);
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setGotResponse(false);
            clientResult.setException(e);
        }
        deInitializeClient();

        return clientResult;

    }

    public SampleClientResult requestDualQuote(String addUrl, String trpUrl,
                                               String prxUrl, String symbol) {
        log.info("sending dual quote request");

        try {
            initializeClient(addUrl, trpUrl, prxUrl, null, 10000);

            payload = StockQuoteHandler.createStandardQuoteRequest(
                    symbol, 1);
            options.setAction("urn:getQuote");
            //serviceClient.engageModule("addressing");
            setCompleted(false);
            serviceClient.sendReceiveNonBlocking(payload, new StockQuoteCallback(this));

            while (true) {
                if (isCompleted()) {
                    log.info("Standard dual channel :: Stock price = $" +
                            StockQuoteHandler.parseStandardQuoteResponse(getResponse()));
                    clientResult.setGotResponse(true);
                    break;
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setGotResponse(false);
            clientResult.setException(e);
        }
        deInitializeClient();

        return clientResult;
    }

    public SampleClientResult requestCustomQuote(String addUrl, String trpUrl,
                                                 String prxUrl, String symbol) {
        log.info("sending custom quote request");

        try {
            initializeClient(addUrl, trpUrl, prxUrl, null, 10000);

            payload = StockQuoteHandler.createCustomQuoteRequest(symbol);
            options.setAction("urn:getQuote");
            OMElement resultElement = serviceClient.sendReceive(payload);
            log.info("Custom :: Stock price = $" +
                    StockQuoteHandler.parseCustomQuoteResponse(resultElement));
            clientResult.setGotResponse(true);
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setGotResponse(false);
            clientResult.setException(e);
        }
        deInitializeClient();

        return clientResult;
    }

    public SampleClientResult placeOrder(String addUrl, String trpUrl, String prxUrl, String symbol) {
        log.info("sending fire and forget (place order) request");

        try {

            initializeClient(addUrl, trpUrl, prxUrl, null, 10000);
            double price = getRandom(100, 0.9, true);
            int quantity = (int) getRandom(10000, 1.0, true);
            payload = StockQuoteHandler.createPlaceOrderRequest(price, quantity, symbol);
            options.setAction("urn:placeOrder");

            serviceClient.fireAndForget(payload);
            Thread.sleep(5000);

            log.info("Order placed for " + quantity
                    + " shares of stock " + symbol
                    + " at a price of $ " + price);
            clientResult.setGotResponse(true);
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setGotResponse(false);
            clientResult.setException(e);
        }
        deInitializeClient();

        return clientResult;
    }

    public SampleClientResult requestRestQuote(String addUrl, String trpUrl,
                                               String prxUrl, String symbol) {
        log.info("sending rest request");

        try {
            initializeClient(addUrl, trpUrl, prxUrl, null, 10000);

            payload = StockQuoteHandler.createStandardQuoteRequest(
                    symbol, 1);
            options.setAction("urn:getQuote");
            options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
            OMElement resultElement = serviceClient.sendReceive(payload);
            log.info("Standard :: Stock price = $" +
                    StockQuoteHandler.parseStandardQuoteResponse(resultElement));
            clientResult.setGotResponse(true);
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setGotResponse(false);
            clientResult.setException(e);
        }
        deInitializeClient();

        return clientResult;

    }


    public SampleClientResult sessionlessClient(String addUrl, String trpUrl, int iterations) {
        try {
            boolean infinite = iterations <= 0;
            OMFactory fac = OMAbstractFactory.getOMFactory();
            OMElement value = fac.createOMElement("Value", null);
            value.setText("Sample string");

            initializeClient(addUrl, trpUrl, null, null, 10000);

            options.setAction("urn:sampleOperation");


            String testString = "";

            long i = 0;
            while (i < iterations || infinite) {
                serviceClient.getOptions().setManageSession(true);
                OMElement responseElement = serviceClient.sendReceive(value);
                String response = responseElement.getText();

                if (!clientResult.gotResponse()) {
                    clientResult.setGotResponse(true);
                }

                i++;
                log.info("Request: " + i + " ==> " + response);
                testString = testString.concat(":" + i + ">" + response + ":");
            }

            clientResult.setFinished(true);
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setFinished(true);
            clientResult.setException(e);
        }
        deInitializeClient();

        return clientResult;
    }

    public SampleClientResult statefulClient(String addUrl, String trpUrl, int iterations) {
        boolean infinite = false;
        String session = null;

        clientResult = new SampleClientResult();
        clientResult.setGotResponse(false);

        try {

            SOAPEnvelope env1 = buildSoapEnvelope("c1", "v1");
            SOAPEnvelope env2 = buildSoapEnvelope("c2", "v1");
            SOAPEnvelope env3 = buildSoapEnvelope("c3", "v1");
            SOAPEnvelope[] envelopes = {env1, env2, env3};

            initializeClient(addUrl, trpUrl, null, null, 10000);

            options.setAction("urn:sampleOperation");

            int i = 0;
            int sessionNumber;
            String[] cookies = new String[3];
            boolean httpSession = session != null && "http".equals(session);
            int cookieNumber;
            while (i < iterations || infinite) {
                i++;
                MessageContext messageContext = new MessageContext();
                sessionNumber = getSessionTurn(envelopes.length);

                messageContext.setEnvelope(envelopes[sessionNumber]);
                cookieNumber = getSessionTurn(cookies.length);
                String cookie = cookies[cookieNumber];
                if (httpSession) {
                    setSessionID(messageContext, cookie);
                }
                try {
                    OperationClient op = serviceClient.createClient(ServiceClient.ANON_OUT_IN_OP);
                    op.addMessageContext(messageContext);
                    op.execute(true);

                    MessageContext responseContext =
                            op.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                    String receivedCookie = extractSessionID(responseContext);
                    String receivedSetCookie = getSetCookieHeader(responseContext);
                    if (httpSession) {

                        if (receivedSetCookie != null && !"".equals(receivedSetCookie)) {
                            cookies[cookieNumber] = receivedCookie;
                        }
                    }

                    SOAPEnvelope responseEnvelope = responseContext.getEnvelope();

                    OMElement vElement =
                            responseEnvelope.getBody().getFirstChildWithName(new QName("Value"));

                    if (!clientResult.gotResponse()) {
                        clientResult.setGotResponse(true);
                    }

                    log.info("Request: " + i + " with Session ID: " +
                                    (httpSession ? cookie : sessionNumber) + " ---- " +
                                    "Response : with  " + (httpSession && receivedCookie != null ?
                                    (receivedSetCookie != null ? receivedSetCookie :
                                            receivedCookie) : " ") + " " + vElement.getText());
                } catch (AxisFault axisFault) {
                    log.error("Request with session id " +
                            (httpSession ? cookie : sessionNumber) + " " +
                            "- Get a Fault : " + axisFault.getMessage(), axisFault);
                }
            }

            clientResult.setFinished(true);
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setFinished(true);
            clientResult.setException(e);
        }
        deInitializeClient();

        return clientResult;
    }


    private int getSessionTurn(int max) {
        Random random = new Random();
        return random.nextInt(max);
    }

    protected String getSetCookieHeader(MessageContext axis2MessageContext) {

        Object o = axis2MessageContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (o != null && o instanceof Map) {
            Map headerMap = (Map) o;
            return (String) headerMap.get(SET_COOKIE);
        }
        return null;
    }

    protected void setSessionID(MessageContext axis2MessageContext, String value) {

        if (value == null) {
            return;
        }
        Map map = (Map) axis2MessageContext.getProperty(HTTPConstants.HTTP_HEADERS);
        if (map == null) {
            map = new HashMap();
            axis2MessageContext.setProperty(HTTPConstants.HTTP_HEADERS, map);
        }
        map.put(COOKIE, value);
    }

    protected String extractSessionID(MessageContext axis2MessageContext) {

        Object o = axis2MessageContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (o != null && o instanceof Map) {
            Map headerMap = (Map) o;
            String cookie = (String) headerMap.get(SET_COOKIE);
            if (cookie == null) {
                cookie = (String) headerMap.get(COOKIE);
            } else {
                cookie = cookie.split(";")[0];
            }
            return cookie;
        }
        return null;
    }

    private SOAPEnvelope buildSoapEnvelope(String clientID, String value) {
        SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();

        SOAPEnvelope envelope = soapFactory.createSOAPEnvelope();

        SOAPHeader header = soapFactory.createSOAPHeader();
        envelope.addChild(header);

        OMNamespace synNamespace = soapFactory.
                createOMNamespace("http://ws.apache.org/ns/synapse", "syn");
        OMElement clientIDElement = soapFactory.createOMElement("ClientID", synNamespace);
        clientIDElement.setText(clientID);
        header.addChild(clientIDElement);

        SOAPBody body = soapFactory.createSOAPBody();
        envelope.addChild(body);

        OMElement valueElement = soapFactory.createOMElement("Value", null);
        valueElement.setText(value);
        body.addChild(valueElement);

        return envelope;
    }


    private double getRandom(double base, double varience, boolean onlypositive) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * varience * base * rand))
                * (onlypositive ? 1 : (rand > 0.5 ? 1 : -1));
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public OMElement getResponse() {
        return response;
    }

    public void setResponse(OMElement response) {
        this.response = response;
    }
}
