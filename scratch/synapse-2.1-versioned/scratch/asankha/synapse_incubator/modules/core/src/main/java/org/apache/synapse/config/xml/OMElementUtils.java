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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.jaxen.JaxenException;

import java.util.Iterator;

/**
 * Holds Axiom utility methods used by Synapse
 */
public class OMElementUtils {

    private static final Log log = LogFactory.getLog(OMElementUtils.class);

    /**
     * Return the namespace with the given prefix, using the given element
     * @param prefix the prefix looked up
     * @param elem the source element to use
     * @return the namespace which maps to the prefix or null
     */
    public static String getNameSpaceWithPrefix(String prefix, OMElement elem) {
        if (prefix == null || elem == null) {
            log.warn("Searching for null NS prefix and/or using null OMElement");
            return null;
        }

        Iterator iter = elem.getAllDeclaredNamespaces();
        while (iter.hasNext()) {
            OMNamespace ns = (OMNamespace) iter.next();
            if (prefix.equals(ns.getPrefix())) {
                return ns.getName();
            }
        }
        return null;
    }

    /**
     * Add xmlns NS declarations of element 'elem' into XPath expression
     * @param xpath
     * @param elem
     * @param log
     */
    public static void addNameSpaces(AXIOMXPath xpath, OMElement elem, Log log) {
        try {
            Iterator it = elem.getAllDeclaredNamespaces();
            while (it.hasNext()) {
                OMNamespace n = (OMNamespace) it.next();
                xpath.addNamespace(n.getPrefix(), n.getName());
            }
        } catch (JaxenException je) {
            String msg = "Error adding declared name spaces of " + elem + " to the XPath : " + xpath;
            log.error(msg);
            throw new SynapseException(msg, je);
        }
    }

}
