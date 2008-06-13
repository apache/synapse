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
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The data structure to hold statistics related to the Proxy Services
 *
 */

public class ProxyServiceStatisticsStack implements StatisticsStack {

    private static final Log log = LogFactory.getLog(ProxyServiceStatisticsStack.class);
    /** The name of the proxy service*/
    private String proxyServiceName;
    /** To check whether statistics is enabled or not */
    private boolean isStatisticsEnable = false;
    /** The time which starts to collect statistics for IN flow */
    private long inTimeForInFlow = -1;
    /** The time which starts to collect statistics for OUT flow */
    private long inTimeForOutFlow = -1;
    /** To indicate whether IN Flow is fault or not*/
    private boolean isINFault;
    /** To indicate whether OUT Flow is fault or not*/
    private boolean isOUTFault;

    /**
     * To put a statistics
     * @param key                   - The Name of the proxy service
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     */
    public void put(String key, long initTime, boolean isInFlow, boolean isStatisticsEnable,
                    boolean isFault) {
        if (isInFlow) {
            this.proxyServiceName = key;
            this.isStatisticsEnable = isStatisticsEnable;
            this.inTimeForInFlow = initTime;
            this.isINFault = isFault;
        }
    }

    /**
     * This method used to report the latest  statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault) {
        synchronized (this) {
            if (proxyServiceName != null && isStatisticsEnable && inTimeForInFlow != -1) {
                inTimeForOutFlow = System.currentTimeMillis();
                isOUTFault = isFault;
                statisticsCollector.reportForProxyService(proxyServiceName, false,
                    inTimeForInFlow, inTimeForOutFlow, isINFault);
                inTimeForInFlow = -1;
            } else if (inTimeForOutFlow != -1) {
                statisticsCollector.reportForProxyService(proxyServiceName, true,
                    inTimeForOutFlow, System.currentTimeMillis(), isFault);
                inTimeForOutFlow = -1;
            }
        }
    }

    /**
     * Report a particular statistics to the StatisticsReporter
     *
     * @param statisticsCollector
     * @param isFault
     * @param name
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault, String name) {
        if (name != null && proxyServiceName != null && proxyServiceName.equals(name)) {
            reportToStatisticsCollector(statisticsCollector, isFault);
        } else {
            handleException("Invalid ProxyService Name " + name + " expected " + proxyServiceName);
        }
    }

    /**
     * This method  used to unreported all statistics to the StatisticsCollector
     * @param statisticsCollector
     * @param isFault
     */
    public void reportAllToStatisticsCollector(StatisticsCollector statisticsCollector,
                                               boolean isFault) {
        reportToStatisticsCollector(statisticsCollector, isFault);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
