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
        if (log.isDebugEnabled()) {
            log.debug("Body : \n" + mc.getEnvelope());
        }

        MessageContext synCtx = Axis2MessageContextFinder.getSynapseMessageContext(mc);
        synCtx.getEnvironment().injectMessage(synCtx);

        // Response handling mechanism for 200/202 and 5XX
        // if smc.isResponse = true then the response will be handled with 200 OK
        // else, response will be 202 OK without an http body
        // if smc.isFaultRespose = true then the response is a fault with 500 Internal Server Error

        if (synCtx.isResponse()) {
            mc.getOperationContext().setProperty(Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
        }
        if (synCtx.isFaultResponse()) {
            // todo: is there a better way to inject faultSoapEnv to the Axis2 Transport
            throw new AxisFault("Synapse Encountered an Error - See Log for More Details");
        }
    }
}
