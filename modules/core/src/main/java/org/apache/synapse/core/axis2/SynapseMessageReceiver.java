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
import org.apache.synapse.statistics.StatisticsStack;
import org.apache.synapse.statistics.impl.ProxyServiceStatisticsStack;

/**
 * This message receiver should be configured in the Axis2 configuration as the
 * default message receiver, which will handle all incoming messages through the
 * synapse mediation
 */
public class SynapseMessageReceiver implements MessageReceiver {

    private static final Log log = LogFactory.getLog(SynapseMessageReceiver.class);

    public void receive(org.apache.axis2.context.MessageContext mc) throws AxisFault {

        log.debug("Synapse received a new message for message mediation...");
        log.debug("Received To: " + (mc.getTo() != null ? mc.getTo().getAddress() : "null"));
        log.debug("SOAPAction: " + (mc.getSoapAction() != null ? mc.getSoapAction() : "null"));
        if (log.isDebugEnabled()) {
            log.debug("Body : \n" + mc.getEnvelope());
        }

        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);
        StatisticsStack synapseServiceStack = (StatisticsStack) synCtx.getProperty(org.apache.synapse.Constants.SYNAPSESERVICE_STATISTICS_STACK);
        if (synapseServiceStack== null) {
            synapseServiceStack= new ProxyServiceStatisticsStack();
            synCtx.setProperty(org.apache.synapse.Constants.SYNAPSESERVICE_STATISTICS_STACK, synapseServiceStack);
        }
        String name = "SynapseService";
        boolean isFault =synCtx.getEnvelope().getBody().hasFault();
        synapseServiceStack.put(name, System.currentTimeMillis(), !synCtx.isResponse(), true,isFault);

        // invoke synapse message mediation
        synCtx.getEnvironment().injectMessage(synCtx);
    }
}
