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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import java.util.Map;
import java.util.Set;

/**
 * Default JobDetailFactory ships with synapse utils
 */
public class DefaultTaskJobDetailFactory implements TaskJobDetailFactory {


    /**
     * @see TaskJobDetailFactory
     */
    public JobDetail createJobDetail(TaskDescription taskDescription, Map resources, Class<Job> jobClass) {

        JobDetail jobDetail = new JobDetail();
        Set xmlProperties = taskDescription.getProperties();
        String className = taskDescription.getTaskClass();
        String name = taskDescription.getName();
        // Give the job a name
        jobDetail.setName(name);
        String group = taskDescription.getGroup();
        if (group != null && !"".equals(group)) {
            jobDetail.setGroup(group);
        } else {
            jobDetail.setGroup(TaskDescription.DEFAULT_GROUP);
        }
        jobDetail.setJobClass(jobClass);
        JobDataMap jdm = new JobDataMap(resources);
        jdm.put(TaskDescription.CLASSNAME, className);
        jdm.put(TaskDescription.PROPERTIES, xmlProperties);
        jobDetail.setJobDataMap(jdm);
        return jobDetail;
    }
}
