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

    public IncomingHandler(SocketChannel socket, Selector selector,
        HttpService httpService) throws IOException {

        this.msgReader = new MessageReader(true); // set up in 'request' parse mode
        this.httpService = httpService;
        this.socket = socket;
        this.socket.configureBlocking(false);
        sk = socket.register(selector, 0);
        sk.attach(this);
        sk.interestOps(SelectionKey.OP_READ);   // we are only interested to read
        selector.wakeup();
    }

    public void setResponse(HttpResponse response) {
        if (msgWriter == null) {
            msgWriter = new MessageWriter(false, response);
        }
        sk.interestOps(SelectionKey.OP_WRITE);
        sk.selector().wakeup();
    }

    /** acp
     * The main handler routine for incoming requests and responses
     */
    public void run() {

        if (!sk.isValid()) {
            sk.cancel();
            return;
        }
        
        try {
            if (sk.isReadable()) {
                log.debug("readable");
                processReadyRead();

            } else if (sk.isWritable()) {
                log.debug("writable");
                processReadyWrite(true);
            }

        } finally{
            if (isBeingProcessed())
                unlock();
        }
    }

    /** acp
     * Process the App buffer which has been read. If we just read the message
     * header right now, then this method fires the handleRequest() call on the
     * http service passing. However, at this point, the body may not have been
     * read completely.
     */
    protected void processAppReadBuffer() {
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
                log.debug("fire event for received HttpResponse");
                httpService.handleRequest(request);
            }

        } catch (IOException e) {
            handleException("Error piping the received App buffer : " + e. getMessage(), e);
        } catch (NHttpException e) {
            handleException(e.getMessage(), e);
        }
    }

}
