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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;

import javax.xml.namespace.QName;

/**
 * A class that can create messages to, and parse replies from our sample StockQuote service
 */
public class StockQuoteHandler {
    /**
     * Create a new custom stock quote request with a body as follows
     * <m0:CheckPriceRequest xmlns:m0="http://www.apache-synapse.org/test">
     *   <m0:Code>symbol</m0:Code>
     * </m0:CheckPriceRequest>
     * @param symbol the stock symbol
     * @return OMElement for SOAP body
     */
    public static OMElement createCustomRequestPayload(String symbol) {
        OMFactory factory   = OMAbstractFactory.getOMFactory();
        OMNamespace ns      = factory.createOMNamespace(
            "http://www.apache-synapse.org/test", "m0");
        OMElement chkPrice  = factory.createOMElement("CheckPriceRequest", ns);
        OMElement code      = factory.createOMElement("Code", ns);
        chkPrice.addChild(code);
        code.setText(symbol);
        return chkPrice;
    }

    /**
     * Create a new erroneous custom stock quote request with a body as follows
     * <m0:CheckPriceRequest xmlns:m0="http://www.apache-synapse.org/test">
     *   <m0:Symbol>symbol</m0:Symbol>
     * </m0:CheckPriceRequest>
     * @param symbol the stock symbol
     * @return OMElement for SOAP body
     */
    public static OMElement createErroneousCustomRequestPayload(String symbol) {
        OMFactory factory   = OMAbstractFactory.getOMFactory();
        OMNamespace ns      = factory.createOMNamespace(
            "http://www.apache-synapse.org/test", "m0");
        OMElement chkPrice  = factory.createOMElement("CheckPriceRequest", ns);
        OMElement code      = factory.createOMElement("Symbol", ns);
        chkPrice.addChild(code);
        code.setText(symbol);
        return chkPrice;
    }

    /**
     * Create a new custom stock quote request with a body as follows
     *  <m:GetQuote xmlns:m="http://services.samples/xsd">
     *      <m:request>
     *          <m:symbol>IBM</m:symbol>
     *      </m:request>
     *  </m:GetQuote>
     * @param symbol the stock symbol
     * @return OMElement for SOAP body
     */
    public static OMElement createStandardRequestPayload(String symbol) {
        OMFactory factory   = OMAbstractFactory.getOMFactory();
        OMNamespace ns      = factory.createOMNamespace("http://services.samples/xsd", "m0");
        OMElement getQuote  = factory.createOMElement("getQuote", ns);
        OMElement request   = factory.createOMElement("request", ns);
        OMElement symb      = factory.createOMElement("symbol", ns);
        request.addChild(symb);
        getQuote.addChild(request);
        symb.setText(symbol);
        return getQuote;
    }

    /**
     * Create a new order for a quantiry of a stock at a given price
     * <m:placeOrder xmlns:m="http://services.samples/xsd">
     *	  <m:order>
     *	      <m:price>3.141593E0</m:price>
     *	      <m:quantity>4</m:quantity>
     *	      <m:symbol>IBM</m:symbol>
     *    </m:order>
     * 	</m:placeOrder>
     *
     * @param purchPrice the purchase price
     * @param qty the quantiry
     * @param symbol the stock
     * @return an OMElement payload for the order
     */
    public static OMElement createPlaceOrderPayload(double purchPrice, int qty, String symbol) {
        OMFactory factory   = OMAbstractFactory.getOMFactory();
        OMNamespace ns      = factory.createOMNamespace("http://services.samples/xsd", "m0");
        OMElement placeOrder= factory.createOMElement("placeOrder", ns);
        OMElement order     = factory.createOMElement("order", ns);
        OMElement price     = factory.createOMElement("price", ns);
        OMElement quantity  = factory.createOMElement("quantity", ns);
        OMElement symb      = factory.createOMElement("symbol", ns);
        price.setText(Double.toString(purchPrice));
        quantity.setText(Integer.toString(qty));
        symb.setText(symbol);
        order.addChild(price);
        order.addChild(quantity);
        order.addChild(symb);
        placeOrder.addChild(order);        
        return placeOrder;
    }

    /**
     * Digests the standard StockQuote response and extracts the last trade price
     * @param result
     * @return
     * @throws javax.xml.stream.XMLStreamException
     *
     *  <ns:getQuoteResponse xmlns:ns="http://services.samples/xsd">
     *      <ns:return>
     *          <ns:change>-2.3238706829151026</ns:change>
     *          ...
     *          <ns:symbol>IBM</ns:symbol>
     *          <ns:volume>17949</ns:volume>
     *      </ns:return>
     *  </ns:getQuoteResponse>
     */
    public static String parseStandardResponsePayload(OMElement result) throws Exception {

        OMElement last = result.getFirstChildWithName(
            new QName("http://services.samples/xsd", "last"));
        if (last != null) {
            return last.getText();
        } else {
            throw new Exception("Unexpected response : " + result);
        }
    }

    /**
     * Digests the custom quote response and extracts the last trade price
     * @param result
     * @return
     * @throws javax.xml.stream.XMLStreamException
     *
     *      <CheckPriceResponse xmlns="http://ws.invesbot.com/" >
     *          <Code>IBM</Code>
     *          <Price>82.90</Price>
     *      </CheckPriceResponse>
     */
    public static String parseCustomResponsePayload(OMElement result) throws Exception {

        OMElement price = result.getFirstChildWithName(
            new QName("http://www.apache-synapse.org/test", "Price"));
        if (price != null) {
            return price.getText();
        } else {
            throw new Exception("Unexpected response : " + result);
        }
    }
}
