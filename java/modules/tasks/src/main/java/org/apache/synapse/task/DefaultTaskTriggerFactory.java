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
package org.apache.synapse.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Random;

/**
 * Default TriggerFactory ship with synapse utils
 */
public class DefaultTaskTriggerFactory implements TaskTriggerFactory {
    private static final Log log = LogFactory.getLog(DefaultTaskTriggerFactory.class);

    /**
     * @see TaskTriggerFactory
     */
    public Trigger createTrigger(TaskDescription taskDescription) {

        String name = taskDescription.getName();
        if (name == null || "".equals(name)) {
            throw new SynapseTaskException("Name of the Task cannot be null", log);
        }

        String cron = taskDescription.getCron();
        int repeatCount = taskDescription.getCount();
        long repeatInterval = taskDescription.getInterval();
        Date startTime = taskDescription.getStartTime();
        Date endTime = taskDescription.getEndTime();

        Trigger trigger;
        if (cron == null || "".equals(cron)) {
            if (repeatCount >= 0) {
                trigger = TriggerUtils.makeImmediateTrigger(repeatCount - 1, repeatInterval);
            } else {
                trigger = TriggerUtils.makeImmediateTrigger(SimpleTrigger.REPEAT_INDEFINITELY, repeatInterval);
            }

        } else {
            CronTrigger cronTrigger = new CronTrigger();
            try {
                cronTrigger.setCronExpression(cron);
                trigger = cronTrigger;
            } catch (ParseException e) {
                throw new SynapseTaskException("Error setting cron expression : " + e.getMessage() + cron, log);
            }
        }

        if (trigger == null) {
            throw new SynapseTaskException("Trigger is null for the Task description : " + taskDescription, log);
        }

        if (startTime != null) {
            trigger.setStartTime(startTime);
        }
        if (endTime != null) {
            trigger.setEndTime(endTime);
        }
        // give the trigger a random name
        trigger.setName(name + "-trigger-" + String.valueOf((new Random()).nextLong()));
        String group = taskDescription.getGroup();
        if (group != null && !"".equals(group)) {
            trigger.setGroup(group);
        } else {
            trigger.setGroup(TaskDescription.DEFAULT_GROUP);
        }
        trigger.setVolatility(taskDescription.isVolatility());
        return trigger;
    }
}
