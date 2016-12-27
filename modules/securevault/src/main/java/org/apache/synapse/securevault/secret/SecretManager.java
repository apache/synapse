/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.securevault.secret;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.securevault.SecureVaultException;
import org.apache.synapse.securevault.commons.MiscellaneousUtil;
import org.apache.synapse.securevault.definition.IdentityKeyStoreInformation;
import org.apache.synapse.securevault.definition.KeyStoreInformationFactory;
import org.apache.synapse.securevault.definition.TrustKeyStoreInformation;
import org.apache.synapse.securevault.keystore.IdentityKeyStoreWrapper;
import org.apache.synapse.securevault.keystore.TrustKeyStoreWrapper;

import java.util.Properties;

/**
 * Entry point for manage secrets
 */
public class SecretManager {

    private static Log log = LogFactory.getLog(SecretManager.class);

    private final static SecretManager SECRET_MANAGER = new SecretManager();

    /* Default configuration file path for secret manager*/
    private final static String PROP_DEFAULT_CONF_LOCATION = "secret-manager.properties";
    /* If the location of the secret manager configuration is provided as a property- it's name */
    private final static String PROP_SECRET_MANAGER_CONF = "secret.manager.conf";
    /* Property key for secretRepositories*/
    private final static String PROP_SECRET_REPOSITORIES = "secretRepositories";
    /* Type of the secret repository */
    private final static String PROP_PROVIDER = "provider";
    /* Dot string */
    private final static String DOT = ".";

    /*Root Secret Repository */
    private SecretRepository parentRepository;
    /* True , if secret manage has been started up properly- need to have a at
    least one Secret Repository*/
    private boolean initialized = false;

    public static SecretManager getInstance() {
        return SECRET_MANAGER;
    }

    private SecretManager() {
    }

    /**
     * Initializes the Secret Manager by providing configuration properties
     *
     * @param properties Configuration properties
     */
    public void init(Properties properties) {

        if (initialized) {
            if (log.isDebugEnabled()) {
                log.debug("Secret Manager already has been started.");
            }
            return;
        }

        if (properties == null) {
            if (log.isDebugEnabled()) {
                log.debug("KeyStore configuration properties cannot be found");
            }
            return;
        }

        String configurationFile = MiscellaneousUtil.getProperty(
                properties, PROP_SECRET_MANAGER_CONF, PROP_DEFAULT_CONF_LOCATION);

        Properties configurationProperties = MiscellaneousUtil.loadProperties(configurationFile);
        if (configurationProperties == null || configurationProperties.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Configuration properties can not be loaded form : " +
                        configurationFile + " Will use synapse properties");
            }
            configurationProperties = properties;

        }

        String repositoriesString = MiscellaneousUtil.getProperty(
                configurationProperties, PROP_SECRET_REPOSITORIES, null);
        if (repositoriesString == null || "".equals(repositoriesString)) {
            if (log.isDebugEnabled()) {
                log.debug("No secret repositories have been configured");
            }
            return;
        }

        String[] repositories = repositoriesString.split(",");
        if (repositories == null || repositories.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No secret repositories have been configured");
            }
            return;
        }


        //Create a KeyStore Information  for private key entry KeyStore
        IdentityKeyStoreInformation identityInformation =
                KeyStoreInformationFactory.createIdentityKeyStoreInformation(properties);

        // Create a KeyStore Information for trusted certificate KeyStore
        TrustKeyStoreInformation trustInformation =
                KeyStoreInformationFactory.createTrustKeyStoreInformation(properties);


        String identityKeyPass = identityInformation
                .getKeyPasswordProvider().getResolvedSecret();
        String identityStorePass = identityInformation
                .getKeyStorePasswordProvider().getResolvedSecret();
        String trustStorePass = trustInformation
                .getKeyStorePasswordProvider().getResolvedSecret();

        if (!validatePasswords(identityStorePass, identityKeyPass, trustStorePass)) {
            if (log.isDebugEnabled()) {
                log.info("Either Identity or Trust keystore password is mandatory" +
                        " in order to initialized secret manager.");
            }
            return;
        }

        IdentityKeyStoreWrapper identityKeyStoreWrapper = new IdentityKeyStoreWrapper();
        identityKeyStoreWrapper.init(identityInformation, identityKeyPass);

        TrustKeyStoreWrapper trustKeyStoreWrapper = new TrustKeyStoreWrapper();
        trustKeyStoreWrapper.init(trustInformation);

        SecretRepository currentParent = null;
        for (String secretRepo : repositories) {

            StringBuffer sb = new StringBuffer();
            sb.append(PROP_SECRET_REPOSITORIES);
            sb.append(DOT);
            sb.append(secretRepo);
            String id = sb.toString();
            sb.append(DOT);
            sb.append(PROP_PROVIDER);

            String provider = MiscellaneousUtil.getProperty(
                    configurationProperties, sb.toString(), null);
            if (provider == null || "".equals(provider)) {
                handleException("Repository provider cannot be null ");
            }

            if (log.isDebugEnabled()) {
                log.debug("Initiating a File Based Secret Repository");
            }

            try {

                Class aClass = getClass().getClassLoader().loadClass(provider.trim());
                Object instance = aClass.newInstance();

                if (instance instanceof SecretRepositoryProvider) {
                    SecretRepository secretRepository = ((SecretRepositoryProvider) instance).
                            getSecretRepository(identityKeyStoreWrapper, trustKeyStoreWrapper);
                    secretRepository.init(configurationProperties, id);
                    if (parentRepository == null) {
                        parentRepository = secretRepository;
                    }
                    secretRepository.setParent(currentParent);
                    currentParent = secretRepository;
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully Initiate a Secret Repository provided by : "
                                + provider);
                    }
                } else {
                    handleException("Invalid class as SecretRepositoryProvider : Class Name : "
                            + provider);
                }

            } catch (ClassNotFoundException e) {
                handleException("A Secret Provider cannot be found for class name : " + provider);
            } catch (IllegalAccessException e) {
                handleException("Error creating a instance from class : " + provider);
            } catch (InstantiationException e) {
                handleException("Error creating a instance from class : " + provider);
            }
        }

        initialized = true;
    }

    /**
     * Returns the secret corresponding to the given alias name
     *
     * @param alias The logical or alias name
     * @return If there is a secret , otherwise , alias itself
     */
    public String getSecret(String alias) {
        if (!initialized || parentRepository == null) {
            if (log.isDebugEnabled()) {
                log.debug("There is no secret repository. Returning alias itself");
            }
            return alias;
        }
        return parentRepository.getSecret(alias);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void shoutDown() {
        this.parentRepository = null;
        this.initialized = false;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SecureVaultException(msg);
    }

    private boolean validatePasswords(String identityStorePass,
                                      String identityKeyPass, String trustStorePass) {
        boolean isValid = false;
        if (trustStorePass != null && !"".equals(trustStorePass)) {
            if (log.isDebugEnabled()) {
                log.debug("Trust Store Password cannot be found.");
            }
            isValid = true;
        } else {
            if (identityStorePass != null && !"".equals(identityStorePass) &&
                    identityKeyPass != null && !"".equals(identityKeyPass)) {
                if (log.isDebugEnabled()) {
                    log.debug("Identity Store Password " +
                            "and Identity Store private key Password cannot be found.");
                }
                isValid = true;
            }
        }
        return isValid;
    }
}
