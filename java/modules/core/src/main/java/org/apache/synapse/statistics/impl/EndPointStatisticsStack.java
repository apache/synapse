/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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

    /**
     * To put statistics
     * @param key                   - The name of the End Point
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     */
    public void put(String key, long initTime, boolean isInFlow, boolean isStatisticsEnable) {
        this.endPointName = key;
        this.initTime = initTime;
        this.isInFlow = isInFlow;
        this.isStatisticsEnable = isStatisticsEnable;
    }

    /**
     * This method used to report the latest  statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector) {
        if (isStatisticsEnable && endPointName!=null) {
            statisticsCollector.reportForEndPoint(endPointName, !isInFlow, initTime, System.currentTimeMillis(), false);
            endPointName =null;
        }
    }

    /**
     * This method  used to unreported all statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportAllToStatisticsCollector(StatisticsCollector statisticsCollector) {
        reportToStatisticsCollector(statisticsCollector);
    }
}
