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
package org.apache.synapse.securevault;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.securevault.secret.SecretCallbackHandler;
import org.apache.synapse.securevault.secret.SecretCallbackHandlerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Factory for creating <code>SecretResolver</code> instances
 */
public class SecretResolverFactory {

    /**
     * Creates an <code>SecretResolver</code> instance from an XML
     *
     * @param configuration <code>SecretResolver</code> configuration as XML object
     * @param isCapLetter   whether the XML element begins with a cap letter
     * @return an <code>SecretResolver</code> instance
     */
    public static SecretResolver create(OMElement configuration, boolean isCapLetter) {

        SecretResolver secretResolver = new SecretResolver();

        QName pwProviderQName;
        QName protectedTokensQName;
        QName pwManagerQName;

        QName parentQName = configuration.getQName();
        String nsURI = parentQName != null ? parentQName.getNamespaceURI() :
                XMLConstants.NULL_NS_URI;
        String nsPrefix = parentQName != null ? parentQName.getPrefix() :
                XMLConstants.DEFAULT_NS_PREFIX;

        if (!isCapLetter) {
            pwManagerQName = new QName(nsURI, SecurityConstants.PASSWORD_MANAGER_SIMPLE, nsPrefix);
            pwProviderQName = new QName(nsURI, SecurityConstants.PASSWORD_PROVIDER_SIMPLE,
                    nsPrefix);
            protectedTokensQName = new QName(nsURI, SecurityConstants.PROTECTED_TOKENS_SIMPLE,
                    nsPrefix);
        } else {
            pwManagerQName = new QName(nsURI, SecurityConstants.PASSWORD_MANAGER_CAP, nsPrefix);
            pwProviderQName = new QName(nsURI, SecurityConstants.PASSWORD_PROVIDER_CAP, nsPrefix);
            protectedTokensQName = new QName(nsURI, SecurityConstants.PROTECTED_TOKENS_CAP,
                    nsPrefix);
        }

        OMElement child = configuration.getFirstChildWithName(pwManagerQName);
        if (child == null) {
            return secretResolver;
        }
        OMElement passwordProviderElement = child.getFirstChildWithName(pwProviderQName);

        if (passwordProviderElement != null) {
            initPasswordManager(secretResolver, passwordProviderElement.getText());
            if (secretResolver.isInitialized()) {
                OMElement protectedTokensElement =
                        child.getFirstChildWithName(protectedTokensQName);
                if (protectedTokensElement != null) {
                    String value = protectedTokensElement.getText();
                    if (value != null && value.trim().length() > 0) {
                        List<String> protectedTokens = new ArrayList<String>(Arrays
                                .asList(value.split(",")));
                        for (String token : protectedTokens) {
                            secretResolver.addProtectedToken(token);
                        }
                    }
                }
            }
        }
        return secretResolver;
    }

    /**
     * Creates an <code>SecretResolver</code> instance from a set of property
     *
     * @param properties     configuration properties
     * @param propertyPrefix prefix to identify suitable configuration properties
     * @return an <code>SecretResolver</code> instance
     */
    public static SecretResolver create(Properties properties, String propertyPrefix) {

        SecretResolver secretResolver = new SecretResolver();

        String prefix = propertyPrefix;
        if (propertyPrefix != null && !"".equals(propertyPrefix) && !propertyPrefix.endsWith(".")) {
            prefix += ".";
        }
        initPasswordManager(secretResolver, properties.getProperty(prefix +
                SecurityConstants.PASSWORD_PROVIDER_SIMPLE));

        if (secretResolver.isInitialized()) {
            String protectedTokens = properties.getProperty(prefix +
                    SecurityConstants.PROTECTED_TOKENS_SIMPLE);
            if (protectedTokens != null && !"".equals(protectedTokens.trim())) {
                ArrayList<String> tokens = new ArrayList<String>(Arrays
                        .asList(protectedTokens.split(",")));
                for (String token : tokens) {
                    secretResolver.addProtectedToken(token);
                }
            }
        }
        return secretResolver;
    }

    /**
     * Creates an <code>SecretResolver</code> instance from a set of DOM Node
     *
     * @param namedNodeMap DOM node set
     * @return an <code>SecretResolver</code> instance
     */
    public static SecretResolver create(NamedNodeMap namedNodeMap) {

        SecretResolver secretResolver = new SecretResolver();

        Node namedItem = namedNodeMap.getNamedItem(SecurityConstants.PASSWORD_PROVIDER_SIMPLE);
        if (namedItem != null) {
            String passwordProvider = namedItem.getNodeValue();
            if (passwordProvider != null && passwordProvider.trim().length() > 0) {
                initPasswordManager(secretResolver, passwordProvider);
            }
        }

        if (secretResolver.isInitialized()) {
            Node protectedTokenAttr = namedNodeMap.getNamedItem(
                    SecurityConstants.PROTECTED_TOKENS_SIMPLE);
            ArrayList<String> protectedTokenList;
            if (protectedTokenAttr != null) {
                String protectedTokens = protectedTokenAttr.getNodeValue();
                if (protectedTokens != null && protectedTokens.trim().length() > 0) {
                    protectedTokenList = new ArrayList<String>(Arrays.asList(protectedTokens
                            .split(",")));
                    for (String token : protectedTokenList) {
                        if (token != null && !"".equals(token)) {
                            secretResolver.addProtectedToken(token);
                        }
                    }
                }
            }
        }
        return secretResolver;
    }

    private static void initPasswordManager(SecretResolver secretResolver, String provider) {
        SecretCallbackHandler callbackHandler =
                SecretCallbackHandlerFactory.createSecretCallbackHandler(provider);
        if (callbackHandler != null) {
            secretResolver.init(callbackHandler);
        }
    }
}
