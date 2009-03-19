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
package org.apache.synapse.aspects.statistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Identifiable;
import org.apache.synapse.aspects.ComponentType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds a record for statistics for current message
 */
public class StatisticsRecord {

    private static final Log log = LogFactory.getLog(StatisticsRecord.class);
    private String id;
    private final List<StatisticsLog> statisticsLogs = new ArrayList<StatisticsLog>();
    private boolean isFaultResponse;
    private String clientIP;
    private String clientHost;
    private ComponentType owner;

    public StatisticsRecord(String id, String clientIP, String clientHost) {
        this.id = id;
        this.clientIP = clientIP;
        this.clientHost = clientHost;
    }

    public String getId() {
        return id;
    }

    public boolean isFaultResponse() {
        return isFaultResponse;
    }

    public String getClientIP() {
        return clientIP;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setFaultResponse(boolean faultResponse) {
        isFaultResponse = faultResponse;
    }

    /**
     * Collecting statistics for a particular component
     *
     * @param identifiable  audit configurable component
     * @param componentType The component that belong statistics
     * @param isResponse    Is this Response or not
     */
    public void collect(Identifiable identifiable, ComponentType componentType, boolean isResponse) {

        if (isValid(identifiable)) {

            String auditID = identifiable.getId();
            if (log.isDebugEnabled()) {
                log.debug("Start to reportForComponent statistics for : " + auditID);
            }
            statisticsLogs.add(new StatisticsLog(auditID, componentType, isResponse));
        }
    }

    /**
     * Gets all the StatisticsLogs
     *
     * @return A Iterator for all StatisticsLogs
     */
    public Iterator<StatisticsLog> getAllStatisticsLogs() {
        final List<StatisticsLog> logs = new ArrayList<StatisticsLog>();
        logs.addAll(statisticsLogs);
        return logs.iterator();
    }

    /**
     * Get all log ids related with a given component
     *
     * @param componentType The component that belong statistics
     * @return A List of Log ids
     */
    public Iterator<String> getAllLogIds(ComponentType componentType) {
        final List<String> logIds = new ArrayList<String>();
        for (StatisticsLog startLog : statisticsLogs) {
            if (startLog != null && startLog.getComponentType() == componentType) {
                String id = startLog.getId();
                if (id != null && !"".equals(id) && !logIds.contains(id)) {
                    logIds.add(id);
                }
            }
        }
        return logIds.iterator();
    }

    public String toString() {
        return new StringBuffer()
                .append("[Message id : ").append(id).append(" ]")
                .append("[Remote  IP : ").append(clientIP).append(" ]")
                .append("[Remote host : ").append(clientHost).append(" ]")
                .toString();
    }

    public void clearLogs() {
        statisticsLogs.clear();
    }

    public ComponentType getOwner() {
        return owner;
    }

    public void setOwner(ComponentType owner) {
        this.owner = owner;
    }

    private boolean isValid(Identifiable identifiable) {

        if (identifiable == null) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid aspects configuration , It is null.");
            }
            return false;
        }

        String auditID = identifiable.getId();
        if (auditID == null || "".equals(auditID)) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid aspects configuration , Audit name is null.");
            }
            return false;
        }
        return true;
    }
}
