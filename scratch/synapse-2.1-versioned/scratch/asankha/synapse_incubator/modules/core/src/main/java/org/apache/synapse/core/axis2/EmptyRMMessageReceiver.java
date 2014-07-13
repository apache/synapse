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
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/*
 * 
 */

public class EmptyRMMessageReceiver implements MessageReceiver {

    private static Log log = LogFactory.getLog(EmptyRMMessageReceiver.class);

    public void receive(MessageContext messageContext) throws AxisFault {
        /*
         Message Recieved with RM
        */

        log.info(
                "EmptyRMMessageReceiver#receive() and inject the Message into Synapse Environment");
        log.debug("Application Message  :  " + messageContext.getEnvelope().toString());

        MessageContext outMsgContext = Utils.createOutMessageContext(messageContext);
        outMsgContext.getOperationContext().addMessageContext(outMsgContext);

        org.apache.synapse.MessageContext synCtx =
                Axis2MessageContextFinder.getSynapseMessageContext(messageContext);

        //setting the property that Application Message in RM Need not to hit RM Mediators anymore
        messageContext.setProperty(org.apache.synapse.Constants.MESSAGE_RECEIVED_RM_ENGAGED,
                                   Boolean.TRUE);

        synCtx.getEnvironment().injectMessage(synCtx);

        Object obj = messageContext.getProperty(org.apache.synapse.Constants.RESPONSE_SOAP_ENVELOPE);
        if (obj != null) {
            outMsgContext.setEnvelope((SOAPEnvelope)obj);
        } else {
            outMsgContext.setEnvelope(messageContext.getEnvelope());
        }

        log.debug("Executed EmptyRMMessageReceiver#receive() and Java Return for RMMediator");

        AxisEngine engine = new AxisEngine(
                messageContext.getOperationContext().getServiceContext().getConfigurationContext());
        engine.send(outMsgContext);

    }
}
