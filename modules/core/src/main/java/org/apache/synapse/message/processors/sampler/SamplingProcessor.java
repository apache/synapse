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
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processors.MessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SamplingProcessor implements MessageProcessor {
    private Log log = LogFactory.getLog(SamplingProcessor.class);

    public static final String LOCK = "lock";
    public static final String EXECUTOR = "Executor";
    public static final String MESSAGE_STORE = "MESSAGE_STORE";
    public static final String QUARTZ_CONF = "quartz.conf";
    public static final String INTERVAL = "interval";
    public static final String CRON_EXPRESSION = "cronExpression";
    public static final String CONCURRENCY = "concurrency";
    public static final String SEQUENCE = "sequence";

    private enum State {
        INITIALIZED,
        START,
        STOP,
        DESTROY
    }

    private Map<String, Object> parameters = null;

    /** The quartz configuration file if specified as a parameter */
    private String quartzConfig = null;

    /** A cron expression to run the sampler */
    private String cronExpression = null;

    /** The interval at which this sampler runs */
    private long interval = 1;

    /** The scheduler, run the the sampler */
    private Scheduler scheduler = null;

    /** Weather sampler is initialized or not */
    private State state = State.DESTROY;

    /** The message store */
    private MessageStore messageStore = null;

    /** Concurrency at the sampler runs, if the concurrency is 2, 2 threads will
     * be used to dispatch 2 messages, when sampler runs */
    private int concurrency = 1;

    /** An executor */
    private ExecutorService executor = null;

    /** A sequence to run when the sampler is executed */
    private String sequence = null;

    private Lock lock = new ReentrantLock();

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
        jobDataMap.put(LOCK, lock);

        jobDetail.setJobDataMap(jobDataMap);

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new SynapseException("Error scheduling job : " + jobDetail
                    + " with trigger " + trigger);
        }
    }

    public void stop() {
        if (state == State.START) {
            try {
                if (scheduler != null && scheduler.isStarted()) {
                    if (log.isDebugEnabled()) {
                        log.debug("ShuttingDown Sampling Scheduler : " + scheduler.getMetaData());
                    }
                    scheduler.standby();
                }

                state = State.STOP;
            } catch (SchedulerException e) {
                throw new SynapseException("Error ShuttingDown Sampling scheduler ", e);
            }
        }
    }

    public void setMessageStore(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;

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
            quartzConfig = o.toString();
        }

        o = parameters.get(SEQUENCE);
        if (o != null) {
            sequence = o.toString();
        }
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public boolean isStarted() {
        return state == State.START;
    }

    public void init(SynapseEnvironment se) {
        executor = se.getExecutorService();

        StdSchedulerFactory sf = new StdSchedulerFactory();
        try {
            if (quartzConfig != null && !"".equals(quartzConfig)) {
                if (log.isDebugEnabled()) {
                    log.debug("Initiating a Scheduler with configuration : " + quartzConfig);
                }

                sf.initialize(quartzConfig);
            }
        } catch (SchedulerException e) {
            throw new SynapseException("Error initiating scheduler factory "
                    + sf + "with configuration loaded from " + quartzConfig, e);
        }

        try {
            scheduler = sf.getScheduler();
        } catch (SchedulerException e) {
            throw new SynapseException("Error getting a  scheduler instance form scheduler" +
                    " factory " + sf, e);
        }

        try {
            scheduler.start();

            state = State.INITIALIZED;
        } catch (SchedulerException e) {
            throw new SynapseException("Error starting the scheduler", e);
        }
    }

    public void destroy() {
        state = State.DESTROY;
    }
}
