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

import org.apache.axis2.transport.niohttp.impl.io.PipedOutputStream;
import org.apache.axis2.transport.niohttp.impl.io.PipedInputStream;
import org.apache.axis2.transport.niohttp.impl.io.ChunkedInputStream;
import org.apache.axis2.transport.niohttp.impl.io.ContentLengthInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.InputStream;

/**
 * TODO
 */
public class MessageReader {

    private static final Log log = LogFactory.getLog(MessageReader.class);

    /** Are we at the streaming of the body state now? */
    private boolean streamingBody = false;
    /** Parsing message as a Http request or a response? */
    private boolean requestMode = true;
    /** Buffer position indicator while parsing the header */
    private int pos = 0;

    /** The http request or response being read */
    private HttpMessage httpMessage;
    /** The piped input stream into the message body */
    private PipedInputStream  bodyInStream;
    /** The piped output stream into the message body */
    private PipedOutputStream bodyOutStream;

    /** The InputStream created to allow clients to read the body */
    private InputStream clientInputStream;

    public MessageReader(boolean requestMode) {
        this.requestMode = requestMode;
        if (requestMode) {
            httpMessage = new HttpRequest();
        } else {
            httpMessage = new HttpResponse();
        }
        reset();
    }

    /**
     * Reset the internal state of this instance for reuse - with keepalive/pipelined connections
     */
    public void reset() {
        pos = 0;
        streamingBody = false;
        bodyInStream  = new PipedInputStream();
        try {
            bodyOutStream = new PipedOutputStream(bodyInStream);
        } catch (IOException e) {
            // this can never occur with the above two new piped in/out streams
        }
    }

    /**
     * This is the main method into the message reader from the Incoming/Outgoing handlers
     * This method digests the contents of this buffer parsed in, and returns the position
     * upto which the content has been digested. The caller can safely discard contents upto
     * this position, but should not not discard anything else. The content thus left over
     * should be passed at the next invocation.
     *
     * This method does <b>NOT</b> rely on the buffer.position() at the time the buffer is
     * passed in, but considers the complete buffer from position() to limit()
     *
     * @param buffer
     * @return the position upto which the content was digested from the start of the buffer
     */
    public int process(ByteBuffer buffer) throws IOException, NHttpException {        
        if (!streamingBody) {
            int bodyStart = getHeaderEndPosition(buffer);
            if (bodyStart != -1) {
                // we have the complete header in this buffer now
                digestHeader(buffer);

                // have we already read any of the body?
                if (bodyStart < buffer.limit()) {
                    // digest what we can and return the position at which we stop

                    int size = bodyOutStream.availableForWrite();
                    if ((buffer.limit() - bodyStart) < size) {
                        size = buffer.limit() - bodyStart;
                    }
                    bodyOutStream.write(buffer.array(), bodyStart, size);
                    return bodyStart + size;
                }
                return bodyStart;
            } else {
                return 0;
            }
        } else {
            // digest what we can and return the position at which we stopped
            int size = bodyOutStream.availableForWrite();
            if (buffer.limit() < size) {
                size = buffer.limit();
            }
            byte[] bytes = buffer.array();
            bodyOutStream.write(bytes, 0, size);
            return size;
        }
    }

    /**
     * Digest and populate the HttpMessage with the header information as contained
     * in this buffer
     *
     * RFC 2616
     * Request  = Request-Line            ; Section 5.1
     *          *(( general-header        ; Section 4.5
     *           | request-header         ; Section 5.3
     *           | entity-header ) CRLF)  ; Section 7.1
     *          CRLF
     *           [ message-body ]         ; Section 4.3
     *
     * Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
     *
     * Response = Status-Line             ; Section 6.1
     *          *(( general-header        ; Section 4.5
     *           | response-header        ; Section 6.2
     *           | entity-header ) CRLF)  ; Section 7.1
     *          CRLF
     *           [ message-body ]         ; Section 7.2
     *
     * Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
     *
     *  @param buffer
     */
    private void digestHeader(ByteBuffer buffer) throws NHttpException {
        // begin from the start of the buffer
        buffer.position(0);

        // read message line (i.e. request/status line)
        String messageLine = null;
        while (messageLine == null || messageLine.length() == 0) {
            // this effectively skips any blank lines
            messageLine = getNextLine(buffer);
        }
        String[] parts = messageLine.split("\\s");
        if (parts.length != 3) {
            throw new NHttpException("Invalid " +
                (requestMode ? "request" : "response") + " line : " + messageLine);
        }

        if (requestMode) {
            HttpRequest req = (HttpRequest) httpMessage;
            req.setMethod(parts[0]);
            req.setPath(parts[1]);
            req.setProtocol(parts[2]);
        } else {
            HttpResponse res = (HttpResponse) httpMessage;
            res.setVersion(parts[0]);
            res.getStatus().setCode(Integer.parseInt(parts[1]));
            res.getStatus().setMessage(parts[2]);
        }

        do {
            messageLine = getNextLine(buffer);
            if (messageLine != null && messageLine.length() > 0) {
                digestHeaderLine(messageLine);
            }
        } while (messageLine != null && messageLine.length() > 0);

        // prepare to allow clients to read the message body from the PipedInputStream
        createClientInputStream();
    }

    /**
     * Create the clientInputStream to allow clients to read the message body
     */
    private void createClientInputStream() {
        if (httpMessage.isChunked()) {
            clientInputStream = new ChunkedInputStream(bodyInStream);
        } else if (httpMessage.getContentLength() > 0) {
            clientInputStream = new ContentLengthInputStream(
                bodyInStream,  httpMessage.getContentLength());
        } else {
            throw new UnsupportedOperationException("Unsupported message body stream");
        }
        httpMessage.setInputStream(clientInputStream);
    }

    /**
     * NOTE: TODO support multiline headers. For now assume single lines only
     * Add the header specified in this line to the HttpMessage being built
     * @param messageLine an http header line
     */
    private void digestHeaderLine(String messageLine) {
        int colon = messageLine.indexOf(':');
        if (colon != -1) {
            httpMessage.addHeader(messageLine.substring(0, colon), messageLine.substring(colon+2));
        }
    }

    /**
     * NOTE: This method assumes the current position into the buffer. Use with care
     *
     * Reads upto the next CRLF and returns it as a String (skipping CRLF, and positioning
     * past the CRLF for the next read)
     *
     * @param buffer
     * @return the next line from the current position of the buffer, or null if not found
     */
    private String getNextLine(ByteBuffer buffer) {
        StringBuffer sb = new StringBuffer();
        int pos = buffer.position();
        int end = buffer.limit();

        // 0 1 2 3 4 5 6 7 8
        // . . . C L . . . E
        // . . . P
        while (pos + 2 < end &&
            buffer.get(pos) != Constants.CR && buffer.get(pos + 1) != Constants.LF) {
            sb.append((char) buffer.get(pos++));
        }

        if (pos + 2 < end &&
            buffer.get(pos) == Constants.CR && buffer.get(pos + 1) == Constants.LF) {
            buffer.position(pos+2); // position next read to skip CRLF we encountered
            return sb.toString();
        } else {
            return null;
        }
    }

    /**
     * If the header of the message has not yet been read, checks if the header has
     * been rceived completely in the buffer passed in. This does not change the buffer
     * but merely looks for the presence of the header-body separator (blank line) and
     * returns its position. (i.e. start of the body)
     *
     * e.g
     * 0 1 2 3 4 5 6 7 8 9
     * H H H C L C L B B B . .
     * returns 7
     *
     * @param buffer
     * @return position of the end of the header (i.e. start of the body) if found, or -1
     */
    private int getHeaderEndPosition(ByteBuffer buffer) {
        while (pos + 3 < buffer.limit()) {
             if (buffer.get(pos) == Constants.CR &&
                buffer.get(pos + 1) == Constants.LF &&
                buffer.get(pos + 2) == Constants.CR &&
                buffer.get(pos + 3) == Constants.LF) {
                 break;
             } else {
                pos++;
             }
        }
        if (pos < buffer.limit()) {
            streamingBody = true;   // prepare to stream body content
            return pos + 4;
        }
        return -1;
    }

    /**
     * Return an InputStream to read the content of the message body. The underlying
     * implementation uses a slightly modified version of a java.io.PipedInputStream
     * and hence the calls to read from this stream will block if content becomes
     * unavailable at the producer (i.e. socket read). As this stream reads directly
     * off the socket, without buffering the complete message into memory, it should be
     * possible to process messages of large size without an overhead.
     *
     * @return an InputStream into the message body
     */
    public InputStream getBodyInputStream() {
        return clientInputStream;
    }

    /**
     * Are we currently in body streaming state?
     * @return true if state is body streaming
     */
    public boolean isStreamingBody() {
        return streamingBody;
    }

    /**
     * Return the HttpMessage being digested
     * @return the current HttpMessage being digested
     */
    public HttpMessage getHttpMessage() {
        return httpMessage;
    }

    /**
     * How many bytes could be written to the body OS without blocking
     * @return number of bytes that can be written to the body OS
     */
    public int availableForWrite() {
        return bodyOutStream.availableForWrite();
    }
}
