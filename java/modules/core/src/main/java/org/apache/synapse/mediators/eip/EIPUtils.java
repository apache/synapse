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

package org.apache.synapse.mediators.eip;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for the EIP implementations
 */
public class EIPUtils {

    /**
     * This will be used for logging purposes
     */
    private static final Log log = LogFactory.getLog(EIPUtils.class);

    /**
     * This static util method will be used to extract out the set of all elements described by the
     * given XPath over the given SOAPEnvelope
     *
     * @param envelope   SOAPEnvelope from which the the elements will be extracted
     * @param expression AXIOMXPath expression describing the elements
     * @return List of OMElements in the envelope matching the expression
     * @throws JaxenException if the XPath expression evaluation fails for some reason
     */
    public static List getMatchingElements(SOAPEnvelope envelope, AXIOMXPath expression)
        throws JaxenException {

        Object o = expression.evaluate(envelope);
        if (o instanceof OMNode) {
            List list = new ArrayList();
            list.add(o);
            return list;
        } else if (o instanceof List) {
            return (List) o;
        } else {
            return null;
        }
    }

    /**
     * @param envelope
     * @param expression
     * @return
     * @throws JaxenException
     */
    public static List getDetachedMatchingElements(SOAPEnvelope envelope, AXIOMXPath expression)
        throws JaxenException {

        List elementList = new ArrayList();
        Object o = expression.evaluate(envelope);
        if (o instanceof OMNode) {
            elementList.add(((OMNode) o).detach());
        } else if (o instanceof List) {
            for (Iterator itr = ((List) o).iterator(); itr.hasNext();) {
                Object elem = itr.next();
                if (elem instanceof OMNode) {
                    elementList.add(((OMNode) elem).detach());
                }
            }
        }
        return elementList;
    }

    /**
     * This static util method will be used to enrich the envelope passed, by the element described
     * by the XPath over the enricher envelope
     *
     * @param envelope   SOAPEnvelope to be enriched with the content
     * @param enricher   SOAPEnvelope from which the enriching element will be extracted
     * @param expression AXIOMXPath describing the enriching element
     */
    public static void enrichEnvelope(SOAPEnvelope envelope, SOAPEnvelope enricher,
        AXIOMXPath expression) throws JaxenException {
        OMElement enrichingElement;
        Object o = getMatchingElements(envelope, expression).get(0);
        if (o instanceof OMElement && ((OMElement) o).getParent() instanceof OMElement) {
            enrichingElement = (OMElement) ((OMElement) o).getParent();
        } else {
            enrichingElement = envelope.getBody();
        }

        Iterator itr = getMatchingElements(enricher, expression).iterator();
        while (itr.hasNext()) {
            o = itr.next();
            if (o != null && o instanceof OMElement) {
                enrichingElement.addChild((OMElement) o);
            }
        }

    }
    
}
