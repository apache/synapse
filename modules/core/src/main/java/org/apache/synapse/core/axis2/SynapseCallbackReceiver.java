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

import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.endpoints.Endpoint;

import java.util.*;

public class SynapseCallbackReceiver implements MessageReceiver {

    private static final Log log = LogFactory.getLog(SynapseCallbackReceiver.class);

    private Map callbackStore;  // this will be made thread safe within the constructor

    public SynapseCallbackReceiver() {
        callbackStore = Collections.synchronizedMap(new HashMap());
    }

    public void addCallback(String MsgID, Callback callback) {
        callbackStore.put(MsgID, callback);
    }

    public void receive(MessageContext messageCtx) throws AxisFault {

        if (messageCtx.getOptions() != null && messageCtx.getOptions().getRelatesTo() != null) {
            String messageID  = messageCtx.getOptions().getRelatesTo().getValue();
            Callback callback = (Callback) callbackStore.remove(messageID);

            if (callback != null) {
                handleMessage(messageCtx, ((AsyncCallback) callback).getSynapseOutMsgCtx());
            } else {
                // TODO invoke a generic synapse error handler for this message
                log.warn("Synapse received a response for the request with message Id : " +
                    messageID + " But a callback has not been registered to process this response");
            }

        } else {
            // TODO invoke a generic synapse error handler for this message
            log.warn("Synapse received a response message without a message Id");
        }
    }

    /**
     * Handle the response or error (during a failed send) message received for an outgoing request
     *
     * @param response the Axis2 MessageContext that has been received and has to be handled
     * @param synapseOutMsgCtx the corresponding (outgoing) Synapse MessageContext for the above
     * Axis2 MC, that holds Synapse specific information such as the error handler stack and
     * local properties etc.
     */
    private void handleMessage(MessageContext response,
        org.apache.synapse.MessageContext synapseOutMsgCtx) {

        if (response.getEnvelope().getBody().hasFault()) {            
            Stack faultStack = synapseOutMsgCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                ((FaultHandler) faultStack.pop()).handleFault(
                    synapseOutMsgCtx,
                    response.getEnvelope().getBody().getFault().getException());
            }

        } else {

            // there can always be only one instance of an Endpoint in the faultStack of a message
            // if the send was successful, so remove it before we proceed any further
            Stack faultStack = synapseOutMsgCtx.getFaultStack();
            if (!faultStack.isEmpty() && faultStack.peek() instanceof Endpoint) {
                faultStack.pop();
            }

            if (log.isDebugEnabled()) {
                log.debug("Synapse received an asynchronous response message");
                log.debug("Received To: " +
                    (response.getTo() != null ? response.getTo().getAddress() : "null"));
                log.debug("SOAPAction: " +
                    (response.getSoapAction() != null ? response.getSoapAction() : "null"));
                log.debug("Body : \n" + response.getEnvelope());
            }

            MessageContext axisOutMsgCtx =
                ((Axis2MessageContext)synapseOutMsgCtx).getAxis2MessageContext();

            response.setOperationContext(axisOutMsgCtx.getOperationContext());
            response.setAxisService(axisOutMsgCtx.getAxisService());

            // set properties on response
            response.setServerSide(true);
            response.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);
            response.setProperty(MessageContext.TRANSPORT_OUT,
                axisOutMsgCtx.getProperty(MessageContext.TRANSPORT_OUT));
            response.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
                axisOutMsgCtx.getProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO));
            response.setTransportIn(axisOutMsgCtx.getTransportIn());
            response.setTransportOut(axisOutMsgCtx.getTransportOut());

            // If request is REST assume that the response is REST too
            response.setDoingREST(axisOutMsgCtx.isDoingREST());

            // create the synapse message context for the response
            Axis2MessageContext synapseInMessageContext =
                new Axis2MessageContext(
                    response,
                    synapseOutMsgCtx.getConfiguration(),
                    synapseOutMsgCtx.getEnvironment());

            synapseInMessageContext.setResponse(true);
            synapseInMessageContext.setTo(null);

            // set the properties of the original MC to the new MC
            Iterator iter = synapseOutMsgCtx.getPropertyKeySet().iterator();

            while (iter.hasNext()) {
                Object key = iter.next();
                synapseInMessageContext.setProperty(
                    (String) key, synapseOutMsgCtx.getProperty((String) key));
            }

            // send the response message through the synapse mediation flow
            synapseOutMsgCtx.getEnvironment().
                injectMessage(synapseInMessageContext);
        }
    }
}
