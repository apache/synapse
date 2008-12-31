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

import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.util.UUIDGenerator;
import org.wso2.eventing.EventingConstants;
import org.wso2.eventing.Subscription;

import java.util.Calendar;

/**
 *
 */
public class SynapseSubscription extends Subscription {

    private SynapseEventFilter filter;
    private Endpoint endpoint;
    private boolean staticEntry;
    public SynapseSubscription() {
        this.setId(UUIDGenerator.getUUID());
        this.setDeliveryMode(EventingConstants.WSE_DEFAULT_DELIVERY_MODE);
        this.setStaticEntry(false);
    }

    public SynapseSubscription(String deliveryMode) {
        this.setId(UUIDGenerator.getUUID());
        this.setDeliveryMode(deliveryMode);
    }

    public SynapseEventFilter getSynapseFilter() {
        return filter;
    }

    public void setFilter(SynapseEventFilter filter) {
        this.filter = filter;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isStaticEntry() {
        return staticEntry;
    }

    public void setStaticEntry(boolean staticEntry) {
        this.staticEntry = staticEntry;
    }
}
