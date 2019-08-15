/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.securevault.keystore;

import java.security.KeyStore;

/**
 * Loads KeyStore from a JKS file
 */
public class JKSKeyStoreLoader extends AbstractKeyStoreLoader {

    private String keyStorePath;
    private String keyStorePassword;

    /**
     * constructs an instance of KeyStoreLoader
     *
     * @param keyStorePath     - path to KeyStore file.  KeyStore must be in JKS format.
     * @param keyStorePassword - password to access keyStore
     */
    public JKSKeyStoreLoader(String keyStorePath, String keyStorePassword) {
        super();
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * Returns KeyStore to be used
     *
     * @return KeyStore instance
     */
    public KeyStore getKeyStore() {
        return getKeyStore(keyStorePath, keyStorePassword, "JKS", null);
    }

}
