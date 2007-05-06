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
import org.apache.axis2.util.Utils;
import org.apache.axis2.transport.nhttp.NhttpConstants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.statistics.StatisticsUtils;
import org.apache.synapse.util.UUIDGenerator;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.rampart.handler.WSSHandlerConstants;

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

        MessageContext messageContext = ((Axis2MessageContext) smc).getAxis2MessageContext();

        // if this is a dummy 202 Accepted message meant only for the http/s transports
        // prevent it from going into any other transport sender
        if (Utils.isExplicitlyTrue(messageContext, NhttpConstants.SC_ACCEPTED) &&
            messageContext.getTransportOut() != null &&
            !messageContext.getTransportOut().getName().startsWith("http")) {
                return;
        }

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
            Axis2FlexibleMEPClient.removeAddressingHeaders(messageContext);
            messageContext.setMessageID(UUIDGenerator.getUUID());

            // temporary workaround for https://issues.apache.org/jira/browse/WSCOMMONS-197
            if (messageContext.isEngaged(WSSHandlerConstants.SECURITY_MODULE_NAME) &&
                messageContext.getEnvelope().getHeader() == null) {
                SOAPFactory fac = messageContext.isSOAP11() ?
                    OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory();
                fac.createSOAPHeader(messageContext.getEnvelope());
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
