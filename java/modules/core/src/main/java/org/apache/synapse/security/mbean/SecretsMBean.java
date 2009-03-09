package org.apache.synapse.security.mbean;

/**
 * Managing MBean for secrct
 */
public interface SecretsMBean {

    /**
     * Add a secret through JMX
     *
     * @param id     identify for what this secret is
     * @param secret Secret
     */
    public void addSecret(String id, String secret);

    /**
     * Remove a Secect for given ID
     *
     * @param id identify for what this secret is
     */
    public void removeSecret(String id);

    /**
     * Clear all secrets
     */
    public void clear();
}
