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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;

import java.util.Collection;
import java.util.Iterator;

public class AbstractMediatorSerializer {

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = fac.createOMNamespace(Constants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS = fac.createOMNamespace(Constants.NULL_NAMESPACE, "");
    private static final MediatorPropertySerializer propSerializer = new MediatorPropertySerializer();

    public void serializeProperties(OMElement parent, Collection props) {
        propSerializer.serializeMediatorProperties(parent, props);
    }

    public void serializeNamespaces(OMElement elem, AXIOMXPath xpath) {
        Iterator iter = xpath.getNamespaces().keySet().iterator();
        while (iter.hasNext()) {
            String prefix = (String) iter.next();
            String uri = xpath.getNamespaceContext().translateNamespacePrefixToUri(prefix);
            if (!Constants.SYNAPSE_NAMESPACE.equals(uri)) {
                elem.declareNamespace(uri, prefix);
            }
        }
    }
}
