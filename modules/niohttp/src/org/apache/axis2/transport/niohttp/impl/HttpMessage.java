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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This represents an abstract HttpMessage - which may be a HttpRequest or a
 * HttpResponse. This class defines the headers and the ByteBuffer that holds
 * the message body in memory, along with an optional starting position within
 * the buffer.
 */
public abstract class HttpMessage {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private static final Log log = LogFactory.getLog(HttpMessage.class);

    /**
     * http headers of this message
     */
    protected Map headers = new HashMap();
    /**
     * holder of the body content of this message
     */
    protected ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);

    /**
     * A flag to detect if the getOutputStream() caller did not properly close() the stream
     */
    protected boolean outputStreamOpened = false;

    public String getHeader(String name) {
        return (String) headers.get(name);
    }

    public Map getHeaders() {
        return headers;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public boolean isChunked() {
        return Constants.CHUNKED.equals(headers.get(Constants.TRANSFER_ENCODING));
    }

    /**
     * Does this message specify a connection-close?
     * @return true if connection should be closed
     */
    public boolean isConnectionClose() {
        return Constants.CLOSE.equals(headers.get(Constants.CONNECTION));
    }

    public void setConnectionClose() {
        headers.put(Constants.CONNECTION, Constants.CLOSE);
    }

    /**
     * Return the legth of the content
     * @return content length
     */
    public int getContentLength() {
        String len = (String) headers.get(Constants.CONTENT_LENGTH);
        if (len != null) {
            return Integer.parseInt(len);
        } else {
            return -1;
        }
    }

    /**
     * Return an InputStream to read the body from the main ByteBuffer of this message
     *
     * @return an InputStream into the main ByteBuffer starting at the body
     */
    public InputStream getInputStream() {
        // position to the start of the body
        buffer.position(0);

        // Returns an input stream for a ByteBuffer.
        // The read() methods use the relative ByteBuffer get() methods.
        return new InputStream() {
            public synchronized int read() throws IOException {
                if (!buffer.hasRemaining()) {
                    return -1;
                }
                return buffer.get();
            }

            public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                // Read only what's left
                len = Math.min(len, buffer.remaining());
                buffer.get(bytes, off, len);
                return len;
            }
        };
    }

    /**
     * Get an OutputStream to write the body of this httpMessage
     * @return an OutputStream to write the body
     */
    public OutputStream getOutputStream() {
        // position for body
        buffer.clear();
        outputStreamOpened = true;
        buffer.position(0);

        // Returns an output stream for a ByteBuffer.
        // The write() methods use the relative ByteBuffer put() methods.
        return new OutputStream() {
            public synchronized void write(int b) throws IOException {
                while (true) {
                    try {
                        buffer.put((byte) b);
                        return;
                    } catch (BufferOverflowException bo) {
                        expandBuffer();
                    }
                }
            }

            public synchronized void write(byte[] bytes, int off, int len) throws IOException {
                while (true) {
                    try {
                        buffer.put(bytes, off, len);
                        return;
                    } catch (BufferOverflowException bo) {
                        expandBuffer();
                    }
                }
            }

            public void close() throws IOException {
                buffer.flip();
                outputStreamOpened = false;
            }
        };
    }

    /**
     * Expand (double) the main ByteBuffer of this message
     */
    private void expandBuffer() {
        ByteBuffer newBuf = ByteBuffer.allocate(buffer.capacity() * 2);
        log.debug("Expanding ByteBuffer to " + newBuf.capacity() + " bytes");
        buffer.flip();
        buffer = newBuf.put(buffer);
    }

    /**
     * Return a string representation of the message in HTTP wire-format
     * @return a String representation of the message in HTTP wire-format
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();

        // print httpMessage-line or status-line
        sb.append(toStringLine());

        // print headers
        Iterator iter = headers.keySet().iterator();
        while (iter.hasNext()) {
            String headerName = (String) iter.next();
            sb.append(headerName + Constants.STRING_COLON + Constants.STRING_SP +
                headers.get(headerName) + Constants.CRLF);
        }
        sb.append(Constants.CRLF);

        if (buffer.limit() > 0) {
            buffer.position(0);

            Charset set = Charset.forName("us-ascii");
            CharsetDecoder dec = set.newDecoder();
            try {
                sb.append(dec.decode(buffer));
            } catch (CharacterCodingException e) {
                e.printStackTrace();
            }
        }

        sb.append(Constants.CRLF);

        return sb.toString();
    }

    /**
     * Return the first line of text for the toString() representation of this object. This would
     * be a httpMessage-line or a status-line as per the RFC
     *
     * @return the first line of text for the toString()
     */
    public abstract String toStringLine();

    /**
     * Reset the internal state of this message to be reused
     */
    public void reset() {
        if (buffer.capacity() > DEFAULT_BUFFER_SIZE) {
            buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        } else {
            buffer.clear();
        }
    }

    /**
     * Return a reference to the internal ByteBuffer of this message
     * @return the reference to the internal ByteBuffer used
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Set the internal buffer to the given ByteBuffer
     * @param buffer
     */
    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}
