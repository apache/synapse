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
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processors.ScheduledMessageProcessor;
import org.quartz.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class SamplingProcessor extends ScheduledMessageProcessor {

    private static final Log log = LogFactory.getLog(SamplingProcessor.class);

    public static final String CONCURRENCY = "concurrency";
    public static final String SEQUENCE = "sequence";

    private AtomicBoolean active = new AtomicBoolean(true);

    private SamplingProcessorView view;

    @Override
    public void init(SynapseEnvironment se) {
        super.init(se);
        view = new SamplingProcessorView(this);

        // register MBean
        org.apache.synapse.commons.jmx.MBeanRegistrar.getInstance().registerMBean(view,
                "Message Sampling Processor view", getName());
    }

    @Override
    protected JobBuilder getJobBuilder() {
        return JobBuilder.newJob(SamplingJob.class).withIdentity(
                name + "-sampling-job", SCHEDULED_MESSAGE_PROCESSOR_GROUP);
    }

    @Override
    protected JobDataMap getJobDataMap() {
        JobDataMap jdm = new JobDataMap();
        jdm.put(PROCESSOR_INSTANCE,this);
        return jdm;

    }

    @Override
    public void destroy() {
         try {
             scheduler.deleteJob(new JobKey(name + "-sampling-job",
                     ScheduledMessageProcessor.SCHEDULED_MESSAGE_PROCESSOR_GROUP));
        } catch (SchedulerException e) {
            log.error("Error while destroying the task " + e);
        }
        state = State.DESTROY;
    }

    public boolean isActive() {
        return active.get();
    }

    public void activate() {
        active.set(true);
    }

    public void deactivate() {
        active.set(false);
    }

    public SamplingProcessorView getView() {
        return view;
    }
}
