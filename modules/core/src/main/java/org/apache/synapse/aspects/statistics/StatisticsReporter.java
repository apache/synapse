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

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Identifiable;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.endpoints.EndpointDefinition;

/**
 * A utility to report statistics
 */

public class StatisticsReporter {

    private static final Log log = LogFactory.getLog(StatisticsReporter.class);

    /**
     * Collects statistics for the given component
     *
     * @param synCtx        Current Message through synapse
     * @param configurable  Instance that can be configured it's audit
     * @param componentType Type of the component need aspect
     */
    public static void reportForComponent(MessageContext synCtx,
                                          StatisticsConfigurable configurable,
                                          ComponentType componentType) {
        if (configurable instanceof Identifiable && configurable.isStatisticsEnable()) {
            StatisticsRecord record = getStatisticsRecord(synCtx);
            record.setOwner(componentType);
            record.collect(createStatisticsLog((Identifiable) configurable, componentType, synCtx));
        }
    }

    /**
     * Collects statistics for any component
     *
     * @param synCtx Current Message through synapse
     */
    public static void reportForAllOnResponseReceived(MessageContext synCtx) {
        synCtx.setProperty(SynapseConstants.SENDING_REQUEST, false);
        StatisticsRecord statisticsRecord =
                (StatisticsRecord) synCtx.getProperty(SynapseConstants.STATISTICS_STACK);
        if (statisticsRecord != null) {
            AspectConfiguration configuration = new AspectConfiguration(
                    SynapseConstants.SYNAPSE_ASPECTS);
            configuration.enableStatistics();
            statisticsRecord.collect(createStatisticsLog(configuration, ComponentType.ANY, synCtx));
        }
    }

    /**
     * Reporting a fault
     *
     * @param synCtx   synCtx  Current Message through synapse
     * @param errorLog the received error information
     */
    public static void reportFaultForAll(MessageContext synCtx, ErrorLog errorLog) {

        StatisticsRecord statisticsRecord =
                (StatisticsRecord) synCtx.getProperty(SynapseConstants.STATISTICS_STACK);
        if (statisticsRecord != null) {
            if (log.isDebugEnabled()) {
                log.debug("Reporting a fault : " + statisticsRecord);
            }
            StatisticsLog statisticsLog = new StatisticsLog(SynapseConstants.SYNAPSE_ASPECTS,
                    ComponentType.ANY);
            statisticsLog.setResponse(synCtx.isResponse() || synCtx.isFaultResponse());
            statisticsLog.setFault(true);
            statisticsLog.setErrorLog(errorLog);
            statisticsRecord.collect(statisticsLog);
        }
    }

    /**
     * Reports statistics on the response message Sent
     *
     * @param synCtx   MessageContext instance
     * @param endpoint EndpointDefinition instance
     */
    public static void reportForAllOnResponseSent(MessageContext synCtx,
                                                  EndpointDefinition endpoint) {
        if (endpoint != null) {
            if (synCtx.getProperty(SynapseConstants.OUT_ONLY) != null) {
                endReportForAll(synCtx, endpoint.isStatisticsEnable());
            }
        } else {
            endReportForAll(synCtx, false);
        }
    }

    /**
     * Ends statistics reporting for any component
     *
     * @param synCtx             MessageContext instance
     * @param isStatisticsEnable is stat enable
     */
    private static void endReportForAll(MessageContext synCtx, boolean isStatisticsEnable) {
        StatisticsRecord statisticsRecord =
                (StatisticsRecord) synCtx.getProperty(SynapseConstants.STATISTICS_STACK);
        if (isStatisticsEnable || statisticsRecord != null) {
            if (!statisticsRecord.isEndReported()) {
                StatisticsLog statisticsLog = new StatisticsLog(SynapseConstants.SYNAPSE_ASPECTS,
                        ComponentType.ANY);
                statisticsLog.setResponse(synCtx.isResponse() || synCtx.isFaultResponse());
                if (isFault(synCtx)) {
                    statisticsLog.setFault(true);
                    statisticsLog.setErrorLog(ErrorLogFactory.createErrorLog(synCtx));
                }
                statisticsLog.setEndAnyLog(true);
                statisticsRecord.collect(statisticsLog);
                statisticsRecord.setEndReported(true);
                addStatistics(synCtx, statisticsRecord);
            }
        }
    }

    /**
     * Ends statistics reporting after request processed
     *
     * @param synCtx              MessageContext instance
     * @param aspectConfiguration main component's aspect conf
     */
    public static void endReportForAllOnRequestProcessed(MessageContext synCtx,
                                                         AspectConfiguration aspectConfiguration) {

        boolean isOutOnly = Boolean.parseBoolean(
                String.valueOf(synCtx.getProperty(SynapseConstants.OUT_ONLY)));
        if (!isOutOnly) {
            isOutOnly = (!Boolean.parseBoolean(
                    String.valueOf(synCtx.getProperty(SynapseConstants.SENDING_REQUEST)))
                    && !synCtx.isResponse());
        }
        if (isOutOnly) {
            endReportForAll(synCtx,
                    (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable()));
        }
    }

    /**
     * Gets a StatisticsRecord
     *
     * @param synCtx MessageContext instance
     * @return a StatisticsRecord
     */
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

    /**
     * Collects statistics
     *
     * @param synCtx MessageContext instance
     * @param record StatisticsRecord instance
     */
    private static void addStatistics(MessageContext synCtx,
                                      StatisticsRecord record) {

        StatisticsCollector collector = synCtx.getEnvironment().getStatisticsCollector();
        if (collector == null) {

            if (log.isDebugEnabled()) {
                log.debug("Setting statistics collector in the synapse environment.");
            }
            collector = new StatisticsCollector();
            synCtx.getEnvironment().setStatisticsCollector(collector);
        }
        synCtx.getPropertyKeySet().remove(SynapseConstants.STATISTICS_STACK);
        if (!collector.contains(record)) {
            collector.collect(record);
        }
    }

    /**
     * Factory method to create a   StatisticsLog
     *
     * @param identifiable  component
     * @param componentType component type
     * @param synCtx        MessageContext instance
     * @return a StatisticsLog
     */
    private static StatisticsLog createStatisticsLog(Identifiable identifiable,
                                                     ComponentType componentType,
                                                     MessageContext synCtx) {
        if (isValid(identifiable)) {
            String auditID = identifiable.getId();
            StatisticsLog statisticsLog = new StatisticsLog(auditID, componentType);
            statisticsLog.setResponse(synCtx.isResponse() || synCtx.isFaultResponse());
            if (isFault(synCtx)) {
                statisticsLog.setFault(true);
                statisticsLog.setErrorLog(ErrorLogFactory.createErrorLog(synCtx));
            }
            return statisticsLog;
        }
        return null;
    }

    /**
     * Checks the validity of the component
     *
     * @param identifiable component as a
     * @return true if the component is valid
     */
    private static boolean isValid(Identifiable identifiable) {

        if (identifiable == null) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid aspects configuration , It is null.");
            }
            return false;
        }

        String auditID = identifiable.getId();
        if (auditID == null || "".equals(auditID)) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid aspects configuration , Audit name is null.");
            }
            return false;
        }
        return true;
    }

    /**
     * Detects a fault
     *
     * @param context MessageContext context
     * @return true if this is a fault
     */
    private static boolean isFault(MessageContext context) {
        boolean isFault = context.isFaultResponse();
        if (!isFault) {
            isFault = context.getProperty(SynapseConstants.ERROR_CODE) != null;

            if (!isFault) {
                SOAPEnvelope envelope = context.getEnvelope();
                if (envelope != null) {
                    isFault = envelope.hasFault();
                }
            }
        }
        return isFault;
    }
}
