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

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;


/**
 * An inline script mediator has the script source embedded in the config XML:
 * 
 * <pre>
 *   &lt;script.LL&amp;gt...src code...&lt;script.LL/&gt;
 * </pre>
 * 
 * <p>
 * where LL is the script language name extension. The environment of the script 
 * has the Synapse MessageContext predefined in a script variable named 'mc'.
 */
public class InlineScriptMediator extends ScriptMediator {

    private String scriptName;

    private String scriptSrc;

    public InlineScriptMediator(String scriptName, String scriptSrc) {
        super(null, null);
        this.scriptName = scriptName;
        this.scriptSrc = scriptSrc;
    }

    public boolean mediate(MessageContext synCtx) {
        try {

            ThreadLocalMessageContext.setMC(new ScriptMessageContext(synCtx, convertor));

            Object response = bsfEngine.eval(scriptName, 0, 0, scriptSrc);

            if (response instanceof Boolean) {
                return ((Boolean) response).booleanValue();
            }

            return true; // default to returning true

        } catch (BSFException e) {
            throw new SynapseException(e);
        }
    }

    public void init() {
        try {
            this.bsfManager.declareBean("mc", new ThreadLocalMessageContext(), ThreadLocalMessageContext.class);
            String scriptLanguage = BSFManager.getLangFromFilename(scriptName);
            this.bsfEngine = bsfManager.loadScriptingEngine(scriptLanguage);
            this.convertor = createOMElementConvertor(scriptName);
            convertor.setEngine(bsfEngine);
        } catch (BSFException e) {
            throw new SynapseException(e);
        }
    }
}
