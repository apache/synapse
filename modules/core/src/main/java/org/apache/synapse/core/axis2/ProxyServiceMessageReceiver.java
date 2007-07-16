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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.*;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.statistics.impl.ProxyServiceStatisticsStack;

/**
 * This is the MessageReceiver set to act on behalf of Proxy services.
 */
public class ProxyServiceMessageReceiver extends SynapseMessageReceiver {

    private static final Log log = LogFactory.getLog(ProxyServiceMessageReceiver.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    /**
     * The name of the Proxy Service
     */
    private String name = null;

    public void receive(org.apache.axis2.context.MessageContext mc) throws AxisFault {

        if (log.isDebugEnabled()) {
            log.debug("Proxy Service " + name + " received a new message...");
            log.debug("Message To: " + (mc.getTo() != null ? mc.getTo().getAddress() : "null"));
            log.debug("SOAPAction: " + (mc.getSoapAction() != null ? mc.getSoapAction() : "null"));
            log.debug("WSA-Action: " + (mc.getWSAAction() != null ? mc.getWSAAction() : "null"));
            String[] cids = mc.getAttachmentMap().getAllContentIDs();
            if (cids != null && cids.length > 0) {
                for (int i = 0; i < cids.length; i++) {
                    log.debug("Attachment : " + cids[i]);
                }
            }
            log.debug("Body : \n" + mc.getEnvelope());
        }

        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);

        try {
            synCtx.setProperty(org.apache.synapse.Constants.PROXY_SERVICE, name);
            ProxyService proxy = synCtx.getConfiguration().getProxyService(name);

            // Setting Required property to collect the proxy service statistics
            boolean statisticsEnable;
            if (proxy != null) {
                statisticsEnable = (
                        org.apache.synapse.Constants.STATISTICS_ON == proxy.getStatisticsEnable());
                if (statisticsEnable) {
                    ProxyServiceStatisticsStack proxyServiceStatisticsStack
                            = new ProxyServiceStatisticsStack();
                    boolean isFault = synCtx.getEnvelope().getBody().hasFault();
                    proxyServiceStatisticsStack.put(name, System.currentTimeMillis(),
                            !synCtx.isResponse(), statisticsEnable, isFault);
                    synCtx.setProperty(org.apache.synapse.Constants.PROXYSERVICE_STATISTICS_STACK,
                            proxyServiceStatisticsStack);
                }
                boolean shouldTrace = (proxy.getTraceState() == Constants.TRACING_ON);
                if (shouldTrace) {
                    trace.trace("Proxy Service " + name + " received a new message...");
                    trace.trace("Received Message :: " + mc.getEnvelope());
                }
                if (proxy.getTargetFaultSequence() != null) {

                    Mediator faultSequence = synCtx.getSequence(proxy.getTargetFaultSequence());
                    if (faultSequence != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Setting the fault-sequence of the " +
                                    "proxy service to MessageContext");
                        }
                        synCtx.pushFaultHandler(new MediatorFaultHandler(
                                synCtx.getSequence(proxy.getTargetFaultSequence())));
                    } else {
                        // when we can not find the reference to the fault sequence of the proxy
                        // service we should not throw an exception because still we have the global
                        // fault sequence and the message mediation can still continue
                        log.warn("Unable to find the fault-sequence for the proxy service " +
                                "specified by the name " + proxy.getTargetFaultSequence());
                    }
                } else if (proxy.getTargetInLineFaultSequence() != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Setting the anonymous fault-sequence of the proxy to context");
                    }
                    synCtx.pushFaultHandler(
                            new MediatorFaultHandler(proxy.getTargetInLineFaultSequence()));
                }

                // Using inSequence for the incoming message mediation
                if (proxy.getTargetInSequence() != null) {
                    Mediator inSequence = synCtx.getSequence(proxy.getTargetInSequence());
                    if (inSequence != null) {
                        String msg = "Using the sequence named "
                                + proxy.getTargetInSequence() + " for message mediation";
                        if (shouldTrace) {
                            trace.trace(msg);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(msg);
                        }
                        inSequence.mediate(synCtx);
                    } else {

                        log.error("Unable to find the in-sequence for the proxy service " +
                                "specified by the name " + proxy.getTargetInSequence());
                        throw new SynapseException("Unable to find the in-sequence for the proxy " +
                                "service specified by the name " + proxy.getTargetInSequence());
                    }
                } else if (proxy.getTargetInLineInSequence() != null) {
                    String msg = "Using the anonymous " +
                            "in-sequence of the proxy service for message mediation";
                    if (shouldTrace) {
                        trace.trace(msg);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(msg);
                    }
                    proxy.getTargetInLineInSequence().mediate(synCtx);
                }

                if (proxy.getTargetEndpoint() != null) {
                    Endpoint endpoint = synCtx.getEndpoint(proxy.getTargetEndpoint());
                    if (endpoint != null) {
                        String msg = "Forwarding message to the endpoint named "
                                + proxy.getTargetEndpoint() + " after message mediation";
                        if (shouldTrace) {
                            trace.trace(msg);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(msg);
                        }
                        endpoint.send(synCtx);
                    } else {

                        log.error("Unable to find the endpoint for the proxy service " +
                                "specified by the name " + proxy.getTargetEndpoint());
                        throw new SynapseException("Unable to find the endpoint for the " +
                                "proxy service specified by the name " + proxy.getTargetEndpoint());
                    }
                } else if (proxy.getTargetInLineEndpoint() != null) {
                    String msg = "Forwarding the message to the anonymous " +
                            "endpoint of the proxy service after message mediation";
                    if (shouldTrace) {
                        trace.trace(msg);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(msg);
                    }
                    proxy.getTargetInLineEndpoint().send(synCtx);
                }

            } else {
                log.error("Proxy Service with the name " + name + " does not exists");
                throw new SynapseException(
                        "Proxy Service with the name " + name + " does not exists");
            }
        } catch (SynapseException syne) {
            if (!synCtx.getFaultStack().isEmpty()) {
                ((FaultHandler) synCtx.getFaultStack().pop()).handleFault(synCtx, syne);
            } else {
                log.error("Synapse encountered an exception, " +
                        "No error handlers found - [Message Dropped]\n" + syne.getMessage());
            }
        }

    }

    /**
     * Set the name of the corresponding proxy service
     *
     * @param name the proxy service name
     */
    public void setName(String name) {
        this.name = name;
    }

}
