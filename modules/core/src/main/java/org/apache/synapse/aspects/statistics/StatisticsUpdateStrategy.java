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

import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.statistics.view.Statistics;

import java.util.Iterator;

/**
 * Updates the given statistics base on statistics logs in the given statistics record.
 * This is to use only at viewing statistics
 */
public class StatisticsUpdateStrategy {

    private final StatisticsRecord statisticsRecord;

    public StatisticsUpdateStrategy(StatisticsRecord statisticsRecord) {
        this.statisticsRecord = statisticsRecord;
    }

    public void updateInFlowStatistics(String id, ComponentType componentType,
                                       Statistics statistics) {
        updateStatistics(id, componentType, statistics, false);
    }

    public void updateOutFlowStatistics(String id, ComponentType componentType,
                                        Statistics statistics) {
        updateStatistics(id, componentType, statistics, true);
    }

    private void updateStatistics(String id, ComponentType componentType,
                                  Statistics statistics, boolean isResponse) {

        StatisticsLog startLog = null;
        StatisticsLog endLog = null;
        final Iterator<StatisticsLog> statisticsLogs = statisticsRecord.getAllStatisticsLogs();
        while (statisticsLogs.hasNext()) {
            StatisticsLog log = statisticsLogs.next();
            if (log == null) {
                continue;
            }
            switch (componentType) {
                case SEQUENCE: {
                    if (componentType == log.getComponentType()) {
                        if (isResponse != log.isResponse()) {
                            continue;
                        }
                        if (!id.equals(log.getId())) {
                            continue;
                        }
                        if (startLog == null) {
                            startLog = log;
                        } else if (startLog.isResponse() == log.isResponse()) {
                            endLog = log;
                        }
                    }
                    break;
                }
                default: {
                    if (!isResponse) {
                        if (componentType == log.getComponentType()) {
                            if (!id.equals(log.getId())) {
                                continue;
                            }
                            if (startLog == null) {
                                startLog = log;
                            }
                        }
                        if (startLog == null) {
                            continue;
                        }
                        if (log.getComponentType() == ComponentType.ANY && endLog == null) {
                            endLog = log;
                        }
                    } else {
                        if (log.getComponentType() == ComponentType.ANY) {
                            if (startLog == null) {
                                startLog = log;
                            } else if (endLog == null) {
                                endLog = log;
                            }
                        }
                    }
                }
            }
        }
        if (endLog != null && startLog != null) {
            statistics.update(endLog.getTime() - startLog.getTime(),
                    statisticsRecord.isFaultResponse());
        }
    }
}
