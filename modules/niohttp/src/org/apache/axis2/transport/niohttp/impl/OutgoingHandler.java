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
import java.nio.channels.SocketChannel;

/**
 * This handler owns sending of a httpMessage to an external endpoint using a WriteHandler
 * and reading back the response. This does not handle persistent/pipelined connections
 */
public class OutgoingHandler implements Runnable {

    private static final Log log = LogFactory.getLog(OutgoingHandler.class);

    private SelectionKey sk;
    private SocketChannel socket;
    private HttpService httpService;

    private WriteHandler writeHandler = new WriteHandler();
    private ReadHandler readHandler = new ReadHandler(false); /* no response from this TODO*/
    private Runnable callback = null;

    OutgoingHandler(SocketChannel socket, SelectionKey sk, HttpRequest request, HttpService httpService) {
        this.httpService = httpService;
        this.socket = socket;
        this.sk = sk;
        writeHandler.setMessage(request.getBuffer(), true /* connection close */);
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
                if (writeHandler.handle(socket)) {
                    log.debug("\tRequest written completely");
                    // response has been written completely
                    // now read response or at least result code
                    sk.interestOps(SelectionKey.OP_READ);
                }

            } else if (sk.isReadable()) {
                log.debug("\tIncomingHandler run() - READABLE");
                if (readHandler.handle(socket, sk)) {
                    log.debug("\tResponse read completely");
                    // if httpMessage processing is complete
                    log.debug("\tFire event for response read");
                    httpService.handleResponse((HttpResponse) readHandler.getHttpMessage(), callback);

                    // if pipelining is used
                    /*if (!readHandler.isConnectionClose()) {
                        // prepare to read another httpMessage
                        readHandler.reset(this);
                        log.debug("\thandler reset");
                    }*/
                    socket.close();
                    sk.cancel();
                    log.debug("Socket closed and SelectionKey cancelled");
                }
            }
        }
        catch (IOException e) {
            log.error("Error in OutGoingHandler : " + e.getMessage(), e);
        }
    }
}