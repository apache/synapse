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

import java.util.Arrays;
import java.util.Vector;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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

    private static final Log log = LogFactory.getLog(InlineScriptMediator.class);

    protected String scriptName;

    protected String scriptSrc;
    
    protected static final String MC_VAR_NAME = "mc";
    protected static final Vector PARAM_NAMES = new Vector(Arrays.asList(new String[]{ MC_VAR_NAME }));

    public InlineScriptMediator(String scriptName, String scriptSrc) {
        super(null, null);
        this.scriptName = scriptName;
        this.scriptSrc = scriptSrc;
    }

    public boolean mediate(MessageContext synCtx) {
        try {
            log.debug("Script mediator mediate()");

            // This is a bit of a hack to deal with inconsistencies in the varrious BSF script engines.
            // The JavaScript engine does not support BSFEngine.apply properly as it doesn't pass the
            // param names and values to the script (actually this looks like a bug in BSFEngineImpl
            // not the JavaScript engine). To circumvent this problem the ThreadLocalMessageContext is
            // added as a declared bean. Some other engines (eg JRuby) seem to have a bug where they don't
            // pick up declared beans so ScriptMessageContext is passed in as a param on the apply call.
            
            ScriptMessageContext scriptMC = new ScriptMessageContext(synCtx, convertor);
            ThreadLocalMessageContext.setMC(scriptMC);
            Vector paramValues = new Vector();
            paramValues.add(scriptMC);

            Object response = bsfEngine.apply(scriptName, 0, 0, scriptSrc, PARAM_NAMES, paramValues);

            if (response instanceof Boolean) {
                return ((Boolean) response).booleanValue();
            }

            return true; // default to returning true

        } catch (BSFException e) {
            log.error("Error executing inline script", e);
            throw new SynapseException(e);
        }
    }

    public void init() {
        try {

            this.bsfManager.declareBean(MC_VAR_NAME, new ThreadLocalMessageContext(), ThreadLocalMessageContext.class);
            String scriptLanguage = BSFManager.getLangFromFilename(scriptName);
            this.bsfEngine = bsfManager.loadScriptingEngine(scriptLanguage);
            this.convertor = createOMElementConvertor(scriptName);
            convertor.setEngine(bsfEngine);

        } catch (BSFException e) {
            throw new SynapseException(e);
        }
    }
}
