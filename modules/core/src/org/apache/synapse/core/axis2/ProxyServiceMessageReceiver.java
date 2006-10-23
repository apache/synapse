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
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.Endpoint;

/**
 * This is the MessageReceiver set to act on behalf of Proxy services.
 */
public class ProxyServiceMessageReceiver extends SynapseMessageReceiver {

    private static final Log log = LogFactory.getLog(ProxyServiceMessageReceiver.class);

    /** The name of the Proxy Service */
    private String name = null;

    /**
     * A target endpoint name requests Synapse to directly forward this message to the
     * endpoint definition with the given name. If a target endpoint or sequence is not
     * specified, the default main mediator would be used for mediation.
     */
    private String targetEndpoint = null;
    /**
     * A target sequence name specifies to Synapse to use the given named sequence for
     * message mediation. If a target endpoint or sequence is not specified, the default
     * main mediator would be used for mediation.
     */
    private String targetSequence = null;

    public void receive(org.apache.axis2.context.MessageContext mc) throws AxisFault {

        log.debug("Proxy Service " + name + " received a new message...");
        log.debug("Message To: " + (mc.getTo() != null ?
            mc.getTo().getAddress() : "null"));
        log.debug("SOAPAction: " + (mc.getWSAAction() != null ?
            mc.getWSAAction() : "null"));
        log.debug("Body : \n" + mc.getEnvelope());

        MessageContext synCtx = Axis2MessageContextFinder.getSynapseMessageContext(mc);

        // if a target endpoint is specified, directly forward to that
        if (targetEndpoint != null) {
            Endpoint endpoint = synCtx.getConfiguration().getNamedEndpoint(targetEndpoint);
            if (endpoint == null) {
                // what else can/should we do instead of just logging the message as an error?
                log.error("The endpoint named '" + targetEndpoint + "' is not defined. Dropping current message");
            } else {
                synCtx.setTo(new EndpointReference(endpoint.getAddress()));
                log.debug("Forwarding message directly to the endpoint named : " + targetEndpoint);

                org.apache.axis2.context.MessageContext axisInMsgContext =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                org.apache.axis2.context.MessageContext axisOutMsgContext =
                    Axis2FlexibleMEPClient.send(
                        false, false,
                        endpoint.getWsSecPolicyKey(),
                        endpoint.isReliableMessagingOn(),
                        endpoint.getWsRMPolicyKey(),
                        synCtx);

                axisOutMsgContext.setServerSide(true);
                axisOutMsgContext.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_OUT,
                    axisInMsgContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_OUT));

                axisOutMsgContext.setTransportIn(axisInMsgContext.getTransportIn());

                // TODO this may be really strange but true.. unless you call the below, sometimes it
                // results in an unbound URI exception for no credible reason - needs more investigation
                // seems like a woodstox issue. Use hack for now
                axisOutMsgContext.getEnvelope().build();
                
                log.debug("Reply Body : \n" + axisOutMsgContext.getEnvelope());

                AxisEngine ae = new AxisEngine(axisOutMsgContext.getConfigurationContext());
                try {
                    axisOutMsgContext.setProperty(org.apache.synapse.Constants.ISRESPONSE_PROPERTY, Boolean.TRUE);
                    mc.getOperationContext().setProperty(Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
                    // check for addressing is alredy engaged for this message.
                    // if engage we sho 	uld use the address enable Configuraion context.
                    ae.send(axisOutMsgContext);

                } catch (AxisFault e) {
                    log.error("Axis fault encountered while forwarding message to endpoint : " + targetEndpoint, e);
                }
            }

        // if a named sequence is specified, use it for message mediation
        } else if (targetSequence != null) {
            Mediator mediator = synCtx.getConfiguration().getNamedSequence(targetSequence);
            if (mediator == null) {
                // what else can/should we do instead of just logging the message as an error?
                log.error("The mediator named '" + targetSequence + "' is not defined. Dropping current message");
            } else {
                log.debug("Using sequence named : " + targetSequence + " for message mediation");
                mediator.mediate(synCtx);
            }

        // else default to the Synapse main mediator
        } else {
            log.debug("Using default 'main' mediator for message mediation");
            synCtx.getEnvironment().injectMessage(synCtx);
        }
    }

    /**
     * Specify a named target endpoint for direct message forwarding
     * @param targetEndpoint the name of the target endpoint to be used
     */
    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    /**
     * Specify a named target sequence to be used for message mediation
     * @param targetSequence the name of the target sequence to be used
     */
    public void setTargetSequence(String targetSequence) {
        this.targetSequence = targetSequence;
    }

    /**
     * Set the name of the corresponding proxy service
     * @param name the proxy service name
     */
    public void setName(String name) {
        this.name = name;
    }
}
