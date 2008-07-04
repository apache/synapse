/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.security.wrappers;

import org.apache.synapse.security.bean.KeyStoreInformation;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.PrivateKey;

/**
 * Represents the private keyStore entry
 * To provide that abstraction , this class exposes both getter methods to public,
 * private and secret keys
 */
public class IdentityKeyStoreWrapper extends KeyStoreWrapper {

    /**
     * @param information   @see KeyStoreWrapper
     * @param storePassword @see KeyStoreWrapper
     * @param keyPassword   @see KeyStoreWrapper
     * @see org.apache.synapse.security.wrappers.KeyStoreWrapper
     */
    public void init(KeyStoreInformation information, String storePassword, String keyPassword) {
        super.init(information, storePassword, keyPassword);
    }

    /**
     * Returns the private or secret  key based on given password and alias
     *
     * @param alias       The alias of the certificate in the specified keyStore
     * @param keyPassword Password to access private key
     * @return PrivateKey if there is a one , otherwise null
     */
    public PrivateKey getPrivateKey(String alias, String keyPassword) {
        Key key = super.getKey(alias, keyPassword);
        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }
        return null;
    }

    /**
     * Returns the private key based on initilization data
     *
     * @return PrivateKey if there is a one , otherwise null
     */
    public PrivateKey getPrivateKey() {
        Key key = super.getKey();
        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }
        return null;
    }

    /**
     * Returns the secret key
     *
     * @param alias       The alias of the certificate in the specified keyStore
     * @param keyPassword Password to access secret key
     * @return SecretKey if there is a one , otherwise null
     */
    public SecretKey getSecretKey(String alias, String keyPassword) {
        Key key = super.getKey(alias, keyPassword);
        if (key instanceof SecretKey) {
            return (SecretKey) key;
        }
        return null;
    }

    /**
     * Returns the secret key based on initilization data
     *
     * @return SecretKey if there is a one , otherwise null
     */
    public SecretKey getSecretKey() {
        Key key = super.getKey();
        if (key instanceof SecretKey) {
            return (SecretKey) key;
        }
        return null;
    }
}
