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

import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;

public class IOHandler {

    class Message {
        Message(String msg) {
            write(msg.getBytes());
        }

        private ByteBuffer buffer = ByteBuffer.allocate(1024);

        public InputStream getInputStream() {
            buffer.position(0);
            return new InputStream() {
                public synchronized int read() throws IOException {
                    if (!buffer.hasRemaining()) {
                        return -1;
                    }
                    return buffer.get();
                }
                public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                    len = Math.min(len, buffer.remaining());
                    buffer.get(bytes, off, len);
                    return len;
                }
            };
        }

        private int writePos;

        public void write(byte[] data) {
            buffer.position(writePos);
            buffer.put(data);
            writePos = buffer.position();
        }
    }

    class B implements Runnable {
        Message m;
        B(Message m) {
            this.m = m;
        }
        public void run() {
            try {
                while (true) {
                    Thread.sleep(1000);
                    m.write(("Message : " + new Date()).getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class C implements Runnable {
        InputStream is = null;
        byte[] data = new byte[1024];
        C(InputStream is) {
            this.is = is;
        }
        public void run() {
            try {
                while (true) {
                    Thread.sleep(1000);
                    int r = is.read(data);
                    if (r > 0) {
                        System.out.println("data : \n" + Util.dumpAsHex(data, r));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void test() {
        Message m = new Message("Hello World");
        new Thread(new B(m)).start();
        new Thread(new C(m.getInputStream())).start();
    }

    public static void main(String[] args) {
        new IOHandler().test();
    }
}
