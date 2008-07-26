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
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.synapse.transport.testkit.listener.DefaultOperationDispatcher;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.apache.synapse.transport.testkit.listener.BinaryPayloadSender;
import org.apache.synapse.transport.testkit.listener.RESTSender;

public class JavaNetSender extends BinaryPayloadSender implements RESTSender {
    public JavaNetSender() {
        super("java.net");
    }
    
    @Override
    public void sendMessage(ListenerTestSetup setup, String endpointReference, String contentType, byte[] content) throws Exception {
        URLConnection connection = new URL(endpointReference).openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", contentType);
        OutputStream out = connection.getOutputStream();
        out.write(content);
        out.close();
        InputStream in = connection.getInputStream();
        IOUtils.copy(in, System.out);
        in.close();
    }
    
    public void sendMessage(String endpointReference) throws Exception {
        URLConnection connection = new URL(endpointReference + "/" + DefaultOperationDispatcher.DEFAULT_OPERATION_NAME).openConnection();
        connection.setDoInput(true);
        InputStream in = connection.getInputStream();
        IOUtils.copy(in, System.out);
        in.close();
    }
}