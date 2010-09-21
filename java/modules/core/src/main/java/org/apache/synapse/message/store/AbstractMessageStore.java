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

package org.apache.synapse.message.store;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;

public abstract class AbstractMessageStore implements MessageStore, ManagedLifecycle {

    /**
     * message store name
     */
    protected String name;

    /**
     * associated redelivery processor
     */
    protected RedeliveryProcessor redeliveryProcessor;

    /**
     * queue that holds the sheduled messages
     */
    protected Queue<StorableMessage> scheduledMessageQueue = new LinkedList<StorableMessage>();

    /**
     * name of the sequence to be executed before storing the message
     */
    protected String sequence;

    /**
     * Message store JMX view
     */
    protected MessageStoreView messageStoreMBean;

    /**
     * synapse configuration reffrence
     */
    protected SynapseConfiguration synapseConfiguration;

    /**
     * synapse environment reffrence
     */
    protected SynapseEnvironment synapseEnvironment;

    /**
     * Message store properties
     */
    protected Map<String,Object> parameters;

    public void init(SynapseEnvironment se) {
        this.synapseEnvironment = se;
        this.synapseConfiguration = synapseEnvironment.getSynapseConfiguration();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        messageStoreMBean = new MessageStoreView(name, this);
        MBeanRegistrar.getInstance().registerMBean(messageStoreMBean,
                "DeadLetterChannel", this.name);
    }

    public void setRedeliveryProcessor(RedeliveryProcessor redeliveryProcessor) {
        this.redeliveryProcessor = redeliveryProcessor;
    }

    public RedeliveryProcessor getRedeliveryProcessor() {
        return redeliveryProcessor;
    }


    public void schedule(StorableMessage storableMessage) {
        if (storableMessage != null) {
            scheduledMessageQueue.add(storableMessage);
        }

        if (scheduledMessageQueue.size() > 0 && redeliveryProcessor != null &&
                !redeliveryProcessor.isStarted()) {
            redeliveryProcessor.start();
        }
    }

    public StorableMessage dequeueScheduledQueue() {
        return scheduledMessageQueue.poll();
    }

    public StorableMessage getFirstSheduledMessage() {
        return scheduledMessageQueue.peek();
    }

    protected void mediateSequence(MessageContext synCtx) {

        if (sequence != null && synCtx != null && "true".equalsIgnoreCase(
                (String) synCtx.getProperty(SynapseConstants.MESSAGE_STORE_REDELIVERED))) {
            Mediator seq = synCtx.getSequence(sequence);
            if (seq != null) {
                seq.mediate(synCtx);
            }
        }
    }

    public int getSize() {
        return -1;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getSequence() {
        return sequence;
    }

    public void setConfiguration(SynapseConfiguration configuration) {
        this.synapseConfiguration = configuration;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getProviderClass() {
        return this.getClass().getName();
    }

    public void destroy() {

    }
}
