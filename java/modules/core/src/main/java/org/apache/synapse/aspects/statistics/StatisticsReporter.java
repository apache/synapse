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
package org.apache.synapse.aspects.statistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Identifiable;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.AspectConfigurationDetectionStrategy;
import org.apache.synapse.aspects.ComponentType;

/**
 * A utility to report statistics
 */

public class StatisticsReporter {

    private static final Log log = LogFactory.getLog(StatisticsReporter.class);

    /**
     * Collects statistics for the given componenet
     *
     * @param synCtx        Current Message through synapse
     * @param configurable  Instance that can be configured it's audit
     * @param componentType Type of the componet need aspect
     */
    public static void reportForComponent(MessageContext synCtx,
                                          StatisticsConfigurable configurable,
                                          ComponentType componentType) {

        if (configurable != null && configurable.isStatisticsEnable()
                && configurable instanceof Identifiable) {

            StatisticsRecord record = StatisticsReporter.getStatisticsRecord(synCtx);
            record.setOwner(componentType);
            collectStatistics(synCtx, record, configurable, componentType);

        }
    }

    /**
     * Collects statistics for any component
     *
     * @param synCtx Current Message through synapse
     */
    public static void reportForAll(MessageContext synCtx) {

        AspectConfiguration configuration =
                AspectConfigurationDetectionStrategy.getAspectConfiguration(synCtx);

        if (configuration != null && configuration.isStatisticsEnable()) {

            StatisticsRecord record = StatisticsReporter.getStatisticsRecord(synCtx);
            collectStatistics(synCtx, record, configuration, ComponentType.ANY);
        }
    }

    /**
     * Reporting a fault
     *
     * @param synCtx synCtx  Current Message through synapse
     */
    public static void reportFaultForAll(MessageContext synCtx) {

        StatisticsRecord statisticsRecord = StatisticsReporter.getStatisticsRecord(synCtx);
        if (statisticsRecord != null) {
            if (log.isDebugEnabled()) {
                log.debug("Reporting a fault : " + statisticsRecord);
            }
            statisticsRecord.collect(
                    new AspectConfiguration(SynapseConstants.SYNAPSE_ASPECTS),
                    ComponentType.ANY, true);
            statisticsRecord.setFaultResponse(true);
        }
    }

    private static StatisticsRecord getStatisticsRecord(MessageContext synCtx) {

        StatisticsRecord statisticsRecord =
                (StatisticsRecord) synCtx.getProperty(SynapseConstants.STATISTICS_STACK);
        if (statisticsRecord == null) {

            if (log.isDebugEnabled()) {
                log.debug("Setting statistics stack on the message context.");
            }
            statisticsRecord = StatisticsRecordFactory.getStatisticsRecord(synCtx);
            synCtx.setProperty(SynapseConstants.STATISTICS_STACK, statisticsRecord);
        }
        return statisticsRecord;
    }

    private static void collectStatistics(MessageContext synCtx,
                                          StatisticsRecord record,
                                          StatisticsConfigurable configurable,
                                          ComponentType componentType) {

        StatisticsCollector collector = synCtx.getEnvironment().getStatisticsCollector();
        if (collector == null) {

            if (log.isDebugEnabled()) {
                log.debug("Setting statistics collector in the synapse environment.");
            }
            collector = new StatisticsCollector();
            synCtx.getEnvironment().setStatisticsCollector(collector);
        }

        record.collect((Identifiable) configurable,
                componentType, synCtx.isResponse());

        if (!collector.contains(record)) {
            collector.collect(record);
        }
    }
}
