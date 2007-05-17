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
package org.apache.synapse.statistics.impl;

import org.apache.synapse.statistics.StatisticsStack;
import org.apache.synapse.statistics.StatisticsCollector;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * The data structure to hold statistics related to the endpoints
 *
 */

public class EndPointStatisticsStack implements StatisticsStack {

    /** list of endpoint statistics */
    private List endpointStatistics;
    /** To decide whether the reporting of the in flow statistics have been completed*/
    private boolean isCompleteInFlowStatisicsReport = false;

    /**
     * To put statistics
     * @param key                   - The name of the End Point
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     * @param isFault
     */
    public void put(String key, long initTime, boolean isInFlow, boolean isStatisticsEnable,
                    boolean isFault) {
        if (endpointStatistics == null) {
            endpointStatistics = new ArrayList();
        }
        endpointStatistics.add(
                new EndPointStatistics(key, initTime, isInFlow, isStatisticsEnable, isFault));
    }

    /**
     * This method used to report the latest  statistics to the StatisticsCollector
     * @param statisticsCollector
     * @param isFault
     */

    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault) {
        if (endpointStatistics != null && !endpointStatistics.isEmpty()) {
            EndPointStatistics statistics =
                    (EndPointStatistics) endpointStatistics.get(
                            endpointStatistics.size() - 1);
            if (statistics != null && statistics.isStatisticsEnable &&
                    statistics.endPointName != null) {
                if (statistics.inTimeForInFlow != -1) {
                    long initTimeForOutFlow = System.currentTimeMillis();
                    statisticsCollector.reportForEndPoint(statistics.endPointName,
                            false, statistics.inTimeForInFlow,
                            initTimeForOutFlow, isFault);
                    statistics.inTimeForInFlow = -1;
                    statistics.inTimeForOutFlow = initTimeForOutFlow;
                } else if (statistics.inTimeForOutFlow != -1 &&
                        isCompleteInFlowStatisicsReport) {
                    statisticsCollector.reportForEndPoint(statistics.endPointName,
                            true, statistics.inTimeForOutFlow,
                            System.currentTimeMillis(), isFault);
                    endpointStatistics.remove(statistics);
                }
            }
        }
    }

    /**
     * Report a particular statistics to the StatisticsCollector
     * @param statisticsCollector
     * @param isFault
     * @param name
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault, String name) {
        if (endpointStatistics != null && !endpointStatistics.isEmpty()) {
            List tobeRemoved = new ArrayList();
            for (Iterator epIterator = endpointStatistics.iterator();
                 epIterator.hasNext();) {
                Object statisticsObj = epIterator.next();
                if (statisticsObj instanceof EndPointStatistics) {
                    EndPointStatistics statistics = (EndPointStatistics) statisticsObj;
                    if (statistics.isStatisticsEnable && statistics.endPointName != null &&
                            statistics.endPointName.equals(name)) {
                        if (statistics.inTimeForInFlow != -1) {
                            long initTimeForOutFlow = System.currentTimeMillis();
                            statisticsCollector.reportForEndPoint(statistics.endPointName,
                                    false, statistics.inTimeForInFlow,
                                    initTimeForOutFlow, isFault);
                            statistics.inTimeForInFlow = -1;
                            statistics.inTimeForOutFlow = initTimeForOutFlow;
                        } else if (statistics.inTimeForOutFlow != -1 &&
                                isCompleteInFlowStatisicsReport) {
                            statisticsCollector.reportForEndPoint(statistics.endPointName,
                                    true, statistics.inTimeForOutFlow,
                                    System.currentTimeMillis(), isFault);
                            tobeRemoved.add(statistics);
                        }
                    }
                }
            }
            endpointStatistics.removeAll(tobeRemoved);
        }
    }

    /**
     * This method  used to unreported all statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportAllToStatisticsCollector(StatisticsCollector statisticsCollector,
                                               boolean isFault) {
        if (endpointStatistics != null && !endpointStatistics.isEmpty()) {
            List tobeRemoved = new ArrayList();
            for (Iterator epIterator = endpointStatistics.iterator();
                 epIterator.hasNext();) {
                Object statisticsObj = epIterator.next();
                if (statisticsObj instanceof EndPointStatistics) {
                    EndPointStatistics statistics = (EndPointStatistics) statisticsObj;
                    if (statistics.isStatisticsEnable && statistics.endPointName != null) {
                        if (statistics.inTimeForInFlow != -1) {
                            long initTimeForOutFlow = System.currentTimeMillis();
                            statisticsCollector.reportForEndPoint(statistics.endPointName,
                                    false, statistics.inTimeForInFlow,
                                    initTimeForOutFlow, isFault);
                            statistics.inTimeForInFlow = -1;
                            statistics.inTimeForOutFlow = initTimeForOutFlow;
                        } else if (statistics.inTimeForOutFlow != -1 &&
                                isCompleteInFlowStatisicsReport) {
                            statisticsCollector.reportForEndPoint(statistics.endPointName,
                                    true, statistics.inTimeForOutFlow,
                                    System.currentTimeMillis(), isFault);
                            tobeRemoved.add(statistics);
                        }
                    }
                }
            }
            endpointStatistics.removeAll(tobeRemoved);
        }
        isCompleteInFlowStatisicsReport = true;
    }

    class EndPointStatistics {

        /** The name of the endpoint    */
        private String endPointName;
         /** To check whether IN message flow or not   */
        private boolean isStatisticsEnable;
        /** To indicate whether this is fault or not  */
        private boolean isFault;
        /** The time which starts to collect statistics for IN flow */
        private long inTimeForInFlow = -1;
        /** The time which starts to collect statistics for OUT flow */
        private long inTimeForOutFlow = -1;

        public EndPointStatistics(String endPointName, long initTime, boolean inFlow,
                                  boolean statisticsEnable, boolean fault) {
            if (inFlow) {
                this.endPointName = endPointName;
                this.inTimeForInFlow = initTime;
                isStatisticsEnable = statisticsEnable;
                isFault = fault;
            }
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EndPointStatistics that = (EndPointStatistics) o;

            if (endPointName != null ? !endPointName.equals(that.endPointName) : that.endPointName != null)
                return false;

            return true;
        }

        public int hashCode() {
            return (endPointName != null ? endPointName.hashCode() : 0);
        }
    }
}
