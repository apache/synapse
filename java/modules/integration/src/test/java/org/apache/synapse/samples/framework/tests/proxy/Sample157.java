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
package org.apache.synapse.samples.framework.tests.proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.samples.framework.SynapseTestCase;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Sample157 extends SynapseTestCase {

    private static final Log log = LogFactory.getLog(Sample157.class);

    private String requestXml;
    private HttpClient httpclient;

    public Sample157() {
        super(157);
        httpclient = new DefaultHttpClient();

        requestXml = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:ser=\"http://services.samples\" xmlns:xsd=\"http://services.samples/xsd\">\n" +
                "       <soap:Header/>\n" +
                "       <soap:Body>\n" +
                "          <ser:getQuote>\n" +
                "             <ser:request>\n" +
                "                <xsd:symbol>IBM</xsd:symbol>\n" +
                "             </ser:request>\n" +
                "          </ser:getQuote>\n" +
                "       </soap:Body>\n" +
                "    </soap:Envelope>";
    }


    public void testRoutingOnHttpHeader() {
        String url = "http://localhost:8280/services/StockQuoteProxy";

        log.info("Running test: Routing Messages based on HTTP URL, HTTP Headers and Query " +
                "Parameters");

        // Create a new HttpClient and Post Header
        HttpPost httpPost = new HttpPost(url);//new HttpPost(availabilityUrl + VERSION_TEXT);

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/soap+xml;charset=UTF-8");
        httpPost.setHeader("foo", "bar");
        HttpResponse response;
        HttpEntity entity;
        try {
            entity = new StringEntity(requestXml, "application/xml", HTTP.UTF_8);
            httpPost.setEntity(entity);
            response = httpclient.execute(httpPost);
            assertNotNull("Did not get a response ", response);
            HttpEntity resEntity = response.getEntity();
            assertNotNull("Response is empty", resEntity);
            BufferedReader rd = new BufferedReader(new InputStreamReader(resEntity.getContent()));
            String result = "";
            String line;
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            System.out.println(result);
            assertTrue("Response is empty", !"".equals(result));
        } catch (Exception e) {
            fail("Errors occurred while sending POST request: " + e.getMessage());
        }

        url = "http://localhost:8280/services/StockQuoteProxy";

        log.info("Running test: Routing Messages based on HTTP URL, HTTP Headers and Query Parameters");

        // Create a new HttpClient and Post Header
        httpPost = new HttpPost(url);//new HttpPost(availabilityUrl + VERSION_TEXT);

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/soap+xml;charset=UTF-8");
        httpPost.setHeader("my_custom_header1", "foo1");

        try {
            entity = new StringEntity(requestXml, "application/xml", HTTP.UTF_8);
            httpPost.setEntity(entity);
            response = httpclient.execute(httpPost);
            assertNotNull("Did not get a response ", response);
            HttpEntity resEntity = response.getEntity();
            assertNotNull("Response is empty", resEntity);
            BufferedReader rd = new BufferedReader(new InputStreamReader(resEntity.getContent()));
            String result = "";
            String line;
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            System.out.println(result);
            assertTrue("Response is empty", !"".equals(result));
        } catch (Exception e) {
            fail("Errors occurred while sending POST request: " + e.getMessage());
        }

        url = "http://localhost:8280/services/StockQuoteProxy?qparam1=qpv_foo&qparam2=qpv_foo2";
        log.info("Running test: Routing Messages based on HTTP URL, HTTP Headers and Query Parameters");

        // Create a new HttpClient and Post Header
        httpPost = new HttpPost(url);//new HttpPost(availabilityUrl + VERSION_TEXT);

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/soap+xml;charset=UTF-8");
        httpPost.setHeader("my_custom_header2", "bar");
        httpPost.setHeader("my_custom_header3", "foo");

        try {
            entity = new StringEntity(requestXml, "application/xml", HTTP.UTF_8);
            httpPost.setEntity(entity);
            response = httpclient.execute(httpPost);
            assertNotNull("Did not get a response ", response);
            HttpEntity resEntity = response.getEntity();
            assertNotNull("Response is empty", resEntity);
            BufferedReader rd = new BufferedReader(new InputStreamReader(resEntity.getContent()));
            String result = "";
            String line;
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            System.out.println(result);
            assertTrue("Response is empty", !"".equals(result));
        } catch (Exception e) {
            fail("Errors occurred while sending POST request: " + e.getMessage());
        }
    }

}
