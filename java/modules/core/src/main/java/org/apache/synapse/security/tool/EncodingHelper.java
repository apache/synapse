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
package org.apache.synapse.security.tool;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.security.enumeration.EncodingType;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

/**
 * Helper to handle encoding and decoding of data streams.
 */
public final class EncodingHelper {

    private static Log log = LogFactory.getLog(EncodingHelper.class);
    
    private EncodingHelper() {
    }
    
    /**
     * Encodes the provided ByteArrayOutputStream using the specified encoding type.
     * 
     * @param baos         The ByteArrayOutputStream to encode
     * @param encodingType The encoding to use
     * 
     * @return The encoded ByteArrayOutputStream as a String
     */
    public static String encode(ByteArrayOutputStream baos, EncodingType encodingType) {
        String encodedString;
        
        switch(encodingType) {
            case BASE64:
                if (log.isDebugEnabled()) {
                    log.debug("base64 encoding on output ");
                }
                encodedString = new BASE64Encoder().encode(baos.toByteArray());
                break;
            case BIGINTEGER16:
                if (log.isDebugEnabled()) {
                    log.debug("BigInteger 16 encoding on output ");
                }
                encodedString = new BigInteger(baos.toByteArray()).toString(16);
                break;
            default:
                throw new IllegalArgumentException("Unsupported encoding type");
        }
        
        return encodedString;
    }

    /**
     * Decodes the provided InputStream using the specified encoding type.
     * 
     * @param inputStream               The InputStream to decode
     * @param encodingType              The encoding to use
     * 
     * @throws IOException              If an error occurs decoding the input stream
     * @throws IllegalArgumentException if the specified encodingType is not supported
     * 
     * @return The decoded InputStream
     */
    public static InputStream decode(InputStream inputStream, EncodingType encodingType) 
        throws IOException {
        
        InputStream decodedInputStream = null;
            switch(encodingType) {
                case BASE64:
                    if (log.isDebugEnabled()) {
                        log.debug("base64 decoding on input  ");
                    }
                    decodedInputStream = new ByteArrayInputStream(
                            new BASE64Decoder().decodeBuffer(inputStream));
                    break;
                case BIGINTEGER16:
                    if (log.isDebugEnabled()) {
                        log.debug("BigInteger 16 encoding on output ");
                    }
                    
                    BigInteger n = new BigInteger(IOUtils.toString(inputStream), 16);
                    decodedInputStream = new ByteArrayInputStream(n.toByteArray());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported encoding type");
            }
        
        return decodedInputStream;
    }
}