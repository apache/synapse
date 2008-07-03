/**
 *
 */
package org.apache.synapse.security.secret;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.security.bean.KeyStoreInformation;
import org.apache.synapse.security.secret.repository.FileBaseSecretRepository;
import org.apache.synapse.security.wrappers.IdentityKeyStoreWrapper;
import org.apache.synapse.security.wrappers.TrustKeyStoreWrapper;
import org.apache.synapse.util.MiscellaneousUtil;

import java.util.Properties;

/**
 * Entry point for manage secrets
 */
public class SecretManager {

    private static Log log = LogFactory.getLog(SecretManager.class);

    private static SecretManager ourInstance = new SecretManager();

    private final static String DEFAULT_CONF_LOCATION = "secret-manager.properties";
    private final static String SECRET_MANAGER_CONF = "secret-manage-conf";
    private final static String SECRET_REPOSITORIES = "secretRepositories";
    private final static String TYPE = "type";
    private final static String IDENTITY_KEY_STORE = "keystore.identity.location";
    private final static String IDENTITY_KEY_STORE_TYPE = "keystore.identity.type";
    private final static String IDENTITY_KEY_STORE_ALIAS = "keystore.identity.alias";
    private final static String TRUST_KEY_STORE = "keystore.trust.location";
    private final static String TRUST_KEY_STORE_TYPE = "keystore.trust.type";
    private final static String TRUST_KEY_STORE_ALIAS = "keystore.trust.alias";
    private final static String DOT = ".";
    private final static String REPO_TYPE_FILE = "file";

    private SecretRepository parentRepository;
    private boolean initialize = false;

    public static SecretManager getInstance() {
        return ourInstance;
    }

    private SecretManager() {
    }

    /**
     * Initializes the Secret Manager .Paswords for both trusted and private keyStores have to be
     * provided separately due to security reasons
     *
     * @param properties        Configuration properties for manager except passwords
     * @param identityStorePass Password to access private  keyStore
     * @param identityKeyPass   Password to access private or secret keys
     * @param trustStorePass    Password to access trusted KeyStore
     */
    public void init(Properties properties, String identityStorePass, String identityKeyPass, String trustStorePass) {

        String configurationFile = MiscellaneousUtil.getProperty(
                properties, SECRET_MANAGER_CONF, DEFAULT_CONF_LOCATION);

        Properties configurationProperties = MiscellaneousUtil.loadProperties(configurationFile);
        if (configurationProperties == null || configurationProperties.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Configuration properties can not be loaded form : " + configurationFile);
            }
            return;

        }

        String repositoriesString = MiscellaneousUtil.getProperty(
                configurationProperties, SECRET_REPOSITORIES, null);
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

        KeyStoreInformation keyStoreInformation = new KeyStoreInformation();

        keyStoreInformation.setAlias(
                MiscellaneousUtil.getProperty(configurationProperties, IDENTITY_KEY_STORE_ALIAS, null));
        keyStoreInformation.setLocation(
                MiscellaneousUtil.getProperty(configurationProperties, IDENTITY_KEY_STORE, null));
        keyStoreInformation.setStoreType(
                MiscellaneousUtil.getProperty(configurationProperties, IDENTITY_KEY_STORE_TYPE, null));

        KeyStoreInformation trustInformation = new KeyStoreInformation();

        trustInformation.setAlias(
                MiscellaneousUtil.getProperty(configurationProperties, TRUST_KEY_STORE, null));
        trustInformation.setLocation(
                MiscellaneousUtil.getProperty(configurationProperties, TRUST_KEY_STORE_ALIAS, null));
        trustInformation.setStoreType(
                MiscellaneousUtil.getProperty(configurationProperties, TRUST_KEY_STORE_TYPE, null));

        IdentityKeyStoreWrapper identityKeyStoreWrapper = new IdentityKeyStoreWrapper();
        identityKeyStoreWrapper.init(keyStoreInformation, identityStorePass, identityKeyPass);

        TrustKeyStoreWrapper trustStoreWrapper = new TrustKeyStoreWrapper();
        trustStoreWrapper.init(keyStoreInformation, trustStorePass);

        SecretRepository currentParent = null;
        for (String secretRepo : repositories) {

            StringBuffer sb = new StringBuffer();
            sb.append(SECRET_REPOSITORIES);
            sb.append(DOT);
            sb.append(secretRepo);
            String id = sb.toString();
            sb.append(DOT);
            sb.append(TYPE);

            String type = MiscellaneousUtil.getProperty(
                    configurationProperties, sb.toString(), null);
            if (type == null || "".equals(type)) {
                handleException("Repository type cannot be null ");
            }

            if (REPO_TYPE_FILE.equals(type)) {
                SecretRepository secretRepository = new FileBaseSecretRepository(
                        identityKeyStoreWrapper, trustStoreWrapper);
                secretRepository.init(configurationProperties, id);
                if (parentRepository == null) {
                    parentRepository = secretRepository;
                }
                secretRepository.setParent(currentParent);
                currentParent = secretRepository;
                initialize = true;
            } else {
                log.warn("Unsupported secret sepository type : " + type);
            }

        }

    }

    /**
     * Returns the secret corresponding to the given alias name
     *
     * @param alias The logical or alias name
     * @return If there is a secret , otherwise , alias itself
     */
    public String getSecret(String alias) {
        if (!initialize || parentRepository == null) {
            return alias;
        }
        return parentRepository.getSecret(alias);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
