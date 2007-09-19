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
 * To hold statistics that will collected during both of in and out message mediation
 */

public class StatisticsHolder {

    private int statisticsCategory ;

    private String key ;

    /**  The statistics that will collect during in coming message mediation  */
    private Statistics inFlowStatistics;

    /**  The statistics that will collect during out going message mediation  */
    private Statistics outFlowStatistics;

    /**
     * To update the statistics
     *
     * @param isResponse - A boolean value that indicate whether message flow is in or out
     * @param inTime     - The processing start time
     * @param outTime    - The processing end time
     * @param isFault    - A boolean value that indicate whether falut has occured or not
     */
    public synchronized void update(boolean isResponse, long inTime, long outTime,
                                    boolean isFault) {
        if (isResponse) {
            if (outFlowStatistics == null) {
                outFlowStatistics = new Statistics();
            }
            outFlowStatistics.update(inTime, outTime, isFault);
        } else {
            if (inFlowStatistics == null) {
                inFlowStatistics = new Statistics();
            }
            inFlowStatistics.update(inTime, outTime, isFault);
        }

    }

    /**
     * To get Statistics related to the In Flow
     * @return  Statistics related to the In Flow
     */
    public synchronized Statistics getInFlowStatistics() {
        return inFlowStatistics;
    }

    /**
     * To get Statistics related to the Out Flow
     * @return  Statistics related to the Out Flow
     */
    public synchronized Statistics getOutFlowStatistics() {
        return outFlowStatistics;
    }

    /**
     * To get statistics category
     * @return  Statistics Category
     */
    public int getStatisticsCategory() {
        return statisticsCategory;
    }

    /**
     * To set statistics category
     * @param statisticsCategory
     */
    public void setStatisticsCategory(int statisticsCategory) {
        this.statisticsCategory = statisticsCategory;
    }

    /**
     * To get key of statistics
     * @return   key of statistics
     */
    public String getKey() {
        return key;
    }

    /**
     * To set key of statistics
     * @param key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * To clear stored statistics
     */
    public synchronized void clearStatistics(){
        this.inFlowStatistics =null;
        this.outFlowStatistics=null;
    }
}
