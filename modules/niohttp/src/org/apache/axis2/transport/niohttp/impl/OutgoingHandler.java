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
import java.nio.channels.Selector;
import java.nio.channels.ClosedChannelException;

/**
 * This handler owns the asynchronous sending of a httpMessage to an external endpoint
 * and reading back the response. This does not handle persistent/pipelined connections yet
 */
public class OutgoingHandler extends GenericIOHandler {

    private static final Log log = LogFactory.getLog(OutgoingHandler.class);

    /** A runnable callback object that would be invoked once a reply to the message is received */
    private Runnable callback = null;

    OutgoingHandler(SocketChannel socket, Selector selector, HttpService httpService)
        throws ClosedChannelException {
        this.httpService = httpService;
        this.socket = socket;
        this.msgReader = new MessageReader(false);
        this.sk = socket.register(selector, SelectionKey.OP_CONNECT);
        sk.attach(this);
    }

    public void setRequest(HttpRequest request) {
        msgWriter = new MessageWriter(true, request);
    }

    public Runnable getCallback() {
        return callback;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    /** acp
     * The main handler routing for outgoing messages and responses
     */
    public void run() {

        if (!sk.isValid()) {
            sk.cancel();
            return;
        }

        try {
            if (sk.isConnectable() && socket.finishConnect()) {
                log.debug("socket was connectable and now connected");
                sk.interestOps(SelectionKey.OP_WRITE);

            } else if (sk.isWritable()) {
                log.debug("writable");
                processReadyWrite(false);

            } else if (sk.isReadable()) {
                log.debug("readable");
                processReadyRead();
            }
            
        } catch (IOException e) {
            handleException("Error connecting to socket : " + e.getMessage(), e);
        } finally{
            if (isBeingProcessed())
                unlock();
        }
    }

    /** acp
     * Process the App buffer which has been read. If we just read the message
     * header right now, then this method fires the handleResponse() callback
     * to the http service passing the registered callback object as well. However,
     * at this point, the body may not have been read completely.
     */
    protected void processAppReadBuffer() {

        boolean readHeader = msgReader.isStreamingBody();

        try {
            // let MessageReader digest what it can. The returned position is where
            // it digested the buffer, which we can then discard
            int pos = msgReader.process(appReadBuffer);

            if (pos > 0) {
                // if the handler digested any bytes, discard and compact the buffer
                appReadBuffer.position(pos);
                appReadBuffer.compact();
                appReadPos = appReadBuffer.position();
            }

            // if we hadn't read the full header earlier, and read it just now
            if (!readHeader && msgReader.isStreamingBody()) {
                log.debug("fire event for received HttpResponse");
                unlock();
                httpService.handleResponse((HttpResponse) msgReader.getHttpMessage(), callback);
            }

            // if we had read the header before, and finished receiving the streamed
            // body just now, we want to close the stream and cancel the SK if
            // a connection close was mentioned
            if (readHeader && !msgReader.isStreamingBody() && msgReader.isConnectionClose()) {
                log.debug("closing the connection as the body has been received completely" +
                    " and the received response indicated a connection close");
                try {
                    socket.close();
                } catch (IOException e) {}
                sk.cancel();
            }

        } catch (IOException e) {
            handleException("Error piping the received App buffer : " + e. getMessage(), e);
        } catch (NHttpException e) {
            handleException(e.getMessage(), e);
        }
    }

}