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
import org.apache.synapse.util.SynapseXPath;

/**
 * 
 */
public class SynapseXPathSerializer {

    private static final Log log = LogFactory.getLog(SynapseXPathSerializer.class);

    public static OMElement serializeXPath(SynapseXPath xpath, OMElement elem, String attribName) {

        AXIOMXPath omXPath = xpath.getAXIOMXPath();
        OMNamespace nullNS = elem.getOMFactory()
            .createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");

        if (omXPath != null) {
            
            elem.addAttribute(elem.getOMFactory().createOMAttribute(
                attribName, nullNS, omXPath.toString()));

            serializeNamespaces(elem, omXPath);

            if (xpath.isBodyRelative()) {
                
                elem.addAttribute(elem.getOMFactory().createOMAttribute(
                    XMLConfigConstants.ATT_XPATH_RELATIVE.getLocalPart(),
                    nullNS, XMLConfigConstants.XPATH_BODY_RELATIVE));
            }
            
        } else {
            handleException("Couldn't find the AXIOMXPath in the SynapseXPath");
        }

        return elem;
    }

    private static void serializeNamespaces(OMElement elem, AXIOMXPath xpath) {

        for (Object o : xpath.getNamespaces().keySet()) {
            String prefix = (String) o;
            String uri = xpath.getNamespaceContext().translateNamespacePrefixToUri(prefix);
            if (!XMLConfigConstants.SYNAPSE_NAMESPACE.equals(uri)) {
                elem.declareNamespace(uri, prefix);
            }
        }
    }

    private static void handleException(String message) {
        log.error(message);
        throw new SynapseException(message); 
    }
}
