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

package org.apache.synapse.transport.vfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.mail.internet.ContentType;

import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.name.Name;

@Name("java.io")
public class VFSClient implements AsyncTestClient<byte[]> {
    private File requestFile;
    
    @SuppressWarnings("unused")
    private void setUp(VFSFileChannel channel) {
        requestFile = channel.getRequestFile();
    }
    
    public ContentType getContentType(ClientOptions options, ContentType contentType) {
        return contentType;
    }

    public void sendMessage(ClientOptions options, ContentType contentType, byte[] message) throws Exception {
        OutputStream out = new FileOutputStream(requestFile);
        out.write(message);
        out.close();
    }
}