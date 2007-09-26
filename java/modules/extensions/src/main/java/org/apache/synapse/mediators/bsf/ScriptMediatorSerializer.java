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
package org.apache.synapse.mediators.bsf;

import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.Mediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.OMTextImpl;

import javax.xml.stream.XMLStreamConstants;

/**
 * Serializer for a script mediator
 * @see org.apache.synapse.mediators.bsf.ScriptMediatorFactory
 */
public class ScriptMediatorSerializer extends AbstractMediatorSerializer {

    private static final Log log = LogFactory.getLog(ScriptMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {
        if (!(m instanceof ScriptMediator) ) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        ScriptMediator scriptMediator = (ScriptMediator) m;
        OMElement script = fac.createOMElement("script", synNS);

        String language = scriptMediator.getLanguage();
        String key = scriptMediator.getKey();
        String function = scriptMediator.getFunction();

        if (key != null) {
            script.addAttribute(fac.createOMAttribute("language", nullNS, language));
            script.addAttribute(fac.createOMAttribute("key", nullNS, key));
            if (!function.equals("mediate")) {
                script.addAttribute(fac.createOMAttribute("function", nullNS, function));
            }
        } else {
            script.addAttribute(fac.createOMAttribute("language", nullNS, language));
            OMTextImpl textData = (OMTextImpl) fac.createOMText(scriptMediator.getScriptSrc().trim());
            textData.setType(XMLStreamConstants.CDATA);
            script.addChild(textData);
        }

        saveTracingState(script, scriptMediator);
        if (parent != null) {
            parent.addChild(script);
        }
        return script;
    }

    public String getMediatorClassName() {
        return ScriptMediator.class.getName();
    }
}
