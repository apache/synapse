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

package org.apache.synapse.util.xpath;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.GetPropertyFunction;
import org.jaxen.Function;
import org.jaxen.UnresolvableException;
import org.jaxen.XPathFunctionContext;

/**
 * <p>XPath function context to be used when resolving XPath functions when using the
 * <code>SynapseXPath</code> and this resolves one function except for the standard XPath functions
 * and Jaxen extension functions.</p>
 *
 * <p>The function that has been resolved by this FunctionContext is; <tt>get-property(String)</tt>
 * which is used to retrieve message context properties</p>
 *
 * @see org.jaxen.XPathFunctionContext
 * @see org.apache.synapse.util.xpath.SynapseXPath
 */
public class SynapseXPathFunctionContext extends XPathFunctionContext {

    /** MessageContext to be used by the function resolver */
    private final MessageContext synCtx;

    /**
     * <p>Initialises the function context and the default XPath functions</p>
     *
     * @param synCtx message to be used for the function initialization
     *
     * @see org.jaxen.XPathFunctionContext
     */
    public SynapseXPathFunctionContext(MessageContext synCtx) {
        super();
        this.synCtx = synCtx;
    }

    /**
     * <p>Initialises the function context and the default XPath functions and the extension
     * functions by Jaxen</p>
     *
     * @param synCtx message to be used for the function initialization
     * @param includeExtensionFunctions whether to include the extensions or not
     *
     * @see org.jaxen.XPathFunctionContext
     */
    public SynapseXPathFunctionContext(MessageContext synCtx, boolean includeExtensionFunctions) {
        super(includeExtensionFunctions);
        this.synCtx = synCtx;
    }

    /**
     * <p>Retrieves the functions from the <code>FunctionContext</code> and this contains the
     * <code>get-property</code> function extension as well</p>
     * 
     * @param namespaceURI namespace of the function to be resolved
     * @param prefix string prefix to be resolved
     * @param localName string local name of the function
     * @return resolved function
     * @throws UnresolvableException if the function specified does not found
     *
     * @see org.jaxen.XPathFunctionContext#getFunction(String, String, String) 
     */
    public Function getFunction(String namespaceURI, String prefix, String localName)
        throws UnresolvableException {

        if (localName != null && SynapseXPathConstants.GET_PROPERTY_FUNCTION.equals(localName)) {
            
            // create an instance of a synapse:get-property()
            // function and set it to the xpath
            GetPropertyFunction getPropertyFunc = new GetPropertyFunction();
            getPropertyFunc.setSynCtx(synCtx);

            return getPropertyFunc;
        }

        // if not the get-property function then fetch default xpath functions
        return super.getFunction(namespaceURI, prefix, localName);
    }
}
