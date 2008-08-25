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

package org.apache.synapse.transport.nhttp;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.commons.io.IOUtils;
import org.apache.synapse.transport.testkit.message.IncomingMessage;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;

@Name("jetty")
@SuppressWarnings("serial")
public class JettyAsyncEndpoint implements AsyncEndpoint<byte[]> {
    private JettyServer server;
    private HttpHandler handler;
    BlockingQueue<IncomingMessage<byte[]>> queue;
    
    @SuppressWarnings("unused")
    private void setUp(JettyServer server, HttpChannel channel) throws Exception {
        this.server = server;
        final String path = "/" + channel.getServiceName();
        queue = new LinkedBlockingQueue<IncomingMessage<byte[]>>();
        handler = new AbstractHttpHandler() {
            public void handle(String pathInContext, String pathParams,
                    HttpRequest request, HttpResponse response) throws HttpException,
                    IOException {
                
                if (pathInContext.equals(path)) {
                    ContentType contentType;
                    try {
                        contentType = new ContentType(request.getContentType());
                    } catch (ParseException ex) {
                        throw new HttpException(500, "Unparsable Content-Type");
                    }
                    byte[] data = IOUtils.toByteArray(request.getInputStream());
                    queue.add(new IncomingMessage<byte[]>(contentType, data));
                    request.setHandled(true);
                }
            }
        };
        server.getContext().addHandler(handler);
        handler.start();
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        handler.stop();
        server.getContext().removeHandler(handler);
        server = null;
        queue = null;
    }
    
    public IncomingMessage<byte[]> waitForMessage(int timeout) throws Throwable {
        return queue.poll(timeout, TimeUnit.MILLISECONDS);
    }
}
