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
package org.apache.axis2.transport.niohttp.impl.io;

import org.apache.axis2.transport.niohttp.impl.Constants;

import java.io.InputStream;
import java.io.IOException;
import java.io.FilterInputStream;
import java.util.List;
import java.util.ArrayList;

/**
 *  This class builds a wrapper around the original chunked input stream
 *
 *  Chunked-Body   = *chunk
 *                   last-chunk
 *                   trailer
 *                   CRLF
 *
 *  chunk          = chunk-size [ chunk-extension ] CRLF
 *                   chunk-data CRLF
 *  chunk-size     = 1*HEX
 *  last-chunk     = 1*("0") [ chunk-extension ] CRLF
 *
 *  chunk-extension= *( ";" chunk-ext-name [ "=" chunk-ext-val ] )
 *  chunk-ext-name = token
 *  chunk-ext-val  = token | quoted-string
 *  chunk-data     = chunk-size(OCTET)
 *  trailer        = *(entity-header CRLF)
 *
 * Copied from HttpCore, and slightly modified.
 */
public class ChunkedInputStream extends FilterInputStream {

    /** Actual underlying input stream */
    private InputStream in;
    /** Current chunk size */
    private int chunkSize;
    /** Position within current chunk */
    private int pos;
    /** Any footers found on the stream */
    private String[] footers;

    /** True if we'are at the beginning of stream */
    private boolean bof = true;
    /** True if we've reached the end of stream */
    private boolean eof = false;

    public ChunkedInputStream(InputStream in) {
        super(in);
        this.in = in;  
    }

    public int read() throws IOException {
        if (pos >= chunkSize) {
            nextChunk();
            if (eof) { 
                return -1;
            }
        }
        pos++;
        return in.read();
    }


    public int read(byte b[], int off, int len) throws IOException {
        if (eof) {
            return -1;
        }
        if (pos >= chunkSize) {
            nextChunk();
            if (eof) {
                return -1;
            }
        }
        len = Math.min(len, chunkSize - pos);
        int count = in.read(b, off, len);
        pos += count;
        return count;
    }

    /**
     * Read the next chunk.
     * @throws IOException If an IO error occurs.
     */
    private void nextChunk() throws IOException {
        chunkSize = getChunkSize();
        if (chunkSize < 0) {
            throw new MalformedStreamException("Negative chunk size");
        }
        bof = false;
        pos = 0;
        if (chunkSize == 0) {
            eof = true;
            parseTrailerHeaders();
        }
    }

    /**
     * Expects the stream to start with a chunksize in hex with optional
     * comments after a semicolon. The line must end with a CRLF: "a3; some
     * comment\r\n" Positions the stream at the start of the next line.
     *
     * @return the chunk size as integer
     * @throws IOException when the chunk size could not be parsed
     */
    private int getChunkSize() throws IOException {
        // skip CRLF
        if (!bof) {
            int cr = in.read();
            int lf = in.read();
            if ((cr != Constants.CR) || (lf != Constants.LF)) {
                throw new MalformedStreamException("CRLF expected at end of chunk");
            }
        }

        // . . C L . .
        // l c
        //   l c
        //     l c
        StringBuffer sb = new StringBuffer();
        int last, curr;

        last = in.read();
        while (true) {
            curr = in.read();
            if (last == Constants.CR && curr == Constants.LF) {
                // we found the end of the line and have read past the chunk size line :-)
                break;
            } else {
                sb.append((char)last);
                last = curr;
            }
        }

        String lengthStr = sb.toString();
        int semiColon = lengthStr.indexOf(';');
        if (semiColon != -1) {
            lengthStr = lengthStr.substring(0, semiColon); 
        }

        try {
            return Integer.parseInt(lengthStr, 16);
        } catch (NumberFormatException e) {
            throw new MalformedStreamException("Bad chunk header");
        }
    }

    /**
     * Reads and stores the Trailer headers.
     *
     * @throws IOException If an IO problem occurs
     */
    private void parseTrailerHeaders() throws IOException {

        List footers = new ArrayList();
        StringBuffer sb = new StringBuffer();
        int last, curr;

        last = in.read();
        while (true) {
            curr = in.read();
            if (last == Constants.CR && curr == Constants.LF) {
                if (sb.length() == 0) {
                    break;
                }
                footers.add(sb.toString());
                sb = new StringBuffer();
                last = in.read();
            } else {
                sb.append(last);
                last = curr;
            }
        }
        // TODO for now just eat :)
        // this.footers = (String[]) footers.toArray();
    }
}
