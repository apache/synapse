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
package org.apache.synapse.message.processors.forward;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseArtifact;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.message.processors.MessageProcessorConsents;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.MessageStore;
import org.quartz.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Redelivery Job will replay all the Messages in the Message Store when executed
 * Excluding ones that are already tried redelivering more than max number of tries
 */
public class ForwardingJob implements StatefulJob {

    private static final Log log = LogFactory.getLog(ForwardingJob.class);


    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();

        /**
         * Get the Globle Objects from DataMap
         */
        MessageStore messageStore = (MessageStore) jdm.get(
                MessageProcessorConsents.MESSAGE_STORE);
        Map<String, Object> parameters = (Map<String, Object>) jdm.get(
                MessageProcessorConsents.PARAMETERS);
        BlockingMessageSender sender =
                (BlockingMessageSender) jdm.get(ScheduledMessageForwardingProcessor.BLOCKING_SENDER);

        ScheduledMessageForwardingProcessor processor =
                (ScheduledMessageForwardingProcessor) jdm.get(ScheduledMessageForwardingProcessor.PROCESSOR_INSTANCE);


        int maxDeliverAttempts = -1;
        String mdaParam = (String) parameters.get(MessageProcessorConsents.MAX_DELIVER_ATTEMPTS);
        if (mdaParam != null) {
            maxDeliverAttempts = Integer.parseInt(mdaParam);

            // Here we look for the edge case
            if(maxDeliverAttempts == 0) {
                processor.deactivate();
            }
        }

        if(!processor.isActive()) {
            return;
        }

        if (maxDeliverAttempts > 0) {
            processor.incrementSendAttemptCount();
        }

        boolean errorStop = false;
        while (!errorStop) {

            MessageContext messageContext = messageStore.peek();
            if (messageContext != null) {
                Set proSet = messageContext.getPropertyKeySet();

                if (proSet != null) {
                    if (proSet.contains(ForwardingProcessorConstants.BLOCKING_SENDER_ERROR)) {
                        proSet.remove(ForwardingProcessorConstants.BLOCKING_SENDER_ERROR);
                    }
                }

                String targetEp =
                        (String) messageContext.getProperty(ForwardingProcessorConstants.TARGET_ENDPOINT);

                if (targetEp != null) {
                    Endpoint ep = messageContext.getEndpoint(targetEp);

                    if (ep instanceof AddressEndpoint) {
                        AddressEndpoint addEp = (AddressEndpoint) ep;
                        String addressUrl = addEp.getDefinition().getAddress();

                        try {
                            MessageContext outCtx = sender.send(messageContext, addressUrl);

                            if (outCtx != null && "true".equals(outCtx.
                                    getProperty(ForwardingProcessorConstants.BLOCKING_SENDER_ERROR))) {
                                // This Means an Error has occurred
                                if (parameters != null &&
                                        parameters.get(
                                                ForwardingProcessorConstants.FAULT_SEQUENCE) != null) {

                                    String seq = (String) parameters.get(
                                            ForwardingProcessorConstants.FAULT_SEQUENCE);
                                    Mediator mediator = outCtx.getSequence(seq);
                                    if (mediator != null) {
                                        mediator.mediate(outCtx);
                                    } else {
                                        log.warn("Can't Send the fault Message , Sequence " + seq +
                                                " Does not Exist");
                                    }

                                }

                                if (maxDeliverAttempts > 0) {
                                    if(processor.getSendAttemptCount() > maxDeliverAttempts) {
                                        processor.deactivate();
                                    }
                                }
                                errorStop = true;
                                continue;

                            }

                            // If there is a sequence defined to send success replies,
                            // we must send the message to it
                            if (parameters != null &&
                                    parameters.get(
                                            ForwardingProcessorConstants.REPLY_SEQUENCE) != null) {
                                if (outCtx != null) {
                                    String seq = (String) parameters.get(
                                            ForwardingProcessorConstants.REPLY_SEQUENCE);
                                    Mediator mediator = outCtx.getSequence(seq);
                                    if (mediator != null) {
                                        mediator.mediate(outCtx);
                                    } else {
                                        log.warn("Can't Send the Out Message , Sequence " + seq +
                                                " Does not Exist");
                                    }
                                }
                            }

                            // If no Exception Occurred We remove the Message
                            messageStore.poll();
                        } catch (Exception e) {
                            if (maxDeliverAttempts > 0) {
                                if (processor.getSendAttemptCount() > maxDeliverAttempts) {
                                    processor.deactivate();
                                }
                            }
                            errorStop = true;
                            log.error("Error Forwarding Message ", e);
                            continue;
                        }
                    } else {
                        // Currently only Address Endpoint delivery is supported
                        log.warn("Address Endpoint Named " + targetEp + " not found.Hence removing " +
                                "the message form store");
                        messageStore.poll();
                    }


                } else {
                    //No Target Endpoint defined for the Message
                    //So we do not have a place to deliver.
                    //Here we log a warning and remove the message
                    //todo: we can improve this by implementing a target inferring mechanism

                    log.warn("Property " + ForwardingProcessorConstants.TARGET_ENDPOINT +
                            " not found in the message context , Hence removing the message ");
                    messageStore.poll();

                }

            } else {
                if (maxDeliverAttempts > 0) {
                    if (processor.getSendAttemptCount() > maxDeliverAttempts) {
                        processor.deactivate();
                    }
                }
                errorStop = true;
            }
        }
    }


    private BlockingMessageSender initMessageSender(Map<String, Object> params) {

        BlockingMessageSender sender = null;
        String axis2repo = (String) params.get(ForwardingProcessorConstants.AXIS2_REPO);
        String axis2Config = (String) params.get(ForwardingProcessorConstants.AXIS2_CONFIG);

        sender = new BlockingMessageSender();

        if (axis2repo != null) {
            sender.setClientRepository(axis2repo);
        }


        if (axis2Config != null) {
            sender.setAxis2xml(axis2Config);
        }
        sender.init();

        return sender;
    }

}
