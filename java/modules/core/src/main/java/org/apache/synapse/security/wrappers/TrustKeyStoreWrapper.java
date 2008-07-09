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

import org.apache.synapse.security.definition.TrustKeyStoreInformation;

import java.security.KeyStore;

/**
 * Represents the abstraction for trusted KeyStore
 * Only expose to get public keys
 */
public class TrustKeyStoreWrapper extends KeyStoreWrapper {
    /**
     * @see org.apache.synapse.security.wrappers.KeyStoreWrapper
     *      There is no keyPassword as trusted Store doesn't keep private or secret keys
     */
    public void init(TrustKeyStoreInformation information) {
        super.init(information, null);
    }

    /**
     * Abstraction for getting Trusted KeyStore
     *
     * @return KeyStore instance
     */
    public KeyStore getTrustKeyStore() {
        return getKeyStore();
    }
}
