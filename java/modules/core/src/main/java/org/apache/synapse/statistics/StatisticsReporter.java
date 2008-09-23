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

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;

/**
 * A utility to process statistics
 */

public class StatisticsReporter {

    private static StatisticsRecord getStatisticsRecord(MessageContext synCtx) {
        return (StatisticsRecord) synCtx.getProperty(SynapseConstants.STATISTICS_STACK);
    }

    public static void collect(MessageContext synCtx, AuditConfigurable auditConfigurable) {

        StatisticsRecord statisticsRecord = StatisticsReporter.getStatisticsRecord(synCtx);
        if (statisticsRecord == null) {
            statisticsRecord = StatisticsRecordFactory.getStatisticsRecord(synCtx);
            synCtx.setProperty(SynapseConstants.STATISTICS_STACK, statisticsRecord);
        }
        statisticsRecord.collect(auditConfigurable);

        StatisticsCollector collector = synCtx.getEnvironment().getStatisticsCollector();
        if (collector == null) {
            collector = new StatisticsCollector();
            synCtx.getEnvironment().setStatisticsCollector(collector);
        }
        if (!collector.contains(statisticsRecord)) {
            collector.collect(statisticsRecord);
        }
    }

    public static void report(MessageContext synCtx, AuditConfigurable auditConfigurable) {

        StatisticsRecord statisticsRecord = StatisticsReporter.getStatisticsRecord(synCtx);
        if (statisticsRecord != null) {
            statisticsRecord.commit(auditConfigurable);
        }
    }

    public static void reportFault(MessageContext synCtx) {
        StatisticsRecord statisticsRecord = StatisticsReporter.getStatisticsRecord(synCtx);
        if (statisticsRecord != null) {
            statisticsRecord.setFaultResponse(true);
        }
    }
}
