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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
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
     * @param envelope - SOAPEnvelope from which the the elements will be extracted
     * @param expression - AXIOMXPath expression describing the elements
     * @return List of OMElements in the envelope matching the expression
     */
    public static List getElements(SOAPEnvelope envelope, AXIOMXPath expression) {
        try {
            Object o = expression.evaluate(envelope);
            if (o instanceof OMNode) {
                List list = new ArrayList();
                list.add(o);
                return list;
            } else if (o instanceof List) {
                return (List) o;
            } else {
                handleException("The evaluation of the XPath expression "
                        + expression + " must result in an OMNode");
            }
        } catch (JaxenException e) {
            handleException("Error evaluating XPath " + expression + " on message");
        }

        return null;
    }

    /**
     * This static util method will be used to create a new MessageContext by passing the
     * MessageContext and the SOAPEnvelope to be filled with the newly created MessageContext
     *
     * @param synCtx - MessageContext which is subjected to the creation of the new MC
     * @param envelope - SOAPEnvelope to be set to the new MC
     * @return MessageContext created from the paased arguments
     */
    public static MessageContext createNewMessageContext(
            MessageContext synCtx, SOAPEnvelope envelope) {

        // create the message context and then copy the transportIn/Out from the original message
        MessageContext newCtx = synCtx.getEnvironment().createMessageContext();
        org.apache.axis2.context.MessageContext axis2MC
                = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        org.apache.axis2.context.MessageContext newAxis2MC
                = ((Axis2MessageContext) newCtx).getAxis2MessageContext();

        newAxis2MC.setTransportIn(axis2MC.getTransportIn());
        newAxis2MC.setTransportOut(axis2MC.getTransportOut());

        newAxis2MC.setServiceContext(axis2MC.getServiceContext());
        newAxis2MC.setOperationContext(axis2MC.getOperationContext());
        newAxis2MC.setAxisMessage(axis2MC.getAxisMessage());
        newAxis2MC.getAxisMessage().setParent(axis2MC.getAxisOperation());
        newAxis2MC.setAxisService(axis2MC.getAxisService());

        newAxis2MC.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
                axis2MC.getProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO));

        try {
            newCtx.setEnvelope(envelope);

            // copy all the properties to the newCtx
            Iterator propItr = synCtx.getPropertyKeySet().iterator();
            while (propItr.hasNext()) {
                Object o = propItr.next();
                // If there are non String keyed properties neglect them rathern than trow exception
                if (o instanceof String) {
                    newCtx.setProperty((String) o, synCtx.getProperty((String) o));
                }
            }

            // set the parent details to the splitted MC 
            newCtx.setProperty(Constants.AGGREGATE_CORELATION, synCtx.getMessageID());

            // set the parent properties to the splitted MC
            newCtx.setTo(synCtx.getTo());
            newCtx.setReplyTo(synCtx.getReplyTo());
            newCtx.setSoapAction(synCtx.getSoapAction());
            newCtx.setWSAAction(synCtx.getWSAAction());

        } catch (AxisFault axisFault) {
            handleException("Unable to split the message" + axisFault.getMessage(), axisFault);
        }

        return newCtx;
    }

    /**
     * This static util method will be used to enrich the envelope passed, by the element described
     * by the XPath over the enricher envelope
     * 
     * @param envelope - SOAPEnvelope to be enriched with the content
     * @param enricher - SOAPEnvelope from which the enriching element will be extracted
     * @param expression - AXIOMXPath describing the enriching element
     */
    public static void enrichEnvelope(SOAPEnvelope envelope,
                                      SOAPEnvelope enricher, AXIOMXPath expression) {
        OMElement enrichingElement;
        Object o = getElements(envelope, expression).get(0);
        if (o instanceof OMElement && ((OMElement) o).getParent() instanceof OMElement) {
            enrichingElement = (OMElement) ((OMElement) o).getParent();
        } else {
            enrichingElement = envelope.getBody();
        }
        
        Iterator itr = getElements(enricher, expression).iterator();
        while (itr.hasNext()) {
            o = itr.next();
            if (o != null && o instanceof OMElement) {
                enrichingElement.addChild((OMElement) o);
            }
        }
        
    }

    /**
     * This static util method will be used to clone the SOAPEnvelope passed to the method
     * 
     * @param env - SOAPEnvelope to be cloned
     * @return SOAPEnvelope cloned from env
     */
    public static SOAPEnvelope cloneEnvelope(SOAPEnvelope env) {

        SOAPEnvelope envelope;
        if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(
                env.getBody().getNamespace().getNamespaceURI())) {
            envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        } else {
            envelope = OMAbstractFactory.getSOAP12Factory().getDefaultEnvelope();
        }
        
        Iterator itr = env.getHeader().getChildren();
        while (itr.hasNext()) {
            envelope.getHeader().addChild(((OMElement) itr.next()).cloneOMElement());
        }
        
        itr = env.getBody().getChildren();
        while (itr.hasNext()) {
            envelope.getBody().addChild(((OMElement) itr.next()).cloneOMElement());
        }
        
        return envelope;
    }

    /**
     * Private method to handle exceptions
     *
     * @param message - String message to be logged and to be put as the exception message
     */
    private static void handleException(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        throw new SynapseException(message);
    }

    /**
     * Private method to handle exceptions
     *
     * @param message - String message to be logged and to be put as the exception message
     * @param e - Cause Exception for this exception
     */
    private static void handleException(String message, Exception e) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        throw new SynapseException(message, e);
    }
}
