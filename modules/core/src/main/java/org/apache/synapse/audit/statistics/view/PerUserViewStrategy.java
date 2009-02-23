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
package org.apache.synapse.audit.statistics.view;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.audit.statistics.StatisticsLog;
import org.apache.synapse.audit.statistics.StatisticsRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy that determine a per user of remote party statistics view
 */
public abstract class PerUserViewStrategy implements StatisticsViewStrategy {

    protected final static int IP = 0;
    protected final static int DOMAIN = 1;
    private Log log;

    protected PerUserViewStrategy() {
        log = LogFactory.getLog(getClass());
    }

    public Map<String, Map<String, Statistics>> determineView(
            List<StatisticsRecord> statisticsRecords,
            int type,
            int userIDType) {

        final Map<String, Map<String, Statistics>> statisticsMap =
                new HashMap<String, Map<String, Statistics>>();

        if (statisticsRecords == null) {
            if (log.isDebugEnabled()) {
                log.debug("Statistics records cannot be found.");
            }
            return statisticsMap;
        }

        for (StatisticsRecord record : statisticsRecords) {

            if (record == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Statistics record cannot be found.");
                }
                continue;
            }

            Map<String, StatisticsLog> statisticsLogMap = null;
            switch (type) {
                case StatisticsLog.ENDPOINT_STATISTICS: {
                    statisticsLogMap = record.getAllEndpointStatisticsRecords();
                    break;
                }
                case StatisticsLog.PROXY_SERVICE_STATISTICS: {
                    statisticsLogMap = record.getAllProxyServiceStatisticsRecords();
                    break;
                }
                case StatisticsLog.MEDIATOR_STATISTICS: {
                    statisticsLogMap = record.getAllMediatorStatisticsRecords();
                    break;
                }
            }

            if (statisticsLogMap == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Statistics Logs cannot be found for statistics record :" + record);
                }
                continue;
            }

            String userID;
            if (IP == userIDType) {
                userID = record.getClientIP();
            } else {
                userID = record.getClientHost();
            }

            if (userID == null || "".equals(userID)) {
                if (log.isDebugEnabled()) {
                    log.debug("user ID cannot be found.");
                }
                continue;
            }

            Map<String, Statistics> perResourceMap;
            if (statisticsMap.containsKey(userID)) {
                perResourceMap = statisticsMap.get(userID);
            } else {
                perResourceMap = new HashMap<String, Statistics>();
                statisticsMap.put(userID, perResourceMap);
            }

            if (perResourceMap == null) {
                if (log.isDebugEnabled()) {
                    log.debug("There are not statistics for user ID : " + userID);
                }
                continue;
            }

            for (String rName : statisticsLogMap.keySet()) {

                if (rName == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resource name cannot be found.");
                    }
                    continue;
                }

                StatisticsLog statisticsLog = statisticsLogMap.get(rName);
                if (statisticsLog == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Statistics Logs cannot be found for resource with given name " +
                                rName);
                    }
                    continue;
                }

                Statistics statistics;
                if (!perResourceMap.containsKey(rName)) {
                    statistics = new Statistics(rName);
                    perResourceMap.put(rName, statistics);
                } else {
                    statistics = perResourceMap.get(rName);
                }

                if (statistics != null) {
                    statistics.update(statisticsLog.getProcessingTime(), record.isFaultResponse());
                }
            }
        }

        return statisticsMap;

    }

    public Map<String, Statistics> determineView(String id,
                                                 List<StatisticsRecord> statisticsRecords,
                                                 int type,
                                                 int userIDType) {

        if (id == null || "".equals(id)) {
            handleException("Resource Id cannot be null");
        }

        Map<String, Statistics> statisticsMap = new HashMap<String, Statistics>();

        if (statisticsRecords == null) {
            if (log.isDebugEnabled()) {
                log.debug("Statistics records cannot be found.");
            }
            return statisticsMap;
        }

        for (StatisticsRecord record : statisticsRecords) {

            if (record == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Statistics record cannot be found.");
                }
                continue;
            }

            StatisticsLog statisticsLog = null;
            switch (type) {
                case StatisticsLog.ENDPOINT_STATISTICS: {
                    statisticsLog = record.getEndpointStatisticsRecord(id);
                    break;
                }
                case StatisticsLog.PROXY_SERVICE_STATISTICS: {
                    statisticsLog = record.getProxyServiceStatisticsRecord(id);
                    break;
                }
                case StatisticsLog.MEDIATOR_STATISTICS: {
                    statisticsLog = record.getMediatorStatisticsRecord(id);
                    break;
                }
            }

            if (statisticsLog == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Statistics Logs cannot be found for statistics record  " + record);
                }
                continue;
            }

            String userID;
            if (IP == userIDType) {
                userID = record.getClientIP();
            } else {
                userID = record.getClientHost();
            }

            if (userID == null || "".equals(userID)) {
                if (log.isDebugEnabled()) {
                    log.debug("user ID cannot be found.");
                }
                continue;
            }

            Statistics statistics;
            if (statisticsMap.containsKey(userID)) {
                statistics = statisticsMap.get(userID);
            } else {
                statistics = new Statistics(userID);
                statisticsMap.put(userID, statistics);
            }

            if (statistics != null) {
                statistics.update(statisticsLog.getProcessingTime(), record.isFaultResponse());
            }
        }
        return statisticsMap;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
