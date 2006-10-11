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
import java.io.IOException;

public class ReactorTester {

    private Reactor r = null;

    public static void main(String[] args) throws Exception {
        ReactorTester rt = new ReactorTester();
        rt.runDemo();
    }

    private void runDemo() throws IOException {

        r = Reactor.createReactor(null, 9001, false,
            new HttpService() {

                public void handleRequest(HttpRequest request) {
                    try {
                        System.out.println("Processing Request : " + request);
                        // create new HttpRequest
                        HttpRequest forwardReq = new HttpRequest(
                            new URL("http://localhost:9000/axis2/services/SimpleStockQuoteService"));

                        Util.copyStreams(request.getInputStream(), forwardReq.getOutputStream());
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

}
