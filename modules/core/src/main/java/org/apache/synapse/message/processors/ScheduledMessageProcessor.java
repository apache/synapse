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
package org.apache.synapse.message.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.ParseException;
import java.util.Map;

public abstract class ScheduledMessageProcessor extends AbstractMessageProcessor {


    public static final String SCHEDULED_MESSAGE_PROCESSOR_GROUP =
            "synapse.message.processor.quartz";
    public static final String PROCESSOR_INSTANCE = "processor.instance";

    /**
     * The scheduler, run the the processor
     */
    protected Scheduler scheduler = null;

    /**
     * The interval at which this processor runs , default value is 1000ms
     */
    protected long interval = 1000;


    protected enum State {
        INITIALIZED,
        START,
        STOP,
        DESTROY
    }

    /**
     * The quartz configuration file if specified as a parameter
     */
    protected String quartzConfig = null;

    /**
     * A cron expression to run the sampler
     */
    protected String cronExpression = null;

    /**
     * Keep the state of the message processor
     */
    protected State state = State.DESTROY;


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
        trigger.setName(name + "-trigger");

        JobDetail jobDetail = getJobDetail();
        JobDataMap jobDataMap = getJobDataMap();
        jobDataMap.put(MessageProcessorConsents.MESSAGE_STORE,
                configuration.getMessageStore(messageStore));
        jobDataMap.put(MessageProcessorConsents.PARAMETERS, parameters);
        jobDetail.setJobDataMap(jobDataMap);
        jobDetail.setGroup(SCHEDULED_MESSAGE_PROCESSOR_GROUP);

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new SynapseException("Error scheduling job : " + jobDetail
                    + " with trigger " + trigger ,e);
        }
    }

    public void stop() {
        if (state == State.START) {
            try {
                if (scheduler != null && scheduler.isStarted()) {
                    if (log.isDebugEnabled()) {
                        log.debug("ShuttingDown Message Processor Scheduler : " + scheduler.getMetaData());
                    }
                    scheduler.standby();
                }

                state = State.STOP;
            } catch (SchedulerException e) {
                throw new SynapseException("Error ShuttingDown Message processor scheduler ", e);
            }
        }
    }


    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);
        if (parameters != null && !parameters.isEmpty()) {
            Object o = parameters.get(MessageProcessorConsents.CRON_EXPRESSION);
            if (o != null) {
                cronExpression = o.toString();
            }

            o = parameters.get(MessageProcessorConsents.INTERVAL);
            if (o != null) {
                interval = Integer.parseInt(o.toString());
            }


            o = parameters.get(MessageProcessorConsents.QUARTZ_CONF);
            if (o != null) {
                quartzConfig = o.toString();
            }

        }
    }

    public void init(SynapseEnvironment se) {
        super.init(se);
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

            this.start();
        } catch (SchedulerException e) {
            throw new SynapseException("Error starting the scheduler", e);
        }
    }

    protected abstract JobDetail getJobDetail();

    protected JobDataMap getJobDataMap() {
        return new JobDataMap();
    }

    public void destroy() {
        try {
            scheduler.deleteJob(name + "-trigger",SCHEDULED_MESSAGE_PROCESSOR_GROUP);
        } catch (SchedulerException e) {
            log.error("Error while destroying the task " + e);
        }
        state = State.DESTROY;
    }

}
