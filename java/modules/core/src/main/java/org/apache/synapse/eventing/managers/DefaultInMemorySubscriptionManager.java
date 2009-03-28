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

package org.apache.synapse.eventing.managers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.eventing.SynapseSubscription;
import org.apache.synapse.eventing.SynapseSubscriptionManager;
import org.apache.synapse.eventing.SynapseEventingConstants;
import org.apache.synapse.eventing.filters.XPathBasedEventFilter;
import org.apache.synapse.eventing.filters.TopicBasedEventFilter;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.wso2.eventing.Subscription;
import org.wso2.eventing.Event;
import org.wso2.eventing.exceptions.EventException;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class DefaultInMemorySubscriptionManager extends SynapseSubscriptionManager {

    private final Map<String, SynapseSubscription> store =
            new ConcurrentHashMap<String, SynapseSubscription>();
    private String topicHeaderName;
    private String topicHeaderNS;
    private SynapseXPath topicXPath;
    private static final Log log = LogFactory.getLog(DefaultInMemorySubscriptionManager.class);

    public String addSubscription(SynapseSubscription subs) {
        if (subs.getId() == null) {
            subs.setId(org.apache.axiom.om.util.UUIDGenerator.getUUID());
        }
        store.put(subs.getId(), subs);
        return subs.getId();
    }

    public boolean deleteSubscription(String id) {
        if (store.containsKey(id)) {
            store.remove(id);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Renew the subscription by setting the expire date time
     *
     * @param subscription
     * @return
     */
    public boolean renewSubscription(SynapseSubscription subscription) {
        SynapseSubscription subscriptionOld = getSubscription(subscription.getId());
        if (subscriptionOld != null) {
            subscriptionOld.setExpires(subscription.getExpires());
            return true;
        } else {
            return false;
        }
    }

    public List<SynapseSubscription> getSynapseSubscribers() {
        LinkedList<SynapseSubscription> list = new LinkedList<SynapseSubscription>();
        for (Map.Entry<String, SynapseSubscription> stringSubscriptionEntry : store.entrySet()) {
            list.add(stringSubscriptionEntry.getValue());
        }
        return list;
    }

    public List<SynapseSubscription> getMatchingSubscribers(MessageContext mc) {
        final LinkedList<SynapseSubscription> list = new LinkedList<SynapseSubscription>();
        String evaluatedValue = null;
        for (Map.Entry<String, SynapseSubscription> stringSubscriptionEntry : store.entrySet()) {
            //TODO : pick the filter based on the dialect
            //XPathBasedEventFilter filter = new XPathBasedEventFilter();
            TopicBasedEventFilter filter = new TopicBasedEventFilter();
            if (filter != null) {
                filter.setResultValue(stringSubscriptionEntry.getValue().getFilterValue());
                filter.setSourceXpath(topicXPath);
                //evaluatedValue = topicXPath.stringValueOf(mc);
            }
            Event<MessageContext> event = new Event(mc);
            if (filter == null || filter.match(event)) {
                SynapseSubscription subscription = stringSubscriptionEntry.getValue();
                Calendar current = Calendar.getInstance(); //Get current date and time
                if (subscription.getExpires() != null) {
                    if (current.before(subscription.getExpires())) {
                        // add only valid subscriptions by checking the expiration
                        list.add(subscription);
                    }
                } else {
                    // If a expiration dosen't exisits treat it as a never expire subscription, valid till unsubscribe
                    list.add(subscription);
                }

            }
        }
        return list;
    }

    public List<SynapseSubscription> getStaticSubscribers() {
        LinkedList<SynapseSubscription> list = new LinkedList<SynapseSubscription>();
        for (Map.Entry<String, SynapseSubscription> stringSubscriptionEntry : store.entrySet()) {
            if ((stringSubscriptionEntry.getValue().getSubscriptionData().getProperty(
                    SynapseEventingConstants.STATIC_ENTRY)).equals("true")) {
                list.add(stringSubscriptionEntry.getValue());
            }
        }
        return list;
    }

    @Deprecated
    public String subscribe(Subscription subscription) throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean unsubscribe(String s) throws EventException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String renew(Subscription subscription) throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Subscription> getSubscriptions() throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Subscription> getAllSubscriptions() throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Subscription> getMatchingSubscriptions(String s) throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Subscription> getSubscribers() throws EventException {
        LinkedList<Subscription> list = new LinkedList<Subscription>();
        for (Map.Entry<String, SynapseSubscription> stringSubscriptionEntry : store.entrySet()) {
            list.add(stringSubscriptionEntry.getValue());
        }
        return list;
    }

    public List<Subscription> getAllSubscribers() throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public SynapseSubscription getSubscription(String id) {
        return store.get(id);
    }

    public Subscription getStatus(String s) throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Subscription getStatus(Subscription subscription) throws EventException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void init() {
        try {
            //TODO: pick values from the constants
            topicXPath = new SynapseXPath(
                    "s11:Header/ns:" + topicHeaderName + " | s12:Header/ns:" + topicHeaderName);
            topicXPath.addNamespace("s11", "http://schemas.xmlsoap.org/soap/envelope/");
            topicXPath.addNamespace("s12", "http://www.w3.org/2003/05/soap-envelope");
            topicXPath.addNamespace("ns", topicHeaderNS);
        } catch (JaxenException e) {
            handleException("Unable to create the topic header XPath", e);
        }


    }

    public String getTopicHeaderName() {
        return topicHeaderName;
    }

    public void setTopicHeaderName(String topicHeaderName) {
        this.topicHeaderName = topicHeaderName;
    }

    public String getTopicHeaderNS() {
        return topicHeaderNS;
    }

    public void setTopicHeaderNS(String topicHeaderNS) {
        this.topicHeaderNS = topicHeaderNS;
    }

    private void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }

    private void handleException(String message, Exception e) {
        log.error(message, e);
        throw new SynapseException(message, e);
    }
}
