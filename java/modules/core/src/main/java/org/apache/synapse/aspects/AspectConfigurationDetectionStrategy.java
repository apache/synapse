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
package org.apache.synapse.aspects;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.statistics.StatisticsConfigurable;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;

/**
 * Take an  AspectConfiguration based on some startegy - Currently only consider statistics
 */
public class AspectConfigurationDetectionStrategy {

    public static AspectConfiguration getAspectConfiguration(MessageContext synCtx) {

        boolean statisticsEnable = false;
        if (XMLConfigConstants.STATISTICS_ENABLE.equals(
                synCtx.getConfiguration().getProperty(
                        SynapseConstants.SYNAPSE_STATISTICS_STATE))) {
            statisticsEnable = true;
        }

        if (synCtx.isResponse()) {

            if (!statisticsEnable) {
                // if this is not a response to a proxy service
                String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);

                if (proxyName != null && !"".equals(proxyName)) {
                    ProxyService proxy = synCtx.getConfiguration().getProxyService(proxyName);
                    if (proxy != null) {
                        StatisticsConfigurable configurable = proxy.getAspectConfiguration();
                        statisticsEnable =
                                configurable != null && configurable.isStatisticsEnable();
                    }
                }
            }

            if (!statisticsEnable) {
                Endpoint endpoint = (Endpoint) synCtx.getProperty(
                        SynapseConstants.LAST_ENDPOINT);
                if (endpoint instanceof AbstractEndpoint) {
                    EndpointDefinition definition = ((AbstractEndpoint) endpoint).getDefinition();
                    statisticsEnable = definition != null && definition.isStatisticsEnable();
                }
            }
        }

        if (statisticsEnable) {
            AspectConfiguration configuration = new AspectConfiguration(
                    SynapseConstants.SYNAPSE_ASPECTS);
            configuration.enableStatistics();
            return configuration;
        }
        return null;
    }
}
