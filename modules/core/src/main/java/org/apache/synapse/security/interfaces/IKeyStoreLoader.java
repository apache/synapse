package org.apache.synapse.security.interfaces;

import java.security.KeyStore;

public interface IKeyStoreLoader {

    /**
     * returns an instance of KeyStore object
     *
     * @return KeyStore Instance
     */
    public abstract KeyStore getKeyStore();
}
