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
package org.apache.synapse.audit.statistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.audit.AuditConfigurable;

/**
 * A utility to process statistics
 */

public class StatisticsReporter {

    private static final Log log = LogFactory.getLog(StatisticsReporter.class);

    /**
     * Initialize the audit details collection by setting a AuditConfiguration
     *
     * @param synCtx            Current Message through synapse
     * @param auditConfigurable Instance that can be configured it's audit
     */
    public static void collect(MessageContext synCtx, AuditConfigurable auditConfigurable) {

        StatisticsRecord statisticsRecord = StatisticsReporter.getStatisticsRecord(synCtx);
        if (statisticsRecord == null) {

            if (log.isDebugEnabled()) {
                log.debug("Setting statistics stack on the message context.");
            }
            statisticsRecord = StatisticsRecordFactory.getStatisticsRecord(synCtx);
            synCtx.setProperty(SynapseConstants.STATISTICS_STACK, statisticsRecord);
        }
        statisticsRecord.collect(auditConfigurable);

        StatisticsCollector collector = synCtx.getEnvironment().getStatisticsCollector();
        if (collector == null) {

            if (log.isDebugEnabled()) {
                log.debug("Setting statistics collector in the synapse environment.");
            }
            collector = new StatisticsCollector();
            synCtx.getEnvironment().setStatisticsCollector(collector);
        }
        if (!collector.contains(statisticsRecord)) {
            collector.collect(statisticsRecord);
        }
    }

    /**
     * Reporting audit for a particular resource
     *
     * @param synCtx            Current Message through synapse
     * @param auditConfigurable Instance that can be configured it's audit
     */
    public static void report(MessageContext synCtx, AuditConfigurable auditConfigurable) {

        StatisticsRecord statisticsRecord = StatisticsReporter.getStatisticsRecord(synCtx);
        if (statisticsRecord != null) {
            statisticsRecord.commit(auditConfigurable);
        }
    }

    /**
     * Reporting a fault
     *
     * @param synCtx synCtx  Current Message through synapse
     */
    public static void reportFault(MessageContext synCtx) {

        StatisticsRecord statisticsRecord = StatisticsReporter.getStatisticsRecord(synCtx);
        if (statisticsRecord != null) {
            if (log.isDebugEnabled()) {
                log.debug("Reporting a fault : " + statisticsRecord);
            }
            statisticsRecord.setFaultResponse(true);
        }
    }

    private static StatisticsRecord getStatisticsRecord(MessageContext synCtx) {
        return (StatisticsRecord) synCtx.getProperty(SynapseConstants.STATISTICS_STACK);
    }
}
