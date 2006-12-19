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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.neethi.Policy;
import org.apache.axiom.soap.SOAPFault;

import java.util.Iterator;

/**
 * This class helps the Axis2SynapseEnvironment implement the send method
 */
public class Axis2Sender {

    private static final Log log = LogFactory.getLog(Axis2Sender.class);

    public static void sendOn(
        org.apache.synapse.MessageContext synapseInMessageContext) {

        try {
            Boolean wsAOn   = (Boolean) synapseInMessageContext.getProperty(
                Constants.OUTFLOW_ADDRESSING_ON);
            Boolean wsRmOn  = (Boolean) synapseInMessageContext.getProperty(
                Constants.OUTFLOW_RM_ON);
            Boolean wsSecOn = (Boolean) synapseInMessageContext.getProperty(
                Constants.OUTFLOW_SECURITY_ON);
            Boolean separateListener = (Boolean) synapseInMessageContext.getProperty(
                    Constants.OUTFLOW_USE_SEPARATE_LISTENER);

            MessageContext axisOutMsgContext =
                Axis2FlexibleMEPClient.send(
                    // WS-A default is off
                    (wsAOn != null && wsAOn.booleanValue()),

                    // WS-Sec default is off
                    (wsSecOn != null && wsSecOn.booleanValue()),

                    // The Rampart security policy
                    (String) synapseInMessageContext.getProperty(
                        Constants.OUTFLOW_SEC_POLICY),

                    // WS-RM default is off
                    (wsRmOn != null && wsRmOn.booleanValue()),

                    // The Sandesha security policy
                    (String) synapseInMessageContext.getProperty(
                        Constants.OUTFLOW_RM_POLICY),

                    // use a separate listener
                    (separateListener != null && separateListener.booleanValue()),
                    
                    // The Axis2 Message context of the Synapse MC
                    synapseInMessageContext);

          
            if (axisOutMsgContext != null && axisOutMsgContext.getEnvelope()!=null) { // if there is no response env will be null
                //set the response Envelop as a property in Original axisMsgCtx
                synapseInMessageContext.setProperty(
                    org.apache.synapse.Constants.RESPONSE_SOAP_ENVELOPE,
                    axisOutMsgContext.getEnvelope());

                // create the synapse message context for the response
                org.apache.synapse.MessageContext synapseOutMessageContext =
                    new Axis2MessageContext(
                        axisOutMsgContext,
                        synapseInMessageContext.getConfiguration(),
                        synapseInMessageContext.getEnvironment());
                synapseOutMessageContext.setResponse(true);

                // now set properties to co-relate to the request i.e. copy over
                // correlation messgae properties from original message to response received
                Iterator iter = synapseInMessageContext.getCorrelationPropertyKeySet().iterator();

                while (iter.hasNext()) {
                    Object key = iter.next();
                    synapseOutMessageContext.setProperty(
                        (String) key, synapseInMessageContext.getCorrelationProperty((String) key));
                }

                // if we have a SOAP Fault, log it - irrespective of the mediation logic
                // http://issues.apache.org/jira/browse/SYNAPSE-42
                if (synapseOutMessageContext.getEnvelope().getBody().hasFault()) {
                    SOAPFault fault = synapseOutMessageContext.getEnvelope().getBody().getFault();
                    log.warn("Synapse received a SOAP fault from : " + synapseInMessageContext.getTo() + 
                        (fault.getNode() != null ? " Node : " + fault.getNode().getNodeValue() : "") +
                        (fault.getReason() != null ? " Reason : " + fault.getReason().getFirstSOAPText() : "") +
                        (fault.getCode() != null ? " Code : " + fault.getCode().getValue() : ""));
                }

                log.debug("Processing incoming message");

                // sets the out sequence if present to the out MC to mediate the response
                if(synapseInMessageContext.getProperty(Constants.OUT_SEQUENCE) != null) {
                    synapseOutMessageContext.setProperty(Constants.OUT_SEQUENCE,
                            synapseInMessageContext.getProperty(Constants.OUT_SEQUENCE));
                }

                // send the response message through the synapse mediation flow
                synapseInMessageContext.getEnvironment().
                    injectMessage(synapseOutMessageContext);
            }

        } catch (Exception e) {
            handleException("Unexpected error during Sending message onwards", e);
        }
    }

    public static void sendBack(org.apache.synapse.MessageContext smc) {

        MessageContext messageContext = ((Axis2MessageContext) smc).
            getAxis2MessageContext();
        AxisEngine ae = new AxisEngine(messageContext.getConfigurationContext());

        try {
            messageContext.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);
            // check if addressing is already engaged for this message.
            // if engaged we should use the addressing enabled Configuraion context.
            ae.send(messageContext);

        } catch (AxisFault e) {
            handleException("Unexpected error during Sending message back", e);
        }
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
