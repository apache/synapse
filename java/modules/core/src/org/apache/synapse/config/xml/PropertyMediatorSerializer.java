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
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.PropertyMediator;

/**
 * <pre>
 * &lt;set-property name="string" (value="literal" | expression="xpath")/&gt;
 * </pre>
 */
public class PropertyMediatorSerializer extends BaseMediatorSerializer
    implements MediatorSerializer {

    private static final Log log = LogFactory.getLog(PropertyMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof PropertyMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        PropertyMediator mediator = (PropertyMediator) m;
        OMElement property = fac.createOMElement("set-property", synNS);
        if (mediator.getName() != null) {
            property.addAttribute(fac.createOMAttribute(
                "name", nullNS, mediator.getName()));
        } else {
            handleException("Invalid property mediator. Name is required");
        }

        if (mediator.getValue() != null) {
            property.addAttribute(fac.createOMAttribute(
                "value", nullNS, mediator.getValue()));

        } else if (mediator.getExpression() != null) {
            property.addAttribute(fac.createOMAttribute(
                "expression", nullNS, mediator.getExpression().toString()));
            super.serializeNamespaces(property, mediator.getExpression());

        } else {
            handleException("Invalid property mediator. Value or expression is required");
        }

        if (parent != null) {
            parent.addChild(property);
        }
        return property;
    }

    public String getMediatorClassName() {
        return PropertyMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
