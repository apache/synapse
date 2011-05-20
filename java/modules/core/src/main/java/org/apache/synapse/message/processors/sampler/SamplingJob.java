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
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.message.processors.MessageProcessorConsents;
import org.apache.synapse.message.processors.ScheduledMessageProcessor;
import org.apache.synapse.message.store.MessageStore;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;

public class SamplingJob implements Job {
    private static Log log = LogFactory.getLog(SamplingJob.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();

        final MessageStore messageStore = (MessageStore) jdm.get(
                MessageProcessorConsents.MESSAGE_STORE);

        Map<String, Object> parameters = (Map<String, Object>) jdm.get(
                MessageProcessorConsents.PARAMETERS);
        SamplingProcessor processor = (SamplingProcessor)
                jdm.get(ScheduledMessageProcessor.PROCESSOR_INSTANCE);

        final Object concurrency = jdm.get(SamplingProcessor.CONCURRENCY);
        final String sequence = (String) parameters.get(SamplingProcessor.SEQUENCE);

        // if processor is not active we do not proceed with the processing
        if(!processor.isActive()) {
            return;
        }

        int conc = 1;
        if (concurrency instanceof Integer) {
            conc = (Integer) concurrency;
        }

        for (int i = 0; i < conc; i++) {
            //lock.lock();
            synchronized (messageStore) {
                final MessageContext messageContext = messageStore.peek();

                if (messageContext != null) {
                    messageStore.poll();
                    final ExecutorService executor = messageContext.getEnvironment().
                            getExecutorService();
                    executor.submit(new Runnable() {
                        public void run() {
                            try {
                                Mediator processingSequence = messageContext.getSequence(sequence);
                                if (processingSequence != null) {
                                    processingSequence.mediate(messageContext);
                                }
                            } catch (Throwable t) {
                                log.error("Error occurred while executing the message", t);
                            }
                        }
                    });
                }
            }
        }
    }

}
