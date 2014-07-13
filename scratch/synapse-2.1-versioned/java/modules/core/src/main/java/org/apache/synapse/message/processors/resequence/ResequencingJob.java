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
package org.apache.synapse.message.processors.resequence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.processors.MessageProcessorConsents;
import org.apache.synapse.message.processors.ScheduledMessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * All necessary logic for Resequencing is implemented with in this class.
 * This class extends from Job class which comes from Quartz
 */
public class ResequencingJob implements Job {

    /**
     * Log is set to the current class
     */
    private static final Log log = LogFactory.getLog(ResequencingJob.class);

    /**
     * This method will takes the necessary parameters from parameter list and do the resequencing
     * Resequencing is done through reading messages until the next-to-send message is found
     * If required is not found then waits until the next instance is created.
     *
     * @param jobExecutionContext - a bundle with information related to environment
     * @throws JobExecutionException - to indicate Quartz scheduler that an error occurred while executing the job
     */
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        final JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
        final MessageStore messageStore = (MessageStore) jdm.get(MessageProcessorConsents.MESSAGE_STORE);
        final ResequencingProcessor processor = (ResequencingProcessor) jdm.get(
                ScheduledMessageProcessor.PROCESSOR_INSTANCE);

        final Map<String, Object> parameters = (Map<String, Object>) jdm.get(MessageProcessorConsents.PARAMETERS);
        final String sequence = (String) parameters.get(ResequencingProcessor.NEXT_SEQUENCE);

        SynapseXPath seqNoxPath = null;

        /** Checking for activation of processor or existence of message store  */
        if (!processor.isActive() || messageStore == null) {
            return;
        }

        /** Extract the SynapseXpath object from parameters to identify the sequence number of the message */
        if (parameters.get(ResequencingProcessor.SEQUENCE_NUMBER_XPATH) != null) {
            seqNoxPath = (SynapseXPath) parameters.get(ResequencingProcessor.SEQUENCE_NUMBER_XPATH);
        }

        /** Extract the number of messages interested to come */
        if (parameters.get(ResequencingProcessor.REQ_INIT_MSGS) != null) {
            processor.setRequiredInitMsgs(new AtomicInteger(Integer.parseInt((String) parameters.get(
                    ResequencingProcessor.REQ_INIT_MSGS))));
        }
        /** Extract the delay wait until the interested messages come */
        if (parameters.get(ResequencingProcessor.REQ_INIT_MSGS_DELAY) != null) {
            processor.setRequiredInitMsgsDelay(new AtomicInteger(Integer.parseInt((String) parameters.get(
                    ResequencingProcessor.REQ_INIT_MSGS_DELAY))));
        }

        /** Extract whether to delete duplicate messages */
        if (parameters.get(ResequencingProcessor.DELETE_DUPLICATES) != null) {
            String result=(String) parameters.get(ResequencingProcessor.DELETE_DUPLICATES);
            if(result.equalsIgnoreCase("TRUE")){
                processor.setDeleteDuplicates(new AtomicBoolean(true));
            }
        }


        if (!processor.isInitSeqNo().get()) {
            /** Deactivating Resequencing processor to avoid executing multiple Job instances */
            processor.deactivate();

            do {
                delay(ResequencingProcessor.STARTING_NUMBER_INIT_DELAY);

                if (messageStore.size() >= processor.getRequiredInitMsgs().get()) {
                    selectStartingSeqNo(processor, messageStore, seqNoxPath);
                    break;
                }
                processor.tried++;
            } while (processor.tried < processor.getRequiredInitMsgsDelay().get());

        }

        if (!processor.isInitSeqNo().get()) {

            while (true) {

                if (messageStore.size() > 0) {
                    selectStartingSeqNo(processor, messageStore, seqNoxPath);

                    if (!processor.isInitSeqNo().get()) {
                        log.warn("Resequencer failed to select starting sequence number with in given timeout !");
                    }

                    break;
                }

            }

        }

        /** Continue to this section happens only after initializing the starting sequence number */
        boolean errorStop = false;
        while (!errorStop) {

            /** Iterate through message store */
            for (int messageIndex = 0; ; messageIndex++) {
                MessageContext messageContext = messageStore.get(messageIndex);

                if (messageContext == null) {
                    errorStop = true;
                    break;
                }

                /** Extract the sequence number from the message */
                int sequenceNo;
                try {
                    sequenceNo = Integer.parseInt(seqNoxPath.stringValueOf(messageContext));
                } catch (Exception e) {
                    log.warn("Can't Find sequence number from message " + e.getMessage());
                    continue;
                }

                String messageId = messageContext.getMessageID();

                /** Remove messages which have less sequence number than required */
                if(sequenceNo<processor.getNextSeqNo() && processor.getDeleteDuplicates()){
                    messageStore.remove(messageId);
                }

                /** Compare the next-to-go sequence number with current message sequence number */
                if (sequenceNo == processor.getNextSeqNo()) {

                    /** Remove selected message from store */

                    messageStore.remove(messageId);
                    /** If sending does not failed increase sequence number */
                    if (send(messageContext, sequence)) {

                        processor.incrementNextSeqNo();
                    }
                    /** Break and start searching from beginning */
                    break;
                }


            }

        }

        /** Reactivating Processor after selecting initial sequence number */
        if (!processor.isActive()) {
            processor.activate();
        }

    }

    /**
     * Selects the smallest sequence number as the starting sequence number from a given message store
     *
     * @param processor    - Resequencing processor which is interested to know starting sequence number
     * @param messageStore - Message store that contains messages
     * @param seqNoxPath   - SynapseXpath object which contains the xpath to find the sequence number from a message
     */
    private void selectStartingSeqNo(ResequencingProcessor processor, MessageStore messageStore,
                                     SynapseXPath seqNoxPath) {
        /** Iterate through message store */
        for (int messageIndex = 0; ; messageIndex++) {
            try {
                MessageContext messageContext = messageStore.get(messageIndex);
                if (messageContext == null) {
                    break;
                }
                /** Extract the sequence number from the message */
                int sequenceNo;

                sequenceNo = Integer.parseInt(seqNoxPath.stringValueOf(messageContext));


                /** If the sequence number is smaller that current next-sequence number, current next-sequence number get replaced */
                if (sequenceNo < processor.getNextSeqNo()) {
                    processor.setNextSeqNo(sequenceNo);
                    processor.setInitSeqNo(new AtomicBoolean(true));
                }


            } catch (NumberFormatException e) {
                handleException("Invalid xPath parameter - Sequence number specified is not an integer ");
            } catch (Exception e) {
                handleException("Failed to initialize starting sequence number at startup: " + e.getMessage());
            }
        }


    }

    /**
     * To timePeriod the processor until next checking up
     * This method is in use wen initializing the starting sequence number of the resequencer
     *
     * @param timePeriod - the time period which waits before a single cycle
     */
    private void delay(long timePeriod) {
        try {
            Thread.sleep(timePeriod);
        } catch (InterruptedException e) {
            log.error("Interrupted while thread sleeping in resequencer", e);
        }
    }

    /**
     * Transmit the message in to a given sequence
     * This method will takes the sequence given in sequence parameter. If no sequence is given this will return false     *
     *
     * @param messageContext - the content of the message that is transferred by Resequencer from message store
     * @param sequence       - the sequence name that the message should be passed
     * @return boolean         - to indicate the success of transferring the message
     */
    private boolean send(MessageContext messageContext, String sequence) {

        Mediator processingSequence = messageContext.getSequence(sequence);
        if (processingSequence != null) {
            processingSequence.mediate(messageContext);
            return true;
        }
        return false;
    }

    /**
     * Handling errors are done here.
     * This will log the error messages and throws SynapseException
     *
     * @param msg - Error message to be set
     * @throws SynapseException - Exception related to Synapse at Runtime
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
