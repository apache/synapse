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
import org.apache.axis2.Constants;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;

/**
 * This message receiver should be configured in the Axis2 configuration as the
 * default message receiver, which will handle all incoming messages through the
 * synapse mediation
 */
public class SynapseMessageReceiver implements MessageReceiver {

    private static final Log log = LogFactory.getLog(SynapseMessageReceiver.class);

    public void receive(org.apache.axis2.context.MessageContext mc) throws AxisFault {

        log.debug("Synapse received a new message...");
        log.debug("Received To: " + (mc.getTo() != null ?
            mc.getTo().getAddress() : "null"));
        log.debug("SOAPAction: " + (mc.getWSAAction() != null ?
            mc.getWSAAction() : "null"));
        log.debug("Body : \n" + mc.getEnvelope());

        MessageContext synCtx = Axis2MessageContextFinder.getSynapseMessageContext(mc);
        synCtx.getEnvironment().injectMessage(synCtx);
    }
}
