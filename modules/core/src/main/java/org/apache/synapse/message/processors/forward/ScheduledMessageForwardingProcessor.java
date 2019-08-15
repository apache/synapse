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

import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2BlockingClient;
import org.apache.synapse.message.processors.ScheduledMessageProcessor;
import org.quartz.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redelivery processor is the Message processor which implements the Dead letter channel EIP
 * It will Time to time Redeliver the Messages to a given target.
 */
public class ScheduledMessageForwardingProcessor extends ScheduledMessageProcessor {

    public static final String BLOCKING_SENDER = "blocking.sender";

    private Axis2BlockingClient sender = null;

    private volatile AtomicBoolean active = new AtomicBoolean(true);

    private volatile AtomicInteger sendAttempts = new AtomicInteger(0);

    private MessageForwardingProcessorView view;

    @Override
    public void init(SynapseEnvironment se) {
        super.init(se);
        view = new MessageForwardingProcessorView(
                se.getSynapseConfiguration().getMessageStore(messageStore), getSender(), this);
        org.apache.synapse.commons.jmx.MBeanRegistrar.getInstance().registerMBean(view,
                "Message Forwarding Processor view", getName());
    }

    @Override
    protected JobBuilder getJobBuilder() {
        return JobBuilder.newJob(ForwardingJob.class).withIdentity(
                name + "-forward job", SCHEDULED_MESSAGE_PROCESSOR_GROUP);
    }

    @Override
    protected JobDataMap getJobDataMap() {
        JobDataMap jdm = new JobDataMap();
        jdm.put(BLOCKING_SENDER, getSender());
        jdm.put(PROCESSOR_INSTANCE,this);
        return jdm;
    }

    private synchronized Axis2BlockingClient getSender() {
        if (sender != null) {
            return sender;
        }
        String axis2repo = (String) parameters.get(ForwardingProcessorConstants.AXIS2_REPO);
        String axis2Config = (String) parameters.get(ForwardingProcessorConstants.AXIS2_CONFIG);
        return new Axis2BlockingClient(axis2repo, axis2Config);
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

    public int getSendAttemptCount() {
        return sendAttempts.get();
    }

    public void incrementSendAttemptCount() {
        sendAttempts.incrementAndGet();
    }
    public void resetSentAttemptCount(){
        sendAttempts.set(0);
    }

    @Override
    public void destroy() {
         try {
             scheduler.deleteJob(new JobKey(name + "-forward job",
                     ScheduledMessageProcessor.SCHEDULED_MESSAGE_PROCESSOR_GROUP));
             scheduler.shutdown(true);
        } catch (SchedulerException e) {
            log.error("Error while destroying the task " + e);
        }
        state = State.DESTROY;
    }


    /**
     * Return the JMS view of Message Processor
     *
     * @return MessageForwardingProcessorView
     */
    public MessageForwardingProcessorView getView() {
        return view;
    }
}
