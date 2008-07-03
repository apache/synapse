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
import org.apache.synapse.security.bean.KeyStoreInformation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Wraps the keyStore and provide abstraction need for ciphering in the synapse.
 */
public abstract class KeyStoreWrapper {
    protected Log log;
    /* Bean that encapsulates the information about KeyStore */
    private KeyStoreInformation keyStoreInformation;
    /* Underlying KeyStore */
    private KeyStore keyStore;
    /* Password to access private key entries*/
    private String keyPassword;

    protected KeyStoreWrapper() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * Initialize the KeyStore wrapper based on provided KeyStoreinformation and passwords
     *
     * @param information   The object that has encapsulated all information for a
     *                      keyStore excepts passwords
     * @param storePassword Specifies the password for the keyStore file
     * @param keyPassword   Specifies the password of the key within the keyStore
     */
    protected void init(KeyStoreInformation information, String storePassword, String keyPassword) {

        if (information == null) {
            handleException("KeyStore information cannot be found");
        }

        if (storePassword == null || "".equals(storePassword)) {
            handleException("KeyStore password need to be provided ");
        }

        this.keyStoreInformation = information;
        this.keyPassword = keyPassword;

        String store = information.getLocation();
        File keyStoreFile = new File(store);
        if (!keyStoreFile.exists()) {
            handleException("KeyStore can not be found at ' " + keyStoreFile + " '");
        }

        String storeType = information.getStoreType();
        BufferedInputStream bis = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Loading KeyStore form : " + store);
            }
            bis = new BufferedInputStream(new FileInputStream(keyStoreFile));
            keyStore = KeyStore.getInstance(storeType);
            keyStore.load(bis, storePassword.toCharArray());

        } catch (KeyStoreException e) {
            handleException("Error loading keyStore from ' " + store + " ' ", e);
        } catch (IOException e) {
            handleException("IOError loading keyStore from ' " + store + " ' ", e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Error loading keyStore from ' " + store + " ' ", e);
        } catch (CertificateException e) {
            handleException("Error loading keyStore from ' " + store + " ' ", e);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ignored) {
                }
            }
        }

    }

    /**
     * Returns the key based on provided alias and key password
     *
     * @param alias       The alias of the certificate in the specified keyStore
     * @param keyPassword Password for key within the KeyStrore
     * @return Key if there is a one , otherwise null
     */
    protected Key getKey(String alias, String keyPassword) {

        if (alias == null || "".equals(alias)) {
            handleException("The alias need to provided to get certificate");
        }
        if (keyPassword != null) {
            try {
                return keyStore.getKey(alias, keyPassword.toCharArray());
            } catch (KeyStoreException e) {
                handleException("Error loading key for alias : " + alias, e);
            } catch (NoSuchAlgorithmException e) {
                handleException("Error loading key for alias : " + alias, e);
            } catch (UnrecoverableKeyException e) {
                handleException("Error loading key for alias : " + alias, e);
            }
        }
        return null;
    }

    /**
     * Returns the key based on certificate of the owner to who given alias belong
     *
     * @param alias The alias of the certificate in the specified keyStore
     * @return Key , if there is a one , otherwise null
     */
    protected Key getKey(String alias) {
        try {
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate != null) {
                return certificate.getPublicKey();
            }
        } catch (KeyStoreException e) {
            handleException("Error loading key for alias : " + alias, e);
        }
        return null;
    }

    /**
     * Retunrs the key based on default alias or password
     *
     * @return Key , if there is a one , otherwise null
     */
    protected Key getKey() {
        if (keyPassword != null) {
            return getKey(keyStoreInformation.getAlias(), keyPassword);
        } else {
            return getKey(keyStoreInformation.getAlias());
        }
    }

    /**
     * Returns the public key for the given alias
     *
     * @param alias The alias of the certificate in the specified keyStore
     * @return PublicKey if there is a one , otherwise null
     */
    public PublicKey getPublicKey(String alias) {
        Key key = getKey(alias);
        if (key instanceof PublicKey) {
            return (PublicKey) key;
        }
        return null;
    }

    /**
     * Returns the public key based on initilization data
     *
     * @return PublicKey if there is a one , otherwise null
     */
    public PublicKey getPublicKey() {
        Key key = getKey();
        if (key instanceof PublicKey) {
            return (PublicKey) key;
        }
        return null;
    }

    protected void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    protected void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
