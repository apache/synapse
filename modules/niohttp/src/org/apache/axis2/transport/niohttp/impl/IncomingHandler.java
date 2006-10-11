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
public class IncomingHandler implements Runnable {

    private static final Log log = LogFactory.getLog(IncomingHandler.class);

    private SelectionKey sk;
    private SocketChannel socket;

    private ReadHandler incomingHandler = new ReadHandler(true);
    private WriteHandler responseHandler = new WriteHandler();

    private HttpService httpService;

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
        responseHandler.setMessage(response.getBuffer(), response.isConnectionClose());
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
            if (incomingHandler.handle(socket, sk)) {
                log.debug("\tA httpMessage has been read completely");
                // if httpMessage processing is complete
                HttpRequest request = (HttpRequest) incomingHandler.getHttpMessage();
                request.setHandler(this);
                log.debug("\tFire event for received httpMessage");
                httpService.handleRequest(request);

                // if pipelining is used
                if (!incomingHandler.isConnectionClose()) {
                    // prepare to read another httpMessage - reset and reuse
                    incomingHandler.reset();
                    log.debug("\tReset read handler to read next pipelined httpMessage");
                }
            }
        } else if (sk.isWritable()) {
            log.debug("\tIncomingHandler run() - WRITEABLE");
            if (responseHandler.handle(socket)) {
                log.debug("\tThe response has been written completely");
                // response has been written completely
                if (responseHandler.isConnectionClose()) {
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
            }
        } else {
            log.warn("IncomingHandler run(!!!unknown event!!!) : " + sk.readyOps());
        }
    }

}
