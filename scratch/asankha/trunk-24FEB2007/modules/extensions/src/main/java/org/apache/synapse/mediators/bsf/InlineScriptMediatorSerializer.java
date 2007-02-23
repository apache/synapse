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
package org.apache.synapse.mediators.bsf;

import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;

/**
 * Serializer for a script mediator, which use a script specified in inline in the Synapse config XML
 */

public class InlineScriptMediatorSerializer extends AbstractMediatorSerializer {

    private static final Log log = LogFactory.getLog(InlineScriptMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {
        if (!(m instanceof InlineScriptMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }
        InlineScriptMediator inlineScriptMediator = (InlineScriptMediator) m;
        String scriptName = inlineScriptMediator.getScriptName();
        OMElement script = fac.createOMElement(scriptName, synNS);
        String scriptSrc = inlineScriptMediator.getScriptSrc();
        OMText src = fac.createOMText(scriptSrc.trim());
        script.addChild(src);
        if (parent != null) {
            parent.addChild(script);
        }
        return script;

    }

    public String getMediatorClassName() {
        return InlineScriptMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}

