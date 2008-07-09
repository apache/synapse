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
package org.apache.synapse.security.definition.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.security.definition.IdentityKeyStoreInformation;
import org.apache.synapse.security.definition.KeyStoreInformation;
import org.apache.synapse.security.definition.TrustKeyStoreInformation;
import org.apache.synapse.util.MiscellaneousUtil;

import java.util.Properties;

/**
 * Factory for creating KeyStoreInformation based on properties
 */
public class KeyStoreInformationFactory {

    private static Log log = LogFactory.getLog(KeyStoreInformationFactory.class);

    /* Private key entry KeyStore location */
    private final static String IDENTITY_KEY_STORE = "keystore.identity.location";
    /* Private key entry KeyStore type  */
    private final static String IDENTITY_KEY_STORE_TYPE = "keystore.identity.type";
    /* Alias for private key entry KeyStore */
    private final static String IDENTITY_KEY_STORE_ALIAS = "keystore.identity.alias";
    /* Password for access keyStore*/
    private final static String IDENTITY_KEY_STORE_PASSWORD = "keystore.identity.storePassword";
    /* Password for get private key*/
    private final static String IDENTITY_KEY_PASSWORD = "keystore.identity.keyPassword";

    private final static String KEY_STORE_PARAMETERS = "keystore.identity.parameters";

    /* Trusted certificate KeyStore location */
    private final static String TRUST_STORE = "keystore.trust.location";
    /* Trusted certificate KeyStore type*/
    private final static String TRUST_STORE_TYPE = "keystore.trust.type";
    /* Alias for certificate KeyStore */
    private final static String TRUST_STORE_ALIAS = "keystore.trust.alias";
    /* Password for access TrustStore*/
    private final static String TRUST_STORE_PASSWORD = "keystore.trust.storePassword";

    private final static String TRUST_STORE_PARAMETERS = "keystore.trust.parameters";

    /**
     * Creates a KeyStoreInformation using synapse properties
     * Uses KeyStore configuration properties
     *
     * @param properties Synapse Properties
     * @return IdentityKeyStoreInformation instance
     */
    public static IdentityKeyStoreInformation createIdentityKeyStoreInformation(Properties properties) {

        String keyStoreLocation = MiscellaneousUtil.getProperty(
                properties, IDENTITY_KEY_STORE, null);
        if (keyStoreLocation == null || "".equals(keyStoreLocation)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot find a KeyStoreLocation for private key store");
            }
            return null;
        }

        IdentityKeyStoreInformation keyStoreInformation = new IdentityKeyStoreInformation();
        keyStoreInformation.setAlias(
                MiscellaneousUtil.getProperty(properties,
                        IDENTITY_KEY_STORE_ALIAS, null));
        keyStoreInformation.setLocation(keyStoreLocation);
        keyStoreInformation.setStoreType(
                MiscellaneousUtil.getProperty(properties,
                        IDENTITY_KEY_STORE_TYPE, null));
        keyStoreInformation.setKeyStorePassword(
                MiscellaneousUtil.getProperty(
                        properties, IDENTITY_KEY_STORE_PASSWORD, null));
        keyStoreInformation.setKeyPassword(
                MiscellaneousUtil.getProperty(
                        properties, IDENTITY_KEY_PASSWORD, null));
        String parameterString = MiscellaneousUtil.getProperty(
                properties, KEY_STORE_PARAMETERS, null);

        //Adds optional parameters
        parseParameter(parameterString, keyStoreInformation);
        return keyStoreInformation;
    }

    /**
     * Creates a TrustKeyStoreInformation using synapse properties
     * Uses TrustStore Configuration properties
     *
     * @param properties Synapse Properties
     * @return TrustKeyStoreInformation instance
     */
    public static TrustKeyStoreInformation createTrustKeyStoreInformation(Properties properties) {

        String keyStoreLocation =
                MiscellaneousUtil.getProperty(properties,
                        TRUST_STORE, null);
        if (keyStoreLocation == null || "".equals(keyStoreLocation)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot find a KeyStoreLocation for trust store");
            }
            return null;
        }

        TrustKeyStoreInformation trustInformation = new TrustKeyStoreInformation();
        trustInformation.setAlias(
                MiscellaneousUtil.getProperty(properties, TRUST_STORE_ALIAS, null));
        trustInformation.setLocation(keyStoreLocation);
        trustInformation.setStoreType(
                MiscellaneousUtil.getProperty(properties,
                        TRUST_STORE_TYPE, null));
        trustInformation.setKeyStorePassword(
                MiscellaneousUtil.getProperty(properties, TRUST_STORE_PASSWORD, null));
        String parameterString = MiscellaneousUtil.getProperty(
                properties, TRUST_STORE_PARAMETERS, null);

        //Adds optional parameters
        parseParameter(parameterString, trustInformation);
        return trustInformation;
    }

    /**
     * Helper method to parse parameter values (String ) and add those to KeyStoreInformation
     *
     * @param parameterString Parameter String
     * @param information     KeyStoreInformation
     */
    private static void parseParameter(String parameterString, KeyStoreInformation information) {

        if (parameterString == null || "".equals(parameterString)) {
            if (log.isDebugEnabled()) {
                log.debug("No additional parameter for KeyStore");
            }
            return;
        }

        String[] parameterPairs = parameterString.split(";");
        if (parameterPairs == null) {
            if (log.isDebugEnabled()) {
                log.debug("No additional parameter for KeyStore");
            }
            return;
        }

        for (String parameterPairString : parameterPairs) {
            String[] parametersPair = parameterPairString.split("=");
            if (parametersPair == null || parameterPairs.length != 2) {
                if (log.isDebugEnabled()) {
                    log.debug("A parameter with no (name,value) pair has been found ");
                }
                return;
            }
            information.addParameter(parametersPair[0], parametersPair[1]);
        }
    }
}
