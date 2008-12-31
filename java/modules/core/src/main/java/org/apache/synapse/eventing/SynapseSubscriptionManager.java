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

public abstract class SynapseSubscriptionManager implements SubscriptionManager {

    private Map<String, String> properties = new HashMap<String, String>();

    public abstract List<SynapseSubscription> getSynapseSubscribers();

    public abstract List<SynapseSubscription> getMatchingSubscribers(MessageContext mc);

    public abstract List<SynapseSubscription> getStaticSubscribers();

    public abstract SynapseSubscription getSubscription(String id);

    public abstract String addSubscription(SynapseSubscription subs);

    public abstract boolean deleteSubscription(String id);

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
