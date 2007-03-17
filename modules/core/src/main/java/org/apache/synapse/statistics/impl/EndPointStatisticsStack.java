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

/**
 * The data structure to hold statistics related to the end points
 *
 */

public class EndPointStatisticsStack implements StatisticsStack {

    /** The name of the endpoint */
    private String endPointName;
    /** The time which starts to collect statistics */
    private long initTime;
    /** To check whether IN message flow or not */
    private boolean isInFlow;
    /** To check whether statistics is enabled or not */
    private boolean isStatisticsEnable;
    /** To indicate whether this is fault or not*/
    private boolean isFault;
    /**
     * To put statistics
     * @param key                   - The name of the End Point
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     * @param isFault
     */
    public void put(String key, long initTime, boolean isInFlow, boolean isStatisticsEnable,boolean isFault) {
        this.endPointName = key;
        this.initTime = initTime;
        this.isInFlow = isInFlow;
        this.isStatisticsEnable = isStatisticsEnable;
        this.isFault = isFault;
    }

    /**
     * This method used to report the latest  statistics to the StatisticsCollector
     * @param statisticsCollector
     * @param isFault
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,boolean isFault) {
        if (isStatisticsEnable && endPointName!=null) {
            statisticsCollector.reportForEndPoint(endPointName, !isInFlow, initTime, System.currentTimeMillis(), isFault);
            endPointName =null;
        }
    }

    /**
     * This method  used to unreported all statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportAllToStatisticsCollector(StatisticsCollector statisticsCollector,boolean isFault) {
        reportToStatisticsCollector(statisticsCollector,isFault);
    }
}
