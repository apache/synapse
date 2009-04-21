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
package org.apache.synapse.security.secret.repository.filebased;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.security.definition.CipherInformation;
import org.apache.synapse.security.enumeration.CipherOperationMode;
import org.apache.synapse.security.enumeration.EncodingType;
import org.apache.synapse.security.secret.SecretRepository;
import org.apache.synapse.security.wrappers.CipherWrapper;
import org.apache.synapse.security.wrappers.IdentityKeyStoreWrapper;
import org.apache.synapse.security.wrappers.TrustKeyStoreWrapper;
import org.apache.synapse.commons.util.MiscellaneousUtil;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Holds all secrets in a file
 */
public class FileBaseSecretRepository implements SecretRepository {

    private static Log log = LogFactory.getLog(FileBaseSecretRepository.class);

    private static final String LOCATION = "location";
    private static final String KEY_STORE = "keyStore";
    private static final String DOT = ".";
    private static final String SECRET = "secret";
    private static final String ALIAS = "alias";
    private static final String ALIASES = "aliases";
    private static final String ALGORITHM = "algorithm";
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final String TRUSTED = "trusted";
    private static final String DEFAULT_CONF_LOCATION = "cipher-text.properties";

    /* Parent secret repository */
    private SecretRepository parentRepository;
    /*Map of secrets keyed by alias for property name */
    private final Map<String, String> secrets = new HashMap<String, String>();
    /*Wrapper for Identity KeyStore */
    private IdentityKeyStoreWrapper identity;
    /* Wrapper for trusted KeyStore */
    private TrustKeyStoreWrapper trust;
    /* Whether this secret repository has been initiated successfully*/
    private boolean initialize = false;

    public FileBaseSecretRepository(IdentityKeyStoreWrapper wrapper, TrustKeyStoreWrapper trust) {
        this.identity = wrapper;
        this.trust = trust;
    }

    /**
     * Initializes the repository based on provided properties
     *
     * @param properties Configuration properties
     * @param id         Identifier to identify properties related to the corresponding repository
     */
    public void init(Properties properties, String id) {
        StringBuffer sb = new StringBuffer();
        sb.append(id);
        sb.append(DOT);
        sb.append(LOCATION);

        String filePath = MiscellaneousUtil.getProperty(properties,
                sb.toString(), DEFAULT_CONF_LOCATION);

        Properties cipherProperties = MiscellaneousUtil.loadProperties(filePath);
        if (cipherProperties == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cipher texts cannot be loaded form : " + filePath);
            }
            return;
        }

        String aliasesString = MiscellaneousUtil.getProperty(cipherProperties, ALIASES, null);
        if (aliasesString == null || "".equals(aliasesString)) {
            if (log.isDebugEnabled()) {
                log.debug("There are no alias names in the cipher text file");
            }
            return;
        }

        String[] aliases = aliasesString.split(",");
        if (aliases == null) {
            if (log.isDebugEnabled()) {
                log.debug("There are no alias names in the cipher text file");
            }
            return;
        }

        for (String alias : aliases) {

            StringBuffer buffer = new StringBuffer();
            buffer.append(alias);
            buffer.append(DOT);
            buffer.append(SECRET);
            String propKey = buffer.toString();

            String secret = MiscellaneousUtil.getProperty(cipherProperties, propKey, null);
            if (secret == null || "".equals(secret)) {
                if (log.isDebugEnabled()) {
                    log.debug("No secret found for alias name " + alias);
                }
                continue;
            }
            buffer.append(DOT);
            buffer.append(KEY_STORE);
            String keyStorePropertyKey = buffer.toString();

            //Load keyStore
            String keyStore = MiscellaneousUtil.getProperty(cipherProperties,
                    keyStorePropertyKey, null);

            StringBuffer sbTwo = new StringBuffer();
            sbTwo.append(propKey);
            sbTwo.append(DOT);
            sbTwo.append(ALGORITHM);
            //Load algorithm
            String algorithm = MiscellaneousUtil.getProperty(cipherProperties,
                    sbTwo.toString(), DEFAULT_ALGORITHM);

            StringBuffer sbThree = new StringBuffer();
            sbThree.append(propKey);
            sbThree.append(DOT);
            sbThree.append(ALIAS);

            //Loads the alias of a certificate
            String aliasOfCert = MiscellaneousUtil.getProperty(
                    cipherProperties, sbThree.toString(), null);

            PublicKey key;
            if (TRUSTED.equals(keyStore)) {
                if (aliasOfCert == null) {
                    key = trust.getPublicKey();
                } else {
                    key = trust.getPublicKey(aliasOfCert);
                }
            } else {
                if (aliasOfCert == null) {
                    key = identity.getPublicKey();
                } else {
                    key = identity.getPublicKey(aliasOfCert);
                }
            }
            //Creates a cipherInformation
            CipherInformation cipherInformation = new CipherInformation();
            cipherInformation.setAlgorithm(algorithm);
            cipherInformation.setCipherOperationMode(CipherOperationMode.DECRYPT);
            cipherInformation.setInType(EncodingType.BASE64);

            //Creates a cipher
            CipherWrapper cipherWrapper = new CipherWrapper(cipherInformation, key);

            ByteArrayInputStream in = new ByteArrayInputStream(secret.getBytes());
            String decryptedText = cipherWrapper.getSecret(in);
            secrets.put(propKey, decryptedText);
            initialize = true;
        }
    }

    /**
     * @param alias Alias name for look up a secret
     * @return Secret if there is any , otherwise ,alias itself
     * @see org.apache.synapse.security.secret.SecretRepository
     */
    public String getSecret(String alias) {

        if (alias == null || "".equals(alias)) {
            return alias; // TODO is it needed to throw an error?
        }

        if (!initialize || secrets.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("There is no secret found for alias '" + alias + "' returning itself");
            }
            return alias;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(alias);
        sb.append(DOT);
        sb.append(SECRET);

        String secret = secrets.get(sb.toString());
        if (secret == null || "".equals(secret)) {
            if (log.isDebugEnabled()) {
                log.debug("There is no secret found for alias '" + alias + "' returning itself");
            }
            return alias;
        }
        return secret;
    }

    public void setParent(SecretRepository parent) {
        this.parentRepository = parent;
    }

    public SecretRepository getParent() {
        return this.parentRepository;
    }
}
