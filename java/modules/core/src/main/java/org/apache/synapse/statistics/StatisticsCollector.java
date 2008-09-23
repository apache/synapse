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

import java.util.ArrayList;
import java.util.List;

/**
 * To collect statistics
 */

public class StatisticsCollector {

    private final List<StatisticsRecord> statisticsCollection = new ArrayList<StatisticsRecord>();

    public void collect(StatisticsRecord statisticsRegistry) {
        this.statisticsCollection.add(statisticsRegistry);
    }

    public Statistics getEndpointStatistics(String id) {
        Statistics statistics = new Statistics();
        for (StatisticsRecord statisticsRegistry : statisticsCollection) {
            if (statisticsRegistry != null) {
                StatisticsLog log = statisticsRegistry.getEndpointStatisticsRecord(id);
                if (log != null) {
                    statistics.update(log.getProcessingTime(), statisticsRegistry.isFaultResponse());
                }
            }
        }
        return statistics;
    }

    public boolean contains(StatisticsRecord statisticsRecord) {
        return statisticsCollection.contains(statisticsRecord);
    }
}
