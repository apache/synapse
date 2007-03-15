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
import org.apache.synapse.Constants;
import org.apache.synapse.mediators.bsf.convertors.*;
import org.apache.synapse.config.Entry;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.bsf.convertors.DefaultOMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.OMElementConvertor;
import org.apache.synapse.mediators.bsf.convertors.RBOMElementConvertor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Vector;
import java.util.Arrays;

/**
 * A Synapse mediator that calls a function in any scripting language supported by the BSF.
 * The ScriptMediator supports scripts specified in-line or those loaded through a registry
 * <p/>
 * <pre>
 *    &lt;script [key=&quot;entry-key&quot;]
 *      [function=&quot;script-function-name&quot;] language="javascript|groovy|ruby"&gt
 *      (text | xml)?
 *    &lt;/script&gt;
 * </pre>
 * <p/>
 * <p/>
 * The function is an optional attribute defining the name of the script function to call,
 * if not specified it defaults to a function named 'mediate'. The function takes a single
 * parameter which is the Synapse MessageContext. The function may return a boolean, if it
 * does not then true is assumed.
 */
public class ScriptMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(ScriptMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    /** The name of the variable made available to the scripting language to access the message */
    private static final String MC_VAR_NAME = "mc";
    private static final Vector PARAM_NAMES = new Vector(Arrays.asList(new String[]{MC_VAR_NAME}));

    /** The registry entry key for a script loaded from the registry */
    private String key;
    /** The language of the script code */
    private String language;
    /** The optional name of the function to be invoked, defaults to mediate */
    private String function = "mediate";
    /** The source code of the script */
    private String scriptSourceCode;
    /** The BSF Manager */
    private BSFManager bsfManager;
    /** The BSF engine created to process each message through the script */
    private BSFEngine bsfEngine;
    /** A converter to get the code for the scripting language from XML */
    private OMElementConvertor convertor;

    /**
     * Create a script mediator for the given language and given script source
     * @param language the BSF language
     * @param scriptSourceCode the source code of the script
     */
    public ScriptMediator(String language, String scriptSourceCode) {
        this.language = language;
        this.scriptSourceCode = scriptSourceCode;
    }

    /**
     * Create a script mediator for the given language and given script entry key and function
     * @param language the BSF language
     * @param key the registry entry key to load the script
     * @param function the function to be invoked
     */
    public ScriptMediator(String language, String key, String function) {
        this.language = language;
        this.key = key;
        this.function = function;
    }

    /**
     * Perform Script mediation
     * @param synCtx the Synapse message context
     * @return true for inline mediation and the boolean result (if any) for other scripts that
     * specify a function that returns a boolean value
     */
    public boolean mediate(MessageContext synCtx) {
        
        log.debug("Script Mediator - mediate() # Language : " + language +
            (key == null ? " inline script" : " script with key : " + key) +
            " function : " + function);

        boolean shouldTrace = shouldTrace(synCtx.getTracingState());
        if (shouldTrace) {
            trace.trace("Start : Script mediator # Language : " + language +
                (key == null ? " inline script" : " script with key : " + key) +
                " function : " + function);
        }

        boolean returnValue = false;
        if (key != null) {
            returnValue = mediateWithExternalScript(synCtx);
        } else {
            returnValue = mediateForInlineScript(synCtx);
        }

        if (shouldTrace && returnValue) {
            trace.trace("End : Script mediator");
        }

        return returnValue;
    }

    /**
     * Mediation implementation when the script to be executed should be loaded from the registry
     * @param synCtx the message context
     * @return script result
     */
    private boolean mediateWithExternalScript(MessageContext synCtx) {

        try {
            Entry entry = synCtx.getConfiguration().getEntryDefinition(key);

            // if the key refers to a dynamic script
            if (entry != null && entry.isDynamic()) {
                if (!entry.isCached() || entry.isExpired()) {
                    scriptSourceCode = ((OMElement) (synCtx.getEntry(key))).getText();
                    loadBSFEngine(synCtx, false);
                }
            // if the key is static, we will load the script and create a BSFEngine only once
            } else {
                // load script if not already loaded
                if (scriptSourceCode == null) {
                    Object o = synCtx.getEntry(key);
                    if (o instanceof OMElement) {
                        scriptSourceCode = ((OMElement) (o)).getText();
                    } else if (o instanceof String) {
                        scriptSourceCode = (String) o;
                    }
                }
                // load BSFEngine if not already loaded
                if (bsfEngine == null) {
                    loadBSFEngine(synCtx, false);
                }
            }

            if (shouldTrace(synCtx.getTracingState())) {
                trace.trace("Invoking script for current message : " + synCtx);
            }

            // prepare engine for the execution of the script
            bsfEngine.exec(language, 0, 0, scriptSourceCode);
            // calling the function with script message context as a parameter
            Object[] args = new Object[]{ new ScriptMessageContext(synCtx, convertor) };
            Object response = bsfEngine.call(null, function, args);

            if (shouldTrace(synCtx.getTracingState())) {
                trace.trace("Result message after execution of script : " + synCtx);
            }

            if (response != null && response instanceof Boolean) {
                return ((Boolean) response).booleanValue();
            }
            return true;

        } catch (BSFException e) {
            handleException("Error invoking " + language +
                " script : " + key + " function : " + function, e);
        }
        return false;
    }

    /**
     * Perform mediation with static inline script of the given scripting language
     * @param synCtx message context
     * @return true, or the script return value
     */
    private boolean mediateForInlineScript(MessageContext synCtx) {

        try {
            if (bsfEngine == null) {
                loadBSFEngine(synCtx, true);
            }

            if (shouldTrace(synCtx.getTracingState())) {
                trace.trace("Invoking inline script for current message : " + synCtx);
            }

            ScriptMessageContext scriptMC = new ScriptMessageContext(synCtx, convertor);
            Vector paramValues = new Vector();
            paramValues.add(scriptMC);

            // applying the source to the specified engine with parameter names and values
            Object response = bsfEngine.apply(
                language, 0, 0, scriptSourceCode, PARAM_NAMES, paramValues);

            if (shouldTrace(synCtx.getTracingState())) {
                trace.trace("Result message after execution of script : " + synCtx);
            }

            if (response != null && response instanceof Boolean) {
                return ((Boolean) response).booleanValue();
            }
            return true;

        } catch (BSFException e) {
            handleException("Error executing inline " + language + " script", e);
        }
        return false;
    }

    /**
     * Load the BSFEngine through the BSFManager. BSF engines should be cached within this
     * mediator.
     * TODO check if the constructed engine thread safe?
     * TODO i.e. if a script defines var X = $1 and then reads it back, and if the
     * TODO first thread sets X to 10, and is context switched and second thread sets X to 20
     * TODO and completes. Now when the first thread comes back, will it read 10 or 20?
     * TODO hopefully 10 
     * @param synCtx the message context
     * @param isInline true for inline scripts
     */
    public synchronized void loadBSFEngine(MessageContext synCtx, boolean isInline) {

        if (bsfManager == null) {
            bsfManager = new BSFManager();
            convertor = getOMElementConvertor();
        }

        try {
            if (isInline) {
                ScriptMessageContext scriptMC = new ScriptMessageContext(synCtx, convertor);
                bsfManager.declareBean(MC_VAR_NAME, scriptMC, ScriptMessageContext.class);
                bsfEngine = bsfManager.loadScriptingEngine(language);
            } else {
                bsfEngine = bsfManager.loadScriptingEngine(language);
            }

        } catch (BSFException e) {
            handleException("Error loading BSF Engine for : " + language +
                (isInline ? " for inline mediation" : " for external script execution"), e);
        }

        convertor.setEngine(bsfEngine);
    }

    /**
     * Return the appropriate OMElementConverter for the language of the script
     * @return a suitable OMElementConverter for the scripting language
     */
    public OMElementConvertor getOMElementConvertor() {
        OMElementConvertor oc = null;

        if ("javascript".equals(language)) {
            return new JSOMElementConvertor();
        } else if ("ruby".equals(language)) {
            return new RBOMElementConvertor();
        } else if ("groovy".equals(language)) {
            return new GROOVYOMElementConvertor();
        } else {
            return new DefaultOMElementConvertor();
        }
    }

    public String getLanguage() {
        return language;
    }

    public String getKey() {
        return key;
    }

    public String getFunction() {
        return function;
    }

    public String getScriptSrc() {
        return scriptSourceCode;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

}
