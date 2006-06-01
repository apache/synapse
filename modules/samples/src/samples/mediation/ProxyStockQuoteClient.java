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
import org.apache.axis2.context.MessageContextConstants;

public class ProxyStockQuoteClient {

    public static void main(String[] args) {

        String symbol   = "IBM";
        String fwdProxy = "http://localhost:8080/axis2/services/InvesbotForwardProxy";
        String defProxy = "http://localhost:8080/axis2/services/InvesbotDefaultProxy";
        String seqProxy = "http://localhost:8080/axis2/services/InvesbotSequenceProxy";

        if (args.length > 0) symbol   = args[0];
        if (args.length > 1) fwdProxy = args[1];
        if (args.length > 2) defProxy = args[2];
        if (args.length > 2) seqProxy = args[3];

        ProxyStockQuoteClient.testProxyQuote(symbol, fwdProxy);
        ProxyStockQuoteClient.testProxyQuote(symbol, defProxy);
        ProxyStockQuoteClient.testProxyQuote(symbol, seqProxy);
    }

    private static void testProxyQuote(String symbol, String url) {
        try {
            OMElement getQuote = CustomQuoteXMLHandler.createStandardRequestPayload(symbol);

            Options options = new Options();
            options.setProperty(MessageContextConstants.TRANSPORT_URL, url);
            options.setAction("http://ws.invesbot.com/GetQuote");

            ServiceClient serviceClient = new ServiceClient();
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote);
            result.build();
            result = result.getFirstElement();
            System.out.println("Proxy :: Stock price = $" + CustomQuoteXMLHandler.parseStandardResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
