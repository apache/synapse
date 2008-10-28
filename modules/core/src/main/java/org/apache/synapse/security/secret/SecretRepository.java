package org.apache.synapse.security.secret;

import java.util.Properties;

/**
 * Represents the abstraction 'Repository of secret'
 * Implementation can be any type - file,jdbc
 */
public interface SecretRepository {
    /**
     * Initializes the repository based on provided properties
     *
     * @param properties Configuration properties
     * @param id         Identifier to identify properties related to the corresponding repository
     */
    void init(Properties properties, String id);

    /**
     * Returns the secret of provided alias name . An alias represents the logical name
     * for a look up secret
     *
     * @param alias Alias name for look up a secret
     * @return Secret if there is any , otherwise ,alias itself
     */
    String getSecret(String alias);

    /**
     * Sets the parent secret repository
     * Secret Repositories are made a chain so that , one can get a secret from other.
     * For example, JDBC password can be in file based secret repository
     *
     * @param parent Parent secret repository
     */
    void setParent(SecretRepository parent);

    /**
     * Returns the parent secret repository
     *
     * @return Parent secret repository
     */
    SecretRepository getParent();

}
