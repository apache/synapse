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
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.rampart.RampartMessageData;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import samples.common.StockQuoteHandler;

import javax.xml.namespace.QName;

/**
 * A simple example showing a dumb client.. the client just sets the transport
 * to a Synapse server, which will determine and use the correct EPR for the
 * message and send out.
 */
public class DumbStockQuoteClient {

    public static void main(String[] args) {

        String symbol  = "IBM";
        String turl    = "http://localhost:9000/axis2/services/SimpleStockQuoteService";
        String sAction = "urn:getQuote";
        String repo    = "client_repo";
        String secpol  = null;

        if (args.length > 0) symbol = args[0];
        if (args.length > 1) turl   = args[1];
        if (args.length > 2) repo   = args[2];
        if (args.length > 3) secpol = args[3];

        try {
            OMElement getQuote = StockQuoteHandler.createStandardRequestPayload(symbol);

            Options options = new Options();
            if (turl != null)
                options.setProperty(Constants.Configuration.TRANSPORT_URL, turl);
            options.setAction(sAction);

            if (secpol != null && secpol.length() > 0) {
                options.setProperty(
                    RampartMessageData.KEY_RAMPART_POLICY, loadPolicy(secpol));
            }

            ServiceClient serviceClient = null;
            if (repo != null) {
                ConfigurationContext configContext =
                    ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repo, null);
                serviceClient = new ServiceClient(configContext, null);
            } else {
                serviceClient = new ServiceClient();
            }
            
            if (secpol != null && secpol.length() > 0) {
                serviceClient.engageModule(new QName("rampart"));
            }
            serviceClient.setOptions(options);

            OMElement result = serviceClient.sendReceive(getQuote).getFirstElement();
            System.out.println("Standard :: Stock price = $" +
                StockQuoteHandler.parseStandardResponsePayload(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Policy loadPolicy(String xmlPath) throws Exception {
        StAXOMBuilder builder = new StAXOMBuilder(xmlPath);
        return PolicyEngine.getPolicy(builder.getDocumentElement());
    }

}
