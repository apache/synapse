/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.config.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.SynapseException;
import org.jaxen.JaxenException;

import java.util.Iterator;

/**
 * Is the abstract superclass of MediatorFactory's
 */
public abstract class AbstractMediatorFactory implements MediatorFactory {
    public void addNameSpaces(OMElement elem, AXIOMXPath xp, Log log) {
        try {
            Iterator it = elem.getAllDeclaredNamespaces();
            while (it.hasNext()) {
                OMNamespace n = (OMNamespace) it.next();
                xp.addNamespace(n.getPrefix(), n.getName());
            }
        } catch (JaxenException je) {
            String msg = "Error adding declared name spaces of " + elem + " to the XPath : " + xp;
            log.error(msg);
            throw new SynapseException(msg, je);
        }
    }
}
