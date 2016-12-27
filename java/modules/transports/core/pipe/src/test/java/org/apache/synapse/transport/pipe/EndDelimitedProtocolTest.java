/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.pipe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class EndDelimitedProtocolTest extends TestCase {
    public void test() throws IOException {
        byte delimiter = 0;
        Random random = new Random();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<byte[]> messages = new LinkedList<byte[]>();
        for (int i=0; i<20; i++) {
            int size = 64 + random.nextInt(2048);
            byte[] data = new byte[size];
            for (int j=0; j<size; j++) {
                data[j] = (byte)(32 + random.nextInt(96));
            }
            out.write(data);
            out.write(delimiter);
            messages.add(data);
        }
        EndDelimitedProtocol protocol = new EndDelimitedProtocol();
        protocol.setDelimiter(delimiter);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProtocolDecoder decoder = protocol.createProtocolDecoder();
        byte[] readBuffer = new byte[1024];
        while (true) {
            int remaining = messages.size();
            while (decoder.inputRequired()) {
                int c = in.read(readBuffer);
                if (c == -1) {
                    assertTrue("Expected " + remaining + " more messages", remaining == 0);
                    return;
                }
                decoder.decode(readBuffer, 0, c);
            }
            assertTrue("Didn't expecte any more messages", remaining != 0);
            byte[] actual = decoder.getNext();
            byte[] expected = messages.remove(0);
            assertEquals(new String(expected, "us-ascii"), new String(actual, "us-ascii"));
        }
    }
}
