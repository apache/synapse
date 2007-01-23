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

import java.util.ArrayList;

/**
 * The data structure to hold statistics related to Sequences
 *
 */

public class SequenceStatisticsStack implements StatisticsStack {

    /** The list to hols SequenceStatistics */
    private ArrayList sequenceStatisticsList = new ArrayList();

    /**
     * To put a statistics
     * @param sequenceName          - The name of the sequence
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     */
    public void put(String sequenceName, long initTime, boolean isInFlow, boolean isStatisticsEnable) {
        sequenceStatisticsList.add(new SequenceStatistics(sequenceName, initTime, isInFlow, isStatisticsEnable));
    }
   /**
     * This method used to report the latest  statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector) {
        popSequenceStatistics(sequenceStatisticsList.size() - 1, statisticsCollector);
    }
    /**
     * This method  used to unreported all statistics to the StatisticsCollector
     * @param statisticsCollector
     */
    public void reportAllToStatisticsCollector(StatisticsCollector statisticsCollector) {
        for (int i = sequenceStatisticsList.size(); i < 0; i--) {
            popSequenceStatistics(i, statisticsCollector);
        }
    }

    /**
     * A helper method to pop a SequenceStatistics
     * @param index
     * @param statisticsCollector
     */
    private void popSequenceStatistics(int index, StatisticsCollector statisticsCollector) {
        SequenceStatistics sequenceStatistics = (SequenceStatistics) sequenceStatisticsList.get(index);
        if (sequenceStatistics != null) {
            if (sequenceStatistics.isStatisticsEnable && sequenceStatistics.sequenceName != null) {
                statisticsCollector.reportForSequence(sequenceStatistics.sequenceName, !sequenceStatistics.isInFlow, sequenceStatistics.initTime, System.currentTimeMillis(), false);
            }
            sequenceStatisticsList.remove(index);
        }
    }

    /**
     * The SequenceStatistics data structure
     */
    class SequenceStatistics {

        /** The name of the Sequence */
        String sequenceName;
        /** The time which starts to collect statistics */
        long initTime;
        /** To check whether IN message flow or not */
        boolean isInFlow;
        /** To check whether statistics is enabled or not */
        boolean isStatisticsEnable;

        public SequenceStatistics(String sequenceName, long initTime, boolean inFlow, boolean statisticsEnable) {
            this.sequenceName = sequenceName;
            this.initTime = initTime;
            isInFlow = inFlow;
            isStatisticsEnable = statisticsEnable;
        }
    }

}
