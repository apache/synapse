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

import java.io.InputStream;
import java.io.IOException;
import java.io.FilterInputStream;

/**
 * This class cuts the wrapped InputStream off after a specified number of bytes.
 *
 * This does not close the underlying stream when closed as the underlying stream
 */
public class ContentLengthInputStream extends FilterInputStream {

    /** Actual underlying input stream */
    private InputStream in;
    /** Content length that can be read off this stream */
    private int contentLength;
    /** Current position of read */
    private int pos;

    public ContentLengthInputStream(InputStream in, int length) {
        super(in);
        this.in = in;
        this.contentLength = length;
    }

    public int read() throws IOException {
        if (pos >= contentLength) {
            return -1;
        }
        pos++;
        return in.read();
    }

    public int read (byte[] b, int off, int len) throws java.io.IOException {
        if (pos >= contentLength) {
            return -1;
        }

        if (pos + len > contentLength) {
            len = contentLength - pos;
        }
        int count = in.read(b, off, len);
        pos += count;
        return count;
    }

}
