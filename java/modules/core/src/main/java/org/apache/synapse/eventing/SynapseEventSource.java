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

package org.apache.synapse.eventing;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.SynapseMessageReceiver;
import org.apache.synapse.eventing.builders.ResponseMessageBuilder;
import org.apache.synapse.eventing.builders.SubscriptionMessageBuilder;
import org.apache.synapse.util.MessageHelper;
import org.wso2.eventing.EventingConstants;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *  Eventsource that accepts the event requests using a message reciver. 
 */
public class SynapseEventSource extends SynapseMessageReceiver {

    private String name;
    private SynapseSubscriptionManager subscriptionManager;
    private static final Log log = LogFactory.getLog(SynapseEventSource.class);

    public SynapseEventSource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SynapseSubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    public void setSubscriptionManager(SynapseSubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    public void buildService(AxisConfiguration axisCfg) throws AxisFault {
        AxisService eventSourceService = new AxisService();
        eventSourceService.setName(this.name);
        AxisOperation mediateOperation =
                new InOutAxisOperation(SynapseConstants.SYNAPSE_OPERATION_NAME);
        AxisOperation subscribeOperation = new InOutAxisOperation(new QName("subscribe"));

        mediateOperation.setMessageReceiver(this);
        subscribeOperation.setMessageReceiver(this);
        subscribeOperation.setSoapAction(EventingConstants.WSE_SUBSCRIBE);

        eventSourceService.addOperation(mediateOperation);
        eventSourceService.addOperation(subscribeOperation);
        axisCfg.addService(eventSourceService);
        //Set the service parameters
        eventSourceService.addParameter("subscriptionManager", subscriptionManager);
        eventSourceService.addParameter("serviceType", "eventing");
    }

    /**
     * Override the Message reciver method to accept subscriptions and events
     *
     * @param mc
     * @throws AxisFault
     */
    public void receive(MessageContext mc) throws AxisFault {
        SynapseConfiguration synCfg = (SynapseConfiguration) mc.getConfigurationContext()
                .getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_CONFIG).getValue();
        SynapseEnvironment synEnv = (SynapseEnvironment) mc.getConfigurationContext()
                .getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_ENV).getValue();
        org.apache.synapse.MessageContext smc = new Axis2MessageContext(mc, synCfg, synEnv);
        ResponseMessageBuilder messageBuilder = new ResponseMessageBuilder(mc);
        if (EventingConstants.WSE_SUBSCRIBE.equals(mc.getWSAAction())) {
            // add new subscription to the SynapseSubscription store through subscription manager
            SynapseSubscription subscription = SubscriptionMessageBuilder.createSubscription(smc);
            if (log.isDebugEnabled()) {
                log.debug("SynapseSubscription request recived  : " + subscription.getId());
            }
            if (subscription.getId() != null) {
                String subID = subscriptionManager.addSubscription(subscription);
                if (subID != null) {
                    // Send the subscription responce
                    if (log.isDebugEnabled()) {
                        log.debug("Sending subscription response for SynapseSubscription ID : " +
                                subscription.getId());
                    }
                    SOAPEnvelope soapEnvelope =
                            messageBuilder.genSubscriptionResponse(subscription);
                    dispatchResponse(soapEnvelope, EventingConstants.WSE_SUbSCRIBE_RESPONSE,
                            mc, false);
                } else {
                    // Send the Fault responce
                    if (log.isDebugEnabled()) {
                        log.debug("SynapseSubscription Failed, sending fault response");
                    }
                    SOAPEnvelope soapEnvelope = messageBuilder.genFaultResponse(mc,
                            EventingConstants.WSE_FAULT_CODE_RECEIVER, "EventSourceUnableToProcess",
                            "Unable to subscribe ", "");
                    dispatchResponse(soapEnvelope, EventingConstants.WSA_FAULT, mc,
                            true);
                }
            } else {
                // Send the Fault responce
                if (log.isDebugEnabled()) {
                    log.debug("SynapseSubscription Failed, sending fault response");
                }
                SOAPEnvelope soapEnvelope = messageBuilder.genFaultResponse(mc,
                        SubscriptionMessageBuilder.getErrorCode(),
                        SubscriptionMessageBuilder.getErrorSubCode(),
                        SubscriptionMessageBuilder.getErrorReason(), "");
                dispatchResponse(soapEnvelope, EventingConstants.WSA_FAULT, mc,
                        true);
            }

        } else if (EventingConstants.WSE_UNSUBSCRIBE.equals(mc.getWSAAction())) {
            // Unsubscribe for responce
            SynapseSubscription subscription =
                    SubscriptionMessageBuilder.createUnSubscribeMessage(smc);
            if (log.isDebugEnabled()) {
                log.debug("UnSubscribe response recived for SynapseSubscription ID : " +
                        subscription.getId());
            }
            if (subscriptionManager.deleteSubscription(subscription.getId())) {
                //send the response
                if (log.isDebugEnabled()) {
                    log.debug("Sending UnSubscribe responce for SynapseSubscription ID : " +
                            subscription.getId());
                }
                SOAPEnvelope soapEnvelope = messageBuilder.genUnSubscribeResponse(subscription);
                RelatesTo relatesTo = new RelatesTo(subscription.getId());
                dispatchResponse(soapEnvelope, EventingConstants.WSE_UNSUBSCRIBE_RESPONSE,
                        mc, false);
            } else {
                // Send the Fault responce
                if (log.isDebugEnabled()) {
                    log.debug("UnSubscription failed, sending fault repsponse");
                }
                SOAPEnvelope soapEnvelope = messageBuilder.genFaultResponse(mc,
                        EventingConstants.WSE_FAULT_CODE_RECEIVER, "EventSourceUnableToProcess",
                        "Unable to Unsubscribe", "");
                dispatchResponse(soapEnvelope, EventingConstants.WSA_FAULT, mc,
                        true);
            }
        } else if (EventingConstants.WSE_GET_STATUS.equals(mc.getWSAAction())) {
            // Get responce status
            SynapseSubscription subscription =
                    SubscriptionMessageBuilder.createGetStatusMessage(smc);
            if (log.isDebugEnabled()) {
                log.debug("GetStatus request recived for SynapseSubscription ID : " +
                        subscription.getId());
            }
            subscription = subscriptionManager.getSubscription(subscription.getId());
            if (subscription != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Sending GetStatus responce for SynapseSubscription ID : " +
                            subscription.getId());
                }
                //send the responce
                SOAPEnvelope soapEnvelope = messageBuilder.genGetStatusResponse(subscription);
                RelatesTo relatesTo = new RelatesTo(subscription.getId());
                dispatchResponse(soapEnvelope, EventingConstants.WSE_GET_STATUS_RESPONSE,
                        mc, false);
            } else {
                // Send the Fault responce
                if (log.isDebugEnabled()) {
                    log.debug("GetStatus failed, sending fault response");
                }
                SOAPEnvelope soapEnvelope = messageBuilder.genFaultResponse(mc,
                        EventingConstants.WSE_FAULT_CODE_RECEIVER, "EventSourceUnableToProcess",
                        "Subscription Not Found", "");
                dispatchResponse(soapEnvelope, EventingConstants.WSA_FAULT, mc,
                        true);
            }
        } else if (EventingConstants.WSE_RENEW.equals(mc.getWSAAction())) {
            // Renew subscription
            SynapseSubscription subscription =
                    SubscriptionMessageBuilder.createRenewSubscribeMessage(smc);
            if (log.isDebugEnabled()) {
                log.debug("ReNew request recived for SynapseSubscription ID : " +
                        subscription.getId());
            }
            String subID = subscription.getId();
            if (subID != null) {
                if (subscriptionManager.renewSubscription(subscription)) {
                    //send the response
                    if (log.isDebugEnabled()) {
                        log.debug("Sending ReNew response for SynapseSubscription ID : " +
                                subscription.getId());
                    }
                    SOAPEnvelope soapEnvelope =
                            messageBuilder.genRenewSubscriptionResponse(subscription);
                    RelatesTo relatesTo = new RelatesTo(subscription.getId());
                    dispatchResponse(soapEnvelope, EventingConstants.WSE_RENEW_RESPONSE,
                            mc, false);
                } else {
                    // Send the Fault responce
                    if (log.isDebugEnabled()) {
                        log.debug("ReNew failed, sending fault response");
                    }
                    SOAPEnvelope soapEnvelope = messageBuilder.genFaultResponse(mc,
                            EventingConstants.WSE_FAULT_CODE_RECEIVER, "UnableToRenew",
                            "Subscription Not Found", "");
                    dispatchResponse(soapEnvelope, EventingConstants.WSA_FAULT, mc,
                            true);
                }
            } else {
                SOAPEnvelope soapEnvelope = messageBuilder.genFaultResponse(mc,
                        SubscriptionMessageBuilder.getErrorCode(),
                        SubscriptionMessageBuilder.getErrorSubCode(),
                        SubscriptionMessageBuilder.getErrorReason(), "");
                dispatchResponse(soapEnvelope, EventingConstants.WSA_FAULT, mc,
                        true);
            }
        } else {
            // Treat as an Event
            if (log.isDebugEnabled()) {
                log.debug("Event recived");
            }
            dispatchEvents(smc);
        }
    }

    /**
     * Dispatch the message to the target endpoint
     *
     * @param soapEnvelope   Soap Enevlop with message
     * @param responseAction WSE action for the response
     * @param mc             Message Context
     * @param faultMessage   Fault message
     * @throws AxisFault
     */
    private void dispatchResponse(SOAPEnvelope soapEnvelope,
                                  String responseAction,
                                  MessageContext mc,
                                  boolean faultMessage) throws AxisFault {
        MessageContext rmc = MessageContextBuilder.createOutMessageContext(mc);
        rmc.getOperationContext().addMessageContext(rmc);
        rmc.setEnvelope(soapEnvelope);
        rmc.setWSAAction(responseAction);
        rmc.setSoapAction(responseAction);
        rmc.setProperty(SynapseConstants.ISRESPONSE_PROPERTY, Boolean.TRUE);
        if(faultMessage){
            AxisEngine.sendFault(rmc);
        }else{
            AxisEngine.send(rmc);
        }
    }

    /**
     *
     * @param msgCtx message context
     */
    public void dispatchEvents(org.apache.synapse.MessageContext msgCtx){
        List<SynapseSubscription> subscribers = subscriptionManager.getMatchingSubscribers(msgCtx);
        // Call event dispatcher
        msgCtx.getEnvironment().getExecutorService()
                .execute(new EventDispatcher(msgCtx, subscribers));
    }
    /**
     * Dispatching events async on a different thread
     */
    class EventDispatcher implements Runnable {
        org.apache.synapse.MessageContext synCtx;
        List<SynapseSubscription> subscribers;

        EventDispatcher(org.apache.synapse.MessageContext synCtx, List<SynapseSubscription> subscribers) {
            this.synCtx = synCtx;
            this.subscribers = subscribers;
        }

        public void run() {
            for (SynapseSubscription subscription : subscribers) {
                synCtx.setProperty(SynapseConstants.OUT_ONLY, "true");    // Set one way message for events
                try {
                    subscription.getEndpoint().send(MessageHelper.cloneMessageContext(synCtx));
                } catch (AxisFault axisFault) {
                    log.error("Event sending failure " + axisFault.toString());
                }
                if (log.isDebugEnabled()) {
                    log.debug("Event push to  : " + subscription.getEndpointUrl());
                }
            }
        }
    }
}
