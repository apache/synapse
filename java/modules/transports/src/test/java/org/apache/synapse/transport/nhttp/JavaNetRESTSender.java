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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.synapse.transport.testkit.listener.AbstractMessageSender;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.AsyncMessageSender;
import org.apache.synapse.transport.testkit.listener.SenderOptions;
import org.apache.synapse.transport.testkit.message.RESTMessage;
import org.apache.synapse.transport.testkit.server.axis2.DefaultOperationDispatcher;

public class JavaNetRESTSender extends AbstractMessageSender<AsyncChannel<?>> implements AsyncMessageSender<AsyncChannel<?>,RESTMessage> {
    public JavaNetRESTSender() {
        super("java.net");
    }
    
    public void sendMessage(AsyncChannel<?> channel, SenderOptions options, RESTMessage message) throws Exception {
        URLConnection connection = new URL(options.getEndpointReference() + "/" + DefaultOperationDispatcher.DEFAULT_OPERATION_NAME).openConnection();
        connection.setDoInput(true);
        InputStream in = connection.getInputStream();
        IOUtils.copy(in, System.out);
        in.close();
    }
}
