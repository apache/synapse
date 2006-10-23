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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.io.InputStream;

public abstract class GenericIOHandler implements Runnable {

    private static final Log log = LogFactory.getLog(GenericIOHandler.class);

    protected SelectionKey sk;
    protected SocketChannel socket;

    protected MessageWriter msgWriter;
    protected HttpService httpService;

    protected ByteBuffer nwReadBuffer   = ByteBuffer.allocate(4096);
    protected ByteBuffer nwWriteBuffer  = ByteBuffer.allocate(4096);
    protected ByteBuffer appReadBuffer  = ByteBuffer.allocate(4096);
    protected ByteBuffer appWriteBuffer = ByteBuffer.allocate(4096);
    protected int nwReadPos, nwWritePos;
    protected int appReadPos, appWritePos;

    private boolean beingProcessed = false;

    public boolean isBeingProcessed() {
        return beingProcessed;
    }

    public synchronized void lock() {
        beingProcessed = true;
    }

    public synchronized void unlock() {
        beingProcessed = false;
    }

    public GenericIOHandler() {}

    protected int readNetworkBuffer(int maxBytes) {

        try {
            // set position within buffer to read from channel
            nwReadBuffer.position(nwReadPos);

            if (nwReadPos == nwReadBuffer.capacity()) {
                ByteBuffer newBuf = ByteBuffer.allocate(nwReadBuffer.capacity() * 2);
                log.debug("Expanding ByteBuffer to " + newBuf.capacity() + " bytes");
                nwReadBuffer.flip();
                nwReadBuffer = newBuf.put(nwReadBuffer);
            }

            // perform read from channel to this location
            // *** Read a maximum of maxBytes ***
            ByteBuffer temp = ByteBuffer.allocate(maxBytes);
            int bytesRead = socket.read(temp);            
            //System.out.println("Read : " + maxBytes + " bytes\n" + Util.dumpAsHex(temp.array(), bytesRead));
            temp.flip();
            nwReadBuffer.put(temp);

            if (bytesRead != -1) {
                if (log.isDebugEnabled()) {
                    log.debug("Read from socket to buffer position: " + nwReadPos + " to: " + nwReadBuffer.position());
                    log.debug(Util.dumpAsHex(nwReadBuffer.array(), nwReadPos));
                }

                // save position for next read
                nwReadPos = nwReadBuffer.position();
                return bytesRead;

            } else {
                // end of stream reached
                log.info("end-of-stream detected and socket closed");
                socket.close();
                sk.cancel();
            }

        } catch (IOException e) {
            log.warn(e.getMessage() + " Closing socket: " + socket);
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            sk.cancel();
        }
        return 0;
    }

    protected void readApplicationBuffer() {

        nwReadBuffer.position(nwReadPos);
        nwReadBuffer.flip();

        appReadBuffer.position(appReadPos);
        while (appReadBuffer.remaining() < nwReadBuffer.limit()) {
            ByteBuffer newBuf = ByteBuffer.allocate(appReadBuffer.capacity() * 2);
            log.debug("Expanding ByteBuffer to " + newBuf.capacity() + " bytes");
            appReadBuffer.flip();
            appReadBuffer = newBuf.put(appReadBuffer);
        }

        appReadBuffer.put(nwReadBuffer);
        appReadPos = appReadBuffer.position();
        appReadBuffer.flip();        
        
        nwReadBuffer.clear();
        nwReadPos = 0;
    }

    protected void writeNetworkBuffer() {
        nwWriteBuffer.position(nwWritePos);
        if (nwWritePos > 0) {
            nwWriteBuffer.compact();
        }

        appWriteBuffer.position(appWritePos);
        appWriteBuffer.flip();

        while (nwWriteBuffer.remaining() < appWriteBuffer.limit()) {
            ByteBuffer newBuf = ByteBuffer.allocate(nwWriteBuffer.capacity() * 2);
            log.debug("Expanding ByteBuffer to " + newBuf.capacity() + " bytes");
            nwWriteBuffer.flip();
            nwWriteBuffer = newBuf.put(nwWriteBuffer);
        }

        nwWriteBuffer.put(appWriteBuffer);
        nwWritePos = nwWriteBuffer.position();
        appWritePos = 0;

        System.out.println("nwWriteBuffer : \n" + Util.dumpAsHex(nwWriteBuffer.array(), nwWritePos));
    }

    protected void writeToNetwork() {
        // write as much as we can
        nwWriteBuffer.position(nwWritePos);
        nwWriteBuffer.flip();
        int total = 0;
        int write = 0;
        do {
            try {
                write = socket.write(nwWriteBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (write > 0) {
                total += write;
            }
        } while (write > 0);

        // compact the left overs
        nwWriteBuffer.compact();
        nwWritePos = nwWriteBuffer.position();
    }

    protected void writeApplicationBuffer() {
        if (!msgWriter.isStreamingBody()) {
            byte[] header = msgWriter.getMessageHeader();

            while (appWriteBuffer.remaining() < header.length) {
                ByteBuffer newBuf = ByteBuffer.allocate(appWriteBuffer.capacity() * 2);
                log.debug("Expanding ByteBuffer to " + newBuf.capacity() + " bytes");
                appWriteBuffer.flip();
                appWriteBuffer = newBuf.put(appWriteBuffer);
            }

            appWriteBuffer.put(header);
            appWritePos = appWriteBuffer.position();
            msgWriter.setStreamingBody(true);

        } else {

            // fill the appWriteBuffer from the message body
            InputStream is = msgWriter.getInputStream();
            appWriteBuffer.position(appWritePos);
            if (appWritePos > 0) {
                appWriteBuffer.compact();
            }

            try {
                while (appWriteBuffer.remaining() > 0) {
                    int c = is.read();
                    if (c == -1) {
                        msgWriter.setStreamingBody(false);
                        break;
                    }
                    appWriteBuffer.put((byte) c);
                }

                appWritePos = appWriteBuffer.position();

                System.out.println("appWriteBuffer : \n" + Util.dumpAsHex(appWriteBuffer.array(), appWritePos));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
