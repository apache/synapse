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

package org.apache.synapse.core.axis2;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.ServerManager;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.ResponseAcceptEncodingProcessor;

import java.util.*;

/**
 * This is the message receiver that receives the responses for outgoing messages sent out
 * by Synapse. It holds a callbackStore that maps the [unique] messageID of each message to
 * a callback object that gets executed on timeout or when a response is received (before timeout)
 *
 * The AnonymousServiceFactory uses this MessageReceiver for all Anonymous services created by it.
 * This however - effectively - is a singleton class
 */
public class SynapseCallbackReceiver implements MessageReceiver {

    private static final Log log = LogFactory.getLog(SynapseCallbackReceiver.class);

    /** This is the synchronized callbackStore that maps outgoing messageID's to callback objects */
    private Map<String, AxisCallback> callbackStore;  // will made thread safe in the constructor

    /**
     * Create the *single* instance of this class that would be used by all anonymous services
     * used for outgoing messaging.
     * @param synCfg the Synapse configuration
     */
    public SynapseCallbackReceiver(SynapseConfiguration synCfg) {

        callbackStore = Collections.synchronizedMap(new HashMap<String, AxisCallback>());
        ServerManager.getInstance().setCallbackStore(callbackStore);

        // create the Timer object and a TimeoutHandler task
        TimeoutHandler timeoutHandler = new TimeoutHandler(callbackStore);
        
        Timer timeOutTimer = synCfg.getSynapseTimer();
        long timeoutHandlerInterval = ServerManager.getInstance().getTimeoutHandlerInterval();

        // schedule timeout handler to run every n seconds (n : specified or defaults to 15s)
        timeOutTimer.schedule(timeoutHandler, 0, timeoutHandlerInterval);
    }


    public void addCallback(String MsgID, AxisCallback callback) {
        callbackStore.put(MsgID, callback);
        if (log.isDebugEnabled()) {
            log.debug("Callback added. Total callbacks waiting for : " + callbackStore.size());
        }
    }

    /**
     * Everytime a response message is received this method gets invoked. It will then select
     * the outgoing *Synapse* message context for the reply we received, and determine what action
     * to take at the Synapse level
     *
     * @param messageCtx the Axis2 message context of the reply received
     * @throws AxisFault
     */
    public void receive(MessageContext messageCtx) throws AxisFault {

        String messageID = null;

        if (messageCtx.getOptions() != null && messageCtx.getOptions().getRelatesTo() != null) {
            // never take a chance with a NPE at this stage.. so check at each level :-)
            Options options = messageCtx.getOptions();
            if (options != null) {
                RelatesTo relatesTo = options.getRelatesTo();
                if (relatesTo != null) {
                    messageID = relatesTo.getValue();
                }
            }
        } else if (messageCtx.getProperty(SynapseConstants.MERCURY_SEQUENCE_KEY) == null) {
            messageID = (String) messageCtx.getProperty(SynapseConstants.RELATES_TO_FOR_POX);
        }

        if (messageID != null) {
            AxisCallback callback = callbackStore.remove(messageID);
            if (log.isDebugEnabled()) {
                log.debug("Callback removed. Pending callbacks count : " + callbackStore.size());
            }

            RelatesTo[] relates = messageCtx.getRelationships();
            if (relates != null && relates.length > 1) {
                // we set a relates to to the response message so that if WSA is not used, we
                // could still link back to the original message. But if WSA was used, this
                // gets duplicated, and we should remove it
                removeDuplicateRelatesTo(messageCtx, relates);
            }
            
            if (callback != null) {
                handleMessage(messageCtx, ((AsyncCallback) callback).getSynapseOutMsgCtx());
                
            } else {
                // TODO invoke a generic synapse error handler for this message
                log.warn("Synapse received a response for the request with message Id : " +
                    messageID + " But a callback is not registered (anymore) to process this response");
            }

        } else if (!messageCtx.isPropertyTrue(NhttpConstants.SC_ACCEPTED)){
            // TODO invoke a generic synapse error handler for this message
            log.warn("Synapse received a response message without a message Id");
        }
    }

    /**
     * Handle the response or error (during a failed send) message received for an outgoing request
     *
     * @param response         the Axis2 MessageContext that has been received and has to be handled
     * @param synapseOutMsgCtx the corresponding (outgoing) Synapse MessageContext for the above
     *                         Axis2 MC, that holds Synapse specific information such as the error
     *                         handler stack and local properties etc.
     * @throws AxisFault       if the message cannot be processed
     */
    private void handleMessage(MessageContext response,
        org.apache.synapse.MessageContext synapseOutMsgCtx) throws AxisFault {

        Object o = response.getProperty(NhttpConstants.SENDING_FAULT);
        if (o != null && Boolean.TRUE.equals(o)) {

            // there is a sending fault. propagate the fault to fault handlers.

            Stack faultStack = synapseOutMsgCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                SOAPEnvelope envelope = response.getEnvelope();
                if (envelope != null) {
                    SOAPFault fault = envelope.getBody().getFault();
                    if (fault != null) {
                        Exception e = fault.getException();
                        if (e == null) {
                            SOAPFaultReason reason = fault.getReason();
                            if (reason != null) {
                                e = new Exception(reason.getText());
                            }
                        }
                        // set an error code to the message context, so that error sequences can
                        // filter using that property to determine the cause of error
                        synapseOutMsgCtx.setProperty(SynapseConstants.ERROR_CODE,
                            SynapseConstants.SENDING_FAULT);
                        SOAPFaultReason faultReason = fault.getReason();
                        if (faultReason != null) {
                            synapseOutMsgCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                                    faultReason.getText());
                        }
                        if (fault.getException() != null) {
                            synapseOutMsgCtx.setProperty(SynapseConstants.ERROR_EXCEPTION,
                                    fault.getException());
                        }
                        ((FaultHandler) faultStack.pop()).handleFault(synapseOutMsgCtx, e);
                    }
                }
            }

        } else {

            // there can always be only one instance of an Endpoint in the faultStack of a message
            // if the send was successful, so remove it before we proceed any further
            Stack faultStack = synapseOutMsgCtx.getFaultStack();
            if (faultStack !=null && !faultStack.isEmpty()
                && faultStack.peek() instanceof Endpoint) {
                faultStack.pop();
            }
            if (log.isDebugEnabled()) {
                log.debug("Synapse received an asynchronous response message");
                log.debug("Received To: " +
                        (response.getTo() != null ? response.getTo().getAddress() : "null"));
                log.debug("SOAPAction: " +
                        (response.getSoapAction() != null ? response.getSoapAction() : "null"));
                log.debug("WSA-Action: " +
                        (response.getWSAAction() != null ? response.getWSAAction() : "null"));
                String[] cids = response.getAttachmentMap().getAllContentIDs();
                if (cids != null && cids.length > 0) {
                    for (String cid : cids) {
                        log.debug("Attachment : " + cid);
                    }
                }
                log.debug("Body : \n" + response.getEnvelope());
            }
            MessageContext axisOutMsgCtx =
                    ((Axis2MessageContext) synapseOutMsgCtx).getAxis2MessageContext();

            //Processes 'Accept-Encoding'
            ResponseAcceptEncodingProcessor.process(response, axisOutMsgCtx);

            response.setServiceContext(null);
            response.setOperationContext(axisOutMsgCtx.getOperationContext());
            response.setAxisMessage(axisOutMsgCtx.getAxisOperation().getMessage(
                    WSDLConstants.MESSAGE_LABEL_OUT_VALUE));

            // set properties on response
            response.setServerSide(true);
            response.setProperty(SynapseConstants.ISRESPONSE_PROPERTY, Boolean.TRUE);
            response.setProperty(MessageContext.TRANSPORT_OUT,
                    axisOutMsgCtx.getProperty(MessageContext.TRANSPORT_OUT));
            response.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
                    axisOutMsgCtx.getProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO));
            response.setTransportIn(axisOutMsgCtx.getTransportIn());
            response.setTransportOut(axisOutMsgCtx.getTransportOut());

            // If request is REST assume that the response is REST too
            response.setDoingREST(axisOutMsgCtx.isDoingREST());
            if (axisOutMsgCtx.isDoingMTOM()) {
                response.setDoingMTOM(true);
                response.setProperty(
                        org.apache.axis2.Constants.Configuration.ENABLE_MTOM,
                        org.apache.axis2.Constants.VALUE_TRUE);
            }
            if (axisOutMsgCtx.isDoingSwA()) {
                response.setDoingSwA(true);
                response.setProperty(
                        org.apache.axis2.Constants.Configuration.ENABLE_SWA,
                        org.apache.axis2.Constants.VALUE_TRUE);
            }

             // copy the message type property thats used by the out message to the response message
            response.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE,
                axisOutMsgCtx.getProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE));
            
            // compare original received message (axisOutMsgCtx) soap version with the response
            // if they are different change to original version 
            if(axisOutMsgCtx.isSOAP11() != response.isSOAP11()) {
            	if(axisOutMsgCtx.isSOAP11()) {
            		SOAPUtils.convertSOAP12toSOAP11(response);
            	} else {
            		SOAPUtils.convertSOAP11toSOAP12(response);
            	}
            }

            if (axisOutMsgCtx.getMessageID() != null) {
                response.setRelationships(
                        new RelatesTo[]{new RelatesTo(axisOutMsgCtx.getMessageID())});
            }

            response.setReplyTo(axisOutMsgCtx.getReplyTo());
            response.setFaultTo(axisOutMsgCtx.getFaultTo());

            if (axisOutMsgCtx.isPropertyTrue(NhttpConstants.IGNORE_SC_ACCEPTED)) {
                response.setProperty(NhttpConstants.FORCE_SC_ACCEPTED, Constants.VALUE_TRUE);
            }

            // create the synapse message context for the response
            Axis2MessageContext synapseInMessageContext =
                    new Axis2MessageContext(
                            response,
                            synapseOutMsgCtx.getConfiguration(),
                            synapseOutMsgCtx.getEnvironment());

            synapseInMessageContext.setResponse(true);
            synapseInMessageContext.setTo(
                new EndpointReference(AddressingConstants.Final.WSA_ANONYMOUS_URL));
            synapseInMessageContext.setTracingState(synapseOutMsgCtx.getTracingState());

            // set the properties of the original MC to the new MC

            for (Object key : synapseOutMsgCtx.getPropertyKeySet()) {
                synapseInMessageContext.setProperty(
                        (String) key, synapseOutMsgCtx.getProperty((String) key));
            }

            // send the response message through the synapse mediation flow
            try {
                synapseOutMsgCtx.getEnvironment().injectMessage(synapseInMessageContext);
            } catch (SynapseException syne) {
                Stack stack = synapseInMessageContext.getFaultStack();
                if (stack != null &&
                        !stack.isEmpty()) {
                    ((FaultHandler) stack.pop()).handleFault(synapseInMessageContext, syne);
                } else {
                    log.error("Synapse encountered an exception, " +
                            "No error handlers found - [Message Dropped]\n" + syne.getMessage());
                }
            }
        }
    }

    /**
     * It is possible for us (Synapse) to cause the creation of a duplicate relatesTo as we
     * try to hold onto the outgoing message ID even for POX messages using the relates to
     * Now once we get a response, make sure we remove any trace of this before we proceed any
     * further
     * @param mc the message context from which a possibly duplicated relatesTo should be removed
     * @param relates the existing relatedTo array of the message
     */
    private void removeDuplicateRelatesTo(MessageContext mc, RelatesTo[] relates) {

        int insertPos = 0;
        RelatesTo[] newRelates = new RelatesTo[relates.length];

        for (RelatesTo current : relates) {
            boolean found = false;
            for (int j = 0; j < newRelates.length && j < insertPos; j++) {
                if (newRelates[j].equals(current) ||
                        newRelates[j].getValue().equals(current.getValue())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newRelates[insertPos++] = current;
            }
        }

        RelatesTo[] trimmedRelates = new RelatesTo[insertPos];
        System.arraycopy(newRelates, 0, trimmedRelates, 0, insertPos);
        mc.setRelationships(trimmedRelates);
    }
}
