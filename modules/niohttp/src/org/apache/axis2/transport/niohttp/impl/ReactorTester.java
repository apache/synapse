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
package org.apache.axis2.transport.niohttp.impl;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.extensions.TestSetup;
import junit.extensions.RepeatedTest;

import java.net.URL;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.ByteBuffer;

public class ReactorTester extends TestCase {

    private static Reactor reactor, synapseReactor = null;
    private static Thread rt = null;

    public ReactorTester(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new RepeatedTest(new ReactorTester("testChunkedPostWithStreaming"), 100));
        suite.addTest(new RepeatedTest(new ReactorTester("testChunkedPostWithoutStreaming"), 100));
        suite.addTest(new RepeatedTest(new ReactorTester("testContentLengthPostWithStreaming"), 100));
        suite.addTest(new RepeatedTest(new ReactorTester("testContentLengthPostWithoutStreaming"), 100));
        //suite.addTest(new RepeatedTest(new ReactorTester("testSimpleGet"), 100));

        TestSetup setup = new TestSetup(suite) {
            protected void setUp() throws Exception {
                System.out.println("One time setup of the reactors");
                reactor = createReactor(9001);
                rt = new Thread(reactor);
                rt.setDaemon(true);
                rt.start();                
            }

            protected void tearDown() throws Exception {
                System.out.println("One time shutdown of the reactors");
                rt.join();
                System.out.println("Reactor shutdown..");
            }
        };
        return setup;
    }

    public void testChunkedPostWithStreaming() throws IOException {

        HttpRequest request = new HttpRequest(new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));
        request.setMethod(Constants.POST);
        request.addHeader("Host", "localhost:9000");
        request.setConnectionClose();
        request.addHeader(Constants.TRANSFER_ENCODING, Constants.CHUNKED);
        request.addHeader(Constants.CONTENT_TYPE, "text/xml; charset=utf-8");
        OutputStream os = request.getOutputStream();

        reactor.send(request, null);

        byte[] bodyBytes = body.getBytes();
        int incr = 32;
        for (int i=0; i<bodyBytes.length ; i += incr) {
            os.write(bodyBytes, i,
                bodyBytes.length - i < incr ? bodyBytes.length - i : incr);
        }
        os.flush();
        os.close();
    }

    public void testChunkedPostWithoutStreaming() throws IOException {

        HttpRequest request = new HttpRequest(new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));
        request.setMethod(Constants.POST);
        request.addHeader("Host", "localhost:9000");
        request.setConnectionClose();
        request.addHeader(Constants.TRANSFER_ENCODING, Constants.CHUNKED);
        request.addHeader(Constants.CONTENT_TYPE, "text/xml; charset=utf-8");
        OutputStream os = request.getOutputStream();

        byte[] bodyBytes = body.getBytes();
        int incr = 32;
        for (int i=0; i<bodyBytes.length ; i += incr) {
            os.write(bodyBytes, i,
                bodyBytes.length - i < incr ? bodyBytes.length - i : incr);
        }
        os.flush();
        os.close();

        reactor.send(request, null);
    }

    public void testContentLengthPostWithoutStreaming() throws IOException {

        HttpRequest request = new HttpRequest(new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));
        request.setMethod(Constants.POST);
        request.addHeader("Host", "localhost:9000");
        request.setConnectionClose();
        request.addHeader(Constants.CONTENT_LENGTH, Integer.toString(body.getBytes().length));
        request.addHeader(Constants.CONTENT_TYPE, "text/xml; charset=utf-8");
        OutputStream os = request.getOutputStream();

        byte[] bodyBytes = body.getBytes();
        int incr = 32;
        for (int i=0; i<bodyBytes.length ; i += incr) {
            os.write(bodyBytes, i,
                bodyBytes.length - i < incr ? bodyBytes.length - i : incr);
        }
        os.flush();
        os.close();

        reactor.send(request, null);
    }

    public void testContentLengthPostWithStreaming() throws IOException {

        HttpRequest request = new HttpRequest(new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));
        request.setMethod(Constants.POST);
        request.addHeader("Host", "localhost:9000");
        request.setConnectionClose();
        request.addHeader(Constants.CONTENT_LENGTH, Integer.toString(body.getBytes().length));
        request.addHeader(Constants.CONTENT_TYPE, "text/xml; charset=utf-8");
        OutputStream os = request.getOutputStream();

        reactor.send(request, null);

        byte[] bodyBytes = body.getBytes();
        int incr = 32;
        for (int i=0; i<bodyBytes.length ; i += incr) {
            os.write(bodyBytes, i,
                bodyBytes.length - i < incr ? bodyBytes.length - i : incr);
        }
        os.flush();
        os.close();
    }

    public void testSimpleGet() throws IOException {
        HttpRequest request = new HttpRequest(
            //new URL("http://localhost:8080/data.jsp")); // content length
            new URL("http://www.asankha.com:80/data.php")); // chunked
        request.setMethod(Constants.GET);
        //request.addHeader("Host", "localhost:8080");
        request.addHeader("Host", "www.asankha.com:80");
        request.setConnectionClose();

        reactor.send(request, null);
    }

    private static Reactor createReactor(int port) throws IOException {

        return Reactor.createReactor(null, port, false, new HttpService() {

            public void handleRequest(HttpRequest request) {
                System.out.println("?");
            }

            public void handleResponse(final HttpResponse response, Runnable callback) {

                Runnable r = new Runnable() {
                    public void run() {
                        System.out.println("Response : " + response);
                        InputStream in = response.getInputStream();

                        try {
                            byte[] buf = new byte[1024];
                            int len;

                            Charset set = Charset.forName("us-ascii");
                            CharsetDecoder dec = set.newDecoder();

                            while ((len = in.read(buf)) > 0) {
                                //System.out.println("Stream : " + Util.dumpAsHex(buf, len));
                                System.out.println("Stream Chunk : " + dec.decode(ByteBuffer.wrap(buf, 0, len)));
                            }
                            in.close();

                            System.out.println("*** TEST PASSED ***");

                        } catch (IOException e) {
                            System.out.println("*** TEST FAILED ***");
                            e.printStackTrace();
                        }


                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
    }

    public void runGenericSynapseUseCase() throws IOException {

        synapseReactor = Reactor.createReactor(null, 9002, false,

            new HttpService() {

                public void handleRequest(HttpRequest request) {
                    try {
                        HttpRequest forwardReq = new HttpRequest(
                            new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));
                        forwardReq.setMethod(Constants.POST);
                        forwardReq.addHeader("Host", "localhost:9000");
                        forwardReq.setConnectionClose();
                        forwardReq.addHeader(Constants.TRANSFER_ENCODING, Constants.CHUNKED);
                        forwardReq.addHeader(Constants.CONTENT_TYPE, "text/xml; charset=utf-8");

                        SimpleCallback cb = new SimpleCallback(request);
                        synapseReactor.send(forwardReq, cb);

                        Util.copyStreams(request.getInputStream(), forwardReq.getOutputStream());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void handleResponse(HttpResponse response, Runnable callback) {
                    System.out.println("Received Response : \n" + response);
                    SimpleCallback cb = (SimpleCallback) callback;
                    cb.setResponse(response);
                    cb.run();
                }
            });

        Thread t = new Thread(synapseReactor);
        t.start();

        /*// wait till the reactor is done
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }


    public class SimpleCallback implements Callback {
        HttpResponse response;
        HttpRequest request;

        SimpleCallback(HttpRequest request) {
            this.request = request;
        }

        public void setResponse(HttpResponse response) {
            this.response = response;
        }

        public void setRequest(HttpRequest request) {
            this.request = request;
        }

        public void run() {
            HttpResponse newResponse = request.createResponse();
            try {
                newResponse.setStatus(ResponseStatus.OK);
                newResponse.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_XML);
                newResponse.addHeader(Constants.TRANSFER_ENCODING, Constants.CHUNKED);
                newResponse.setConnectionClose();

                newResponse.commit();

                Util.copyStreams(response.getInputStream(), newResponse.getOutputStream());
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final String body =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soapenv:Header/>\n" +
            "  <soapenv:Body>\n" +
            "     <m0:getQuote xmlns:m0=\"http://services.samples/xsd\">\n" +
            "        <m0:request>\n" +
            "           <m0:symbol>IBM</m0:symbol>\n" +
            "        </m0:request>\n" +
            "     </m0:getQuote>\n" +
            "  </soapenv:Body>\n" +
            "</soapenv:Envelope>";
}
