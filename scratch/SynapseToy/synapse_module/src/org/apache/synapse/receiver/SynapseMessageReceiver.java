package org.apache.synapse.receiver;

import org.apache.axis2.receivers.AbstractInMessageReceiver;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.synapse.SynapseConstant;
import org.apache.synapse.Mediators;
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
*
* @author : Deepal Jayasinghe (deepal@apache.org)
*
*/

public class SynapseMessageReceiver extends AbstractInMessageReceiver {

    public void invokeBusinessLogic(MessageContext messageContext) throws AxisFault {
        Mediators med =(Mediators)messageContext.getProperty(SynapseConstant.CURRENT_MEDIATOR);
        System.out.println("Mediator:" + med.getClass().getName());
        boolean  status = med.mediate(messageContext);
        if(!status){
            MessageContext newmsg = copyMessageContext(messageContext);
            AxisEngine engine = new AxisEngine(messageContext.getSystemContext());
            engine.receive(newmsg);
        } else {
            System.out.println("Done , need to send the message");
        }
    }

    private  MessageContext copyMessageContext(MessageContext inMessageContext) throws AxisFault {
        MessageContext newmsgCtx =
                new MessageContext(inMessageContext.getSystemContext(),
                        inMessageContext.getSessionContext(),
                        inMessageContext.getTransportIn(),
                        inMessageContext.getTransportOut());

        newmsgCtx.setProperty(MessageContext.TRANSPORT_OUT,
                inMessageContext.getProperty(MessageContext.TRANSPORT_OUT));
        newmsgCtx.setProperty(HTTPConstants.HTTPOutTransportInfo,
                inMessageContext.getProperty(HTTPConstants.HTTPOutTransportInfo));

        //Setting the charater set encoding
        newmsgCtx.setProperty(MessageContext.CHARACTER_SET_ENCODING, inMessageContext
                .getProperty(MessageContext.CHARACTER_SET_ENCODING));

        newmsgCtx.setDoingREST(inMessageContext.isDoingREST());
        newmsgCtx.setDoingMTOM(inMessageContext.isDoingMTOM());
        newmsgCtx.setServerSide(inMessageContext.isServerSide());
        newmsgCtx.setServiceGroupContextId(inMessageContext.getServiceGroupContextId());
        newmsgCtx.setEnvelope(inMessageContext.getEnvelope());
        newmsgCtx.setProperty(SynapseConstant.CURRENT_RULE_OBJECT,inMessageContext.getProperty(SynapseConstant.CURRENT_RULE_OBJECT));
        newmsgCtx.setProperty(SynapseConstant.PRIVOUS_RULE_ID,inMessageContext.getProperty(SynapseConstant.CURRENT_RULE_ID));
        return newmsgCtx;
    }
}
