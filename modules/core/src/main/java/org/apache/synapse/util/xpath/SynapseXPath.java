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

import org.apache.axiom.om.OMNamespace;
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
import java.util.Map;

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
public class SynapseXPath implements XPath {

    private static final Log log = LogFactory.getLog(SynapseXPath.class);

    private AXIOMXPath xpath;

    /**
     * <p>Initializes the <code>SynapseXPath</code> with the given <code>xpathString</code> as the
     * XPath</p>
     *
     * @param xpathString xpath in its string format
     * @throws JaxenException in case of an initialization failure
     */
    public SynapseXPath(String xpathString) throws JaxenException {
        xpath = new AXIOMXPath(xpathString);
        xpath.setVariableContext(new ThreadSafeDelegatingVariableContext());
        xpath.setFunctionContext(new ThreadSafeDelegatingFunctionContext());
    }

    /**
     * <p>Evaluates the XPath over the specified SOAPEnvelope. This overides the evaluate method
     * of the <code>BaseXPath</code> and provides a better acceess to the envelope using the XPath
     * variables for <code>SOAPBody</code> and <code>SOAPHeader</code>.</p>
     *
     * @param o object to be evaluated to get the result (MessageContext | SOAPEnvelope | OMElement)
     * @return evaluated value of the xpath over the given message
     * @throws JaxenException in case of a failure in evaluation
     *
     * @see org.jaxen.BaseXPath#evaluate(Object)
     * @see org.apache.synapse.util.xpath.SynapseXPathVariableContext#getVariableValue(
     * String, String, String) 
     */
    public Object evaluate(Object o) throws JaxenException {
        setContexts(o);
        return xpath.evaluate(getEvaluationObject(o));
    }

    /**
     * @deprecated
     */
    public String valueOf(Object o) throws JaxenException {
        return xpath.valueOf(o);
    }

    public String stringValueOf(Object o) throws JaxenException {

        if (o instanceof MessageContext) {
            return stringValueOf((MessageContext) o);
        } else {
            setContexts(o);
            return xpath.stringValueOf(getEvaluationObject(o));
        }
    }

    public boolean booleanValueOf(Object o) throws JaxenException {
        setContexts(o);
        return xpath.booleanValueOf(getEvaluationObject(o));
    }

    public Number numberValueOf(Object o) throws JaxenException {
        setContexts(o);
        return xpath.numberValueOf(getEvaluationObject(o));
    }

    public List selectNodes(Object o) throws JaxenException {
        setContexts(o);
        return xpath.selectNodes(getEvaluationObject(o));
    }

    public Object selectSingleNode(Object o) throws JaxenException {

        return xpath.selectSingleNode(o);
    }

    public void addNamespace(String prefix, String nsURI) throws JaxenException {
        xpath.addNamespace(prefix, nsURI);
    }

    public void setNamespaceContext(NamespaceContext namespaceContext) {
        xpath.setNamespaceContext(namespaceContext);
    }

    public void setFunctionContext(FunctionContext functionContext) {
        throw new UnsupportedOperationException("Setting the function context directly is " +
                "prohibited, use ((ThreadSafeFunctionContext) " +
                "getFunctionContext()).setDelegate() instead");
    }

    public void setVariableContext(VariableContext variableContext) {
        
        throw new UnsupportedOperationException("Setting the variable context directly is " +
                "prohibited, use ((ThreadSafeVariableContext) " +
                "getVariableContext()).setDelegate() instead");
    }

    public NamespaceContext getNamespaceContext() {
        return xpath.getNamespaceContext();
    }

    public FunctionContext getFunctionContext() {
        return xpath.getFunctionContext();
    }

    public VariableContext getVariableContext() {
        return xpath.getVariableContext();
    }

    public Navigator getNavigator() {
        return xpath.getNavigator();
    }

    public String toString() {
        return xpath.toString();
    }

    /**
     * <P>Evaluates the XPath expression against the MessageContext of the current message and
     * returns a String representation of the result</p>
     *
     * @param synCtx the source message which holds the MessageContext against full context
     * @return a String representation of the result of evaluation
     */
    public String stringValueOf(MessageContext synCtx) {

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

    public void addNamespace(OMNamespace ns) throws JaxenException {
        xpath.addNamespace(ns.getPrefix(), ns.getNamespaceURI());
    }

    public Map getNamespaces() {
        return xpath.getNamespaces();
    }

    private Object getEvaluationObject(Object obj) {
        if (obj instanceof MessageContext) {
            return ((MessageContext) obj).getEnvelope();
        } else {
            return obj;
        }
    }

    private void setContexts(Object obj) {

        if (obj instanceof MessageContext) {
            setContexts((MessageContext) obj);
        } else if (obj instanceof SOAPEnvelope) {
            setContexts((SOAPEnvelope) obj);
        } else {
            
            ((ThreadSafeDelegatingVariableContext)
                    xpath.getVariableContext()).setDelegate(new SimpleVariableContext());

            ((ThreadSafeDelegatingFunctionContext)
                    xpath.getFunctionContext()).setDelegate(new XPathFunctionContext(true));
        }
    }

    private void setContexts(SOAPEnvelope env) {

        ((ThreadSafeDelegatingVariableContext)
                xpath.getVariableContext()).setDelegate(
                new SynapseXPathVariableContext(env));

        ((ThreadSafeDelegatingFunctionContext)
                xpath.getFunctionContext()).setDelegate(
                new XPathFunctionContext(true));
    }

    private void setContexts(MessageContext synCtx) {

        ((ThreadSafeDelegatingVariableContext)
                xpath.getVariableContext()).setDelegate(
                new SynapseXPathVariableContext(synCtx));

        ((ThreadSafeDelegatingFunctionContext)
                xpath.getFunctionContext()).setDelegate(
                new SynapseXPathFunctionContext(synCtx, true));
    }

    private void handleException(String msg, Throwable e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}