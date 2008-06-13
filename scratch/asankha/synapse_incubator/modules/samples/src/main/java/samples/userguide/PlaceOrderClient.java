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
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.Constants;
import samples.common.StockQuoteHandler;

public class PlaceOrderClient {

    public static void main(String[] args) {

        String symbol  = "IBM";
        String turl    = "http://localhost:8080/StockQuote";
        String sAction = "urn:placeOrder";

        if (args.length > 0) symbol = args[0];
        if (args.length > 1) turl   = args[1];

        try {
            double price = getRandom(100, 0.9, true);
            int quantity = (int) getRandom(10000, 1.0, true);
            OMElement placeOrder =
                StockQuoteHandler.createPlaceOrderPayload(price, quantity, symbol);

            Options options = new Options();
            options.setProperty(Constants.Configuration.TRANSPORT_URL, turl);
            options.setAction(sAction);

            ServiceClient serviceClient = new ServiceClient();
            serviceClient.setOptions(options);

            serviceClient.fireAndForget(placeOrder);
            Thread.sleep(5000);
            
            System.out.println("Order placed for " + quantity + " shares of stock " +
                symbol + " at a price of $ " + price);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double getRandom(double base, double varience, boolean onlypositive) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * varience * base * rand))
            * (onlypositive ? 1 : (rand > 0.5 ? 1 : -1));
    }
}
