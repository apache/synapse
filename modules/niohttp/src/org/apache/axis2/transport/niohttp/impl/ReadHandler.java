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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * A generic parser for HTTP incoming request/response digesting
 * <p/>
 * RFC 2616
 * Request  = Request-Line            ; Section 5.1
 * *(( general-header        ; Section 4.5
 * | request-header         ; Section 5.3
 * | entity-header ) CRLF)  ; Section 7.1
 * CRLF
 * [ message-body ]         ; Section 4.3
 * Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
 * <p/>
 * Response  = Status-Line             ; Section 6.1
 * *(( general-header        ; Section 4.5
 * | response-header        ; Section 6.2
 * | entity-header ) CRLF)  ; Section 7.1
 * CRLF
 * [ message-body ]         ; Section 7.2
 * Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
 */
public class ReadHandler {

    private static final Log log = LogFactory.getLog(ReadHandler.class);

    CharsetDecoder asciiDecoder = Charset.forName("us-ascii").newDecoder();

    ByteBuffer buffer = ByteBuffer.allocate(4096);
    ByteBuffer chunkedBuffer = ByteBuffer.allocate(4096);
    HttpMessage httpMessage;

    // where should new bytes read from the incoming channel be stored
    int readPos;
    int processPos; /* position within the main 'buffer' */

    // holders for parsed data
    String curHeaderName = null;
    StringBuffer curHeaderValue = new StringBuffer();
    int bodyStart;
    int contentLength;
    int currentChunkRemainder;
    boolean lastChunkReceived = false;

    // holders of internal state of the parser
    private boolean requestMode = true;
    private boolean parsingMessageLine = true;
    private boolean parsingHeader = true;
    private boolean parsingChunks = false;
    private boolean messageComplete = false;

    public void reset() {
        buffer.clear();
        chunkedBuffer.clear();
        if (requestMode) {
            httpMessage = new HttpRequest();
        } else {
            httpMessage = new HttpResponse();
        }
        readPos = 0;
        processPos = 0;
        curHeaderName = null;
        curHeaderValue = new StringBuffer();
        bodyStart = 0;
        contentLength = 0;
        currentChunkRemainder = 0;
        lastChunkReceived = false;
        parsingMessageLine = true;
        parsingHeader = true;
        parsingChunks = false;
        messageComplete = false;
    }

    public ReadHandler(boolean requestMode) {
        this.requestMode = requestMode;
        if (requestMode) {
            httpMessage = new HttpRequest();
        } else {
            httpMessage = new HttpResponse();
        }
    }

    public boolean handle(SocketChannel socket, SelectionKey sk) {
        try {
            // set position within buffer to read from channel
            buffer.position(readPos);

            if (readPos == buffer.capacity()) {
                ByteBuffer newBuf = ByteBuffer.allocate(buffer.capacity() * 2);
                log.debug("Expanding ByteBuffer to " + newBuf.capacity() + " bytes");
                buffer.flip();
                buffer = newBuf.put(buffer);
            }

            // perform read from channel to this location
            int bytesRead = socket.read(buffer);
            if (bytesRead == -1) {
                // end of stream reached
                socket.close();
                sk.cancel();
                debug("end-of-stream detected and socket closed");
                return false;
            }

            if (log.isDebugEnabled()) {
                debug("Read from socket to buffer position: " + readPos + " to: " + buffer.position());
                debug(Util.dumpAsHex(buffer.array(), readPos));
            }

            // save position for next read
            readPos = buffer.position();
            return processIncomingMessage();

        } catch (IOException e) {
            log.warn(e.getMessage() + " Closing socket: " + socket);
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            sk.cancel();
        }
        return false;
    }

    private boolean isMessageComplete() {
        return messageComplete;
    }

    public boolean isConnectionClose() {
        return httpMessage.isConnectionClose();
    }

    private boolean processIncomingMessage() {

        debug("\tprocessing httpMessage");
        if (parsingMessageLine) {
            debug("\t\tparsing httpMessage line");
            parseMessageLine();
        }
        if (!parsingMessageLine && parsingHeader) {
            debug("\t\tparsing headers");
            parseHeaders();
        }
        if (!parsingHeader && !messageComplete) {
            debug("\t\tparsing body");
            parseBody();
        }

        return messageComplete;
    }

    private void skipBlankLines(ByteBuffer buf) {

        buffer.position(processPos);
        int pos = processPos;
        int start = pos;

        while (pos + 1 < readPos &&
            buf.get(pos) == Constants.CR &&
            buf.get(pos + 1) == Constants.LF) {
            pos += 2;
        }
        // did we really skip any?
        if (pos > start) {
            processPos = pos; // advanced processed position
        }
    }

    private String readToSpace(ByteBuffer buf) {
        return readToDelimeter(buf, Constants.SP);
    }

    private String readToColon(ByteBuffer buf) {
        return readToDelimeter(buf, Constants.COLON);
    }

    /**
     * read to the position of the byte given by 'delim' from the
     * processPos position of the buffer.
     * <p/>
     * updates processPos if 'delim' was found, to the found position
     *
     * @param buf
     * @param delim
     * @return the bytes from the start position to the delimiter (excluding) as a string
     */
    private String readToDelimeter(ByteBuffer buf, final byte delim) {

        buffer.position(processPos);
        int pos = processPos;
        int start = pos;

        //     processPos (what we have not digested yet)
        //     |   delim   readPos (where we will read to next)
        //     |   |       |
        // x x x x D x x x . . . .
        //     ^ ^
        //     what we should get as a result of this operation

        while (pos < readPos && buf.get(pos) != delim) {
            pos++;
        }

        if (pos < readPos) {
            debug("\t\t\t$$readToDelimeter(" + delim + ") FOUND");
            processPos = pos + 1;   // advance over processed bytes and delim
            return extractAsString(start, pos - 1);
        } else {
            debug("\t\t\t$$readToDelimeter(" + delim + ") NOT FOUND");
            return null;
        }
    }

    private String extractAsString(int start, int end) {

        if (end < start) return "";

        byte[] temp = new byte[end - start + 1];
        buffer.position(start);
        buffer.get(temp, 0, end - start + 1);

        try {
            debug("\t\t@@ extractAsString(" + start + ", " + end + ") from Buffer "
                + buffer + " as : " + asciiDecoder.decode(ByteBuffer.wrap(temp)).toString());
            return asciiDecoder.decode(ByteBuffer.wrap(temp)).toString();
        } catch (CharacterCodingException e) {
            e.printStackTrace();
            return null; // TODO
        }
    }

    private String readToCRLF(ByteBuffer buf) {

        buffer.position(processPos);
        int pos = processPos;
        int start = pos;

        while (pos + 1 < readPos &&
            buf.get(pos) != Constants.CR && buf.get(pos + 1) != Constants.LF) {
            pos++;
        }

        if (pos < readPos) {
            debug("\t\t\t$$readToCRLF() FOUND");
            processPos = pos + 2;   // advance over processed bytes and CR LF
            return extractAsString(start, pos - 1);
        }
        return null;
    }

    private void parseMessageLine() {

        // skip any blank lines
        skipBlankLines(buffer);

        if (requestMode) {
            parseRequestLine();
        } else {
            parseStatusLine();
        }
    }

    private void parseRequestLine() {
        HttpRequest request = (HttpRequest) httpMessage;

        // read method
        if (request.getMethod() == null) {
            String method = readToSpace(buffer);
            if (method != null) {
                request.setMethod(method);
            } else {
                return;
            }
        }

        // read URI
        if (request.getPath() == null) {
            String uri = readToSpace(buffer);
            if (uri != null) {
                request.setPath(uri);
            } else {
                return;
            }
        }

        // read version string
        if (request.getProtocol() == null) {
            String proto = readToCRLF(buffer);
            if (proto != null) {
                request.setProtocol(proto);
                parsingMessageLine = false;
            } else {
                return;
            }
        }
    }

    private void parseStatusLine() {
        HttpResponse response = (HttpResponse) httpMessage;

        // read version
        if (response.getVersion() == null) {
            String version = readToSpace(buffer);
            if (version != null) {
                response.setVersion(version);
            } else {
                return;
            }
        }

        // read code
        if (response.getStatus().getCode() == 0) {
            String code = readToSpace(buffer);
            if (code != null) {
                response.getStatus().setCode(Integer.parseInt(code));
            } else {
                return;
            }
        }

        // read message
        if (response.getStatus().getMessage() == null) {
            String msg = readToCRLF(buffer);
            if (msg != null) {
                response.getStatus().setMessage(msg);
                parsingMessageLine = false;
            } else {
                return;
            }
        }
    }

    private void parseHeaders() {
        while ((processPos < readPos) && parseHeader()) {
            debug("\t\t\t... parsing headers...");
        }

        debug("\t\t\t...exit parsingheaders loop : processPos: " +
            processPos + " readPos : " + readPos);
    }

    // return false after reading a blank line
    private boolean parseHeader() {

        debug("\t\tParse Header processPos: " + processPos + " readPos: " + readPos);

        String line = readToCRLF(buffer);
        if (line == null) {
            return false;
        } else {
            // handle multi line headers later todo
            int colon = line.indexOf(":");
            if (colon != -1) {
                httpMessage.addHeader(
                    line.substring(0, colon),
                    line.substring(colon + 2/* include skip space too*/));
            } else if (line.length() == 0) {

                debug("\t\t\theaders parsed");
                parsingHeader = false;

                // prepare to parse body
                bodyStart = processPos;
                debug("\t\t\tparsed headers. begin parsing body to buffer position:" + bodyStart);

                if (httpMessage.isChunked()) {
                    parsingChunks = true;
                } else {
                    contentLength = httpMessage.getContentLength();
                }

                return false;
            }
        }
        return true;
    }

    private String parseHeaderName(ByteBuffer buf) {
        return readToColon(buf);
    }

    private String parseHeaderValue(ByteBuffer buf) {
        int firstChar;
        do {
            String value = readToCRLF(buf);
            if (value != null) {
                curHeaderValue.append(value);
            }
            firstChar = buf.get(buf.position());
        } while (firstChar == Constants.SP || firstChar == Constants.HT);
        return curHeaderValue.toString();
    }

    private boolean parseNextChunk() {
        debug("\t\t\tparseNextChunk(currentChunkRemainder: " + currentChunkRemainder +
            " processPos: " + processPos + " readPos: " + readPos);
        if (currentChunkRemainder > 0) {
            // now start processing from where we left off until we reach the end
            buffer.position(processPos);

            byte b;
            while (currentChunkRemainder > 0 && buffer.position() < readPos) {
                b = buffer.get();
                chunkedBuffer.put(b);
                processPos++;
                currentChunkRemainder--;
            }

            if (currentChunkRemainder == 0) {
                // read to end of data CRLF and discard trailing CRLF
                debug("\t\t\tcurrentChunkRemainder is 0 .. reading to CRLF..");
                readToCRLF(buffer);
            }
        }
        if (currentChunkRemainder == 0) {
            // is there another chunk?
            String chunkHead = readToCRLF(buffer);
            debug("\t\t\treading chunkHead : " + chunkHead);
            if (chunkHead != null && chunkHead.length() > 0) {
                int lenEnd = chunkHead.indexOf(';');
                if (lenEnd != -1) {
                    currentChunkRemainder = Integer.parseInt(
                        chunkHead.substring(0, lenEnd).trim(), 16 /* radix */);
                } else {
                    currentChunkRemainder = Integer.parseInt(
                        chunkHead.trim(), 16 /* radix */);
                }
            } else {
                return true;
            }

            // did we encounter the "0" chunk?
            if (currentChunkRemainder == 0) {

                debug("\t\t\tall chunks received");
                chunkedBuffer.flip();

                // read upto end of next CRLF
                String footer;
                do {
                    debug("\t\t\t...parsing chunk footers...");
                    footer = readToCRLF(buffer);
                    // TODO process footers if we need
                } while (footer != null && !"".equals(footer));

                /*try {
                    CharBuffer cb = asciiDecoder.decode(chunkedBuffer);
                    debug("Chunked Buffer : \n" + cb.toString());
                } catch (CharacterCodingException e) {
                    e.printStackTrace();
                }*/

                if ("".equals(footer)) {
                    lastChunkReceived = true;
                    messageComplete = true;
                    return false;
                }
            }
        }

        return true; // continue to recursively call this same method
    }

    private void parseBody() {
        debug("\t\t\tparseBody(processPos: " + processPos + ", readPos: " + readPos + ")");
        if (parsingChunks && !lastChunkReceived) {
            while (processPos < readPos && parseNextChunk()) {
                debug("\t\t\t...parsing body chunk....");
            }

            if (lastChunkReceived && messageComplete) {
                // copy chunked body to main buffer, to start at the bodyStart position
                buffer.position(bodyStart);
                chunkedBuffer.position(0);
                buffer.put(chunkedBuffer);
                buffer.flip();
                httpMessage.setBuffer(buffer, bodyStart);
            }

        } else {

            if (readPos >= bodyStart + contentLength) {
                // do we have the whole body in our buffer?
                processPos = readPos;
                buffer.position(processPos);
                buffer.flip();

                debug("\t\t\tfinish reading. body starts from: " +
                    bodyStart + " and ends: " + processPos + " in buffer : " + buffer);
                httpMessage.setBuffer(buffer, bodyStart);
                messageComplete = true;
            }
        }
    }

    public HttpMessage getHttpMessage() {
        return httpMessage;
    }

    // TODO remove this method... this is too much debugging!!
    private static void debug(String msg) {
        //System.out.println(msg);
    }
}
