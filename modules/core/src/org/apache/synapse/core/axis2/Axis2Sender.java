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
import org.apache.axis2.engine.AxisEngine;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;

import java.util.Set;
import java.util.Iterator;

/**
 * This class helps the Axis2SynapseEnvironment implement the send method
 */
public class Axis2Sender {

    public static void sendOn(org.apache.synapse.MessageContext synapseInMessageContext) {

        try {
            MessageContext axis2MessageContext = ((Axis2MessageContext) synapseInMessageContext).getAxis2MessageContext();
            // At any time any QOS is disengaged. It's engaged iff, a flag is
            // set in execution chain. ex: addressing will be engage in outpath iff ADDRESSING_PROCESSED is set.

            if (synapseInMessageContext.getProperty(Constants.ENGAGE_ADDRESSING_IN_MESSAGE) != null) {
                axis2MessageContext.setProperty(Constants.ENGAGE_ADDRESSING_IN_MESSAGE, Boolean.TRUE);
            }

            //Now handle the outbound message with addressing
            if (synapseInMessageContext.getProperty(Constants.ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE) != null) {
                axis2MessageContext.setProperty(Constants.ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE, Boolean.TRUE);
            }

            MessageContext axisOutMsgContext = Axis2FlexibleMEPClient.send(axis2MessageContext);

            // run all rules on response
            synapseInMessageContext.setResponse(true);
            axisOutMsgContext.setServerSide(true);
            axisOutMsgContext.setProperty(
                MessageContext.TRANSPORT_OUT,
                axis2MessageContext.getProperty(MessageContext.TRANSPORT_OUT));

            axisOutMsgContext.setTransportIn(
                axis2MessageContext.getTransportIn());

            // create the synapse message context for the response
            org.apache.synapse.MessageContext synapseOutMessageContext =
                new Axis2MessageContext(
                    axisOutMsgContext,
                    synapseInMessageContext.getConfiguration(),
                    synapseInMessageContext.getEnvironment());

            // now set properties to co-relate to the request i.e. copy over
            // correlate/* messgae properties from original message to response received
            Iterator iter = synapseInMessageContext.getPropertyKeySet().iterator();
            while (iter.hasNext()) {
                Object key = iter.next();
                if (key instanceof String && ((String)key).startsWith(Constants.CORRELATE)) {
                    synapseOutMessageContext.setProperty(
                        (String) key,
                        synapseInMessageContext.getProperty((String) key)
                    );
                }
            }


            // send the response message through the synapse mediation flow
            synapseInMessageContext.getEnvironment().injectMessage(synapseOutMessageContext);

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
