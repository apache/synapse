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
package samples.services;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(targetNamespace="http://services.samples", serviceName="JAXWSStockQuoteService")
public class StockQuoteService {
    private final AtomicInteger orderCount = new AtomicInteger();
    
    private static double getRandom(double base, double varience, boolean onlypositive) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * varience * base * rand))
            * (onlypositive ? 1 : (rand > 0.5 ? 1 : -1));
    }
    
    @WebMethod(action="urn:getQuote")
    @WebResult(name="return", targetNamespace="http://services.samples")
    @RequestWrapper(className="samples.services.GetQuoteWrapper",
                    localName="getQuote", targetNamespace="http://services.samples")
    @ResponseWrapper(className="samples.services.GetQuoteResponseWrapper",
                     localName="getQuoteResponse", targetNamespace="http://services.samples")
    public GetQuoteResponse getQuote(
            @WebParam(name="request", targetNamespace="http://services.samples") GetQuote request) {
        
        String symbol = request.getSymbol();
        System.out.println(new Date() + " " + this.getClass().getName() +
                " :: Generating quote for : " + request.getSymbol());
        GetQuoteResponse response = new GetQuoteResponse();
        response.setSymbol(symbol);
        double last = getRandom(100, 0.9, true);
        response.setLast(last);
        response.setLastTradeTimestamp(new Date().toString());
        double change = getRandom(3, 0.5, false);
        response.setChange(change);
        response.setOpen(getRandom(last, 0.05, false));
        response.setHigh(getRandom(last, 0.05, false));
        response.setLow(getRandom(last, 0.05, false));
        response.setVolume((int)getRandom(10000, 1.0, true));
        response.setMarketCap(getRandom(10E6, 5.0, false));
        double prevClose = getRandom(last, 0.15, false);
        response.setPrevClose(prevClose);
        response.setPercentageChange(change / prevClose * 100);
        response.setEarnings(getRandom(10, 0.4, false));
        response.setPeRatio(getRandom(20, 0.30, false));
        response.setName(symbol + " Company");
        return response;
    }

    @Oneway
    @WebMethod(action="urn:placeOrder")
    @RequestWrapper(className="samples.services.PlaceOrderWrapper",
                    localName="placeOrder", targetNamespace="http://services.samples")
    public void placeOrder(
            @WebParam(name="order", targetNamespace="http://services.samples") PlaceOrder order) {
        System.out.println(new Date() + " " + this.getClass().getName() +
            "  :: Accepted order #" + orderCount.incrementAndGet() + " for : " +
            order.getQuantity() + " stocks of " + order.getSymbol() + " at $ " +
            order.getPrice());
    }
    
    public static void main(String[] args) {
        Endpoint.publish("http://localhost:7777/stock", new StockQuoteService());
    }
}
