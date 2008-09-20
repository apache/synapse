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

package org.apache.synapse.transport.testkit.http;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.synapse.transport.testkit.message.IncomingMessage;
import org.apache.synapse.transport.testkit.name.Name;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;

@Name("jetty")
public abstract class JettyAsyncEndpoint<M> extends JettyEndpoint implements AsyncEndpoint<M> {
    private BlockingQueue<IncomingMessage<M>> queue;
    
    @SuppressWarnings("unused")
    private void setUp() throws Exception {
        queue = new LinkedBlockingQueue<IncomingMessage<M>>();
    }
    
    @Override
    protected void handle(String pathParams, HttpRequest request, HttpResponse response)
            throws HttpException, IOException {
        
        queue.add(handle(request));
    }
    
    protected abstract IncomingMessage<M> handle(HttpRequest request) throws HttpException, IOException;
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        queue = null;
    }
    
    public void clear() throws Exception {
        queue.clear();
    }

    public IncomingMessage<M> waitForMessage(int timeout) throws Throwable {
        return queue.poll(timeout, TimeUnit.MILLISECONDS);
    }
}
