/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package samples.common;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMElement;
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
     *  <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
     *  <soap:Body>
     *      <CheckPriceResponse xmlns="http://ws.invesbot.com/" >
     *          <Code>IBM</Code>
     *          <Price>82.90</Price>
     *      </CheckPriceResponse>
     *  </soap:Body>
     *  </soap:Envelope>
     */
    public static String parseCustomResponsePayload(OMElement result) throws Exception {

        OMElement chkPResp = result.getFirstChildWithName(
            new QName("http://www.apache-synapse.org/test", "CheckPriceResponse"));
        if (chkPResp != null) {
            OMElement price = chkPResp.getFirstChildWithName(new QName("http://www.apache-synapse.org/test", "Price"));
            return price.getText();
        } else {
            throw new Exception("Unexpected response : " + result);
        }
    }
}
