/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.mediators.template;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.base.SequenceMediator;

import java.util.Collection;
import java.util.Stack;

/**
 * This mediator is responsible for handling function templates for synapse. This will parse parameters
 * passed from an <invoke> mediator and will be made available to sequences defined within this template.
 * parameters will be accessible through synapse 'function stack'. Users should access these through an
 *  xpath extension var or function , defined under function scope
 *  ie:- $func:p1 or get-property('func','p2')
 */
public class TemplateMediator extends SequenceMediator {

    private Collection<String> paramNames;

    private String eipPatternName;

    public void setParameters(Collection<String> paramNames) {
        this.paramNames = paramNames;
    }

    public Collection<String> getParameters() {
        return paramNames;
    }

    public void setName(String name) {
        this.eipPatternName = name;
    }

    public String getName() {
        return eipPatternName;
    }

    public void init(SynapseEnvironment env) {
        super.init(env);
    }

    public boolean mediate(MessageContext synCtx) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : EIP Sequence " + "paramNames : " + paramNames);

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }
        pushFuncContextTo(synCtx);
        boolean result = false;
        try {
            result = super.mediate(synCtx);
        } finally {
            popFuncContextFrom(synCtx);
        }
        return result;
    }

    /**
     * for each message coming to this function template ,pushes a function context containing
     * parameters into Synapse Function Stack.
     * @param synCtx  Synapse Message context
     */
    private void pushFuncContextTo(MessageContext synCtx) {
        TemplateContext funcContext = new TemplateContext(eipPatternName, paramNames);
        //process the raw parameters parsed in
        funcContext.setupParams(synCtx);
        //if a function stack has not already been created for this message flow create new one
        Stack<TemplateContext> stack = (Stack) synCtx.getProperty(SynapseConstants.SYNAPSE__FUNCTION__STACK);
        if (stack == null) {
            stack = new Stack<TemplateContext>();
            stack.push(funcContext);
            synCtx.setProperty(SynapseConstants.SYNAPSE__FUNCTION__STACK, stack);
        } else {
            stack.push(funcContext);
        }
    }

    private void popFuncContextFrom(MessageContext synCtx) {
        Stack<TemplateContext> stack = (Stack) synCtx.getProperty(SynapseConstants.SYNAPSE__FUNCTION__STACK);
        if (stack != null) {
            stack.pop();
        }
    }

}
