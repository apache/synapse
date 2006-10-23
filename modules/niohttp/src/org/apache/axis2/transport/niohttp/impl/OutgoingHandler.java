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

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * This handler owns sending of a httpMessage to an external endpoint using a WriteHandler
 * and reading back the response. This does not handle persistent/pipelined connections
 */
public class OutgoingHandler extends GenericIOHandler implements Runnable {

    private static final Log log = LogFactory.getLog(OutgoingHandler.class);

    private Runnable callback = null;
    private MessageReader msgReader = new MessageReader(false);

    OutgoingHandler(SocketChannel socket, SelectionKey sk, HttpRequest request, HttpService httpService) {
        this.httpService = httpService;
        this.socket = socket;
        this.sk = sk;
        request.getWireBuffer().position(0);
        if (!request.isChunked()) {
            request.addHeader(Constants.CONTENT_LENGTH, Integer.toString(request.getBuffer().limit()));
        }

        msgWriter = new MessageWriter(true, request);
        //writeHandler.setMessage(request.getWireBuffer(), true /* connection close */);
    }

    public Runnable getCallback() {
        return callback;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    public void run() {
        try {
            if (sk.isConnectable() && socket.finishConnect()) {
                log.debug("\tIncomingHandler run() - CONNECTABLE and CONNECTED");
                sk.interestOps(SelectionKey.OP_WRITE);

            } else if (sk.isWritable()) {
                log.debug("\tIncomingHandler run() - WRITEABLE");

                writeApplicationBuffer();
                writeNetworkBuffer();
                writeToNetwork();

                if (nwWritePos == 0 && appWritePos == 0 && !msgWriter.isStreamingBody()) {
                    log.debug("\tRequest written completely");
                    // response has been written completely
                    // now read response or at least result code
                    sk.interestOps(SelectionKey.OP_READ);
                }

            } else if (sk.isReadable()) {
                log.debug("\tOutgoingHandler run() - READABLE");

                if (msgReader.availableForWrite() == 0) {
                    return; // reject read
                }

                if (readNetworkBuffer(msgReader.availableForWrite()) > 0) {
                    //System.out.println("NW Buffer read : \n" + Util.dumpAsHex(nwReadBuffer.array(), nwReadPos));
                    readApplicationBuffer();
                    
                    //System.out.println(Thread.currentThread().getName() + " Processing App Buffer : \n" + Util.dumpAsHex(appReadBuffer.array(), appReadPos));
                    processAppReadBuffer();
                }
            }
        }
        catch (IOException e) {
            log.error("Error in OutGoingHandler : " + e.getMessage(), e);
        } finally{
            if (isBeingProcessed())
                unlock();
        }
    }

    private void processAppReadBuffer() {
        boolean readHeader = msgReader.isStreamingBody();

        try {
            int pos = msgReader.process(appReadBuffer);
            // if the handler digested any bytes, discard and compact the buffer
            if (pos > 0) {
                appReadBuffer.position(pos);
                appReadBuffer.compact();
                appReadPos = appReadBuffer.position();
            }

            // if we hadn't read the full header earlier, and read it just now
            if (!readHeader && msgReader.isStreamingBody()) {
                log.debug("\tFire event for received HttpResponse");
                unlock();
                httpService.handleResponse((HttpResponse) msgReader.getHttpMessage(), callback);
            }

            /*socket.close();
            sk.cancel();
            log.debug("Socket closed and SelectionKey cancelled");*/

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NHttpException e) {
            e.printStackTrace();
        }
    }

}