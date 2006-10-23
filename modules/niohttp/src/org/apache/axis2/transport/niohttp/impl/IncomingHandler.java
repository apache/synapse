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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * This is a generic handler, which will delegate events to a httpMessage/response
 * specific handler as necessary. An instance of this class is expected to be
 * dedicated to a single HTTP connection
 *
 * An IncomingHandler created for each accepted connection would be responsible for
 * reading from the newly opened channel, and subsequently firing events when a
 * message has been read and ready for processing. If the httpMessage handling logic then
 * sets a response to this handler (or the corresponding HttpResponse commits itself)
 * this handler will own (and delegate) the sending of that response back.
 */
public class IncomingHandler extends GenericIOHandler {

    private static final Log log = LogFactory.getLog(IncomingHandler.class);

    private MessageReader msgReader = new MessageReader(true);

    public IncomingHandler(SocketChannel socket, Selector selector,
        HttpService httpService) throws IOException {

        this.httpService = httpService;
        this.socket = socket;
        socket.configureBlocking(false);
        // Optionally try first read now
        sk = socket.register(selector, 0);
        sk.attach(this);
        sk.interestOps(SelectionKey.OP_READ);   // we are only interested to read
        selector.wakeup();
    }

    public void setResponse(HttpResponse response) {
        // TODO writeHandler.setMessage(response.getWireBuffer(), response.isConnectionClose());
        sk.interestOps(SelectionKey.OP_WRITE);
        sk.selector().wakeup();
        log.debug("\tIncomingHandler.setResponse()");
    }

    /**
     * The main handler routine
     */
    public void run() {

        if (sk.isReadable()) {
            log.debug("\tIncomingHandler run() - READABLE");
            
            if (msgReader.availableForWrite() == 0) {
                //System.out.println("Reject read");
                return;
            } else {
                //System.out.println("Accept read");
            }

            if (readNetworkBuffer(msgReader.availableForWrite()) > 0) {
                //System.out.println("NW Buffer read : \n" + Util.dumpAsHex(nwReadBuffer.array(), nwReadPos));
                readApplicationBuffer();

                //System.out.println(Thread.currentThread().getName() + " Processing App Buffer : \n" + Util.dumpAsHex(appReadBuffer.array(), appReadPos));
                processAppReadBuffer();
            }

        } else if (sk.isWritable()) {
            log.debug("\tIncomingHandler run() - WRITEABLE");

            log.debug("\tIncomingHandler run() - WRITEABLE");

            writeApplicationBuffer();
            writeNetworkBuffer();
            writeToNetwork();

            if (nwWritePos == 0 && appWritePos == 0 && !msgWriter.isStreamingBody()) {
                log.debug("\tRequest written completely");
                if (msgWriter.isConnectionClose()) {
                    log.debug("\tClosing connection normally");
                    sk.cancel();
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.warn("Error during socket close : " + e.getMessage(), e);
                    }
                } else {
                    // response has been written completely
                    // now read response or at least result code
                    sk.interestOps(SelectionKey.OP_READ);
                }
            }

            /*if (writeHandler.handle(socket)) {
                log.debug("\tThe response has been written completely");
                // response has been written completely
                if (writeHandler.isConnectionClose()) {
                    log.debug("\tClosing connection normally");
                    sk.cancel();
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.warn("Error during socket close : " + e.getMessage(), e);
                    }
                } else {
                    // now we are again interested to read
                    sk.interestOps(SelectionKey.OP_READ);
                }
            }*/
        } else {
            log.warn("IncomingHandler run(!!!unknown event!!!) : " + sk.readyOps());
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
                HttpRequest request = (HttpRequest) msgReader.getHttpMessage();
                request.setHandler(this);
                log.debug("\tFire event for received HttpRequest");
                httpService.handleRequest(request);
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
