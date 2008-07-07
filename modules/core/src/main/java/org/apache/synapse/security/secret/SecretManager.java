/**
 *
 */
package org.apache.synapse.security.secret;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.security.definition.IdentityKeyStoreInformation;
import org.apache.synapse.security.definition.TrustKeyStoreInformation;
import org.apache.synapse.security.definition.factory.KeyStoreInformationFactory;
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

    /* Default configuration file path for secret manager*/
    private final static String DEFAULT_CONF_LOCATION = "secret-manager.properties";
    /* If the location of the secret manager configuration is provided as a property- it's name */
    private final static String SECRET_MANAGER_CONF = "secret.manager.conf";
    /* Property key for secretRepositories*/
    private final static String SECRET_REPOSITORIES = "secretRepositories";
    /* Type of the secret repository */
    private final static String TYPE = "type";

    private final static String DOT = ".";
    /* Secret Repository type - file */
    private final static String REPO_TYPE_FILE = "file";

    /*Root Secret Repository */
    private SecretRepository parentRepository;
    /* True , if secret manage has been started up properly- need to have a at
    least one Secret Repository*/
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

        Properties keyStoreProperties = SynapsePropertiesLoader.loadSynapseProperties();
        if (keyStoreProperties == null) {
            if (log.isDebugEnabled()) {
                log.debug("KeyStore configuration properties cannot be found");
            }
            return;
        }

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

        //Create a KeyStore Information  for private key entry KeyStore
        IdentityKeyStoreInformation keyStoreInformation =
                KeyStoreInformationFactory.createIdentityKeyStoreInformation(keyStoreProperties);
        keyStoreInformation.setKeyStorePassword(identityStorePass);

        // Create a KeyStore Information for trusted certificate KeyStore
        TrustKeyStoreInformation trustInformation =
                KeyStoreInformationFactory.createTrustKeyStoreInformation(keyStoreProperties);
        trustInformation.setKeyStorePassword(trustStorePass);

        IdentityKeyStoreWrapper identityKeyStoreWrapper = new IdentityKeyStoreWrapper();
        identityKeyStoreWrapper.init(keyStoreInformation, identityKeyPass);

        TrustKeyStoreWrapper trustStoreWrapper = new TrustKeyStoreWrapper();
        trustStoreWrapper.init(trustInformation);

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

                if (log.isDebugEnabled()) {
                    log.debug("Initiating a File Based Secret Repository");
                }

                SecretRepository secretRepository = new FileBaseSecretRepository(
                        identityKeyStoreWrapper, trustStoreWrapper);
                secretRepository.init(configurationProperties, id);
                if (parentRepository == null) {
                    parentRepository = secretRepository;
                }
                secretRepository.setParent(currentParent);
                currentParent = secretRepository;
                initialize = true;

                if (log.isDebugEnabled()) {
                    log.debug("Successfully Initiate a File Based Secret Repository");
                }
            } else {
                log.warn("Unsupported secret repository type : " + type);
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
            if (log.isDebugEnabled()) {
                log.debug("There is no secret repository. Returning alias itself");
            }
            return alias;
        }
        return parentRepository.getSecret(alias);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
