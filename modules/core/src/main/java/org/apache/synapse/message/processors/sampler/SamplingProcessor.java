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
package org.apache.synapse.message.processors.sampler;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processors.MessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class SamplingProcessor implements MessageProcessor, ManagedLifecycle{
    public static final String EXECUTOR = "Executor";
    public static final String MESSAGE_STORE = "MESSAGE_STORE";
    private Log log = LogFactory.getLog(SamplingProcessor.class);

    private final String QUARTZ_CONF = "quartz.conf";

    public static final String INTERVAL = "interval";

    public static final String CRON_EXPRESSION = "cronExpression";

    public static final String CONCURRENCY = "concurrency";

    public static final String SEQUENCE = "sequence";

    private String cronExpression = null;

    private long interval = 1;

    private String quartzConf = null;

    private Scheduler scheduler = null;

    private boolean initialized = false;

    private MessageStore messageStore = null;

    private Mediator onProcessSequence = null;

    private Mediator onSubmitSequence = null;

    private int concurrency = 1;

    private ExecutorService executor = null;

    private String sequence = null;
    /**
     * Creates a Quartz Scheduler and schedule the message processing logic.
     */
    public void start() {
        Trigger trigger;
        if (cronExpression == null || "".equals(cronExpression)) {
            trigger = TriggerUtils.makeImmediateTrigger(SimpleTrigger.REPEAT_INDEFINITELY, interval);
        } else {
            CronTrigger cronTrigger = new CronTrigger();
            try {
                cronTrigger.setCronExpression(cronExpression);
                trigger = cronTrigger;
            } catch (ParseException e) {
                throw new SynapseException("Error setting cron expression : " +
                        e.getMessage() + cronExpression, e);
            }
        }
        trigger.setName(messageStore.getName() + "-trigger");

        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(messageStore.getName() + "-job");
        jobDetail.setJobClass(SamplingJob.class);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CONCURRENCY, concurrency);
        jobDataMap.put(EXECUTOR, executor);
        jobDataMap.put(MESSAGE_STORE, messageStore);
        jobDataMap.put(SEQUENCE, sequence);

        jobDetail.setJobDataMap(jobDataMap);

        StdSchedulerFactory sf = new StdSchedulerFactory();
        try {
            if (quartzConf != null && !"".equals(quartzConf)) {
                if (log.isDebugEnabled()) {
                    log.debug("Initiating a Scheduler with configuration : " + quartzConf);
                }

                sf.initialize(quartzConf);
            }
        } catch (SchedulerException e) {
            throw new SynapseException("Error initiating scheduler factory "
                    + sf + "with configuration loaded from " + quartzConf, e);
        }


        try {
            scheduler = sf.getScheduler();
        } catch (SchedulerException e) {
            throw new SynapseException("Error getting a  scheduler instance form scheduler" +
                    " factory " + sf, e);
        }

        try {
            scheduler.start();

            scheduler.scheduleJob(jobDetail, trigger);

            initialized = true;
        } catch (SchedulerException e) {
            throw new SynapseException("Error scheduling job : " + jobDetail
                    + " with trigger " + trigger);
        }
    }

    public void stop() {
        if (initialized) {
            try {
                if (scheduler != null && scheduler.isStarted()) {
                    if (log.isDebugEnabled()) {
                        log.debug("ShuttingDown Sampling Scheduler : " + scheduler.getMetaData());
                    }
                    scheduler.shutdown();
                }
                initialized = false;
            } catch (SchedulerException e) {
                throw new SynapseException("Error ShuttingDown Sampling scheduler ", e);
            }
        }
    }

    public void setMessageStore(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    public MessageStore getMessageStore() {
        return messageStore;
    }

    public void setOnProcessSequence(Mediator mediator) {
        this.onProcessSequence = mediator;
    }

    public Mediator getOnProcessSequence() {
        return onProcessSequence;
    }

    public void setOnSubmitSequence(Mediator mediator) {
        this.onSubmitSequence = mediator;
    }

    public Mediator getOnSubmitSequence() {
        return onSubmitSequence;
    }

    public void setParameters(Map<String, Object> parameters) {
        Object o = parameters.get(CRON_EXPRESSION);
        if (o != null) {
            cronExpression = o.toString();
        }

        o = parameters.get(INTERVAL);
        if (o != null) {
            interval = Integer.parseInt(o.toString());
        }

        o = parameters.get(CONCURRENCY);
        if (o != null) {
            concurrency = Integer.parseInt(o.toString());
        }

        o = parameters.get(QUARTZ_CONF);
        if (o != null) {
            quartzConf = o.toString();
        }

        o = parameters.get(SEQUENCE);
        if (o != null) {
            sequence = o.toString();
        }
    }

    public Map<String, Object> getParameters() {
        return null;
    }

    public boolean isStarted() {
        return initialized;
    }

    public void init(SynapseEnvironment se) {
        executor = se.getExecutorService();
    }

    public void destroy() {

    }
}
