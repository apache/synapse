/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.synapse.mediators.GetPropertyFunction;
import org.jaxen.JaxenException;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;

import java.util.Iterator;
import java.util.List;

/**
 * Holds utility methods used by Synapse
 */
public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    /**
     * Evaluates the given XPath expression against the SOAPEnvelope of the
     * current message and returns a String representation of the result
     * @param xpath the expression to evaluate
     * @param synCtx the source message which holds the SOAP envelope
     * @return a String representation of the result of evaluation
     */
    public static String getStringValue(AXIOMXPath xpath, MessageContext synCtx) {

        if (xpath != null) {
            try {
                // create an instance of a synapse:get-property() function and set it to the xpath
                GetPropertyFunction getPropertyFunc = new GetPropertyFunction();
                getPropertyFunc.setSynCtx(synCtx);

                // set function context into XPath
                SimpleFunctionContext fc = new XPathFunctionContext();
                fc.registerFunction(Constants.SYNAPSE_NAMESPACE, "get-property", getPropertyFunc);
                xpath.setFunctionContext(fc);

                // register namespace for XPath extension function
                xpath.addNamespace("synapse", Constants.SYNAPSE_NAMESPACE);

            } catch (JaxenException je) {
                String msg = "Error setting up the Synapse XPath extension function for XPath : " + xpath;
                log.error(msg, je);
                throw new SynapseException(msg, je);
            }
        }

        try {
            Object result = xpath.evaluate(synCtx.getEnvelope());
            StringBuffer textValue = new StringBuffer();

            if (result instanceof List) {
                Iterator iter = ((List) result).iterator();
                while (iter.hasNext()) {
                    Object o = iter.next();
                    if (o instanceof OMTextImpl) {
                        textValue.append(((OMTextImpl) o).getText());
                    } else if (o instanceof OMElementImpl) {
                        textValue.append(((OMElementImpl) o).getText());
                    }
                }
            } else {
                textValue.append(result.toString());
            }
            return textValue.toString();

        } catch (JaxenException je) {
            String msg = "Evaluation of the XPath expression " + xpath.toString() + " resulted in an error";
            log.error(msg, je);
            throw new SynapseException(msg, je);
        }
    }

    /**
     * Return the namespace with the given prefix, using the given element
     * @param prefix the prefix looked up
     * @param elem the source element to use
     * @return the namespace which maps to the prefix or null
     */
    public static String getNameSpaceWithPrefix(String prefix, OMElement elem) {
        if (prefix == null || elem == null) {
            log.warn("Searching for null NS prefix and/or using null OMElement");
            return null;
        }

        Iterator iter = elem.getAllDeclaredNamespaces();
        while (iter.hasNext()) {
            OMNamespace ns = (OMNamespace) iter.next();
            if (prefix.equals(ns.getPrefix())) {
                return ns.getName();
            }
        }
        return null;
    }

    /**
     * Add xmlns NS declarations of element 'elem' into XPath expression
     * @param xpath
     * @param elem
     * @param log
     */
    public static void addNameSpaces(AXIOMXPath xpath, OMElement elem, Log log) {
        try {
            Iterator it = elem.getAllDeclaredNamespaces();
            while (it.hasNext()) {
                OMNamespace n = (OMNamespace) it.next();
                xpath.addNamespace(n.getPrefix(), n.getName());
            }
        } catch (JaxenException je) {
            String msg = "Error adding declared name spaces of " + elem + " to the XPath : " + xpath;
            log.error(msg);
            throw new SynapseException(msg, je);
        }
    }

    public static void setErrorInformation(MessageContext synCtx, SynapseException e) {
        synCtx.setProperty(Constants.ERROR_CODE, "00000"); //TODO not yet defined
        synCtx.setProperty(Constants.ERROR_MESSAGE, e.getMessage());
        synCtx.setProperty(Constants.ERROR_DETAIL, e.getStackTrace().toString());
    }
}
