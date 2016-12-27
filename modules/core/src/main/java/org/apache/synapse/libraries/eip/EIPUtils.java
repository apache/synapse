/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
