package org.apache.synapse.mediators.eventing;
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
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.eventing.SynapseSubscriptionManager;
import org.apache.synapse.eventing.SynapseSubscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;

import java.util.List;

public class EventPublisherMediator extends AbstractMediator {
    private static final Log log = LogFactory.getLog(EventPublisherMediator.class);
    private String eventSourceName=null;
    public boolean mediate(MessageContext synCtx){
        if (log.isDebugEnabled()) {
             log.debug("Mediation for Event Publisher started");
         }
        //sendResponce(synCtx); TODO need to investigate this further 
        SynapseEventSource eventSource = synCtx.getConfiguration().getEventSource(eventSourceName);
        SynapseSubscriptionManager subscriptionManager = eventSource.getSubscriptionManager();
            List<SynapseSubscription> subscribers = subscriptionManager.getMatchingSubscribers(synCtx);
            for (SynapseSubscription subscription : subscribers) {
                //TODO: send a 202 responce to the client, client wait and time outs
                synCtx.setProperty("OUT_ONLY", "true");    // Set one way message for events
                try {
                    subscription.getEndpoint().send(MessageHelper.cloneMessageContext(synCtx));
                } catch (AxisFault axisFault) {
                    log.error("Event sending failure "+axisFault.toString());
                    return false;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Event push to  : " + subscription.getEndpointUrl());
                }
            }
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getEventSourceName() {
        return eventSourceName;
    }

    public void setEventSourceName(String eventSourceName) {
        this.eventSourceName = eventSourceName;
    }
    private void sendResponce(MessageContext synCtx){
        MessageContext mc = null;
        try {
            mc = MessageHelper.cloneMessageContext(synCtx);
        String replyAddress = mc.getReplyTo().getAddress();
        AddressEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition def = new EndpointDefinition();
        def.setAddress(replyAddress.trim());
        def.setAddressingOn(true);
        endpoint.setDefinition(def);
        //mc.setEnvelope(null);
        mc.setTo(new EndpointReference(replyAddress));
        mc.setResponse(true);
        endpoint.send(mc);
            } catch (AxisFault axisFault) {
            axisFault.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
