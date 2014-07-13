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
package org.apache.synapse.statistics;

/**
 *  This interface need to be implemented by each of the entry that
 *  need to collect statistics
 *
 */

public interface StatisticsStack {

    /**
     * This method is used to put current statistics
     * @param key
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     * @param isFault
     */
    public void put(String key,long initTime, boolean isInFlow, boolean isStatisticsEnable,
                    boolean isFault);

    /**
     * This method used to report the latest  statistics to the StatisticsCollector
     * @param statisticsCollector
     * @param isFault
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault);

    /**
     * Report the particular statistics to the StatisticsCollector
     *
     * @param statisticsCollector
     * @param isFault
     * @param name
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault, String name);

    /**
     * This method  used to unreported all statistics to the StatisticsCollector
     * @param statisticsCollector
     * @param isFault
     */
    public void reportAllToStatisticsCollector(StatisticsCollector statisticsCollector,
                                               boolean isFault);

}
