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
import org.apache.synapse.message.processors.AbstractMessageProcessor;
import org.apache.synapse.message.processors.MessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.securevault.commons.MBeanRegistrar;
import org.quartz.JobDetail;

import java.util.Map;

/**
 * Redelivery processor is the Message processor which implements the Dead letter channel EIP
 * It will Time to time Redeliver the Messages to a given target.
 */
public class RedeliveryProcessor extends AbstractMessageProcessor{
      @Override
    protected JobDetail getJobDetail() {
        JobDetail jobDetail = new JobDetail();
        jobDetail.setName(messageStore.getName() + "- redelivery job");
        jobDetail.setJobClass(RedeliveryJob.class);
        return jobDetail;
    }
}
