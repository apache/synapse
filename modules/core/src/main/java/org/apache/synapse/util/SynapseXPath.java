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

package org.apache.synapse.util;

import org.apache.axiom.om.impl.llom.OMDocumentImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.GetPropertyFunction;
import org.jaxen.JaxenException;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.SimpleVariableContext;

import java.util.List;

/**
 * 
 */
public class SynapseXPath extends AXIOMXPath {

    private static final Log log = LogFactory.getLog(SynapseXPath.class);

    public SynapseXPath(String xpathString) throws JaxenException {
        super(xpathString);
    }

    public Object evaluate(MessageContext synCtx) throws JaxenException {

        try {
            // create an instance of a synapse:get-property()
            // function and set it to the xpath
            GetPropertyFunction getPropertyFunc = new GetPropertyFunction();
            getPropertyFunc.setSynCtx(synCtx);

            // set function context into XPath
            SimpleFunctionContext fc = new XPathFunctionContext();
            fc.registerFunction(SynapseConstants.SYNAPSE_NAMESPACE,
                "get-property", getPropertyFunc);
            fc.registerFunction(null, "get-property", getPropertyFunc);
            setFunctionContext(fc);

            // register namespace for XPath extension function
            addNamespace("synapse", SynapseConstants.SYNAPSE_NAMESPACE);
            addNamespace("syn", SynapseConstants.SYNAPSE_NAMESPACE);

        } catch (JaxenException je) {
            handleException("Error setting up the Synapse XPath " +
                "extension function for XPath : " + this.toString(), je);
        }

        return evaluate(synCtx.getEnvelope());
    }

    public Object evaluate(SOAPEnvelope env) throws JaxenException {

        SimpleVariableContext varContext = new SimpleVariableContext();
        if (this.toString().indexOf("$body") != -1) {
            varContext.setVariableValue("body", env.getBody());
        }
        if (this.toString().indexOf("$header") != -1) {
            varContext.setVariableValue("header", env.getHeader());
        }
        setVariableContext(varContext);

        return super.evaluate(env);
    }

    /**
     * Evaluates the XPath expression against the SOAPEnvelope of the current message and returns a
     * String representation of the result
     *
     * @param synCtx the source message which holds the SOAP envelope against full envelope
     * @return a String representation of the result of evaluation
     */
    public synchronized String getStringValue(MessageContext synCtx) {

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