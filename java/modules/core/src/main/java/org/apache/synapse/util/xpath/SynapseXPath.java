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

import org.apache.axiom.om.impl.llom.OMDocumentImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.jaxen.*;

import java.util.List;

/**
 * <p>XPath that has been used inside Synapse xpath processing. This has a extension function named
 * <code>get-property</code> which is use to retrieve message context properties with the given
 * name from the function</p>
 *
 * <p>For example the following function <code>get-property('prop')</code> can be evaluatedd using
 * an XPath to retrieve the message context property value with the name <code>prop</code>.</p>
 *
 * <p>Apart from that this XPath has a certain set of XPath variables associated with it. They are
 * as follows;
 * <dl>
 *   <dt><tt>body</tt></dt>
 *   <dd>The SOAP 1.1 or 1.2 body element.</dd>
 *   <dt><tt>header</tt></dt>
 *   <dd>The SOAP 1.1 or 1.2 header element.</dd>
 * </dl>
 * </p>
 *
 * <p>Also there are some XPath prefixes defined in <code>SynapseXPath</code> to access various
 * properties using XPath variables, where the variable name represents the particular prefix and
 * the property name as the local part of the variable. Those variables are;
 * <dl>
 *   <dt><tt>ctx</tt></dt>
 *   <dd>Prefix for Synapse MessageContext properties</dd>
 *   <dt><tt>axis2</tt></dt>
 *   <dd>Prefix for Axis2 MessageContext properties</dd>
 *   <dt><tt>trp</tt></dt>
 *   <dd>Prefix for the transport headers</dd>
 * </dl>
 * </p>
 *
 * <p>This XPath is Thread Safe, and provides a special set of evaluate functions for the
 * <code>MessageContext</code> and <code>SOAPEnvelope</code> as well as a method to retrieve
 * string values of the evaluated XPaths</p>
 *
 * @see org.apache.axiom.om.xpath.AXIOMXPath
 * @see org.apache.synapse.util.xpath.SynapseXPathFunctionContext
 * @see org.apache.synapse.util.xpath.SynapseXPathVariableContext
 */
public class SynapseXPath extends AXIOMXPath {

    private static final Log log = LogFactory.getLog(SynapseXPath.class);

    /**
     * <p>Initializes the <code>SynapseXPath</code> with the given <code>xpathString</code> as the
     * XPath</p>
     *
     * @param xpathString xpath in its string format
     * @throws JaxenException in case of an initialization failure
     */
    public SynapseXPath(String xpathString) throws JaxenException {
        super(xpathString);
        setVariableContext(new ThreadSafeDelegatingVariableContext());
        setFunctionContext(new ThreadSafeDelegatingFunctionContext());
    }

    /**
     * <p>Evaluates the XPath over the specified MessageContext. This overides the evaluate method
     * of the <code>BaseXPath</code> and provides a better acceess to the message context
     * properties and transport information</p>
     *
     * @param synCtx context to be evaluated for the XPath
     * @return evaluated value of the xpath over the given message context
     * @throws JaxenException in case of a failure in evaluation
     *
     * @see org.jaxen.BaseXPath#evaluate(Object)
     * @see org.apache.synapse.util.xpath.SynapseXPathFunctionContext#getFunction(
     * String, String, String)
     * @see org.apache.synapse.util.xpath.SynapseXPathVariableContext#getVariableValue(
     * String, String, String)
     */
    public Object evaluate(MessageContext synCtx) throws JaxenException {

        // retrieves the variable context and the function context to restore after
        VariableContext varCtx = ((ThreadSafeDelegatingVariableContext)
                getVariableContext()).getDelegate();
        FunctionContext funCtx = ((ThreadSafeDelegatingFunctionContext)
            getFunctionContext()).getDelegate();

        try {

            // set the synapse variable and function contexts before evaluation
            ((ThreadSafeDelegatingVariableContext)
                getVariableContext()).setDelegate(new SynapseXPathVariableContext(synCtx));
            ((ThreadSafeDelegatingFunctionContext)
                getFunctionContext()).setDelegate(new SynapseXPathFunctionContext(synCtx, true));

            return super.evaluate(synCtx.getEnvelope());

        } finally {

            // restore the variable and function contexts
            ((ThreadSafeDelegatingVariableContext) getVariableContext()).setDelegate(varCtx);
            ((ThreadSafeDelegatingFunctionContext) getFunctionContext()).setDelegate(funCtx);
        }
    }

    /**
     * <p>Evaluates the XPath over the specified SOAPEnvelope. This overides the evaluate method
     * of the <code>BaseXPath</code> and provides a better acceess to the envelope using the XPath
     * variables for <code>SOAPBody</code> and <code>SOAPHeader</code>.</p>
     *
     * @param env message to be evaluated to get the result
     * @return evaluated value of the xpath over the given message
     * @throws JaxenException in case of a failure in evaluation
     *
     * @see org.jaxen.BaseXPath#evaluate(Object)
     * @see org.apache.synapse.util.xpath.SynapseXPathVariableContext#getVariableValue(
     * String, String, String) 
     */
    public Object evaluate(SOAPEnvelope env) throws JaxenException {

        // retrieves the variable context to restore after
        VariableContext varCtx = ((ThreadSafeDelegatingVariableContext)
                getVariableContext()).getDelegate();

        try {

            // set the synapse variable context before evaluation
            ((ThreadSafeDelegatingVariableContext)
                getVariableContext()).setDelegate(new SynapseXPathVariableContext(env));

            return super.evaluate(env);

        } finally {

            // restore the variable context            
            ((ThreadSafeDelegatingVariableContext) getVariableContext()).setDelegate(varCtx);
        }
    }

    /**
     * <P>Evaluates the XPath expression against the MessageContext of the current message and
     * returns a String representation of the result</p>
     *
     * @param synCtx the source message which holds the MessageContext against full context
     * @return a String representation of the result of evaluation
     */
    public String getStringValue(MessageContext synCtx) {

        try {

            Object result = evaluate(synCtx);

            if (result == null) {
                return null;
            }

            StringBuffer textValue = new StringBuffer();
            if (result instanceof List) {

                List list = (List) result;
                for (Object o : list) {

                    if (o == null && list.size() == 1) {
                        return null;
                    }

                    if (o instanceof OMTextImpl) {
                        textValue.append(((OMTextImpl) o).getText());
                    } else if (o instanceof OMElementImpl) {

                        String s = ((OMElementImpl) o).getText();

                        if (s.trim().length() == 0) {
                            s = o.toString();
                        }
                        textValue.append(s);

                    } else if (o instanceof OMDocumentImpl) {

                        textValue.append(
                            ((OMDocumentImpl) o).getOMDocumentElement().toString());
                    }
                }

            } else {
                textValue.append(result.toString());
            }

            return textValue.toString();

        } catch (JaxenException je) {
            handleException("Evaluation of the XPath expression " + this.toString() +
                " resulted in an error", je);
        }

        return null;
    }

    private void handleException(String msg, Throwable e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

}