package org.apache.synapse.security.interfaces;

import java.security.KeyStore;

/**
 * ICACertsLoader provides an uniform interface to create a keystore containing CA certs (truststore)
 */
public interface ICACertsLoader {
    public abstract KeyStore loadTrustStore(String CACertificateFilesPath);
}
