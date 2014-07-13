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
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processors.ScheduledMessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.quartz.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class will be used as the processor of the resequencer and set up
 * the necessary environment for the ResequencingJob.
 * This should be run periodically after given time interval and
 * for that this should be inherited from ScheduledMessageProcessor class
 */
public class ResequencingProcessor extends ScheduledMessageProcessor {

    /**
     * Log is set to the current class
     */
    private static final Log log = LogFactory.getLog(ResequencingProcessor.class);

    /**
     * State of the processor
     */
    private AtomicBoolean active = new AtomicBoolean(true);

    /**
     * To indicate whether the starting sequence number is set while initializing the processor
     */
    private AtomicBoolean initSeqNo = new AtomicBoolean(false);

    /**
     * Sequence number of the message that should be send next
     */
    private AtomicInteger nextSeqNo = new AtomicInteger(Integer.MAX_VALUE);

    /**
     * Number of messages interested to come
     * Number of messages that the Resequencing processor should wait for before selecting the starting sequence number.
     * Default value is 4
     */
    private AtomicInteger requiredInitMsgs = new AtomicInteger(4);

    /**
     * Time to wait for interested number of messages to come
     */
    private AtomicInteger requiredInitMsgsDelay = new AtomicInteger(5);

    /**
     * xpath expression to extract the sequence number
     */
    public static final String SEQUENCE_NUMBER_XPATH = "seqNumXpath";

    /**
     * Sequence that the messages should be passed to
     */
    public static final String NEXT_SEQUENCE = "nextEsbSequence";

    /**
     * Required initial number of messages
     */
    public static final String REQ_INIT_MSGS = "requiredInitMessages";

    /**
     * Delay until getting required number of messages receive
     */
    public static final String REQ_INIT_MSGS_DELAY = "requiredInitMessagesDelay";

    /**
     * Delay time period required for Resequencer processor while initializing starting sequence number
     */
    public static final int STARTING_NUMBER_INIT_DELAY = 6000;

    /**
     * Number of times currently processor waited until required number of messages come
     * Max value is determined by   requiredInitMsgs variable value
     */
    public int tried = 0;

    public static final String DELETE_DUPLICATES="deleteDuplicateMessages";

    private AtomicBoolean deleteDuplicates=new AtomicBoolean(false);


    /**
     * Initiate the processor with SynapseEnvironment
     *
     * @param se - SynapseEnvironment to be set
     */
    @Override
    public void init(SynapseEnvironment se) {
        super.init(se);

        /** Set the initial sequence number */
        findFirstSeqNum();

    }


    /**
     * This method use to find the minimum sequence number in the message store at the startup
     */
    private void findFirstSeqNum() {
        MessageStore store = configuration.getMessageStore(messageStore);
        SynapseXPath seqNoxPath = null;

        /** Extract the SynapseXpath configuration.getMessageStore(messageStore)object from parameters to
         * identify the sequence number of the message */
        if (parameters != null && parameters.get(ResequencingProcessor.SEQUENCE_NUMBER_XPATH) != null) {
            seqNoxPath = (SynapseXPath) parameters.get(ResequencingProcessor.SEQUENCE_NUMBER_XPATH);
        }


        /** Iterate through message store */
        for (int messageIndex = 0; ; messageIndex++) {

            try {
                MessageContext messageContext = store.get(messageIndex);
                if (messageContext == null) {
                    break;
                }

                /** Extract the sequence number from the message */
                int sequenceNo = Integer.parseInt(seqNoxPath.stringValueOf(messageContext));

                /** If the sequence number is smaller that current next-sequence number, current next-sequence
                 * number get replaced */
                if (sequenceNo < getNextSeqNo()) {
                    setNextSeqNo(sequenceNo);
                    /** To indicate that starting sequence number is initialized */
                    initSeqNo = new AtomicBoolean(true);
                }

            } catch (NumberFormatException e) {
                handleException("Invalid xPath parameter - Sequence number specified is not an integer ");
            } catch (Exception e) {
                handleException("Failed to initialize starting sequence number at startup: " + e.getMessage());
            }
        }

    }

    /**
     * Get the job details with Name and Job Class
     *
     * @return jobDetail - created JobDetail object with Name and JobClass
     */
    @Override
    protected JobBuilder getJobBuilder() {
        return JobBuilder.newJob(ResequencingJob.class).withIdentity(
                name + "-resequencing-job", SCHEDULED_MESSAGE_PROCESSOR_GROUP);
    }

    /**
     * Get the map that contains parameters related  to Resequencing job
     *
     * @return jobDataMap - created Job Data Map along with the processor instance
     */
    @Override
    protected JobDataMap getJobDataMap() {
        JobDataMap jdm = new JobDataMap();
        jdm.put(PROCESSOR_INSTANCE, this);
        return jdm;
    }

    /**
     * Destroy the processor's resequencing job
     */
    @Override
    public void destroy() {
        try {
            scheduler.deleteJob(new JobKey(name + "-resequencing-job",
                    ScheduledMessageProcessor.SCHEDULED_MESSAGE_PROCESSOR_GROUP));
            scheduler.shutdown();
        } catch (SchedulerException e) {
        }
    }

    /**
     * Activate the Resequencing processor
     * Set the active value to true
     */
    public void activate() {
        active.set(true);
    }

    /**
     * Check if the processor is active or not
     *
     * @return active - boolean expression that tells the status of the processor
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * De-activate the resequencing processor
     * Set the active value to false
     */
    public void deactivate() {
        active.set(false);
    }

    /**
     * Returns the next sequence number
     *
     * @return nextSeqNo - The sequence number of the message that to be send next
     */
    public synchronized int getNextSeqNo() {
        return nextSeqNo.get();
    }

    /**
     * This method allow to change the value of nextSeqNo variable, which is used to determine
     * the sequence number of the message next to go
     *
     * @param value - The value to set
     */
    public synchronized void setNextSeqNo(int value) {
        nextSeqNo.set(value);
    }

    /**
     * Increase the sequence number by one
     */
    public synchronized void incrementNextSeqNo() {
        nextSeqNo.incrementAndGet();
    }

    /**
     * Indicate whether the initial sequencer number is set
     *
     * @return initSeqNo - boolean value containing true or false
     */
    public AtomicBoolean isInitSeqNo() {
        return initSeqNo;
    }

    /**
     * Set or clear the initSeqNo value
     *
     * @param initSeqNo - boolean value to set
     */
    public void setInitSeqNo(AtomicBoolean initSeqNo) {
        this.initSeqNo = initSeqNo;
    }

    /**
     * Get the number initial messages required before set starting sequence number
     *
     * @return requiredInitMsgs - int value of required messages
     */
    public AtomicInteger getRequiredInitMsgs() {
        return requiredInitMsgs;
    }

    /**
     * Set the number of messages required before set starting sequence number
     *
     * @param requiredInitMsgs - number of messages need to wait
     */
    public void setRequiredInitMsgs(AtomicInteger requiredInitMsgs) {
        this.requiredInitMsgs = requiredInitMsgs;
    }

    /**
     * Delay until requiredInitMsgs get set
     *
     * @return requiredInitMessagesDelay delay value
     */
    public AtomicInteger getRequiredInitMsgsDelay() {
        return requiredInitMsgsDelay;
    }

    /**
     * Set the delay until requiredInitMsgs get set
     *
     * @param requiredInitMsgsDelay - value for delay
     */
    public void setRequiredInitMsgsDelay(AtomicInteger requiredInitMsgsDelay) {
        this.requiredInitMsgsDelay = requiredInitMsgsDelay;
    }

    /**
     * Check whether to delete duplicate messages or not
     * @return   value of deleteDuplicates
     */
    public boolean getDeleteDuplicates() {
        return deleteDuplicates.get();
    }

    /**
     * Set to delete duplicate messages
     * @param deleteDuplicates
     */
    public void setDeleteDuplicates(AtomicBoolean deleteDuplicates) {
        this.deleteDuplicates = deleteDuplicates;
    }


    /**
     * Handling errors are done here.
     * This will log the error messages and throws SynapseException
     *
     * @param msg - Error message to be set
     * @throws org.apache.synapse.SynapseException
     *          - Exception related to Synapse at Runtime
     */
    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}