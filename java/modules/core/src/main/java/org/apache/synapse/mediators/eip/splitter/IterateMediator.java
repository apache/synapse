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

package org.apache.synapse.mediators.eip.splitter;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.util.MessageHelper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
import org.jaxen.JaxenException;

import java.util.List;
import java.util.Iterator;

/**
 * This mediator will split the message in the criterian specified to it and inject in to Synapse
 */
public class IterateMediator extends AbstractMediator {

    /**
     * This holds whether to continue mediation on the parent message or not
     */
    private boolean continueParent = false;

    /**
     * This holds whether to preserve the payload and attach the iteration child to specified node
     * or to attach the child to the body of the envelope
     */
    private boolean preservePayload = false;

    /**
     * This holds the expression which will be evaluated for the presence of elements in the
     * mediating message for iterations
     */
    private AXIOMXPath expression = null;

    /**
     * This holds the node to which the iteration childs will be attached. This does not have any
     * meaning when the preservePayload is set to false
     */
    private AXIOMXPath attachPath = null;

    /**
     * This holds the target object for the newly created messages by the iteration
     */
    private Target target = null;

    /**
     * This method implemenents the Mediator interface and this mediator implements the message
     * splitting logic
     *
     * @param synCtx - MessageContext to be mediated
     * @return boolean false if need to stop processing the parent message, boolean true if further
     *         processing of the parent message is required
     */
    public boolean mediate(MessageContext synCtx) {

        // initializes the logging and tracing for the mediator
        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Iterate mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        try {
            // get a copy of the message for the processing, if the continueParent is set to true
            // this original message can go in further mediations and hence we should not change
            // the original message context
            SOAPEnvelope envelope = MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope());

            // get the iteration elements and iterate through the list,
            // this call will also detach all the iteration elements 
            List splitElements = EIPUtils.getDetachedMatchingElements(envelope, expression);
            if (splitElements != null) {

                int msgCount = splitElements.size();
                int msgNumber = 0;

                // if not preservePayload remove all the child elements
                if (!preservePayload && envelope.getBody() != null) {
                    for (Iterator itr = envelope.getBody().getChildren(); itr.hasNext();) {
                        ((OMNode) itr.next()).detach();
                    }
                }

                // iterate through the list
                for (Object o : splitElements) {

                    // clone the message for the mediation in iteration
                    MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);
                    // set the messageSequence property for possibal aggreagtions
                    newCtx.setProperty(EIPConstants.MESSAGE_SEQUENCE,
                        msgNumber + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + msgCount);
                    // get a clone of the envelope to be attached
                    SOAPEnvelope newEnvelope = MessageHelper.cloneSOAPEnvelope(envelope);

                    // for the moment iterator will look for an OMNode as the iteration element
                    if (!(o instanceof OMNode)) {
                        handleException(
                            "Error in splitting the message with expression : " + expression,
                            synCtx);
                    }

                    // if payload should be preserved then attach the iteration element to the
                    // node specified by the attachPath
                    if (preservePayload) {

                        Object attachElem = attachPath.evaluate(newEnvelope);
                        if (attachElem instanceof List) {
                            attachElem = ((List) attachElem).get(0);
                        }

                        // for the moment attaching element should be an OMElement
                        if (attachElem instanceof OMElement) {
                            ((OMElement) attachElem).addChild((OMNode) o);
                        } else {
                            handleException("Error in attaching the splitted elements :: " +
                                "Unable to get the attach path specified by the expression " +
                                attachPath, synCtx);
                        }
                        // if not preserve payload then attach the iteration element to the body
                    } else if (o instanceof OMNode && newEnvelope.getBody() != null) {
                        newEnvelope.getBody().addChild((OMNode) o);
                    }

                    // set the envelope ant mediate as specified in the target
                    newCtx.setEnvelope(newEnvelope);
                    target.mediate(newCtx);
                    msgNumber++;

                }

            } else {
                handleException(
                    "Splitting by expression : " + expression + " did not yeild in an OMElement",
                    synCtx);
            }

        } catch (JaxenException e) {
            handleException("Error evaluating XPath expression : " + expression, e, synCtx);
        } catch (AxisFault axisFault) {
            handleException("Unable to split the message using the expression : " + expression,
                axisFault, synCtx);
        }

        // finalizing the tracing and logging on the iterate mediator
        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Iterate mediator");
        }

        // if the continuation of the parent message is stopped from here set the RESPONSE_WRITTEN
        // property to SKIP to skip the blank http response 
        OperationContext opCtx
            = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext();
        if (!continueParent && opCtx != null) {
            opCtx.setProperty(Constants.RESPONSE_WRITTEN,"SKIP");
        }

        // whether to continue mediation on the original message
        return continueParent;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //                        Getters and Setters                                        //
    ///////////////////////////////////////////////////////////////////////////////////////

    public boolean isContinueParent() {
        return continueParent;
    }

    public void setContinueParent(boolean continueParent) {
        this.continueParent = continueParent;
    }

    public boolean isPreservePayload() {
        return preservePayload;
    }

    public void setPreservePayload(boolean preservePayload) {
        this.preservePayload = preservePayload;
    }

    public AXIOMXPath getExpression() {
        return expression;
    }

    public void setExpression(AXIOMXPath expression) {
        this.expression = expression;
    }

    public AXIOMXPath getAttachPath() {
        return attachPath;
    }

    public void setAttachPath(AXIOMXPath attachPath) {
        this.attachPath = attachPath;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

}
