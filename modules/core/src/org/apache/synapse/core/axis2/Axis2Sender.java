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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisEngine;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;

/**
 * This class helps the Axis2SynapseEnvironment implement the send method
 */
public class Axis2Sender {

    public static void sendOn(org.apache.synapse.MessageContext synapseMessageContext) {

        try {
            MessageContext axis2MessageContext = ((Axis2MessageContext) synapseMessageContext).getAxis2MessageContext();
            // At any time any QOS is disengaged. It's engaged iff, a flag is
            // set in execution chain. ex: addressing will be engage in outpath iff ADDRESSING_PROCESSED is set.

            if (synapseMessageContext.getProperty(Constants.ENGAGE_ADDRESSING_IN_MESSAGE) != null) {
                axis2MessageContext.setProperty(Constants.ENGAGE_ADDRESSING_IN_MESSAGE, Boolean.TRUE);
            }

            //Now handle the outbound message with addressing
            if (synapseMessageContext.getProperty(Constants.ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE) != null) {
                axis2MessageContext.setProperty(Constants.ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE, Boolean.TRUE);
            }

            MessageContext axisOutMsgContext = Axis2FlexibleMEPClient.send(axis2MessageContext);

            // run all rules on response
            synapseMessageContext.setResponse(true);
            axisOutMsgContext.setServerSide(true);

            axisOutMsgContext.setProperty(
                MessageContext.TRANSPORT_OUT,
                axis2MessageContext.getProperty(MessageContext.TRANSPORT_OUT));

            axisOutMsgContext.setTransportIn(
                axis2MessageContext.getTransportIn());

            synapseMessageContext.getEnvironment().injectMessage(
                new Axis2MessageContext(
                    axisOutMsgContext,
                    synapseMessageContext.getConfiguration(),
                    synapseMessageContext.getEnvironment()));

        } catch (Exception e) {
            e.printStackTrace();
            throw new SynapseException(e);
        }
    }

    public static void sendBack(org.apache.synapse.MessageContext smc) {

        MessageContext messageContext = ((Axis2MessageContext) smc).getAxis2MessageContext();

        AxisEngine ae = new AxisEngine(messageContext.getConfigurationContext());
        try {
            messageContext.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);
            // check for addressing is alredy engaged for this message.
            // if engage we should use the address enable Configuraion context.
            ae.send(messageContext);

        } catch (AxisFault e) {
            throw new SynapseException(e);
        }
    }

}
