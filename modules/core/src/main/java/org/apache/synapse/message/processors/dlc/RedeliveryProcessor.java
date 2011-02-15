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
import org.apache.synapse.*;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.message.processors.MessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.securevault.commons.MBeanRegistrar;

import java.util.Map;

/**
 * Redelivery processor is the Message processor which implements the Dead letter channel EIP
 * It will Time to time Redeliver the Messages to a given target.
 */
public class RedeliveryProcessor implements MessageProcessor {

    private static final Log log = LogFactory.getLog(RedeliveryProcessor.class);

    /**
     * Associated MessageStore
     */
    private MessageStore messageStore;

    private Map<String, Object> parameters;

    /**
     * Maximum number of redelivery's per message
     */
    private int maxRedeleveries = 0;

    /**
     * Delay between two consecutive redelivery attempts
     */
    private int redeliveryDelay = 2000;

    /**
     * enable/disable exponential backoff
     */
    private boolean exponentialBackoff = false;

    /**
     * the multiplier that will be used in the exponential backoff algorithm
     */
    private int backOffMultiplier = -1;


    private DeadLetterChannelViewMBean dlcView;

    private boolean started;

    public static final String REDELIVERY_DELAY = "redelivery.delay";

    public static final String MAX_REDELIVERY_COUNT = "redelivery.count";

    public static final String EXPONENTIAL_BACKOFF = "redelivery.exponentialBackoff";

    public static final String BACKOFF_MUTIPLIER = "redelivery.backoffMultiplier";


    public static final String REPLAY_ENDPOINT = "replay.endpoint";

    public static final String REPLAY_SEQUENCE = "replay.sequence";

    public static final String NO_OF_REDELIVERIES = "number.of.redeliveries";

    public void start() {
        if (!started) {
            Thread t = new Thread(new Worker());
            t.start();
        }
    }

    public void stop() {
        started = false;
    }

    public void setMessageStore(MessageStore messageStore) {
        this.messageStore = messageStore;
        if(messageStore !=null) {
            DeadLetterChannelView view = new DeadLetterChannelView(messageStore);
            this.dlcView = view;
            MBeanRegistrar.getInstance().registerMBean(view,"Dead Letter Channel",
                    messageStore.getName());
        }
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        if (parameters.containsKey(REDELIVERY_DELAY)) {
            redeliveryDelay = Integer.parseInt((String) parameters.get(REDELIVERY_DELAY));
        }

        if (parameters.containsKey(MAX_REDELIVERY_COUNT)) {
            maxRedeleveries = Integer.parseInt((String) parameters.get(MAX_REDELIVERY_COUNT));
        }

        if (parameters.containsKey(EXPONENTIAL_BACKOFF)) {
            if ("true".equals(parameters.get(EXPONENTIAL_BACKOFF))) {
                exponentialBackoff = true;
            }
        }

        if (parameters.containsKey(BACKOFF_MUTIPLIER)) {
            backOffMultiplier = Integer.parseInt((String) parameters.get(BACKOFF_MUTIPLIER));
        }
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void init(SynapseEnvironment se) {

    }

    public void destroy() {

    }


    private class Worker implements Runnable {

        public void run() {
            while (started) {
                try {
                    synchronized (this) {
                        int delay = redeliveryDelay;
                        MessageContext messageContext;
                        messageContext = messageStore.getMessages(0, 0).get(0);

                        if (messageContext == null) {
                            continue;
                        }

                        SynapseArtifact artifact = getReplayTarget(messageContext);
                        messageStore.unstore(0, 0);
                        if (messageContext.getProperty(NO_OF_REDELIVERIES) == null) {
                            messageContext.setProperty(NO_OF_REDELIVERIES, "0");
                            delay = redeliveryDelay;
                        }

                        String numberS = (String) messageContext.getProperty(NO_OF_REDELIVERIES);
                        int number = Integer.parseInt(numberS);

                        if (number >= maxRedeleveries) {

                            if (log.isDebugEnabled()) {
                                log.debug("Maximum number of attempts tried for Message with ID " +
                                        messageContext.getMessageID() +
                                        "will be put back to the Message Store");

                            }
                            messageStore.store(messageContext);
                            continue;
                        }

                        messageContext.setProperty(NO_OF_REDELIVERIES, "" + (number + 1));

                        if (exponentialBackoff && backOffMultiplier == -1) {
                            delay = (number + 1) * redeliveryDelay;
                        } else if (exponentialBackoff) {
                            delay = (int) Math.pow(backOffMultiplier, number) * redeliveryDelay;
                        }


                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ignored) {

                        }

                        if (artifact instanceof Endpoint) {
                            if (!handleEndpointReplay((Endpoint) artifact, messageContext)) {
                                messageStore.store(messageContext);
                            }
                        } else if (artifact instanceof Mediator) {
                            if (!handleSequenceReplay((Mediator) artifact, messageContext)) {
                                messageStore.store(messageContext);
                            }
                        } else {
                            messageStore.store(messageContext);
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("sent \n" + messageContext.getEnvelope());
                        }
                    }
                } catch (Throwable e) {
                    log.warn("Error while Running Redelivery process " + e.getMessage());
                }
            }

        }
    }

    public static SynapseArtifact getReplayTarget(MessageContext context) {
        //Endpoint replay get priority
        if (context.getProperty(REPLAY_ENDPOINT) != null) {
            String endpointName = (String) context.getProperty(REPLAY_ENDPOINT);
            return context.getConfiguration().getDefinedEndpoints().get(endpointName);
        } else if (context.getProperty(REPLAY_SEQUENCE) != null) {
            String sequenceName = (String) context.getProperty(REPLAY_SEQUENCE);

            return context.getConfiguration().getSequence(sequenceName);
        }

        return null;
    }


    public static boolean handleEndpointReplay(Endpoint endpoint, MessageContext messageContext) {
        if (endpoint.readyToSend()) {
            endpoint.send(messageContext);
            return true;
        }

        return false;
    }


    public static boolean handleSequenceReplay(Mediator mediator, MessageContext messageContext) {
        mediator.mediate(messageContext);
        return true;
    }

    /**
     * Get the DLC related JMX API
     * @return   instance of Dead letter channel jms api
     */
    public DeadLetterChannelViewMBean getDlcView() {
        return dlcView;
    }

    /**
     * Get the started status of the Message processor
     * @return started status of message processor (true/false)
     */
    public boolean isStarted() {
        return started;
    }
}
