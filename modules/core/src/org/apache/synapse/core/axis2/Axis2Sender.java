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

package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.policy.Policy;

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

            MessageContext axisOutMsgContext =
                Axis2FlexibleMEPClient.send(
                    // WS-A default is off
                    (wsAOn != null && wsAOn.booleanValue()),

                    // WS-Sec default is off
                    (wsSecOn != null && wsSecOn.booleanValue()),

                    // The OutflowSecurity Parameter
                    (Parameter) synapseInMessageContext.getProperty(
                        Constants.OUTFLOW_SEC_PARAMETER),

                    // The InflowSecurity Parameter
                    (Parameter) synapseInMessageContext.getProperty(
                        Constants.INFLOW_SEC_PARAMETER),

                    // WS-RM default is off
                    (wsRmOn != null && wsRmOn.booleanValue()),

                    // The outflow RM Policy (or override)
                    (Policy) synapseInMessageContext.getProperty(
                        Constants.OUTFLOW_RM_POLICY),

                    // The Axis2 Message context of the Synapse MC
                    ((Axis2MessageContext) synapseInMessageContext).
                        getAxis2MessageContext());

            // create the synapse message context for the response
            org.apache.synapse.MessageContext synapseOutMessageContext =
                new Axis2MessageContext(
                    axisOutMsgContext,
                    synapseInMessageContext.getConfiguration(),
                    synapseInMessageContext.getEnvironment());
            synapseOutMessageContext.setResponse(true);

            // now set properties to co-relate to the request i.e. copy over
            // correlate/* messgae properties from original message to response received
            Iterator iter = synapseInMessageContext.getPropertyKeySet().iterator();

            while (iter.hasNext()) {
                Object key = iter.next();

                if (key instanceof String &&
                    ((String) key).startsWith(Constants.CORRELATE)) {

                    synapseOutMessageContext.setProperty(
                            (String) key,
                            synapseInMessageContext.getProperty((String) key)
                    );
                }
            }

            // send the response message through the synapse mediation flow
            synapseInMessageContext.getEnvironment().
                injectMessage(synapseOutMessageContext);

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
