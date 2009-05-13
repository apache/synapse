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
package org.apache.synapse.commons.security.wrappers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.security.definition.CipherInformation;
import org.apache.synapse.commons.security.enumeration.CipherOperationMode;
import org.apache.synapse.commons.security.tool.EncodingHelper;
import org.apache.synapse.commons.SynapseCommonsException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

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
    private CipherInformation cipherInformation;

    /**
     * Cipher needs a key and some information to work. Therefore, constructs the cipher with
     * providing those.
     *
     * @param cipherInformation Encapsulated object contains all information required to cipher
     * @param key               The key that will be used by the cipher either for encryption and 
     *                          encryption
     */
    public CipherWrapper(CipherInformation cipherInformation, Key key) {
        this.cipherInformation = cipherInformation;
        String algorithm = cipherInformation.getAlgorithm();
        CipherOperationMode opMode = cipherInformation.getCipherOperationMode();

        if (log.isDebugEnabled()) {
            log.debug("Initializing cipher with algorithm " +
                    "'" + algorithm + "' in mode '" + opMode + "'");
        }
        try {
            cipher = Cipher.getInstance(algorithm);
            if (opMode == CipherOperationMode.ENCRYPT) {
                cipher.init(Cipher.ENCRYPT_MODE, key);
            } else if (opMode == CipherOperationMode.DECRYPT) {
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
     * Constructs a cipher wrapper using the provided information and pass phrase.
     * 
     * @param cipherInformation Encapsulated object contains all information required to cipher
     * @param passphrase        The pass phrase used to construct a secret key using the same algorithm
     *                          that will be used to de- or encrypt data.
     */
    public CipherWrapper(CipherInformation cipherInformation, String passphrase) {
        this(cipherInformation, new SecretKeySpec(passphrase.getBytes(), cipherInformation.getAlgorithm()));
    }

    /**
     * Returns the output of the cipher operation.
     * This expose the 'getSecret' abstraction and hide operation of the underlying cipher
     *
     * @param inputStream Input as a stream. This can be either cipher or plain text
     * @return Secret as String.This can be either cipher or plain text
     */
    public String getSecret(InputStream inputStream) {

        InputStream sourceStream = null;
        if (cipherInformation.getInType() != null) {
            try {
                sourceStream = EncodingHelper.decode(inputStream, cipherInformation.getInType());
            } catch (IOException e) {
                handleException("IOError when decoding the input stream for cipher ", e);
            }
        } else {
            sourceStream = inputStream;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CipherOutputStream out = new CipherOutputStream(baos, cipher);

        byte[] buffer = new byte[64];
        int length;
        try {
            while ((length = sourceStream.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            handleException("IOError when reading the input stream for cipher ", e);
        } finally {
            try {
                sourceStream.close();
                out.flush();
                out.close();
            } catch (IOException ignored) {
                // ignore exception
            }
        }
        
        String secret;
        if (cipherInformation.getOutType() != null) {            
            secret = EncodingHelper.encode(baos, cipherInformation.getOutType());
        } else {
            secret = baos.toString();
        }
        return secret;
    }

    
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseCommonsException(msg, e);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseCommonsException(msg);
    }
}
