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

import org.apache.axis2.client.async.Callback;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

public class AsyncCallback extends Callback {

    private static final Log log = LogFactory.getLog(AsyncCallback.class);

    org.apache.synapse.MessageContext synapseOutMsgCtx = null;

    public AsyncCallback(org.apache.synapse.MessageContext synapseOutMsgCtx) {
        this.synapseOutMsgCtx = synapseOutMsgCtx;
    }

    public void onComplete(AsyncResult result) {

        log.debug("Synapse received an Async response to a callback");
        MessageContext response = result.getResponseMessageContext();

        MessageContext axisOutMsgCtx =
            ((Axis2MessageContext)synapseOutMsgCtx).getAxis2MessageContext();

        // set properties on response
        response.setServerSide(true);
        response.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);
        response.setProperty(MessageContext.TRANSPORT_OUT,
            axisOutMsgCtx.getProperty(MessageContext.TRANSPORT_OUT));
        response.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
            axisOutMsgCtx.getProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO));
        response.setProperty(
                org.apache.synapse.Constants.PROCESSED_MUST_UNDERSTAND,
                axisOutMsgCtx.getProperty(
                        org.apache.synapse.Constants.PROCESSED_MUST_UNDERSTAND));
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

        // now set properties to co-relate to the request i.e. copy over
        // correlation messgae properties from original message to response received
        Iterator iter = synapseOutMsgCtx.getCorrelationPropertyKeySet().iterator();

        while (iter.hasNext()) {
            Object key = iter.next();
            synapseInMessageContext.setProperty(
                (String) key, synapseOutMsgCtx.getCorrelationProperty((String) key));
        }

        // sets the out sequence if present to the in MC to mediate the response
        if(synapseOutMsgCtx.getProperty(Constants.OUT_SEQUENCE) != null) {
            synapseInMessageContext.setProperty(Constants.OUT_SEQUENCE,
                    synapseOutMsgCtx.getProperty(Constants.OUT_SEQUENCE));
        }

        // send the response message through the synapse mediation flow
        synapseOutMsgCtx.getEnvironment().
            injectMessage(synapseInMessageContext);
    }

    public void onError(Exception e) {
        // this will never be called as our custom SynapseCallbackReceiver will push
        // faults as well through the onComplete()
    }

    public void setSynapseOutMshCtx(org.apache.synapse.MessageContext synapseOutMsgCtx) {
        this.synapseOutMsgCtx = synapseOutMsgCtx;
    }
}
