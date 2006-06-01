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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.AxisFault;

public class AdvancedQuoteClient {

    public static void main(String[] args) {

        String xurl   = "http://ws.invesbot.com/stockquotes.asmx";
        String turl   = "http://localhost:8080/StockQuote";

        if (args.length > 0) xurl   = args[0];
        if (args.length > 1) turl   = args[1];

        testStandardQuote("IBM", xurl, turl);
        testAdvancedQuote("SUN", xurl, turl);
        testErroneousQuote("MSFT", xurl, turl);
    }

    private static void testAdvancedQuote(String symbol, String xurl, String turl) {
        try {
            OMElement getQuote = CustomQuoteXMLHandler.createCustomRequestPayload(symbol);

            Options options = new Options();
            options.setTo(new EndpointReference(xurl));
            options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
            options.setAction("http://ws.invesbot.com/GetQuote");

            ServiceClient serviceClient = new ServiceClient();
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote);
            System.out.println("Custom :: Stock price = $" + CustomQuoteXMLHandler.parseCustomResponsePayload(result.getFirstElement()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testErroneousQuote(String symbol, String xurl, String turl) {
        try {
            OMElement getQuote = CustomQuoteXMLHandler.createErroneousRequestPayload(symbol);

            Options options = new Options();
            options.setTo(new EndpointReference(xurl));
            options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
            options.setAction("http://ws.invesbot.com/GetQuote");

            ServiceClient serviceClient = new ServiceClient();
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote);
            System.out.println("Error :: Stock price = $" + CustomQuoteXMLHandler.parseCustomResponsePayload(result.getFirstElement()));

        } catch (Exception e) {
            if (e instanceof AxisFault) {
                System.out.println("Fault : " + ((AxisFault)e).getFaultElements());
            } else {
                e.printStackTrace();
            }
        }
    }

    private static void testStandardQuote(String symbol, String xurl, String turl) {
        try {
            OMElement getQuote = CustomQuoteXMLHandler.createStandardRequestPayload(symbol);

            Options options = new Options();
            options.setTo(new EndpointReference(xurl));
            options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
            options.setAction("http://ws.invesbot.com/GetQuote");

            ServiceClient serviceClient = new ServiceClient();
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote).getFirstElement();
            System.out.println("Standard :: Stock price = $" + CustomQuoteXMLHandler.parseStandardResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}