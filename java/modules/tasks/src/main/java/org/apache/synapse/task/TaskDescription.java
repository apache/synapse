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


import org.apache.axiom.om.OMElement;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates details about a task
 * Properties are self descriptive and related with quartz
 */
public class TaskDescription {

    public static final String CLASSNAME = "ClassName";
    public static final String PROPERTIES = "Properties";
    public static final String DEFAULT_GROUP = "synapse.simple.quartz";
    private String cron;
    private int repeatCount = -1;
    private long repeatInterval; // in milliseconds
    private String className;
    private List pinnedServers;
    private Set<OMElement> xmlProperties = new HashSet<OMElement>();
    private String name;
    private String group;
    private Date startTime;
    private Date endTime;
    private boolean volatility = true;


    public String getTaskClass() {
        return className;
    }

    public void setTaskClass(String attributeValue) {
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

    public List getPinnedServers() {
        return pinnedServers;
    }

    public void setPinnedServers(List pinnedServers) {
        this.pinnedServers = pinnedServers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public boolean isVolatility() {
        return volatility;
    }

    public void setVolatility(boolean volatility) {
        this.volatility = volatility;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("[Task Description [ Name : ").append(name).
                append(" ][ClassName : ").append(className).append(" ] ]");
        return stringBuffer.toString();
    }
}
