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
package org.apache.synapse.aspects.statistics.mbean;

import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.statistics.StatisticsCollector;
import org.apache.synapse.aspects.statistics.view.InOutStatisticsView;
import org.apache.synapse.aspects.statistics.view.StatisticsViewStrategy;
import org.apache.synapse.aspects.statistics.view.SystemViewStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class StatisticsView implements StatisticsViewMBean {

    private final StatisticsCollector collector;
    private final StatisticsViewStrategy systemViewStrategy = new SystemViewStrategy();

    public StatisticsView(StatisticsCollector collector) {
        this.collector = collector;
    }

    public List<String> getSystemEndpointStats(String id) {
        return getAsList(
                this.collector.getStatistics(id, ComponentType.ENDPOINT, systemViewStrategy));
    }

    public List<String> getSystemSequnceStats(String id) {
        return getAsList(this.collector.getStatistics(id,
                ComponentType.SEQUENCE, systemViewStrategy));
    }

    public List<String> getSystemProxyServiceStats(String id) {
        return getAsList(this.collector.getStatistics(id,
                ComponentType.PROXYSERVICE, systemViewStrategy));
    }

    public void clearAllStatistics() {
        this.collector.clearStatistics();
    }

    private List<String> getAsList(Map<String, InOutStatisticsView> viewMap) {
        List<String> returnList = new ArrayList<String>();
        for (InOutStatisticsView view : viewMap.values()) {
            if (view != null) {
                returnList.add(view.toString());
            }
        }
        return returnList;
    }
}
