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

import org.apache.synapse.MessageContext;
import org.wso2.eventing.SubscriptionManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subscription Manager for Synapse
 */
public abstract class SynapseSubscriptionManager implements SubscriptionManager {

    private Map<String, String> properties = new HashMap<String, String>();

    /**
     * Return all Active subscriptions
     *
     * @return List of subscriptions
     */
    public abstract List<SynapseSubscription> getSynapseSubscribers();

    /**
     * Get the matching subscriptions for a given filter.
     *
     * @param mc Message context
     * @return List of subscriptions
     */
    public abstract List<SynapseSubscription> getMatchingSubscribers(MessageContext mc);

    /**
     * Get the static subscription defined in the configuration
     *
     * @return List of static subscriptions
     */
    public abstract List<SynapseSubscription> getStaticSubscribers();

    /**
     * Get a subscription by subscription ID
     *
     * @param id subscription ID
     * @return SynapseSubscription
     */
    public abstract SynapseSubscription getSubscription(String id);

    /**
     * Add a new subscription to the store
     *
     * @param subs Subscription object
     * @return String subscription ID
     */
    public abstract String addSubscription(SynapseSubscription subs);

    /**
     * Delete a given subscription
     *
     * @param id Subscription ID
     * @return True|False
     */
    public abstract boolean deleteSubscription(String id);

    /**
     * Renew a given subscription
     *
     * @param subscription subscription object
     * @return True|False
     */
    public abstract boolean renewSubscription(SynapseSubscription subscription);

    public abstract void init();

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    public String getPropertyValue(String name) {
        return properties.get(name);
    }
}
