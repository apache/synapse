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

import org.apache.axiom.om.OMElement;
import org.apache.bsf.BSFEngine;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Property;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.bsf.convertors.DefaultOMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.OMElementConvertor;

/**
 * A Synapse mediator that calls a function in any scripting language supported by BSF. The ScriptMediator using a registry property to define the
 * registry property which contains the script source.
 * <p>
 * 
 * <pre>
 *    &lt;script key=&quot;property-key&quot; function=&quot;script-function-name&quot; &lt;script/&gt;
 * </pre>
 * 
 * <p>
 * The function is an optional attribute defining the name of the script function to call, if not specified it defaults to a function named 'mediate'.
 * The function takes a single parameter which is the Synapse MessageContext. The function may return a boolean, if it does not then true is assumed.
 */
public class ScriptMediator extends AbstractMediator {

    protected String scriptKey;

    protected String functionName;

    protected BSFEngine bsfEngine;

    protected OMElementConvertor convertor;

    protected BSFManager bsfManager;

    public ScriptMediator(String scriptKey, String functionName) {
        this.scriptKey = scriptKey;
        this.functionName = functionName;
        bsfManager = new BSFManager();
    }

    public boolean mediate(MessageContext synCtx) {
        try {

            BSFEngine engine = getBSFEngine(synCtx.getConfiguration());

            Object[] args = new Object[] { new ScriptMessageContext(synCtx, convertor) };

            Object response = engine.call(null, functionName, args);
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
        // boolean requiresRefresh = (dp != null) && (!dp.isCached() || dp.isExpired());
        // if (bsfEngine == null || requiresRefresh) { TODO: sort out caching
        if (bsfEngine == null) {
            OMElement el = (OMElement) synapseConfig.getProperty(scriptKey);
            String scriptSrc = el.getText();
            String scriptName = dp.getSrc().toString();
            this.bsfEngine = createBSFEngine(scriptName, scriptSrc);
            this.convertor = createOMElementConvertor(scriptName);
            convertor.setEngine(bsfEngine);
        }

        return bsfEngine;
    }

    protected BSFEngine createBSFEngine(String scriptName, String scriptSrc) {
        try {

            String scriptLanguage = BSFManager.getLangFromFilename(scriptName);
            BSFEngine bsfEngine = bsfManager.loadScriptingEngine(scriptLanguage);
            bsfEngine.exec(scriptName, 0, 0, scriptSrc);

            return bsfEngine;

        } catch (BSFException e) {
            throw new SynapseException(e.getTargetException());
        }
    }

    protected OMElementConvertor createOMElementConvertor(String scriptName) {
        OMElementConvertor oc = null;
        int lastDot = scriptName.lastIndexOf('.');
        if (lastDot > -1) {
            String suffix = scriptName.substring(lastDot + 1).toUpperCase();
            String className = OMElementConvertor.class.getName();
            int i = className.lastIndexOf('.');
            String packageName = className.substring(0, i + 1);
            String convertorClassName = packageName + suffix + className.substring(i + 1);
            try {
                oc = (OMElementConvertor) Class.forName(convertorClassName, true, getClass().getClassLoader()).newInstance();
            } catch (Exception e) {
                // ignore
            }
        }
        if (oc == null) {
            oc = new DefaultOMElementConvertor();
        }
        return oc;
    }

}
