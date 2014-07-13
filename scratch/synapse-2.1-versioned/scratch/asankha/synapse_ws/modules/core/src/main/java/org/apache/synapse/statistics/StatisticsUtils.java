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

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.statistics.impl.SequenceStatisticsStack;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;
import org.apache.synapse.statistics.impl.ProxyServiceStatisticsStack;

/**
 * A utils to process statistics
 *
 */

public class StatisticsUtils {

    /**
     * To process statistics related to the proxy services
     *
     * @param synCtx
     */
    public static void processProxyServiceStatistics(MessageContext synCtx) {

        StatisticsCollector statisticsCollector = getStatisticsCollector(synCtx);
        boolean isFault = synCtx.getEnvelope().getBody().hasFault();
        ProxyServiceStatisticsStack proxyServiceStatisticsStack = (ProxyServiceStatisticsStack)
                synCtx.getProperty(SynapseConstants.PROXY_STATS);
        if (proxyServiceStatisticsStack != null) {
            proxyServiceStatisticsStack.reportToStatisticsCollector(statisticsCollector,isFault);
        }
        ProxyServiceStatisticsStack synapseServiceStatisticsStack = (ProxyServiceStatisticsStack)
                synCtx.getProperty(SynapseConstants.SERVICE_STATS);
        if (synapseServiceStatisticsStack != null) {
            synapseServiceStatisticsStack.reportToStatisticsCollector(statisticsCollector,isFault);
        }
    }

    /**
     * To process statistics related to the End Points
     *
     * @param synCtx
     */
    public static void processEndPointStatistics(MessageContext synCtx) {
        StatisticsCollector statisticsCollector = getStatisticsCollector(synCtx);
        boolean isFault = synCtx.getEnvelope().getBody().hasFault();
        EndPointStatisticsStack endPointStatisticsStack = (EndPointStatisticsStack)
                synCtx.getProperty(SynapseConstants.ENDPOINT_STATS);
        if (endPointStatisticsStack != null) {
            Object endpointObj = synCtx.getProperty(SynapseConstants.PROCESSED_ENDPOINT);
            if (endpointObj instanceof Endpoint) {
                Endpoint endpoint = (Endpoint) endpointObj;
                String name = endpoint.getName();
                if (name == null) {
                    endPointStatisticsStack.reportToStatisticsCollector(
                            statisticsCollector, isFault);
                } else {
                    endPointStatisticsStack.reportToStatisticsCollector(
                            statisticsCollector, isFault, name);
                }
                endPointStatisticsStack.reportAllToStatisticsCollector(statisticsCollector, true);
            }
        }
    }

    /**
     * To process statistics related to the sequence
     *
     * @param synCtx
     */
    public static void processSequenceStatistics(MessageContext synCtx) {
        StatisticsCollector statisticsCollector = getStatisticsCollector(synCtx);
        boolean isFault = synCtx.getEnvelope().getBody().hasFault();
        SequenceStatisticsStack sequenceStatisticsStack = (SequenceStatisticsStack)
                synCtx.getProperty(SynapseConstants.SEQUENCE_STATS);
        if (sequenceStatisticsStack != null) {
            sequenceStatisticsStack.reportToStatisticsCollector(statisticsCollector,isFault);
        }
    }

     /**
     * To process all statistics related to the sequence
     *
     * @param synCtx
     */
    public static void processAllSequenceStatistics(MessageContext synCtx) {
        StatisticsCollector statisticsCollector = getStatisticsCollector(synCtx);
        boolean isFault = synCtx.getEnvelope().getBody().hasFault();
        SequenceStatisticsStack sequenceStatisticsStack = (SequenceStatisticsStack)
                synCtx.getProperty(SynapseConstants.SEQUENCE_STATS);
        if (sequenceStatisticsStack != null) {
            sequenceStatisticsStack.reportAllToStatisticsCollector(statisticsCollector,isFault);
        }
    }
    /**
     * A helper method to get StatisticsCollector from the Synapse Message Context
     *
     * @param synCtx
     * @return StatisticsCollector
     */
    private static StatisticsCollector getStatisticsCollector(MessageContext synCtx) {
        SynapseEnvironment synEnv = synCtx.getEnvironment();
        StatisticsCollector statisticsCollector = null;
        if (synEnv != null) {
            statisticsCollector = synEnv.getStatisticsCollector();
            if (statisticsCollector == null) {
                statisticsCollector = new StatisticsCollector();
                synEnv.setStatisticsCollector(statisticsCollector);
            }
        }
        return statisticsCollector;
    }
}
