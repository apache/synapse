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
package samples.config;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import samples.mediation.*;

import javax.xml.namespace.QName;

public class SimpleStockQuoteClient {

    public static void main(String[] args) {

        String symbol = "IBM";
        String xurl   = "http://ws.invesbot.com/stockquotes.asmx";
        String turl   = "http://localhost:8080/StockQuote";
        String repo   = "./../synapse_repository";

        if (args.length > 0) symbol = args[0];
        if (args.length > 1) xurl   = args[1];
        if (args.length > 2) turl   = args[2];
        if (args.length > 3) repo   = args[3];


        testSimpleStockQuote(symbol, xurl, turl, repo);
    }

    private static void testSimpleStockQuote(String symbol, String xurl, String turl, String repo) {
        try {
            OMElement getQuote = CustomQuoteXMLHandler.createStandardRequestPayload(symbol);

            Options options = new Options();
            options.setTo(new EndpointReference(xurl));
            options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
            options.setAction("http://ws.invesbot.com/GetQuote");

            ConfigurationContext configContext =
                ConfigurationContextFactory.createConfigurationContextFromFileSystem(repo, null);
            ServiceClient serviceClient = new ServiceClient(configContext, null);
            serviceClient.setOptions(options);

            //Engage Addressing on  outgoing message.
            serviceClient.engageModule(new QName("addressing"));

            OMElement result = serviceClient.sendReceive(getQuote).getFirstElement();
            System.out.println("Simple :: Stock price = $" + CustomQuoteXMLHandler.parseStandardResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
