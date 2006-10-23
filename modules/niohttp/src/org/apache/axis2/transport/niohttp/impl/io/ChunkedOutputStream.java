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

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class implements chunked transfer coding
 *
 * Copied from HttpCore
 */
public class ChunkedOutputStream extends FilterOutputStream {

    /** The default chunk size */
    private static final int DEFAULT_CHUNK_SIZE = 2048;
    /** Current chunk size */
    private int chunkSize;
    /** Write position within current chunk */
    private int pos;
    /** Any footers to write on the stream */
    private String[] footers;
    /** The final chunk has been written */
    private boolean lastChunkWritten = false;

    private ByteBuffer cache = null;

    public ChunkedOutputStream(OutputStream out) {
        this(out, DEFAULT_CHUNK_SIZE);
    }

    public ChunkedOutputStream(OutputStream out, int chunkSize) {
        super(out);
        this.out = out;
        this.chunkSize = chunkSize;
        cache = ByteBuffer.allocate(chunkSize);
    }

    public void write(int b) throws IOException {
        cache.put((byte) b);
        pos++;
        if (pos == chunkSize) {
            flushCache();
        }
    }

    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte src[], int off, int len) throws IOException {
        if (len >= cache.remaining()) {
            flushCacheWithAppend(src, off, len);
        } else {
            cache.put(src, off, len);
            pos += len;
        }
    }

    public void flush() throws IOException {
        if (!lastChunkWritten) {
            flushCache();
            writeClosingChunk();
            out.close();
            lastChunkWritten = true;
        }
    }

    private void writeClosingChunk() throws IOException {
        // Write the final chunk.
        out.write("0".getBytes());
        out.write("\r\n\r\n".getBytes());
    }

    private void flushCacheWithAppend(byte bufferToAppend[], int off, int len) throws IOException {
        out.write(Integer.toHexString(pos + len).getBytes());
        out.write("\r\n".getBytes());
        cache.flip();
        out.write(cache.array(), 0, pos + len);
        out.write(bufferToAppend, off, len);
        out.write("\r\n".getBytes());

        cache.clear();
        pos = 0;
    }

    private void flushCache() throws IOException {
        if (pos > 0) {
            out.write(Integer.toHexString(pos).getBytes());
            out.write("\r\n".getBytes());
            cache.flip();
            out.write(cache.array(), 0, pos);
            out.write("\r\n".getBytes());

            cache.clear();
            pos = 0;
        }
    }

}
