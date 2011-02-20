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

import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processors.AbstractMessageProcessor;
import org.apache.synapse.message.processors.ScheduledMessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.quartz.JobDetail;

/**
 * Redelivery processor is the Message processor which implements the Dead letter channel EIP
 * It will Time to time Redeliver the Messages to a given target.
 */
public class ScheduledRedeliveryProcessor extends ScheduledMessageProcessor{

    /**Dead Letter channel JMX API*/
    private DeadLetterChannelView dlcView;

    @Override
    public void init(SynapseEnvironment se) {
        super.init(se);
        dlcView = new DeadLetterChannelView(configuration.getMessageStore(messageStore));
        org.apache.synapse.commons.jmx.MBeanRegistrar.getInstance().registerMBean(dlcView,
                "Dead Letter Channel", messageStore);
    }

    @Override
    protected JobDetail getJobDetail() {
        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(messageStore + "- redelivery job");
        jobDetail.setJobClass(RedeliveryJob.class);
        return jobDetail;
    }

    public DeadLetterChannelView getDlcView() {
        return dlcView;
    }
}
