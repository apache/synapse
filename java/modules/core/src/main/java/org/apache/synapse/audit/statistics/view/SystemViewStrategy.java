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
 * Strategy that determine a system wide statistics view
 */
public class SystemViewStrategy implements StatisticsViewStrategy {

    private static final Log log = LogFactory.getLog(SystemViewStrategy.class);

    public Map<String, Map<String, Statistics>> determineView(List<StatisticsRecord> statisticsRecords, int type) {

        Map<String, Map<String, Statistics>> statisticsMap = new HashMap<String, Map<String, Statistics>>();
        if (statisticsRecords == null) {
            if (log.isDebugEnabled()) {
                log.debug("Statistics records cannot be found.");
            }
            return statisticsMap;
        }

        Map<String, Statistics> perResourceMap = new HashMap<String, Statistics>();

        for (StatisticsRecord record : statisticsRecords) {

            if (record != null) {

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
                        log.debug("Cannot find the statistics logs for : " + record);
                    }
                    continue;
                }

                for (String rName : statisticsLogMap.keySet()) {

                    if (rName == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Cannot find the resource name ");
                        }
                        continue;
                    }

                    StatisticsLog statisticsLog = statisticsLogMap.get(rName);
                    if (statisticsLog == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Cannot find the statistics log for resource with name : " + rName);
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

        }

        statisticsMap.put(Statistics.ALL, perResourceMap);
        return statisticsMap;

    }

    public Map<String, Statistics> determineView(String id, List<StatisticsRecord> statisticsRecords, int type) {

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

        Statistics statistics = new Statistics(Statistics.ALL);
        for (StatisticsRecord record : statisticsRecords) {

            if (record != null) {
                StatisticsLog log = null;

                switch (type) {
                    case StatisticsLog.ENDPOINT_STATISTICS: {
                        log = record.getEndpointStatisticsRecord(id);
                        break;
                    }
                    case StatisticsLog.PROXY_SERVICE_STATISTICS: {
                        log = record.getProxyServiceStatisticsRecord(id);
                        break;
                    }
                    case StatisticsLog.MEDIATOR_STATISTICS: {
                        log = record.getMediatorStatisticsRecord(id);
                        break;
                    }
                }

                if (log != null) {
                    statistics.update(log.getProcessingTime(), record.isFaultResponse());
                }
            }
        }

        statisticsMap.put(Statistics.ALL, statistics);
        return statisticsMap;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
