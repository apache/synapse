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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.commons.io.IOUtils;
import org.apache.synapse.transport.testkit.message.IncomingMessage;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.util.LogManager;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;

@Name("jetty")
@SuppressWarnings("serial")
public class JettyAsyncEndpoint implements AsyncEndpoint<byte[]> {
    private LogManager logManager;
    private JettyServer server;
    private HttpHandler handler;
    BlockingQueue<IncomingMessage<byte[]>> queue;
    
    @SuppressWarnings("unused")
    private void setUp(LogManager logManager, JettyServer server, HttpChannel channel) throws Exception {
        this.logManager = logManager;
        this.server = server;
        final String path = "/" + channel.getServiceName();
        queue = new LinkedBlockingQueue<IncomingMessage<byte[]>>();
        handler = new AbstractHttpHandler() {
            public void handle(String pathInContext, String pathParams,
                    HttpRequest request, HttpResponse response) throws HttpException,
                    IOException {
                
                byte[] data = IOUtils.toByteArray(request.getInputStream());
                logRequest(request, data);
                if (pathInContext.equals(path)) {
                    ContentType contentType;
                    try {
                        contentType = new ContentType(request.getContentType());
                    } catch (ParseException ex) {
                        throw new HttpException(500, "Unparsable Content-Type");
                    }
                    queue.add(new IncomingMessage<byte[]>(contentType, data));
                    request.setHandled(true);
                }
            }
        };
        server.getContext().addHandler(handler);
        handler.start();
    }
    
    void logRequest(HttpRequest request, byte[] data) throws IOException {
        OutputStream out = logManager.createLog("jetty");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out), false);
        for (Enumeration<?> e = request.getFieldNames(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            for (Enumeration<?> e2 = request.getFieldValues(name); e2.hasMoreElements(); ) {
                pw.print(name);
                pw.print(": ");
                pw.println((String)e2.nextElement());
            }
        }
        pw.println();
        pw.flush();
        out.write(data);
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        handler.stop();
        server.getContext().removeHandler(handler);
        server = null;
        queue = null;
        logManager = null;
    }
    
    public IncomingMessage<byte[]> waitForMessage(int timeout) throws Throwable {
        return queue.poll(timeout, TimeUnit.MILLISECONDS);
    }
}
