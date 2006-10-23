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

/**
 * A stream wrapper that closes itself after a defined number of bytes.
 *
 * Copied from HttpCore
 */
public class ContentLengthOutputStream extends FilterOutputStream {

    /** Content length that can be read off this stream */
    private int contentLength;
    /** Current position of write */
    private int pos;

    public ContentLengthOutputStream(OutputStream out, int length) {
        super(out);
        this.out = out;
        this.contentLength = length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (pos < contentLength) {
            long max = contentLength - pos;
            if (len > max) {
                len = (int) max;
            }
            out.write(b, off, len);
            pos += len;
        }
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(int b) throws IOException {
        if (pos < contentLength) {
            out.write(b);
            pos++;
        }
    }
}
