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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;

/**
 * Redelivery processor does the redelivery of the messages scheduled in the Message store
 * It will poll the redelivery queue of the Message store and deliver messages according to
 * a pre configured policy
 */
public class RedeliveryProcessor {

    private static final Log log = LogFactory.getLog(RedeliveryProcessor.class);

    /**hold reference to the associated message store */
    private MessageStore messageStore;

    /**Maximum number of redelivery's per message */
    private int maxRedeleveries = 1;

    /** Delay between two consecutive redelivery attempts */
    private int redeliveryDelay = 2000;

    /** enable/disable exponential backoff*/
    private boolean exponentialBackoff = false;

    /**the multipler that will be used in the exponential backoff algorithm */
    private int backOffMultiplier = -1;

    /** is Redelivery processr started*/
    private transient boolean start;


    public RedeliveryProcessor(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    /**
     * get the associated Message store
     * @return  message store
     */
    public MessageStore getMessageStore() {
        return messageStore;
    }

    /**
     * start the redelivery processor
     */
    public void start() {
        try {
            Thread.sleep(redeliveryDelay);
        } catch (InterruptedException e) {
            log.error(e);
        }

        Thread t = new Thread(new Worker());

        start = true;
        t.start();
        if (log.isDebugEnabled()) {
            log.debug("Redelivery Started");
        }
    }

    /**
     * stop the redelivery processor
     */
    public void stop() {
        start = false;
        if (log.isDebugEnabled()) {
            log.debug("Redelivery Stopped");
        }
    }

    /**
     * set the redelivery delay
     *
     * @param redeliveryDelay redelivery delay in milliseconds
     */
    public void setRedeliveryDelay(int redeliveryDelay) {
        this.redeliveryDelay = redeliveryDelay;
    }

    /**
     * set the Maximum number of redelivery's per message
     *
     * @param maxRedeleveries maximum number of redeliveries
     */
    public void setMaxRedeleveries(int maxRedeleveries) {
        this.maxRedeleveries = maxRedeleveries;
    }

    /**
     *
     * @return  is redelivery processor started
     */
    public boolean isStarted() {
        return start;
    }

    /**
     *
     * @return is exponental back off enable
     */
    public boolean isExponentialBackoffEnable() {
        return exponentialBackoff;
    }

    /**
     *
     * @param exponentialBackoff  true/false
     */
    public void setExponentialBackoff(boolean exponentialBackoff) {
        this.exponentialBackoff = exponentialBackoff;
    }

    /**
     *
     * @return the multipler that will be used in the exponential backoff algorithm
     */
    public int getBackOffMultiplier() {
        return backOffMultiplier;
    }

    /**
     * set Back of mutipller value to be used in the backoff algorithm
     *
     * @param backOffMultiplier factor for exponential backoff
     */
    public void setBackOffMultiplier(int backOffMultiplier) {
        this.backOffMultiplier = backOffMultiplier;
    }

    /**
     *
     * @return max number of redelivery's per message
     */
    public int getMaxRedeleveries() {
        return maxRedeleveries;
    }

    /**
     *
     * @return redelivery delay
     */
    public int getRedeliveryDelay() {
        return redeliveryDelay;
    }


    private class Worker implements Runnable {

        public void run() {
            while (start) {
                synchronized (this) {

                    int delay = redeliveryDelay;
                    StorableMessage storableMessage;

                    storableMessage = messageStore.getFirstSheduledMessage();

                    if (storableMessage == null || storableMessage.getEndpoint() == null ||
                            !storableMessage.getEndpoint().readyToSend()) {
                        continue;
                    }

                    messageStore.dequeueScheduledQueue();
                    MessageContext synCtx = storableMessage.getMessageContext();

                    if (synCtx.getProperty(
                            SynapseConstants.MESSAGE_STORE_REDELIVERY_COUNT) == null) {
                        synCtx.setProperty(SynapseConstants.MESSAGE_STORE_REDELIVERY_COUNT, "1");
                        delay = redeliveryDelay;

                    } else {
                        String numberS = (String) synCtx.getProperty(
                                SynapseConstants.MESSAGE_STORE_REDELIVERY_COUNT);
                        int number = Integer.parseInt(numberS);

                        if (number >= maxRedeleveries) {
                            if (log.isDebugEnabled()) {
                                log.debug("Maximum number of redelivery attempts has exceeded " +
                                        "for the message: " + synCtx.getMessageID() + " - " +
                                        "Message will be put back to the message store.");

                            }
                            messageStore.store(storableMessage);
                            continue;
                        }

                        synCtx.setProperty(SynapseConstants.MESSAGE_STORE_REDELIVERY_COUNT, "" +
                                (number + 1));
                        if (maxRedeleveries <= (number+1)) {
                            synCtx.setProperty(SynapseConstants.MESSAGE_STORE_REDELIVERED, "true");
                        }

                        if (exponentialBackoff && backOffMultiplier == -1) {
                            delay = (number + 1) * redeliveryDelay;

                        } else if (exponentialBackoff) {
                            delay = (int) Math.pow(backOffMultiplier, number) * redeliveryDelay;

                        }
                    }

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {

                    }

                    synCtx.pushFaultHandler((FaultHandler) storableMessage.getEndpoint());
                    storableMessage.getEndpoint().send(storableMessage.getMessageContext());

                    if (log.isDebugEnabled()) {
                        log.debug("Sent: " + storableMessage.getMessageContext().getEnvelope());
                    }
                }
            }
        }
    }
}
