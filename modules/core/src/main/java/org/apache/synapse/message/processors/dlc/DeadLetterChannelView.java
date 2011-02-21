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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static Log log = LogFactory.getLog(DeadLetterChannelView.class);


    public DeadLetterChannelView(MessageStore messageStore) {
        if (messageStore != null) {
            this.messageStore = messageStore;
        } else {
            throw new SynapseException("Error , Can not create Dead Letter Channel view with null " +
                    "message store");
        }

    }

    public void resendAll() {
        log.info("Manually Resending All messages in the Message Store " + messageStore.getName());
        if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
            try {
                int size = messageStore.size();
                for (int i = 0; i < size; i++) {
                    MessageContext messageContext = messageStore.poll();
                    if (messageContext != null) {
                        redeliver(messageContext);
                    }
                }
            } finally {
                ((AbstractMessageStore) messageStore).getLock().unlock();
            }
        } else {
            log.info("Message store being used Can't perform resendAll operation");
            throw new SynapseException("Error Message store being used re try later");
        }
    }

    public void deleteAll() {
        log.info("Manually deleting all messages in Message store");
        if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
            try {
                int size = messageStore.size();
                for (int i = 0; i < size; i++) {
                    messageStore.poll();
                }
            } finally {
                ((AbstractMessageStore) messageStore).getLock().unlock();
            }
        } else {
            log.info("Message store being used Can't perform deleteAll operation");
            throw new SynapseException("Error Message store being used re try later");
        }
    }

    public List<String> getMessageIds() {
        int size = messageStore.size();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            MessageContext messageContext = messageStore.peek();
            if (messageContext != null) {
                list.add(messageContext.getMessageID());
            }
        }
        return list;
    }

    public void resend(String messageID) {
        log.info(" Manually re-sending the Message with id " + messageID);
        if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
            try {
                MessageContext messageContext = messageStore.remove(messageID);
                if (messageContext != null) {
                    redeliver(messageContext);
                }
            } finally {
                ((AbstractMessageStore) messageStore).getLock().unlock();
            }
        } else {
            log.info("Message store being used Can't perform resend operation for" +
                        " message with id " +messageID);
            throw new SynapseException("Error Message store being used re try later");
        }
    }

    public void delete(String messageID) {
        log.info(" Manually deleting the Message with id " + messageID);

            if (((AbstractMessageStore) messageStore).getLock().tryLock()) {
                try {
                    messageStore.remove(messageID);
                } finally {
                    ((AbstractMessageStore) messageStore).getLock().unlock();
                }
            } else {
                log.info("Message store being used Can't perform delete operation for" +
                        " message with id " +messageID);
                throw new SynapseException("Error Message store being used re try later");
            }

    }

    public String getEnvelope(String messageID) {
        MessageContext messageContext = messageStore.get(messageID);
        if (messageContext != null) {
            return messageContext.getEnvelope().toString();
        }

        return null;
    }

    public int getSize() {
        return messageStore.size();
    }

    private void redeliver(MessageContext messageContext) {
        SynapseArtifact artifact = RedeliveryJob.getReplayTarget(messageContext);
        if (artifact instanceof Endpoint) {
            if (!RedeliveryJob.handleEndpointReplay((Endpoint) artifact,
                    messageContext)) {
                messageStore.offer(messageContext);
            }
        } else if (artifact instanceof Mediator) {
            if (!RedeliveryJob.handleSequenceReplay((Mediator) artifact,
                    messageContext)) {
                messageStore.offer(messageContext);
            }
        } else {
            messageStore.offer(messageContext);
        }
    }
}
