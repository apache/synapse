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
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.EndpointDefinition;
import org.apache.synapse.statistics.StatisticsUtils;

import java.util.Iterator;

/**
 * This class helps the Axis2SynapseEnvironment implement the send method
 */
public class Axis2Sender {

    private static final Log log = LogFactory.getLog(Axis2Sender.class);

    public static void sendOn(
            EndpointDefinition endpoint,
            org.apache.synapse.MessageContext synapseInMessageContext) {

        try {
                Axis2FlexibleMEPClient.send(
                    // The endpoint where we are sending to
                    endpoint,

                    // The Axis2 Message context of the Synapse MC
                    synapseInMessageContext);

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
            if (smc.isResponse()) {
                //Process statistics
                StatisticsUtils.processEndPointStatistics(smc);
                StatisticsUtils.processProxyServiceStatistics(smc);
                StatisticsUtils.processAllSequenceStatistics(smc);
            }
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
