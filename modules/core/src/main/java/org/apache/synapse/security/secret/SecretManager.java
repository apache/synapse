/**
 *
 */
package org.apache.synapse.security.secret;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.security.definition.IdentityKeyStoreInformation;
import org.apache.synapse.security.definition.TrustKeyStoreInformation;
import org.apache.synapse.security.definition.factory.KeyStoreInformationFactory;
import org.apache.synapse.security.wrappers.IdentityKeyStoreWrapper;
import org.apache.synapse.security.wrappers.TrustKeyStoreWrapper;
import org.apache.synapse.security.mbean.SecretManagerAdmin;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.commons.util.MBeanRegistrar;
import org.apache.synapse.commons.util.secret.*;

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
    /* Property key secret manager */
    private final static String PROP_SECRET_MANAGER = "secretManager";
    /* Property key password provider */
    private final static String PROP_PASSWORD_PROVIDER = "passwordProvider";
    /* Prompt for trust store password*/
    private final static String TRUSTSTORE_PASSWORD_PROMPT = "Trust Store Password > ";
    /* Prompt for identity store password*/
    private final static String IDENTITYSTORE_PASSWORD_PROMPT = "Identity Store Password > ";
    /* Prompt for identity store private key password*/
    private final static String IDENTITYSTORE_PRIVATE_KEY_PASSWORD_PROMPT
            = "Identity Store Private Key Password > ";
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
        registerMBean();
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

        SecretCallbackHandler secretCallbackHandler =
                SecretCallbackHandlerFactory.createSecretCallbackHandler(properties,
                        PROP_SECRET_MANAGER + DOT + PROP_PASSWORD_PROVIDER);

        if (secretCallbackHandler == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to find a SecretCallbackHandler and so " +
                        " cannot get passwords required for " +
                        "root level secret repositories - trust store password or  identity " +
                        "store password and it's private key password");
            }
            return;
        }

        String identityStorePass;
        String identityKeyPass;
        String trustStorePass;

        // Creating required password class backs
        SingleSecretCallback trustStorePassSecretCallback
                = new SingleSecretCallback(TRUSTSTORE_PASSWORD_PROMPT);
        SingleSecretCallback identityStorePassSecretCallback
                = new SingleSecretCallback(IDENTITYSTORE_PASSWORD_PROMPT);
        SingleSecretCallback identityKeyPassSecretCallback
                = new SingleSecretCallback(IDENTITYSTORE_PRIVATE_KEY_PASSWORD_PROMPT);

        // Group all as a one callback
        MultiSecretCallback callback = new MultiSecretCallback();
        callback.addSecretCallback(trustStorePassSecretCallback);
        callback.addSecretCallback(identityStorePassSecretCallback);
        callback.addSecretCallback(identityKeyPassSecretCallback);
        SecretCallback[] secretCallbacks = new SecretCallback[]{callback};

        // Create and initiating SecretLoadingModule
        SecretLoadingModule secretLoadingModule = new SecretLoadingModule();
        secretLoadingModule.init(new SecretCallbackHandler[]{secretCallbackHandler});

        //load passwords
        secretLoadingModule.load(secretCallbacks);

        identityKeyPass = identityKeyPassSecretCallback.getSecret();
        identityStorePass = identityStorePassSecretCallback.getSecret();
        trustStorePass = trustStorePassSecretCallback.getSecret();

        if (validatePasswords(identityStorePass, identityKeyPass, trustStorePass)) {
            if (log.isDebugEnabled()) {
                log.debug("Either Identity or Trust keystore password is mandotory" +
                        " in order to initialized secret manager.");
            }
            return;
        }

        //Create a KeyStore Information  for private key entry KeyStore
        IdentityKeyStoreInformation keyStoreInformation =
                KeyStoreInformationFactory.createIdentityKeyStoreInformation(properties);
        keyStoreInformation.setKeyStorePassword(identityStorePass);

        // Create a KeyStore Information for trusted certificate KeyStore
        TrustKeyStoreInformation trustInformation =
                KeyStoreInformationFactory.createTrustKeyStoreInformation(properties);
        trustInformation.setKeyStorePassword(trustStorePass);

        IdentityKeyStoreWrapper identityKeyStoreWrapper = new IdentityKeyStoreWrapper();
        identityKeyStoreWrapper.init(keyStoreInformation, identityKeyPass);

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
        throw new SynapseException(msg);
    }

    private void registerMBean() {
        MBeanRegistrar mBeanRegistrar = MBeanRegistrar.getInstance();
        mBeanRegistrar.registerMBean(new SecretManagerAdmin(this),
                "SecurityAdminServices", "SecretManagerAdmin");
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