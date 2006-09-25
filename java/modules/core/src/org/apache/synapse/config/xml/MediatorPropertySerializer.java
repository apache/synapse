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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.MediatorProperty;

import java.util.Collection;
import java.util.Iterator;

public class MediatorPropertySerializer extends BaseMediatorSerializer {

    private static final Log log = LogFactory.getLog(MediatorPropertySerializer.class);

    public void serializeMediatorProperties(OMElement parent, Collection props) {

        Iterator iter = props.iterator();
        while (iter.hasNext()) {
            MediatorProperty mp = (MediatorProperty) iter.next();
            OMElement prop = fac.createOMElement("property", synNS, parent);
            if (mp.getName() != null) {
                prop.addAttribute(fac.createOMAttribute("name", nullNS, mp.getName()));
            } else {
                handleException("Mediator property name missing");
            }

            if (mp.getValue() != null) {
                prop.addAttribute(fac.createOMAttribute("value", nullNS, mp.getValue()));

            } else if (mp.getExpression() != null) {
                prop.addAttribute(fac.createOMAttribute("expression", nullNS,
                    mp.getExpression().toString()));
                super.serializeNamespaces(prop, mp.getExpression());

            } else {
                handleException("Mediator property must have a literal value or be an expression");
            }
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
