package org.apache.synapse.security.interfaces;

import java.security.KeyStore;

/**
 * Provides way to load KeyStore
 */
public interface IKeyStoreLoader {

    /**
     * returns an instance of KeyStore object
     *
     * @return KeyStore Instance
     */
    public abstract KeyStore getKeyStore();
}
