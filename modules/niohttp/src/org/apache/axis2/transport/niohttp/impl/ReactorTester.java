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

import java.net.URL;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.ByteBuffer;

public class ReactorTester {

    private Reactor r = null;

    public static void main(String[] args) throws Exception {
        ReactorTester rt = new ReactorTester();
        //rt.runDemo();
        //rt.simpleGet();
        rt.simplePost();
    }

    private void simplePost() throws IOException {

        HttpRequest request = new HttpRequest(
            new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));
        request.setMethod(Constants.POST);
        request.addHeader("Host", "localhost:9000");
        request.setSecure(true);
        request.setConnectionClose();
        request.addHeader(Constants.TRANSFER_ENCODING, Constants.CHUNKED);
        request.addHeader(Constants.CONTENT_TYPE, "text/xml; charset=utf-8");
        OutputStream os = request.getOutputStream();
        
        r = Reactor.createReactor(null, 9001, false, new HttpService() {
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

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        System.out.println("*** TEST COMPLETED ***");
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        new Thread(r).start();
        r.send(request, null);

        byte[] bodyBytes = body.getBytes();
        int incr = 32;
        for (int i=0; i<bodyBytes.length ; i += incr) {
            os.write(bodyBytes, i,
                bodyBytes.length - i < incr ? bodyBytes.length - i : incr);
        }
        os.flush();
        os.close();
    }

    private void simpleGet() throws IOException {
        HttpRequest request = new HttpRequest(
            //new URL("http://localhost:8080/data.jsp")); // content length
            new URL("http://www.asankha.com:80/data.php")); // chunked
        request.setMethod(Constants.GET);
        //request.addHeader("Host", "localhost:8080");
        request.addHeader("Host", "www.asankha.com:80");
        request.setEmptyBody();
        request.setSecure(true);
        request.setConnectionClose();

        r = Reactor.createReactor(null, 9001, false, new HttpService() {
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

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        System.out.println("*** TEST COMPLETED ***");
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        new Thread(r).start();
        r.send(request, null);
    }



    private void runDemo() throws IOException {

        r = Reactor.createReactor(null, 9001, false,
            new HttpService() {

                public void handleRequest(HttpRequest request) {
                    try {
                        // create new HttpRequest
                        HttpRequest forwardReq = new HttpRequest(
                            new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));

                        //Util.copyStreams(request.getInputStream(), forwardReq.getOutputStream());

                        SimpleCallback cb = new SimpleCallback(request);
                        r.send(forwardReq, cb);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void handleResponse(HttpResponse response, Runnable callback) {
                    System.out.println("Received Response : " + response);
                    SimpleCallback cb = (SimpleCallback) callback;
                    cb.setResponse(response);
                    cb.run();
                }
            });

        new Thread(r).start();
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
                Util.copyStreams(response.getInputStream(), newResponse.getOutputStream());
                newResponse.setStatus(ResponseStatus.OK);
                newResponse.addHeader(Constants.CONTENT_TYPE, Constants.TEXT_XML);
            } catch (IOException e) {
                e.printStackTrace();
            }
            newResponse.commit();
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
