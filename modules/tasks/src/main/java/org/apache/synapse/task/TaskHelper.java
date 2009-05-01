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
 *//**
 *
 */
package org.apache.synapse.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.SynapseUtilException;

/**
 * Helper class to a share Scheduler and  TaskDescriptionRepository with in a single class space
 */
public class TaskHelper {

    private static final Log log = LogFactory.getLog(TaskHelper.class);
    private static TaskHelper ourInstance = new TaskHelper();
    private TaskDescriptionRepository taskDescriptionRepository;
    private TaskScheduler taskScheduler;
    private boolean initialized = false;

    public static TaskHelper getInstance() {
        return ourInstance;
    }

    /**
     * Initialize with given TaskDescriptionRepository and TaskScheduler instances .
     * if these are null , new instances will be created.
     *
     * @param taskDescriptionRepository TaskDescriptionRepository  instance
     * @param taskScheduler             TaskScheduler instance
     */
    public void init(TaskDescriptionRepository taskDescriptionRepository, TaskScheduler taskScheduler) {

        if (taskDescriptionRepository != null) {
            this.taskDescriptionRepository = taskDescriptionRepository;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Creating new TaskDescriptionRepository as given instance is null.");
            }
            this.taskDescriptionRepository =
                    TaskDescriptionRepositoryFactory.getTaskDescriptionRepository(
                            TaskConstants.TASK_DESCRIPTION_REPOSITORY);
        }

        if (taskScheduler != null) {
            this.taskScheduler = taskScheduler;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Creating new TaskScheduler as given instance is null.");
            }
            this.taskScheduler = TaskSchedulerFactory.getTaskScheduler(TaskConstants.TASK_SCHEDULER);

        }
        initialized = true;
    }

    public TaskDescriptionRepository getTaskDescriptionRepository() {
        assertInitialized();
        return taskDescriptionRepository;
    }

    public TaskScheduler getTaskScheduler() {
        assertInitialized();
        return taskScheduler;
    }

    private void assertInitialized() {
        if (!initialized) {
            String msg = "Task helper has not been initialized, it requires to be initialized";
            log.error(msg);
            throw new SynapseUtilException(msg);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void cleanup() {
        assertInitialized();
        taskDescriptionRepository.clear();
        taskScheduler.shutDown();
    }

    public void pauseAll() {
        if (taskScheduler != null) {
            if(taskScheduler.isInitialized()) {
                try {
                    taskScheduler.pauseAll();
                } catch (SynapseTaskException ignore) {
                    // This exceptions has already been logged and we don't want to interrupt the flow
                }
            }
        }
    }

    public void resumeAll() {
        if (taskScheduler != null) {
            try {
                taskScheduler.resumeAll();
            } catch (SynapseTaskException ignore) {
                // This exceptions has already been logged and we don't want to interrupt the flow
            }
        }
    }
}
