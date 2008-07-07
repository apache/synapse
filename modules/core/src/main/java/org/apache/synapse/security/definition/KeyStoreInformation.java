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
package org.apache.synapse.security.definition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.security.enumeration.KeyStoreType;
import org.apache.synapse.security.interfaces.ICACertsLoader;
import org.apache.synapse.security.interfaces.IKeyStoreLoader;
import org.apache.synapse.security.keystore.CACertsLoader;
import org.apache.synapse.security.keystore.JKSKeyStoreLoader;
import org.apache.synapse.security.keystore.PKCS12KeyStoreLoader;
import org.apache.synapse.security.keystore.PKCS8KeyStoreLoader;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the keyStore related information
 */
public abstract class KeyStoreInformation {

    private static final Log log = LogFactory.getLog(KeyStoreInformation.class);

    public static final String KEYSTORE_CERTIFICATE_FILE_PATH = "keyStoreCertificateFilePath";
    public static final String ENABLE_HOST_NAME_VERIFIER = "enableHostnameVerifier";
    private KeyStoreType storeType;
    private String alias;
    private String location;
    private String keyStorePassword;
    private String provider;

    private final Map parameters = new HashMap();

    public void setStoreType(String storeType) {
        if (storeType == null || "".equals(storeType)) {
            if (log.isDebugEnabled()) {
                log.debug("Given store type is null , using default type : JKS");
            }
        }
        this.storeType = KeyStoreType.valueOf(storeType);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        if (alias == null || "".equals(alias)) {
            if (log.isDebugEnabled()) {
                log.debug("Alias for a key entry or a certificate is null");
            }
            return;
        }
        this.alias = alias;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        if (location != null && "".equals(location)) {
            handleException("KeyStore location can not be null");
        }
        this.location = location;
    }

    protected void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    protected void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    public void addParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getParameter(String name) {
        return (String) parameters.get(name);
    }

    protected KeyStore getKeyStore() {
        switch (storeType) {
            case JKS:
                IKeyStoreLoader jksKeyStoreLoader = new JKSKeyStoreLoader(location,
                        keyStorePassword);
                return jksKeyStoreLoader.getKeyStore();

            case PKCS12:
                IKeyStoreLoader pkcs12KeyStoreLoader = new PKCS12KeyStoreLoader(location,
                        keyStorePassword);
                return pkcs12KeyStoreLoader.getKeyStore();
            case PKCS8:
                IKeyStoreLoader pkcs8KeyStoreLoader = new PKCS8KeyStoreLoader(location,
                        (String) parameters.get(KEYSTORE_CERTIFICATE_FILE_PATH),
                        keyStorePassword, alias);
                return pkcs8KeyStoreLoader.getKeyStore();
            case CA_CERTIFICATES_PATH:
                ICACertsLoader caCertsLoader = new CACertsLoader();
                return caCertsLoader.loadTrustStore(location);
            default:
                return null;
        }
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

}
