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
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Map;
import java.util.Properties;

/**
 * Abstraction for scheduling a Task
 */
public class TaskScheduler {

    private static Log log = LogFactory.getLog(TaskScheduler.class);

    /**
     * scheduler instance
     */
    private Scheduler scheduler;
    /* determine whether scheduler has been initialized or not - Ready to schedule a Task or not */
    private boolean initialized = false;

    /**
     * Default trigger factory
     */
    private TaskTriggerFactory triggerFactory = new DefaultTaskTriggerFactory();
    /**
     * Default job detail factory
     */
    private TaskJobDetailFactory jobDetailFactory = new DefaultTaskJobDetailFactory();

    /**
     * Property look up key for get a quartz configuration
     */
    public static String QUARTZ_CONF = "quartz.conf";

    private String name;

    public TaskScheduler(String name) {
        this.name = name;
    }

    /**
     * Initialize the scheduler based on provided properties
     * Looking for  'quartz.conf' and if found , use it for initiating quartz scheduler
     *
     * @param properties Properties
     */
    public void init(Properties properties) {

        StdSchedulerFactory sf = new StdSchedulerFactory();

        if (properties != null) {
            String quartzConf = properties.getProperty(QUARTZ_CONF);
            try {
                if (quartzConf != null && !"".equals(quartzConf)) {
                    sf.initialize(quartzConf);
                }
            } catch (SchedulerException e) {
                throw new SynapseTaskException("Error initiating scheduler factory "
                        + sf + "with configuration loaded from " + quartzConf, e, log);
            }
        }

        try {
            if (name != null) {
                scheduler = sf.getScheduler(name);
            }
            if (scheduler == null) {
                scheduler = sf.getScheduler();
            }
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error getting a  scheduler instance form scheduler factory " + sf, e, log);
        }
        initialized = true;
        start();
    }

    /**
     * Explicitly start up call for scheduler, return if already it has been started
     */
    public void start() {

        validateInit();
        try {
            if (!scheduler.isStarted()) {
                scheduler.start();
            }
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error starting scheduler ", e, log);
        }
    }

    /**
     * Schedule a Task
     *
     * @param taskDescription TaskDescription , an information about Task
     * @param resources       Any initial resources for task
     * @param jobClass        Quartz job class
     */
    public void scheduleTask(TaskDescription taskDescription, Map resources, Class jobClass) {

        validateInit();
        validateStart();

        if (taskDescription == null) {
            throw new SynapseTaskException("Task Description can not be found", log);
        }

        if (jobClass == null) {
            throw new SynapseTaskException("Job Class can not be found", log);
        }

        if (!Job.class.isAssignableFrom(jobClass)) {
            throw new SynapseTaskException("Invalid Job Class : [ Expected " + Job.class.getName() + "]" +
                    " [ Found " + jobClass.getName() + " ]", log);
        }

        if (triggerFactory == null) {
            throw new SynapseTaskException("TriggerFactory can not be found", log);
        }

        if (jobDetailFactory == null) {
            throw new SynapseTaskException("JobDetailFactory can not be found", log);
        }

        Trigger trigger = triggerFactory.createTrigger(taskDescription);
        if (trigger == null) {
            throw new SynapseTaskException("Trigger can not be created from : " + taskDescription, log);
        }

        JobDetail jobDetail = jobDetailFactory.createJobDetail(taskDescription, resources, jobClass);
        if (jobDetail == null) {
            throw new SynapseTaskException("JobDetailcan not be created from : " + taskDescription +
                    " and job class " + jobClass.getName(), log);
        }

        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error scheduling job : " + jobDetail + " with trigger " + trigger);
        }

    }

    /**
     * ShutDown the underlying quartz scheduler
     */
    public void shutDown() {

        validateInit();
        validateStart();
        try {
            scheduler.shutdown();
            initialized = false;
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error shutingDown scheduler ", e, log);
        }
    }

    /**
     * @return Returns true if the scheduler is ready for schedule a task
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Deletes a Task
     *
     * @param name  Name of the Task
     * @param group Group name of the task
     *              Default value @see org.apache.synapse.util.task.TaskDescription.DEFAULT_GROUP
     */
    public void deleteTask(String name, String group) {

        validateInit();
        validateStart();

        if (name == null || "".equals(name)) {
            throw new SynapseTaskException("Task Name can not be null", log);
        }

        if (group == null || "".equals(group)) {
            group = TaskDescription.DEFAULT_GROUP;
            if (log.isDebugEnabled()) {
                log.debug("Task group is null or empty , using default group :" + TaskDescription.DEFAULT_GROUP);
            }
        }

        try {
            scheduler.deleteJob(name, group);
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error deleting a job with  [ Name :" + name + " ]" +
                    " [ Group :" + group + " ]");
        }
    }

    /**
     * Sets a Trigger Factory , if it needs to void using default factory
     *
     * @param triggerFactory TaskTriggerFactory instance
     */
    public void setTriggerFactory(TaskTriggerFactory triggerFactory) {
        this.triggerFactory = triggerFactory;
    }

    /**
     * Sets a JobDetail Factory, if it needs to void using default factory
     *
     * @param jobDetailFactory TaskJobDetailFactory instance
     */
    public void setJobDetailFactory(TaskJobDetailFactory jobDetailFactory) {
        this.jobDetailFactory = jobDetailFactory;
    }

    private void validateInit() {

        if (!initialized) {
            throw new SynapseTaskException("Scheduler has not been initialled yet", log);
        }
    }

    private void validateStart() {

        try {
            if (!scheduler.isStarted()) {
                throw new SynapseTaskException("Scheduler has not been started yet", log);
            }
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error determine start state of the cheduler ", e, log);
        }
    }

}
