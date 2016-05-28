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
import org.apache.commons.logging.Log;
import org.apache.synapse.SynapseException;
import org.jaxen.JaxenException;
import org.jaxen.XPath;

import java.util.Iterator;

/**
 * Holds Axiom utility methods used by Synapse
 */
public class OMElementUtils {
    /**
     * Add the namespace declarations of a given {@link OMElement} to the namespace
     * context of an XPath expression. Typically this method is used with an XPath
     * expression appearing in an attribute of the given element.
     * <p>
     * Note that the default namespace is explicitly excluded and not added to the
     * namespace context. This implies that XPath expressions
     * appearing in Synapse configuration files follow the same rule as in XSL
     * style sheets. Indeed, the XSLT specification defines the namespace context of
     * an XPath expression as follows:
     * <blockquote>
     * the set of namespace declarations are those in scope on the element which has the
     * attribute in which the expression occurs; [...] the default namespace
     * (as declared by xmlns) is not part of this set
     * </blockquote>
     * 
     * @param xpath
     * @param elem
     * @param log
     */
    public static void addNameSpaces(XPath xpath, OMElement elem, Log log) {
        Iterator it = elem.getNamespacesInScope();
        while (it.hasNext()) {

            OMNamespace n = (OMNamespace) it.next();
            // Exclude the default namespace as explained in the Javadoc above
            if (n.getPrefix().length() > 0) {

                try {
                    xpath.addNamespace(n.getPrefix(), n.getNamespaceURI());
                } catch (JaxenException je) {
                    String msg = "Error adding declared name space with prefix : "
                        + n.getPrefix() + "and uri : " + n.getNamespaceURI()
                        + " to the XPath : " + xpath;
                    log.error(msg);
                    throw new SynapseException(msg, je);
                }
            }
        }
    }
}
