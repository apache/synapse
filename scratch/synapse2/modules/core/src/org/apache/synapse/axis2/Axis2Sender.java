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

package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisEngine;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;


/**
 * This class helps the Axis2SynapseEnvironment implement the send method
 */
public class Axis2Sender {

    public static void sendOn(SynapseMessage smc) {

        try {

            MessageContext messageContext = ((Axis2SynapseMessage) smc)
                .getMessageContext();
            // At any time any QOS is disengaged. It's engaged iff, a flag is
            // set in execution chain.
            // ex: addressing will be engage in outpath iff ADDRESSING_PROCESSED
            // is set.

            if (smc.getProperty(Constants.ENGAGE_ADDRESSING_IN_MESSAGE) != null) {

                messageContext.setProperty(
                    Constants.ENGAGE_ADDRESSING_IN_MESSAGE, Boolean.TRUE);

            }
            //Now hadle the outbound message with addressing
            if (smc.getProperty(
                Constants.ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE) != null) {
                messageContext.setProperty(
                    Constants.ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE,
                    Boolean.TRUE);

            }

            MessageContext outMsgContext = Axis2FlexibleMEPClient
                .send(messageContext);

            // run all rules on response

            smc.setResponse(true);//

            outMsgContext.setServerSide(true);

            Object os = messageContext
                .getProperty(MessageContext.TRANSPORT_OUT);
            outMsgContext.setProperty(MessageContext.TRANSPORT_OUT, os);
            TransportInDescription ti = messageContext.getTransportIn();

            outMsgContext.setTransportIn(ti);

            if (smc.getSynapseEnvironment() == null) {
                throw new SynapseException("no Synapse Env set on message");
            }
            smc.getSynapseEnvironment().injectMessage(new Axis2SynapseMessage(
                outMsgContext, smc.getSynapseEnvironment()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new SynapseException(e);
        }
    }

    public static void sendBack(SynapseMessage smc) {
        MessageContext messageContext = ((Axis2SynapseMessage) smc)
            .getMessageContext();
        AxisEngine ae =
            new AxisEngine(messageContext.getConfigurationContext());
        try {
//


            messageContext
                .setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);
            // check for addressing is alredy engaged for this message.
            // if engage we should use the address enable Configuraion context.
//

            ae.send(messageContext);
        } catch (AxisFault e) {
            throw new SynapseException(e);

        }

    }

}
