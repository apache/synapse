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
package org.apache.synapse.commons.security.secret;


/**
 * Encapsulates the All information related to a DataSource
 */
public class SecretInformation {

    private String user;
    private String aliasSecret;
    private String secretPrompt;
    private SecretCallbackHandler secretProvider;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAliasSecret() {
        return aliasSecret;
    }

    public void setAliasSecret(String aliasSecret) {
        this.aliasSecret = aliasSecret;
    }
    
    public String getSecretPrompt() {
        return secretPrompt;
    }

    public void setSecretPrompt(String secretPrompt) {
        this.secretPrompt = secretPrompt;
    }

    /**
     * Get actual password based on SecretCallbackHandler and alias password
     * If SecretCallbackHandler is null, then returns alias password
     * @return  Actual password
     */
    public String getResolvedSecret() {

        if (secretProvider != null) {
            if (aliasSecret != null && !"".equals(aliasSecret)) {
                return getSecret(secretProvider, aliasSecret, secretPrompt);
            }
        }
        return aliasSecret;
    }
    
    public SecretCallbackHandler getSecretProvider() {
        return secretProvider;
    }

    public void setSecretProvider(SecretCallbackHandler secretProvider) {
        this.secretProvider = secretProvider;
    }
    
    private String getSecret(SecretCallbackHandler secretCallbackHanlder, String encryptedPassword, String prompt) {
        SecretLoadingModule secretLoadingModule = new SecretLoadingModule();
        secretLoadingModule.init(new SecretCallbackHandler[]{secretCallbackHanlder});
        SingleSecretCallback secretCallback = new SingleSecretCallback(encryptedPassword);
        if (prompt != null) {
            secretCallback.setPrompt(prompt);
        }
        secretLoadingModule.load(new SecretCallback[]{secretCallback});
        return secretCallback.getSecret();
    }


}
