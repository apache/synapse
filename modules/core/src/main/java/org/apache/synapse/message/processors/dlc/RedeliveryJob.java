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
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseArtifact;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.message.processors.MessageProcessorConsents;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.MessageStore;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Redelivery Job will replay all the Messages in the Message Store when executed
 * Excluding ones that are already tried redelivering more than max number of tries
 */
public class RedeliveryJob implements Job {

    private static final Log log = LogFactory.getLog(RedeliveryJob.class);

    private MessageStore messageStore;
    private Lock lock;
    private int maxNumberOfRedelivers;

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();

        messageStore = (MessageStore) jdm.get(MessageProcessorConsents.MESSAGE_STORE);
        lock = ((AbstractMessageStore) messageStore).getLock();
        Map<String, Object> parameters = (Map<String, Object>) jdm.get(
                MessageProcessorConsents.PARAMETERS);
        Object o = parameters.get(DLCConstents.MAX_REDELIVERY_COUNT);
        if(o == null) {
            maxNumberOfRedelivers = 0;
        } else {
            maxNumberOfRedelivers = Integer.parseInt((String)o);
        }

        /** We are not going to access message Store if max number of redelivery is less than 0*/
        if(maxNumberOfRedelivers <= 0) {
            return;
        }

        /**
         * We will keep the message store lock till the redelivery is over
         */
        if (lock.tryLock()) {

            try {
                int size = messageStore.size();
                for (int i = 0; i < size; i++) {
                    MessageContext messageContext = messageStore.poll();
                    if (messageContext != null) {
                        SynapseArtifact artifact = getReplayTarget(messageContext);

                        if (messageContext.getProperty(DLCConstents.NO_OF_REDELIVERS) == null) {
                            messageContext.setProperty(DLCConstents.NO_OF_REDELIVERS, "0");
                        }

                        String numberS = (String) messageContext.getProperty(
                                                                    DLCConstents.NO_OF_REDELIVERS);
                        int number = Integer.parseInt(numberS);

                        if (number >= maxNumberOfRedelivers) {

                            if (log.isDebugEnabled()) {
                                log.debug("Maximum number of attempts tried for Message with ID " +
                                        messageContext.getMessageID() +
                                        "will be put back to the Message Store");

                            }
                            messageStore.offer(messageContext);
                            continue;
                        }

                        messageContext.setProperty(DLCConstents.NO_OF_REDELIVERS, "" + (number + 1));


                        if (artifact instanceof Endpoint) {
                            if (!handleEndpointReplay((Endpoint) artifact, messageContext)) {
                                messageStore.offer(messageContext);
                            }
                        } else if (artifact instanceof Mediator) {
                            if (!handleSequenceReplay((Mediator) artifact, messageContext)) {
                                messageStore.offer(messageContext);
                            }
                        } else {
                            log.warn("Replay mediator or endpoint not specified. You may need to set" +
                                    " property " + DLCConstents.REPLAY_ENDPOINT +" or " +
                                        DLCConstents.REPLAY_SEQUENCE);
                            messageStore.offer(messageContext);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("Processed \n" + messageContext.getEnvelope());
                        }

                    }
                }
            } finally {
                lock.unlock();
            }

        }


    }

    /**
     * This will handle the Message replay to the endpoints
     * @param endpoint   target endpoint to be redelivered
     * @param messageContext message context of the message to be redelivered
     * @return  true if success
     */
   static boolean handleEndpointReplay(Endpoint endpoint, MessageContext messageContext) {
        setFaultHandler(messageContext);
        if (endpoint != null && messageContext != null) {
            if (endpoint.readyToSend()) {
                if(log.isDebugEnabled()) {
                    log.debug("Re sending Message via Endpoint " + endpoint.getName());
                }
                endpoint.send(messageContext);
                return true;
            } else {
                return false;
            }

        } else {
            return false;
        }

    }

    /**
     * This will handle the Message replay to sequences
     * @param mediator  target mediator to replay
     * @param messageContext message context of the message to be replayed
     * @return  true of success
     */
    static boolean handleSequenceReplay(Mediator mediator, MessageContext messageContext) {
        if (mediator != null && messageContext != null) {
            setFaultHandler(messageContext);
            if(log.isDebugEnabled()) {
                log.debug("Re sending message via Mediator " +
                        messageContext.getProperty(DLCConstents.REPLAY_SEQUENCE));
            }
            mediator.mediate(messageContext);
            return true;
        }
        return true;
    }

    /**
     * Get the replay target from the message context
     * @param context Message Context
     * @return Endpoint or Mediator to be replayed
     */
   static SynapseArtifact getReplayTarget(MessageContext context) {
        //Endpoint replay get priority
        if (context.getProperty(DLCConstents.REPLAY_ENDPOINT) != null) {
            String endpointName = (String) context.getProperty(DLCConstents.REPLAY_ENDPOINT);
            return context.getConfiguration().getDefinedEndpoints().get(endpointName);
        } else if (context.getProperty(DLCConstents.REPLAY_SEQUENCE) != null) {
            String sequenceName = (String) context.getProperty(DLCConstents.REPLAY_SEQUENCE);

            return context.getConfiguration().getSequence(sequenceName);
        }

        return null;
    }

    private static void setFaultHandler(MessageContext messageContext) {
        String replayFaultHandler = (String)messageContext.getProperty(
                DLCConstents.REPLAY_FAULT_HANDLER);
        if(replayFaultHandler != null) {
            if(messageContext.getConfiguration().getDefinedEndpoints().
                    containsKey(replayFaultHandler)) {
                Endpoint ep = messageContext.getEndpoint(replayFaultHandler);
                messageContext.pushFaultHandler((FaultHandler)ep);
            } else if (messageContext.getConfiguration().getDefinedSequences().
                    containsKey(replayFaultHandler)) {
                Mediator mediator = messageContext.getSequence(replayFaultHandler);
                MediatorFaultHandler faultHandler = new MediatorFaultHandler(mediator);
                messageContext.pushFaultHandler(faultHandler);
            } else {
                log.warn("Error handler " + replayFaultHandler + " Not defined in the synapse " +
                        "configuration. You may need to set the property " +
                        DLCConstents.REPLAY_FAULT_HANDLER);
            }
        }else {
            log.warn("No fault handler defined for the replaying Message with id " +
                    messageContext.getMessageID());
        }
    }
}
