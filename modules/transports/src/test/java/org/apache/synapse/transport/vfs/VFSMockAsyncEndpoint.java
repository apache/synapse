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
import java.io.InputStream;

import javax.mail.internet.ContentType;

import org.apache.commons.io.IOUtils;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;

import de.schlichtherle.io.FileInputStream;

public class VFSMockAsyncEndpoint implements AsyncEndpoint<ByteArrayMessage> {
    private final VFSFileChannel channel;
    private final ContentType contentType;
    
    public VFSMockAsyncEndpoint(VFSFileChannel channel, ContentType contentType) {
        this.channel = channel;
        this.contentType = contentType;
    }
    
    public ByteArrayMessage waitForMessage(int timeout) throws Throwable {
        long time = System.currentTimeMillis();
        File file = channel.getRequestFile();
        while (System.currentTimeMillis() < time + timeout) {
            if (file.exists()) {
                InputStream in = new FileInputStream(file);
                try {
                    return new ByteArrayMessage(contentType, IOUtils.toByteArray(in));
                } finally {
                    in.close();
                }
            }
            Thread.sleep(100);
        }
        return null;
    }

    public void remove() throws Exception {
        // Nothing to do
    }
}
