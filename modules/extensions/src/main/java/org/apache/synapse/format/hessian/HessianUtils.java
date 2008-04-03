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

package org.apache.synapse.format.hessian;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Util class for writing the Hessian Fault to the output stream
 */
public class HessianUtils {

    public static void printString(String v, int offset, int length, OutputStream os) throws IOException {
        for (int i = 0; i < length; i++) {
            char ch = v.charAt(i + offset);

            if (ch < 0x80)
                os.write(ch);
            else if (ch < 0x800) {
                os.write(0xc0 + ((ch >> 6) & 0x1f));
                os.write(0x80 + (ch & 0x3f));
            }
            else {
                os.write(0xe0 + ((ch >> 12) & 0xf));
                os.write(0x80 + ((ch >> 6) & 0x3f));
                os.write(0x80 + (ch & 0x3f));
            }
        }
    }

    public static void writeString(String value, OutputStream os) throws IOException {
        if (value == null) {
            os.write('N');
        }
        else {
            int length = value.length();
            int offset = 0;

            while (length > 0x8000) {
                int sublen = 0x8000;

                os.write('s');
                os.write(sublen >> 8);
                os.write(sublen);

                printString(value, offset, sublen, os);

                length -= sublen;
                offset += sublen;
            }

            os.write('S');
            os.write(length >> 8);
            os.write(length);

            printString(value, offset, length, os);
        }
    }

    public static void startReply(OutputStream os) throws IOException {
        os.write('r');
        os.write(1);
        os.write(0);
    }

    public static void completeReply(OutputStream os) throws IOException {
        os.write('z');
    }

    public static void writeFault(String code, String message, String detail, OutputStream os)
            throws IOException {

        startReply(os);

        os.write('f');
        writeString("code", os);
        writeString(code, os);

        writeString("message", os);
        writeString(message, os);

        if (detail != null) {
            writeString("detail", os);
            writeString(detail, os);
        }
        
        os.write('z');

        completeReply(os);
    }
}
