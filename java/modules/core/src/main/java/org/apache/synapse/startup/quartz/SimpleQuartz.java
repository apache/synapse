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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ServerManager;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.startup.AbstractStartup;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskDescriptionRepository;
import org.apache.synapse.task.TaskScheduler;
import org.apache.synapse.task.TaskSchedulerFactory;

import javax.xml.namespace.QName;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class is instantiated by SimpleQuartzFactory (or by hand)
 * When it is initialized it creates a Quartz Scheduler with a job and a trigger
 * The class it starts is always an instance of SimpleQuartzJob
 * SimpleQuartzJob is there to set the properties and start the actual business-logic class
 * It wraps up any properties that the job needs as in the JobDetail and JDMap
 */
public class SimpleQuartz extends AbstractStartup {

    private static final Log log = LogFactory.getLog(SimpleQuartz.class);

    private TaskDescription taskDescription;

    private TaskDescriptionRepository repository;


    public QName getTagQName() {
        return SimpleQuartzFactory.TASK;
    }

    public void destroy() {

        if (taskDescription == null) {
            if (log.isDebugEnabled()) {
                log.debug("There is no Task to be deleted");
            }
            return;
        }

        TaskScheduler taskScheduler = TaskSchedulerFactory.getTaskScheduler(
                SynapseConstants.SYNAPSE_STARTUP_TASK_SCHEDULER);

        if (taskScheduler != null && taskScheduler.isInitialized()) {
            taskScheduler.deleteTask(taskDescription.getName(), taskDescription.getGroup());
        }

        if (repository != null) {
            repository.removeTaskDescription(taskDescription.getName());
        }
    }

    public void init(SynapseEnvironment synapseEnvironment) {

        if (taskDescription == null) {
            handleException("TaskDescription is null");
        }

        repository = synapseEnvironment.getSynapseConfiguration().getTaskDescriptionRepository();

        if (repository == null) {
            handleException("Task Description Repository can not found");
        }

        repository.addTaskDescription(taskDescription);

        // this server name given by system property SynapseServerName
        // otherwise take host-name
        // else assume localhost
        String thisServerName = ServerManager.getInstance().getServerName();
        if (thisServerName == null || thisServerName.equals("")) {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                thisServerName = addr.getHostName();

            } catch (UnknownHostException e) {
                log.warn("Could not get local host name", e);
            }

            if (thisServerName == null || thisServerName.equals("")) {
                thisServerName = "localhost";
            }
        }
        log.debug("Synapse server name : " + thisServerName);

        // start proxy service if either,
        // pinned server name list is empty
        // or pinned server list has this server name
        List pinnedServers = taskDescription.getPinnedServers();
        if (pinnedServers != null && !pinnedServers.isEmpty()) {
            if (!pinnedServers.contains(thisServerName)) {
                log.info("Server name not in pinned servers list. Not starting Task : " + getName());
                return;
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SimpleQuartzJob.SYNAPSE_ENVIRONMENT, synapseEnvironment);

        try {

            TaskScheduler taskScheduler = TaskSchedulerFactory.getTaskScheduler(
                    SynapseConstants.SYNAPSE_STARTUP_TASK_SCHEDULER);
            if (taskScheduler != null) {
                if (!taskScheduler.isInitialized()) {
                    taskScheduler.init(synapseEnvironment.getSynapseConfiguration().getProperties());
                }
                taskScheduler.scheduleTask(taskDescription, map, SimpleQuartzJob.class);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("TaskScheduler cannot be found for :" +
                            SynapseConstants.SYNAPSE_STARTUP_TASK_SCHEDULER + " , " +
                            "therefore ignore scheduling of Task  " + taskDescription);
                }
            }

        } catch (Exception e) {
            log.fatal("Error starting up Scheduler", e);
            throw new SynapseException("Error starting up Scheduler", e);
        }

    }

    public TaskDescription getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(TaskDescription taskDescription) {
        this.taskDescription = taskDescription;
    }

    private static void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }
}
