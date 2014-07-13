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

package org.apache.synapse.receivers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.MessageInformationHeaders;
import org.apache.axis2.addressing.miheaders.RelatesTo;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.UUIDGenerator;

/**
 * This is the Absract IN-OUT MEP MessageReceiver. The
 * protected abstract methods are only for the sake of breaking down the logic
 */
public abstract class SpringAbstractInOutSyncMessageReceiver extends SpringAbstractMessageReceiver {
    public abstract void invokeBusinessLogic(MessageContext inMessage,
                                             MessageContext outMessage)
            throws AxisFault;

    public final void receive(MessageContext msgContext) throws AxisFault {
        MessageContext outMsgContext =
                new MessageContext(msgContext.getSystemContext(),
                        msgContext.getSessionContext(),
                        msgContext.getTransportIn(),
                        msgContext.getTransportOut());

        MessageInformationHeaders oldMessageInfoHeaders =
                msgContext.getMessageInformationHeaders();
        MessageInformationHeaders messageInformationHeaders =
                new MessageInformationHeaders();
        messageInformationHeaders.setMessageId(UUIDGenerator.getUUID());
        messageInformationHeaders.setTo(oldMessageInfoHeaders.getReplyTo());
        messageInformationHeaders.setFaultTo(
                oldMessageInfoHeaders.getFaultTo());
        messageInformationHeaders.setFrom(oldMessageInfoHeaders.getTo());
        messageInformationHeaders.setRelatesTo(
                new RelatesTo(oldMessageInfoHeaders.getMessageId(),
                        AddressingConstants.Submission.WSA_RELATES_TO_RELATIONSHIP_TYPE_DEFAULT_VALUE));
        messageInformationHeaders.setAction(oldMessageInfoHeaders.getAction());
        outMsgContext.setMessageInformationHeaders(messageInformationHeaders);
        outMsgContext.setOperationContext(msgContext.getOperationContext());
        outMsgContext.setServiceContext(msgContext.getServiceContext());
        outMsgContext.setServiceGroupContextId(msgContext.getServiceGroupContextId());
        outMsgContext.setProperty(MessageContext.TRANSPORT_OUT,
                msgContext.getProperty(MessageContext.TRANSPORT_OUT));
        outMsgContext.setProperty(HTTPConstants.HTTPOutTransportInfo,
                msgContext.getProperty(HTTPConstants.HTTPOutTransportInfo));
        
        //Setting the charater set encoding
        outMsgContext.setProperty(MessageContext.CHARACTER_SET_ENCODING, msgContext
				.getProperty(MessageContext.CHARACTER_SET_ENCODING));
        
        outMsgContext.setDoingREST(msgContext.isDoingREST());
        outMsgContext.setDoingMTOM(msgContext.isDoingMTOM());
        outMsgContext.setServerSide(msgContext.isServerSide());

        invokeBusinessLogic(msgContext, outMsgContext);

        AxisEngine engine =
                new AxisEngine(
                        msgContext.getOperationContext().getServiceContext()
                .getEngineContext());
        engine.send(outMsgContext);
    }
}
