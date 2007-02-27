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
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.statistics.StatisticsUtils;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;
import org.apache.synapse.statistics.impl.ProxyServiceStatisticsStack;
import org.apache.synapse.config.Endpoint;
import org.apache.axiom.om.OMNode;

/**
 * This is the MessageReceiver set to act on behalf of Proxy services.
 */
public class ProxyServiceMessageReceiver extends SynapseMessageReceiver {

    private static final Log log = LogFactory.getLog(ProxyServiceMessageReceiver.class);

    /**
     * The name of the Proxy Service
     */
    private String name = null;

    /**
     * A target endpoint name requests Synapse to directly forward this message to the
     * endpoint definition with the given name. If a target endpoint or sequence is not
     * specified, the default main mediator would be used for mediation.
     */
    private String targetEndpoint = null;
    /**
     * A target inSequence name specifies to Synapse to use the given named sequence for
     * message mediation for incoming messages. If a target endpoint or inSequence is not
     * specified, the default main mediator would be used for incoming message mediation.
     */
    private String targetInSequence = null;
    /**
     * A target outSequence name specifies to Synapse to use the given named sequence for
     * message mediation for outgoing messages. If a target endpoint or outSequence is not
     * specified, the default main mediator would be used for outgoing message mediation.
     */
    private String targetOutSequence = null;

    private String targetFaultSequence = null;
       /** The target endpoint, if assigned   */
    private Endpoint targetInLineEndpoint = null;
    /** The target inSequence, if assigned */
    private SequenceMediator targetInLineInSequence = null;
    /** The target outSequence, if assigned  */
    private SequenceMediator targetInLineOutSequence = null;
    /** The target faultSequence, if assigned  */
    private SequenceMediator targetInLineFaultSequence = null;

    public void receive(org.apache.axis2.context.MessageContext mc) throws AxisFault {

        log.debug("Proxy Service " + name + " received a new message...");
        log.debug("Message To: " + (mc.getTo() != null ? mc.getTo().getAddress() : "null"));
        log.debug("SOAPAction: " + (mc.getSoapAction() != null ? mc.getSoapAction() : "null"));
        if (log.isDebugEnabled()) {
            log.debug("Body : \n" + mc.getEnvelope());
        }

        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);

        // Setting Required property to collect the proxy service statistics
        boolean statisticsEnable;
        ProxyService currentProxyService = synCtx.getConfiguration().getProxyService(name);
        if (currentProxyService != null) {
            statisticsEnable = (org.apache.synapse.Constants.STATISTICS_ON == currentProxyService.getStatisticsEnable());
            if (statisticsEnable) {
                ProxyServiceStatisticsStack proxyServiceStatisticsStack = new ProxyServiceStatisticsStack();
                boolean isFault = synCtx.getEnvelope().getBody().hasFault();
                proxyServiceStatisticsStack.put(name, System.currentTimeMillis(), !synCtx.isResponse(), statisticsEnable, isFault);
                synCtx.setCorrelationProperty(org.apache.synapse.Constants.PROXYSERVICE_STATISTICS_STACK, proxyServiceStatisticsStack);
            }
        }

        // if a target endpoint is specified, directly forward to that
        if (targetEndpoint != null) {
            Endpoint endpoint = synCtx.getConfiguration().getNamedEndpoint(targetEndpoint);
            if (endpoint == null) {
                // what else can/should we do instead of just logging the message as an error?
                log.error("The endpoint named '" + targetEndpoint
                        + "' is not defined. Dropping current message");
            } else {
                synCtx.setTo(new EndpointReference(endpoint.getAddress()));
                log.debug("Forwarding message directly to the endpoint named : " + targetEndpoint);

                org.apache.axis2.context.MessageContext axisInMsgContext =
                        ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                Axis2FlexibleMEPClient.send(endpoint, synCtx);
            }

        } else {

            // if a named outSequence os specified set it as a property to the MessageContext
            if (targetOutSequence != null) {
                log.debug("OutSequence " + targetOutSequence
                        + " for the proxy set to the MessageContext");
                synCtx.setProperty(org.apache.synapse.Constants.OUT_SEQUENCE, targetOutSequence);
            }

            // if a named inSequence is specified, use it for message mediation
            if (targetInSequence != null) {
                Mediator mediator = synCtx.getConfiguration().getNamedSequence(targetInSequence);
                if (mediator == null) {
                    // what else can/should we do instead of just logging the message as an error?
                    log.error("The mediator named '" + targetInSequence
                            + "' is not defined. Dropping current message");
                } else {
                    log.debug("Using sequence named : " + targetInSequence
                            + " for message mediation");
                    mediator.mediate(synCtx);
                }

                // else default to the Synapse main mediator
            } else {
                log.debug("Using default 'main' mediator for message mediation");
                synCtx.getEnvironment().injectMessage(synCtx);
            }
        }
    }

    /**
     * Specify a named target endpoint for direct message forwarding
     *
     * @param targetEndpoint the name of the target endpoint to be used
     */
    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    /**
     * Specify a named target sequence to be used for message mediation for incoming messages
     *
     * @param targetInSequence the name of the target sequence to be used for incoming messages
     */
    public void setTargetInSequence(String targetInSequence) {
        this.targetInSequence = targetInSequence;
    }

    /**
     * Specify a named target sequence to be used for message mediation for outgoing messages
     *
     * @param targetOutSequence the name of the target sequence to be used for outgoing messages
     */
    public void setTargetOutSequence(String targetOutSequence) {
        this.targetOutSequence = targetOutSequence;
    }

    /**
     * Set the name of the corresponding proxy service
     *
     * @param name the proxy service name
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setTargetFaultSequence(String targetFaultSequence) {
        this.targetFaultSequence = targetFaultSequence;
    }

    public void setTargetInLineEndpoint(Endpoint targetInLineEndpoint) {
        this.targetInLineEndpoint = targetInLineEndpoint;
    }

    public void setTargetInLineInSequence(SequenceMediator targetInLineInSequence) {
        this.targetInLineInSequence = targetInLineInSequence;
    }

    public void setTargetInLineOutSequence(SequenceMediator targetInLineOutSequence) {
        this.targetInLineOutSequence = targetInLineOutSequence;
    }

    public void setTargetInLineFaultSequence(SequenceMediator targetInLineFaultSequence) {
        this.targetInLineFaultSequence = targetInLineFaultSequence;
    }
}
