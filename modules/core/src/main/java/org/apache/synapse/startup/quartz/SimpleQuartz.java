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

package org.apache.synapse.startup.quartz;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.startup.AbstractStartup;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.DirectSchedulerFactory;

/*
 * This class is instantiated by SimpleQuartzFactory (or by hand)
 * When it is initialized it creates a Quartz Scheduler with a job and a trigger
 * The class it starts is always an instance of SimpleQuartzJob
 * SimpleQuartzJob is there to set the properties and start the actual business-logic class
 * It wraps up any properties that the job needs as in the JobDetail and JDMap
 */
public class SimpleQuartz extends AbstractStartup {

    private static final Log log = LogFactory.getLog(SimpleQuartz.class);
    private static final int THREADPOOLSIZE = 5;

    private String cron;
    private int repeatCount = -1;
    private long repeatInterval;
    private String className;
    private Scheduler sch;
    Set xmlProperties = new HashSet();

    public QName getTagQName() {
        return SimpleQuartzFactory.TASK;
    }

    public void destroy() {
        if (sch != null) {
            try {
                sch.shutdown();
            } catch (SchedulerException e) {
                log.warn("Error shutting down scheduler", e);
                throw new SynapseException("Error shutting down scheduler", e);
            }
        }
    }

    public void init(SynapseEnvironment synapseEnvironment) {

        try {
            DirectSchedulerFactory.getInstance().createVolatileScheduler(THREADPOOLSIZE);
            sch = DirectSchedulerFactory.getInstance().getScheduler();

            Trigger trigger = null;
            if (cron == null) {
                if (repeatCount >= 0) {
                    trigger = TriggerUtils.makeImmediateTrigger(repeatCount - 1, repeatInterval);
                } else {
                    trigger = TriggerUtils.makeImmediateTrigger(-1, repeatInterval);
                }

            } else {
                CronTrigger cronTrig = new CronTrigger();
                cronTrig.setCronExpression(cron);
                trigger = cronTrig;
            }

            // give the trigger a random name
            trigger.setName("Trigger" + String.valueOf((new Random()).nextLong()));
            trigger.setGroup("synapse.simple.quartz");
            trigger.setVolatility(true);
            JobDetail jobDetail = new JobDetail();

            // Give the job a name
            jobDetail.setName(name);
            jobDetail.setGroup("synapse.simple.quartz");
            jobDetail.setJobClass(SimpleQuartzJob.class);
            JobDataMap jdm = new JobDataMap();
            jdm.put(SimpleQuartzJob.SYNAPSE_ENVIRONMENT, synapseEnvironment);
            jdm.put(SimpleQuartzJob.CLASSNAME, className);
            jdm.put(SimpleQuartzJob.PROPERTIES, xmlProperties);
            jobDetail.setJobDataMap(jdm);

            sch.scheduleJob(jobDetail, trigger);
            sch.start();
            log.info("Scheduled job " + jobDetail.getFullName() + " for class " + className);

        } catch (Exception e) {
            log.fatal("Error starting up Scheduler", e);
            throw new SynapseException("Error starting up Scheduler", e);
        }

    }

    public String getJobClass() {
        return className;
    }

    public void setJobClass(String attributeValue) {
        className = attributeValue;

    }

    public void setInterval(long l) {
        repeatInterval = l;

    }

    public long getInterval() {
        return repeatInterval;
    }

    public void setCount(int i) {
        repeatCount = i;
    }

    public int getCount() {
        return repeatCount;
    }

    public void addProperty(OMElement prop) {
        xmlProperties.add(prop);
    }

    public Set getProperties() {
        return xmlProperties;
    }

    public void setCron(String attributeValue) {
        cron = attributeValue;

    }

    public String getCron() {
        return cron;
    }

}
