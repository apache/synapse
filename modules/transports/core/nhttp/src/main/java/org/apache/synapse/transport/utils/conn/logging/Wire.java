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

package org.apache.synapse.transport.utils.conn.logging;

import org.apache.commons.logging.Log;

import java.nio.ByteBuffer;

/**
 * A utility for logging wire-level information of HTTP connections.
 */
public class Wire {

    private final Log log;

    public Wire(final Log log) {
        this.log = log;
    }

    private void wire(final String header, final byte[] b, int pos, int off) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < off; i++) {
            int ch = b[pos + i];
            if (ch == 13) {
                builder.append("[\\r]");
            } else if (ch == 10) {
                builder.append("[\\n]\"");
                builder.insert(0, "\"");
                builder.insert(0, header);
                this.log.debug(builder.toString());
                builder.setLength(0);
            } else if ((ch < 32) || (ch > 127)) {
                builder.append("[0x");
                builder.append(Integer.toHexString(ch));
                builder.append("]");
            } else {
                builder.append((char) ch);
            }
        }
        if (builder.length() > 0) {
            builder.append('\"');
            builder.insert(0, '\"');
            builder.insert(0, header);
            this.log.debug(builder.toString());
        }
    }


    public boolean isEnabled() {
        return this.log.isDebugEnabled();
    }

    public void output(final byte[] b, int pos, int off) {
        wire("<< ", b, pos, off);
    }

    public void input(final byte[] b, int pos, int off) {
        wire(">> ", b, pos, off);
    }

    public void output(byte[] b) {
        output(b, 0, b.length);
    }

    public void input(byte[] b) {
        input(b, 0, b.length);
    }

    public void output(int b) {
        output(new byte[] {(byte) b});
    }

    public void input(int b) {
        input(new byte[] {(byte) b});
    }

    public void output(final ByteBuffer b) {
        if (b.hasArray()) {
            output(b.array(), b.arrayOffset() + b.position(), b.remaining());
        } else {
            byte[] tmp = new byte[b.remaining()];
            b.get(tmp);
            output(tmp);
        }
    }

    public void input(final ByteBuffer b) {
        if (b.hasArray()) {
            input(b.array(), b.arrayOffset() + b.position(), b.remaining());
        } else {
            byte[] tmp = new byte[b.remaining()];
            b.get(tmp);
            input(tmp);
        }
    }
}
