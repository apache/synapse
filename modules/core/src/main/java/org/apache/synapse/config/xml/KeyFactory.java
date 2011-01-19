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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.Key;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;

/**
 * Factory for {@link org.apache.synapse.mediators.Key} instances.
 */
public class KeyFactory {

    private static final Log log = LogFactory.getLog(KeyFactory.class);

    private static final QName ATT_KEY = new QName("key");

    /**
     * Create a key instance
     *
     * @param elem OMElement
     * @return Key
     */
    public Key createKey(OMElement elem) {

        Key key = null;
        OMAttribute attXslt = elem.getAttribute(ATT_KEY);

        if (attXslt != null) {
            String attributeValue = attXslt.getAttributeValue();
            if (isDynamicKey(attributeValue)) {
                /** dynamic key */
                SynapseXPath synXpath = createSynXpath(elem, attributeValue);
                key = new Key(synXpath);
            } else {
                /** static key */
                key = new Key(attributeValue);
            }
        } else {
            handleException("The 'key' attribute is required for the XSLT mediator");
        }
        return key;
    }


    /**
     * Validate the given key to identify whether it is static or dynamic key
     * If the key is in the {} format then it is dynamic key(XPath)
     * Otherwise just a static key
     *
     * @param keyValue string to validate as a key
     * @return isDynamicKey representing key type
     */
    public boolean isDynamicKey(String keyValue) {
        boolean dynamicKey = false;

        final char startExpression = '{';
        final char endExpression = '}';

        char firstChar = keyValue.charAt(0);
        char lastChar = keyValue.charAt(keyValue.length() - 1);

        if (startExpression == firstChar && endExpression == lastChar) {
            dynamicKey = true;
        }
        return dynamicKey;
    }

    /**
     * Create synapse xpath expression
     * {} type user input is used to create real xpath expression
     *
     * @param elem the element
     * @param key xpath expression with {}
     * @return SynapseXpath
     */
    public SynapseXPath createSynXpath(OMElement elem, String key) {
        //derive XPath Expression from key
        String xpathExpr = key.trim().substring(1, key.length() - 1);

        SynapseXPath synapseXPath = null;

        try {
            synapseXPath = SynapseXPathFactory.getSynapseXPath(elem, xpathExpr);
        } catch (JaxenException e) {
            handleException("Can not create Synapse Xpath from given key");
        }

        return synapseXPath;
    }

    /**
     * Handle exceptions
     *
     * @param msg error message
     */
    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
