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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The data structure to hold statistics related to Sequences
 *
 */

public class SequenceStatisticsStack implements StatisticsStack {

    /** The list to hold SequenceStatistics */
    private List sequenceStatisticsList = new ArrayList();

    /**
     * To put a statistics
     *
     * @param sequenceName       - The name of the sequence
     * @param initTime
     * @param isInFlow
     * @param isStatisticsEnable
     * @param isFault
     */
    public void put(String sequenceName, long initTime, boolean isInFlow,
                    boolean isStatisticsEnable, boolean isFault) {
        sequenceStatisticsList.add(new SequenceStatistics(sequenceName, initTime,
                isInFlow, isStatisticsEnable, isFault));
    }

    /**
     * This method used to report the latest  statistics to the StatisticsCollector
     *
     * @param statisticsCollector
     * @param isFault
     */
    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault) {
        int top = sequenceStatisticsList.size();
        if (top > 0) {
            popSequenceStatistics(sequenceStatisticsList.size() - 1, statisticsCollector);
        }
    }

    public void reportToStatisticsCollector(StatisticsCollector statisticsCollector,
                                            boolean isFault, String name) {
        if (!sequenceStatisticsList.isEmpty()) {
            List tobeRemoved = new ArrayList();
            for (Iterator seqIterator = sequenceStatisticsList.iterator();
                 seqIterator.hasNext();) {
                SequenceStatistics sequenceStatistics =
                        (SequenceStatistics) seqIterator.next();
                if (sequenceStatistics != null) {
                    if (sequenceStatistics.isStatisticsEnable &&
                            sequenceStatistics.sequenceName != null &&
                            sequenceStatistics.sequenceName.equals(name)
                            ) {
                        statisticsCollector.reportForSequence(sequenceStatistics.sequenceName,
                                !sequenceStatistics.isInFlow, sequenceStatistics.initTime,
                                System.currentTimeMillis(), sequenceStatistics.isFault);
                    }
                    tobeRemoved.add(sequenceStatistics);
                }
            }
            sequenceStatisticsList.removeAll(tobeRemoved);
        }
    }

    /**
     * This method  used to unreported all statistics to the StatisticsCollector
     *
     * @param statisticsCollector
     * @param isFault
     */
    public void reportAllToStatisticsCollector(StatisticsCollector statisticsCollector,
                                               boolean isFault) {
        int i = sequenceStatisticsList.size() - 1;
        for (; i >= 0; i--) {
            popSequenceStatistics(i, statisticsCollector);
        }
    }

    /**
     * A helper method to pop a SequenceStatistics
     * @param index
     * @param statisticsCollector
     */
    private void popSequenceStatistics(int index, StatisticsCollector statisticsCollector) {
        if (index >= 0) {
            SequenceStatistics sequenceStatistics =
                    (SequenceStatistics) sequenceStatisticsList.get(index);
            if (sequenceStatistics != null) {
                if (sequenceStatistics.isStatisticsEnable &&
                        sequenceStatistics.sequenceName != null) {
                    statisticsCollector.reportForSequence(sequenceStatistics.sequenceName,
                            !sequenceStatistics.isInFlow, sequenceStatistics.initTime,
                            System.currentTimeMillis(), sequenceStatistics.isFault);
                }
                sequenceStatisticsList.remove(index);
            }
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
        /** To indicate whether this is fault or not*/
        private boolean isFault;

        public SequenceStatistics(String sequenceName, long initTime, boolean inFlow,
                                  boolean statisticsEnable, boolean isFault) {
            this.sequenceName = sequenceName;
            this.initTime = initTime;
            isInFlow = inFlow;
            isStatisticsEnable = statisticsEnable;
            this.isFault = isFault;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final SequenceStatistics that = (SequenceStatistics) o;

            if (!sequenceName.equals(that.sequenceName)) return false;

            return true;
        }

        public int hashCode() {
            return sequenceName.hashCode();
        }
    }

}
