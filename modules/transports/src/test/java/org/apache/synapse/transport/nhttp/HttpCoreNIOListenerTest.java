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

import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.transport.TransportListenerTestTemplate;

public class HttpCoreNIOListenerTest extends TransportListenerTestTemplate {
    @Override
    protected TransportInDescription createTransportInDescription() {
        TransportInDescription trpInDesc = new TransportInDescription("http");
        trpInDesc.setReceiver(new HttpCoreNIOListener());
        return trpInDesc;
    }

    @Override
    protected void sendMessage(String endpointReference, String contentType, byte[] content) throws Exception {
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
}
