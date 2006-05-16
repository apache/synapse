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
package samples.mediation;

import org.apache.axiom.om.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;

public class CustomQuoteXMLHandler {

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
        OMNamespace ns      = factory.createOMNamespace("http://www.apache-synapse.org/test", "m0");
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
    public static OMElement createErroneousRequestPayload(String symbol) {
        OMFactory factory   = OMAbstractFactory.getOMFactory();
        OMNamespace ns      = factory.createOMNamespace("http://www.apache-synapse.org/test", "m0");
        OMElement chkPrice  = factory.createOMElement("CheckPriceRequest", ns);
        OMElement code      = factory.createOMElement("Symbol", ns);
        chkPrice.addChild(code);
        code.setText(symbol);
        return chkPrice;
    }

    /**
     * Create a new custom stock quote request with a body as follows
        <m:GetQuote xmlns:m="http://www.webserviceX.NET/">
			<m:symbol>IBM</m:symbol>
		</m:GetQuote>
     * @param symbol the stock symbol
     * @return OMElement for SOAP body
     */
    public static OMElement createStandardRequestPayload(String symbol) {
        OMFactory factory   = OMAbstractFactory.getOMFactory();
        OMNamespace ns      = factory.createOMNamespace("http://ws.invesbot.com/", "m0");
        OMElement getQuote  = factory.createOMElement("GetQuote", ns);
        OMElement symb      = factory.createOMElement("symbol", ns);
        getQuote.addChild(symb);
        symb.setText(symbol);
        return getQuote;
    }

    /**
     * Digests the standard StockQuote response and extracts the last trade price
     * @param result
     * @return
     * @throws XMLStreamException
     *
        <GetQuoteResponse xmlns="http://ws.invesbot.com/">
			<GetQuoteResult>
				<StockQuote xmlns="">
					<Symbol>IBM</Symbol>
                    ...
                    <Price>82.47</Price>
                    .......
                </StockQuote>
			</GetQuoteResult>
		</GetQuoteResponse>
     */
    public static String parseStandardResponsePayload(OMElement result) throws Exception {

        OMElement getQResp = result.getFirstChildWithName(
            new QName("http://ws.invesbot.com/", "StockQuote"));
        if (getQResp != null) {
            OMElement price = getQResp.getFirstChildWithName(
                new QName("http://ws.invesbot.com/", "Price"));
            return price.getText();
        } else {
            throw new Exception("Unexpected response : " + result);
        }
    }

    /**
     * Digests the custom quote response and extracts the last trade price
     * @param result
     * @return
     * @throws XMLStreamException
     *
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <soap:Body>
            <CheckPriceResponse xmlns="http://ws.invesbot.com/" >
                <Code>IBM</Code>
                <Price>82.90</Price>
            </CheckPriceResponse>
        </soap:Body>
        </soap:Envelope>
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