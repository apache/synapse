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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.statistics.StatisticsReporter;
import org.apache.synapse.config.xml.XMLConfigConstants;

/**
 * Contains helper methods required for auditing.
 * This class need to evolved as any audit related things are adding
 */
public class AspectHelper {

    private static final Log log = LogFactory.getLog(AspectHelper.class);

    /**
     * Sets the Global audit configuration if it has been forced by setting
     *
     * @param synCtx Current Message through synapse
     */
    public static void setGlobalAudit(MessageContext synCtx) {

        if (XMLConfigConstants.STATISTICS_ENABLE.equals(
                synCtx.getConfiguration().getProperty(SynapseConstants.SYNAPSE_AUDIT_STATE))) {
            
            AspectConfigurable aspectConfigurable = new AspectConfiguration(SynapseConstants.SYNAPSE_AUDIT, true);

            if (log.isDebugEnabled()) {
                log.debug("Global Audit is enabled. System-wide auditing will be occurred.");
            }

            StatisticsReporter.collect(synCtx, aspectConfigurable);
            synCtx.setProperty(SynapseConstants.SYNAPSE_AUDIT_CONFIGURATION,
                    aspectConfigurable);
        }
    }

    /**
     * Report Global audit for this message
     *
     * @param synCtx Current Message through synapse
     */
    public static void reportGlobalAudit(MessageContext synCtx) {

        AspectConfigurable aspectConfigurable = (AspectConfigurable) synCtx.getProperty(
                SynapseConstants.SYNAPSE_AUDIT_CONFIGURATION);
        
        if (aspectConfigurable != null) {
            if (log.isDebugEnabled()) {
                log.debug("System-wide aspects record is reported.");
            }
            StatisticsReporter.report(synCtx, aspectConfigurable);
        }
    }
}
