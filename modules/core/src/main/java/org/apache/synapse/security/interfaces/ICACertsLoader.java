package org.apache.synapse.security.interfaces;

import java.security.KeyStore;

/**
 * ICACertsLoader provides an uniform interface to create a keyStore containing CA certs
 * (truststore)
 */
public interface ICACertsLoader {
    /**
     * @param CACertificateFilesPath Path to the CA certificates directory
     * @return KeyStore Instance
     */
    public abstract KeyStore loadTrustStore(String CACertificateFilesPath);
}
