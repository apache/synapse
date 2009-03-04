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
import org.apache.synapse.security.wrappers.IdentityKeyStoreWrapper;
import org.apache.synapse.security.wrappers.TrustKeyStoreWrapper;
import org.apache.synapse.security.mbean.SecretManagerAdmin;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.commons.util.MBeanRegistrar;

import java.util.Properties;

/**
 * Entry point for manage secrets
 */
public class SecretManager {

    private static Log log = LogFactory.getLog(SecretManager.class);

    private final static SecretManager SECRET_MANAGER = new SecretManager();

    /* Default configuration file path for secret manager*/
    private final static String DEFAULT_CONF_LOCATION = "secret-manager.properties";
    /* If the location of the secret manager configuration is provided as a property- it's name */
    private final static String SECRET_MANAGER_CONF = "secret.manager.conf";
    /* Property key for secretRepositories*/
    private final static String SECRET_REPOSITORIES = "secretRepositories";
    /* Type of the secret repository */
    private final static String PROVIDER = "provider";

    private final static String DOT = ".";

    /*Root Secret Repository */
    private SecretRepository parentRepository;
    /* True , if secret manage has been started up properly- need to have a at
    least one Secret Repository*/
    private boolean initialized = false;
    /* Identity key store password*/
    private String identityStorePassword;
    /* Identity key password */
    private String identityKeyPassword;
    /* Trust store password */
    private String trustStorePassword;

    public static SecretManager getInstance() {
        return SECRET_MANAGER;
    }

    private SecretManager() {
        registerMBean();
    }

    /**
     * Initialize the Secret Manager
     */
    public void init() {

        if (initialized) {
            if (log.isDebugEnabled()) {
                log.debug("Secret Manager already has been started.");
            }
            return;
        }

        Properties configurationProperties = loadConfigurationPropertis();
        if (configurationProperties == null) {
            if (log.isDebugEnabled()) {
                log.debug("Secret Manager configuration properties cannot be found. " +
                        "Cannot initialize SecretManager");
            }
            return;
        }

        if (assertValidPasswords(identityStorePassword, identityKeyPassword, trustStorePassword)) {
            init(configurationProperties, identityStorePassword, identityKeyPassword, trustStorePassword);
        }
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

    public void setIdentityStorePassword(String identityStorePassword) {
        this.identityStorePassword = identityStorePassword;
    }

    public void setIdentityKeyPassword(String identityKeyPassword) {
        this.identityKeyPassword = identityKeyPassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    /**
     * Initializes the Secret Manager .Paswords for both trusted and private keyStores have to be
     * provided separately due to security reasons
     *
     * @param configurationProperties Secret Manager configuration properties
     * @param identityStorePass       Password to access private  keyStore
     * @param identityKeyPass         Password to access private or secret keys
     * @param trustStorePass          Password to access trusted KeyStore
     */
    private void init(Properties configurationProperties, String identityStorePass,
                      String identityKeyPass, String trustStorePass) {

        Properties keyStoreProperties = SynapsePropertiesLoader.loadSynapseProperties();
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

        TrustKeyStoreWrapper trustKeyStoreWrapper = new TrustKeyStoreWrapper();
        trustKeyStoreWrapper.init(trustInformation);

        SecretRepository currentParent = null;
        for (String secretRepo : repositories) {

            StringBuffer sb = new StringBuffer();
            sb.append(SECRET_REPOSITORIES);
            sb.append(DOT);
            sb.append(secretRepo);
            String id = sb.toString();
            sb.append(DOT);
            sb.append(PROVIDER);

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
                        log.debug("Successfully Initiate a Secret Repository provided by : " +
                                provider);
                    }
                } else {
                    handleException("Invalid class as SecretRepositoryProvider : Class Name : " +
                            provider);
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

    private boolean assertValidPasswords(String identityStorePass,
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
                isValid = true;
            }
        }
        return isValid;
    }

    private Properties loadConfigurationPropertis() {
        Properties keyStoreProperties = SynapsePropertiesLoader.loadSynapseProperties();
        if (keyStoreProperties != null) {
            String configurationFile = MiscellaneousUtil.getProperty(
                    keyStoreProperties, SECRET_MANAGER_CONF, DEFAULT_CONF_LOCATION);

            Properties configurationProperties = MiscellaneousUtil.loadProperties(configurationFile);
            if (configurationProperties == null || configurationProperties.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Configuration properties can not be loaded form : " +
                            configurationFile + " Will use synapse properties");
                }
                configurationProperties = keyStoreProperties;

            }
            return configurationProperties;
        }
        return null;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void registerMBean() {
        MBeanRegistrar mBeanRegistrar = MBeanRegistrar.getInstance();
        mBeanRegistrar.registerMBean(new SecretManagerAdmin(this), "SecurityAdminServices",
                "SecretManagerAdmin");
    }
}
