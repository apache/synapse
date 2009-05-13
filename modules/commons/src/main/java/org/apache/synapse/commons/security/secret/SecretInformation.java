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
    private String aliasPassword;
    private String passwordPrompt;
    private SecretCallbackHandler passwordProvider;
    
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAliasPassword() {
        return aliasPassword;
    }

    public void setAliasPassword(String aliasPassword) {
        this.aliasPassword = aliasPassword;
    }
    
    public String getPasswordPrompt() {
        return passwordPrompt;
    }

    public void setPasswordPrompt(String passwordPrompt) {
        this.passwordPrompt = passwordPrompt;
    }

    /**
     * Get actual password based on SecretCallbackHandler and alias password
     * If SecretCallbackHandler is null, then returns alias password
     * @return  Actual password
     */
    public String getResolvedPassword() {

        if (passwordProvider != null) {
            if (aliasPassword != null && !"".equals(aliasPassword)) {
                return getSecret(passwordProvider, aliasPassword, passwordPrompt);
            }
        }
        return aliasPassword;
    }
    
    public SecretCallbackHandler getPasswordProvider() {
        return passwordProvider;
    }

    public void setPasswordProvider(SecretCallbackHandler passwordProvider) {
        this.passwordProvider = passwordProvider;
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
