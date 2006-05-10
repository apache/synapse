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
package org.apache.synapse.mediators.builtin;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Util;
import org.apache.axiom.om.xpath.AXIOMXPath;

/**
 * The property mediator would save a named property as a local property
 * of the Synapse Message Context. Properties set this way could be
 * extracted through the XPath extension function "synapse:get-property(prop-name)"
 */
public class PropertyMediator extends AbstractMediator {

    private String name = null;
    private String value = null;
    private AXIOMXPath expression = null;

    /**
     * Sets a property into the current (local) Synapse Context
     * @param smc the message context
     * @return true always
     */
    public boolean mediate(MessageContext smc) {
        smc.setProperty(getName(),
            (value != null ? getValue() : Util.getStringValue(getExpression(), smc)));
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AXIOMXPath getExpression() {
        return expression;
    }

    public void setExpression(AXIOMXPath expression) {
        this.expression = expression;
    }
}
