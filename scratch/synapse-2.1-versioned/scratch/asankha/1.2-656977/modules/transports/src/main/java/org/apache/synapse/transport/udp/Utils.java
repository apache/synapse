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
package org.apache.synapse.transport.udp;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class with methods used by the UDP transport.
 */
public class Utils {
    private Utils() {}
    
    public static void hexDump(StringBuilder buffer, byte[] data, int length) {
        for (int start = 0; start < length; start += 16) {
            for (int i=0; i<16; i++) {
                int index = start+i;
                if (index < length) {
                    buffer.append(StringUtils.leftPad(Integer.toHexString(data[start+i]), 2, '0'));
                } else {
                    buffer.append("  ");
                }
                buffer.append(' ');
                if (i == 8) {
                    buffer.append(' ');
                }
            }
            buffer.append(" |");
            for (int i=0; i<16; i++) {
                int index = start+i;
                if (index < length) {
                    int b = data[index] & 0xFF;
                    if (32 <= b && b < 128) {
                        buffer.append((char)b);
                    } else {
                        buffer.append('.');
                    }
                } else {
                    buffer.append(' ');
                }
            }
            buffer.append('|');
            buffer.append('\n');
        }
    }
}
