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

public abstract class GenericIOHandler extends AbstractIOHandler {

    private static final Log log = LogFactory.getLog(GenericIOHandler.class);

    protected SelectionKey sk;
    protected SocketChannel socket;

    protected MessageWriter msgWriter;
    protected MessageReader msgReader;
    protected HttpService   httpService;

    protected ByteBuffer nwReadBuffer   = ByteBuffer.allocate(4096);
    protected ByteBuffer nwWriteBuffer  = ByteBuffer.allocate(4096);
    protected ByteBuffer appReadBuffer  = ByteBuffer.allocate(4096);
    protected ByteBuffer appWriteBuffer = ByteBuffer.allocate(4096);

    protected int nwReadPos, nwWritePos;
    protected int appReadPos, appWritePos;

    /** acp
     * Process a ready read event, and reject it if we are unable to process.
     * If content was read in, then it would be processed as well - ie. fire event
     * for received message header, or stream in content received
     */
    protected void processReadyRead() {

        if (msgReader.availableForWrite() == 0) {
            log.trace("reject available read as message reader is full");
            return;
        }

        log.trace("attempting to read into NW a maximum of "
            + msgReader.availableForWrite() + " bytes");

        if (readFromPhysicalNetwork(msgReader.availableForWrite()) >= 0) {

            if (log.isTraceEnabled()) {
                log.trace("read into NW buffer : \n" +
                    Util.dumpAsHex(nwReadBuffer.array(), nwReadPos));
            }

            log.trace("attempt to read NW buffer into App buffer");
            readFromNetworkBuffer();

            if (log.isTraceEnabled()) {
                log.trace("read into App buffer : \n" +
                    Util.dumpAsHex(appReadBuffer.array(), appReadPos));
            }

            log.trace("attempt to process App buffer contents");
            processAppReadBuffer();
        }
    }

    /** acp
     * Read from the physical NW until the NW read buffer is filled up
     * @param maxBytes maximum number of bytes to read from the NW
     * @return the number of bytes actually read
     */
    protected int readFromPhysicalNetwork(int maxBytes) {

        log.debug("attempting to read from the physical NW");

        // set position within buffer to read from channel
        nwReadBuffer.position(nwReadPos);
        int bytesRead = 0;

        try {
            // dont bite more than we can chew.. if nwReadBuffer cannot accomodate what
            // can digest, read only what we can
            if (maxBytes < appReadPos + nwReadPos) {
                return 0;
                
            } else {
                bytesRead = socket.read(nwReadBuffer);
            }

        } catch (IOException e) {
            handleException("Error reading into NW buffer from socket : " + e.getMessage(), e);
        }

        if (bytesRead >= 0) {
            log.debug("read " + bytesRead + " bytes from the physical NW");
            nwReadPos += bytesRead;
            if (log.isTraceEnabled() && bytesRead > 0) {
                log.trace("current nwReadBuffer\n" +
                    Util.dumpAsHex(nwReadBuffer.array(), bytesRead));
            }
            return bytesRead;

        } else {
            // end of stream reached
            log.debug("end-of-stream detected and socket closed");
            sk.cancel();
            try {
                socket.close();
            } catch (IOException e) {}

        }
        return 0;
    }

    /** acp
     * Read from the NW buffer into the App buffer, the complete NW buffer contents
     */
    protected void readFromNetworkBuffer() {

        nwReadBuffer.position(nwReadPos);
        nwReadBuffer.flip();

        appReadBuffer.position(appReadPos);
        while (appReadBuffer.remaining() < nwReadBuffer.limit()) {
            ByteBuffer newBuf = ByteBuffer.allocate(appReadBuffer.capacity() * 2);
            log.debug("expanding appReadBuffer to " + newBuf.capacity() + " bytes");
            appReadBuffer.flip();
            appReadBuffer = newBuf.put(appReadBuffer);
        }

        appReadBuffer.put(nwReadBuffer);
        appReadPos = appReadBuffer.position();
        appReadBuffer.flip();

        nwReadBuffer.clear();
        nwReadPos = 0;
    }
    
    /** acp
     * The Incoming and Outgoing Handlers should implement this method, and provide
     * thier custom implementations to handle received http messages
     */
    abstract protected void processAppReadBuffer();

    /** acp
     * Process a ready write event
     * @param closeConnectionIfDone close the connection if all data is written
     * to the wire and a connection close has been requested after the write
     */
    protected void processReadyWrite(boolean closeConnectionIfDone) {

        log.debug("attempt to write to the App buffer from the message writer");
        writeToApplicationBuffer();

        log.debug("attempt to write to the NW buffer from the App buffer");
        writeToNetworkBuffer();

        log.debug("attempt to write to the physical NW from the NW buffer");
        writeToPhysicalNetwork();

        if (nwWritePos == 0 && appWritePos == 0 && !msgWriter.isStreamingBody()) {
            // if both our App & NW buffers are empty, and body stream has ended
            log.debug("message written completely to the wire");
            if (closeConnectionIfDone && msgWriter.isConnectionClose()) {
                log.debug("closing connection normally as connection-close requested");
                sk.cancel();
                try {
                    socket.close();
                } catch (IOException e) {}
            } else {
                // response has been written completely
                // now read response or result code
                sk.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    /** acp
     * Reads from the msgWriter and writes to the application buffer
     * If the message header has not been read, the complete header is read (which
     * is known not to block) and written at once, expanding the application buffer
     * as required. If the header has been written, the msgWriter would be in the
     * body-streaming mode, in which case, it is attempted to read until the application
     * buffer is filled up - or the end of stream is reached
     */
    protected void writeToApplicationBuffer() {

        if (!msgWriter.isStreamingBody()) {
            log.debug("attempting to write message header into App buffer");
            byte[] header = msgWriter.getMessageHeader();

            appWriteBuffer.position(appWritePos);
            while (appWriteBuffer.remaining() < header.length) {
                ByteBuffer newBuf = ByteBuffer.allocate(appWriteBuffer.capacity() * 2);
                log.debug("expanding App buffer to " + newBuf.capacity() + " bytes");
                appWriteBuffer.flip();
                appWriteBuffer = newBuf.put(appWriteBuffer);
            }

            appWriteBuffer.put(header);
            appWritePos = appWriteBuffer.position();
            msgWriter.setStreamingBody(true);   // switch into body streaming mode

        } else {

            log.debug("attempting to write from message body into App buffer");

            InputStream is = msgWriter.getInputStream();
            // does this message have a body? i.e. this could be a GET
            if (is == null) {
                log.debug("message has an empty body");
                msgWriter.setStreamingBody(false);   // indicate end of body
                return;
            }
            appWriteBuffer.position(appWritePos);

            try {
                while (appWriteBuffer.remaining() > 0) {
                    int c = is.read();
                    if (c == -1) {
                        msgWriter.setStreamingBody(false);
                        log.debug("end of message body stream detected");
                        break;
                    }
                    appWriteBuffer.put((byte) c);
                }

                appWritePos = appWriteBuffer.position();

            } catch (IOException e) {
                handleException("Error reading message body stream : " + e.getMessage(), e);
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("current appWriteBuffer : \n" +
                Util.dumpAsHex(appWriteBuffer.array(), appWritePos));
        }
    }

    /** acp
     * Write all of the contents of the App buffer into the NW buffer, expanding
     * it as required
     * TODO handle SSL later
     */
    protected void writeToNetworkBuffer() {

        appWriteBuffer.position(appWritePos);
        appWriteBuffer.flip();

        // expand NW buffer to hold App buffer contents
        while (nwWriteBuffer.remaining() < appWriteBuffer.limit()) {
            ByteBuffer newBuf = ByteBuffer.allocate(nwWriteBuffer.capacity() * 2);
            log.debug("expanding NW buffer to " + newBuf.capacity() + " bytes");
            nwWriteBuffer.flip();
            nwWriteBuffer = newBuf.put(nwWriteBuffer);
        }

        nwWriteBuffer.position(nwWritePos);
        nwWriteBuffer.put(appWriteBuffer);
        nwWritePos = nwWriteBuffer.position();

        // clear App buffer as we wrote all of it to the NW buffer
        appWritePos = 0;
        appWriteBuffer.clear();

        if (log.isTraceEnabled()) {
            log.trace("current nwWriteBuffer : \n" +
                Util.dumpAsHex(nwWriteBuffer.array(), nwWritePos));
        }
    }

    /** acp
     * Write the contents of the NW buffer into the physical socket
     */
    protected void writeToPhysicalNetwork() {
        // write as much as we can from our NW buffer, without blocking of course!
        nwWriteBuffer.position(nwWritePos);
        nwWriteBuffer.flip();
        int write = 0;
        do {
            try {
                write = socket.write(nwWriteBuffer);
            } catch (IOException e) {
                handleException("Error writing to socket : " + e.getMessage(), e);
            }
        } while (write > 0 && nwWriteBuffer.remaining() > 0);

        // compact any left overs and capture new position
        nwWriteBuffer.compact();
        nwWritePos = nwWriteBuffer.position();
    }

    /**
     * Handle an exception encountered. TODO need to streamline error handling and cleanup
     * @param msg a message indicating the exception
     * @param e the Exception thrown
     */
    protected void handleException(String msg, Exception e) {
        log.error(msg, e);

        if (sk.isValid()) {
            sk.cancel();
        }
        try {
            if (socket != null && socket.isOpen()) {
                socket.close();
            }
        } catch (IOException e1) {}

        //throw new NHttpException(msg, e);
    }
    
}
