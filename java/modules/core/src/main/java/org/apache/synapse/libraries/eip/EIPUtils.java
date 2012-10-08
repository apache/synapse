package org.apache.synapse.libraries.eip;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.template.TemplateContext;

import java.util.Stack;

/**
 * This is an utility class to fetch parameter values parsed by call template mediators
 */
public class EIPUtils {

    /**
     * Fetch parameter values parsed by call template mediators
     * @param synCtx MessageContext
     * @param paramName String name
     * @return paramValue Object
     */
    public static Object lookupFunctionParam(MessageContext synCtx, String paramName) {
        Stack<TemplateContext> funcStack = (Stack) synCtx.getProperty(SynapseConstants.SYNAPSE__FUNCTION__STACK);
        TemplateContext currentFuncHolder = funcStack.peek();
        Object paramValue = currentFuncHolder.getParameterValue(paramName);
        return paramValue;
    }
}
