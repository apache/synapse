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
package org.apache.synapse.message.processors.dlc;

import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseArtifact;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.MessageStore;

import java.util.ArrayList;
import java.util.List;

public class DeadLetterChannelView implements DeadLetterChannelViewMBean {

    private MessageStore messageStore;

    private RedeliveryProcessor redeliveryProcessor;


    public DeadLetterChannelView(MessageStore messageStore) {
        this.messageStore = messageStore;
        this.redeliveryProcessor = (RedeliveryProcessor) messageStore.getMessageProcessor();
    }

    public void resendAll() {
        if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
            int size = messageStore.getSize();
            for (int i = 0; i < size; i++) {
                MessageContext messageContext = messageStore.unstore(0, 0).get(0);
                if (messageContext != null) {
                    redeliver(messageContext);
                }

            }
        } else {
            throw new SynapseException("Error Message store being used re try later");
        }
    }

    public void deleteAll() {
        if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
            int size = messageStore.getSize();
            for (int i = 0; i < size; i++) {
                messageStore.unstore(0, 0);
            }
        } else {
            throw new SynapseException("Error Message store being used re try later");
        }
    }

    public List<String> getMessageIds() {
        int size = messageStore.getSize();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            MessageContext messageContext = messageStore.getMessages(0, 0).get(0);
            if (messageContext != null) {
                list.add(messageContext.getMessageID());
            }
        }
        return list;
    }

    public void resend(String messageID) {
        if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
            MessageContext messageContext = messageStore.unstore(messageID);
            if (messageContext != null) {
                redeliver(messageContext);
            }
        } else {
            throw new SynapseException("Error Message store being used re try later");
        }
    }

    public void delete(String messageID) {
        if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
            messageStore.unstore(messageID);
        }
    }

    public String getEnvelope(String messageID) {
        MessageContext messageContext = messageStore.getMessage(messageID);
        if (messageContext != null) {
            return messageContext.getEnvelope().toString();
        }

        return null;
    }

    public int getSize() {
        return messageStore.getSize();
    }

    private void redeliver(MessageContext messageContext) {
        SynapseArtifact artifact = RedeliveryJob.getReplayTarget(messageContext);
        if (artifact instanceof Endpoint) {
            if (!RedeliveryJob.handleEndpointReplay((Endpoint) artifact,
                    messageContext)) {
                messageStore.store(messageContext);
            }
        } else if (artifact instanceof Mediator) {
            if (!RedeliveryJob.handleSequenceReplay((Mediator) artifact,
                    messageContext)) {
                messageStore.store(messageContext);
            }
        } else {
            messageStore.store(messageContext);
        }
    }
}
