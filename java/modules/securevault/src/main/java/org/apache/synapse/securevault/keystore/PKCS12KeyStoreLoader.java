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


public class PKCS12KeyStoreLoader extends AbstractKeyStoreLoader {

    private String keyStorePath;
    private String keyStorePassword;

    /**
     * constructs an instance of KeyStoreLoader
     *
     * @param keystorePath     - path to KeyStore file.  KeyStore must be in pkcs12 format.
     * @param keyStorePassword - password to access keyStore
     */
    public PKCS12KeyStoreLoader(String keystorePath, String keyStorePassword) {
        this.keyStorePath = keystorePath;
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * returns KeyStore to be used
     */
    public KeyStore getKeyStore() {
        return getKeyStore(keyStorePath, keyStorePassword, "PKCS12", "SunJSSE");
    }
}
