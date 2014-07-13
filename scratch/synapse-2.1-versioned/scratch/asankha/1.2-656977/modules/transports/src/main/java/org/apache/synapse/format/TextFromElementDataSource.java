/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;

/**
 * Data source that represents the text of a given {@link OMElement}.
 * <p>
 * The expression
 * <pre>new TextFromElementDataSource(element, charset, contentType)</pre>
 * produces a DataSource implementation that is equivalent to
 * <pre>new ByteArrayDataSource(element.getText().getBytes(charset), contentType)</pre>
 * but that is more efficient.
 */
public class TextFromElementDataSource implements DataSource {
    private static class InputStreamImpl extends InputStream {
        private final XMLStreamReader reader;
        private final String charset;
        private byte[] buffer;
        private int offset;
        
        public InputStreamImpl(XMLStreamReader reader, String charset) {
            this.reader = reader;
            this.charset = charset;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            return read(b) == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                int read = 0;
                outer: while (len > 0) {
                    if (buffer == null || offset == buffer.length) {
                        // Refill buffer
                        while (true) {
                            if (!reader.hasNext()) {
                                break outer;
                            }
                            int eventType = reader.next();
                            if (eventType == XMLStreamReader.CHARACTERS ||
                                    eventType == XMLStreamReader.CDATA) {
                                // Note: this is not entirely correct for encodings such as UTF-16.
                                // Once IO-158 is implemented, we could avoid this by implementing a
                                // Reader and using ReaderInputStream.
                                buffer = reader.getText().getBytes(charset);
                                offset = 0;
                                break;
                            }
                        }
                    }
                    int c = Math.min(len, buffer.length-offset);
                    System.arraycopy(buffer, offset, b, off, c);
                    offset += c;
                    off += c;
                    len -= c;
                    read += c;
                }
                return read == 0 ? -1 : read;
            }
            catch (XMLStreamException ex) {
                IOException ioException = new IOException("Unable to read from XMLStreamReader");
                ioException.initCause(ex);
                throw ioException;
            }
        }
    }
    
    private final OMElement element;
    private final String charset;
    private final String contentType;
    
    public TextFromElementDataSource(OMElement element, String charset, String contentType) {
        this.element = element;
        this.charset = charset;
        this.contentType = contentType;
    }
    
    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return null;
    }

    public InputStream getInputStream() throws IOException {
        return new InputStreamImpl(element.getXMLStreamReader(), charset);
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }
}
