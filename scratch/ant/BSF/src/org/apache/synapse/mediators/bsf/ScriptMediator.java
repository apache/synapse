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

import org.apache.axiom.om.OMElement;
import org.apache.bsf.BSFEngine;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Property;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * A Synapse mediator that calls a function in any scripting language supportted by BSF.
 */
public class ScriptMediator extends AbstractMediator {

    private String scriptKey;

    private BSFEngine bsfEngine;

    public ScriptMediator(String scriptKey) {
        this.scriptKey = scriptKey;
    }

    public boolean mediate(MessageContext synCtx) {
        try {

            Object[] args = new Object[] { new ScriptMessageContext(synCtx) };
            SynapseConfiguration synapseConfig = synCtx.getConfiguration();

            Object response = getBSFEngine(synapseConfig).call(null, "mediate", args);
            if (response instanceof Boolean) {
                return ((Boolean) response).booleanValue();
            }

            return true; // default to returning true

        } catch (BSFException e) {
            throw new SynapseException(e);
        }
    }

    public synchronized BSFEngine getBSFEngine(SynapseConfiguration synapseConfig) {

        Property dp = synapseConfig.getPropertyObject(scriptKey);
        boolean requiresRefresh = (dp != null) && (!dp.isCached() || dp.isExpired());

        if (bsfEngine == null || requiresRefresh) {
            OMElement el = (OMElement) synapseConfig.getProperty(scriptKey);
            String scriptSrc = el.getText();
            this.bsfEngine = createBSFEngine(dp.getSrc().toString(), scriptSrc);
        }

        return bsfEngine;
    }

    public BSFEngine createBSFEngine(String scriptName, String scriptSrc) {
        try {

            BSFManager bsfManager = new BSFManager();
            bsfManager.setClassLoader(BSFManager.class.getClassLoader());

            String scriptLanguage = BSFManager.getLangFromFilename(scriptName);
            BSFEngine bsfEngine = bsfManager.loadScriptingEngine(scriptLanguage);
            bsfEngine.exec(scriptName, 0, 0, scriptSrc);

            return bsfEngine;

        } catch (BSFException e) {
            throw new SynapseException(e.getTargetException());
        }
    }

}
