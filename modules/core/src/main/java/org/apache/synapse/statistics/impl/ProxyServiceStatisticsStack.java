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
 * The data structure to hold statistics related to the Proxy Services
 *
 */

public class ProxyServiceStatisticsStack implements StatisticsStack {

    /** The name of the proxy service*/
    private String proxyServiceName;
    /** To check whether statistics is enabled or not */
    private boolean isStatisticsEnable = false;
    /** The time which starts to collect statistics for IN flow */
    private long inTimeForInFlow = -1;
    /** The time which starts to collect statistics for OUT flow */
    private long inTimeForOutFlow = -1;

    /**
     * To put a statistics
     * @param key                   - The Name of the proxy service
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     */
    public void put(String key, long initTime, boolean isInFlow, boolean isStatisticsEnable) {

        if (isInFlow) {
            this.proxyServiceName = key;
            this.isStatisticsEnable = isStatisticsEnable;
            this.inTimeForInFlow = initTime;
        }
    }

    /**
     * This method used to report the latest  statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector) {

        if (proxyServiceName != null && isStatisticsEnable && inTimeForInFlow != -1) {
            inTimeForOutFlow = System.currentTimeMillis();
            statisticsCollector.reportForProxyService(proxyServiceName, false, inTimeForInFlow, inTimeForOutFlow, false);
            inTimeForInFlow = -1;
        } else if (inTimeForOutFlow != -1) {
            statisticsCollector.reportForProxyService(proxyServiceName, true, inTimeForOutFlow, System.currentTimeMillis(), false);
            inTimeForOutFlow = -1;
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
