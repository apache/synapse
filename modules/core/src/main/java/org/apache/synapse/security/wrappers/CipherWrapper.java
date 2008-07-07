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
package org.apache.synapse.security.wrappers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.security.definition.CipherInformation;
import org.apache.synapse.security.tool.CipherTool;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * Wraps the cipher and expose abstraction need for synapse ciphering
 */
public class CipherWrapper {

    private static Log log = LogFactory.getLog(CipherWrapper.class);

    /* Underlying cipher instance*/
    private Cipher cipher;
    /* Bean containing information required for cipher */
    private CipherInformation information;

    /**
     * Cipher needs a key and some information to work. Therefore, constructs the cipher with
     * providing those
     *
     * @param information Encapsulated object contains all information required to cipher
     * @param key         The key that will be used by the cipher either for encryption and encryption
     */
    public CipherWrapper(CipherInformation information, Key key) {
        this.information = information;
        String algorithm = information.getAlgorithm();
        String opMode = information.getOperationMode();

        if (log.isDebugEnabled()) {
            log.debug("Initializing cipher with algorithm " +
                    "'" + algorithm + "' in mode '" + opMode + "'");
        }
        try {
            cipher = Cipher.getInstance(algorithm);
            if (CipherTool.ENCRYPT.equals(opMode)) {
                cipher.init(Cipher.ENCRYPT_MODE, key);
            } else if (CipherTool.DECRYPT.equals(opMode)) {
                cipher.init(Cipher.DECRYPT_MODE, key);
            } else {
                handleException("Invalid mode : " + opMode);
            }

        } catch (NoSuchAlgorithmException e) {
            handleException("There is no algorithm support for " +
                    "'" + algorithm + "' in the operation mode '" + opMode + "'" + e);
        } catch (NoSuchPaddingException e) {
            handleException("There is no padding scheme  for " +
                    "'" + algorithm + "' in the operation mode '" + opMode + "'" + e);
        } catch (InvalidKeyException e) {
            handleException("Invalid key ", e);
        }
    }

    /**
     * Returns the output of the cipher operation.
     * This expose the 'getSecret' abstraction and hide operation of the underlying cipher
     *
     * @param inputStream Input as a stream. This can be either cipher or plain text
     * @return Secret as String.This can be either cipher or plain text
     */
    public String getSecret(InputStream inputStream) {

        if (CipherTool.BASE64.equals(information.getInType())) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("base64 decoding on input  ");
                }
                inputStream = new ByteArrayInputStream(
                        new BASE64Decoder().decodeBuffer(inputStream));
            } catch (IOException e) {
                handleException("Error decoding input ", e);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CipherOutputStream out = new CipherOutputStream(baos, cipher);

        byte[] buffer = new byte[8];
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            handleException("IOError !! ", e);
        } finally {
            try {
                inputStream.close();
                out.close();
                out.flush();
            } catch (IOException ignored) {

            }
        }

        if (CipherTool.BASE64.equals(information.getOutType())) {
            if (log.isDebugEnabled()) {
                log.debug("base64 encoding on output ");
            }
            return new BASE64Encoder().encode(baos.toByteArray());
        } else {
            return baos.toString();
        }
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
