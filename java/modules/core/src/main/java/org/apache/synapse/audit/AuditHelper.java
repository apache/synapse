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
package org.apache.synapse.audit;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.audit.statatistics.StatisticsReporter;
import org.apache.synapse.config.xml.XMLConfigConstants;

/**
 *
 */
public class AuditHelper {

    public static void setGlobalAudit(MessageContext synCtx) {

        if (XMLConfigConstants.STATISTICS_ENABLE.equals(
                synCtx.getConfiguration().getProperty(SynapseConstants.SYNAPSE_STATISTICS_STATE))) {
            AuditConfigurable auditConfigurable = new AuditConfiguration(SynapseConstants.SYNAPSE_STATISTICS, true);
            StatisticsReporter.collect(synCtx, auditConfigurable);
            synCtx.setProperty(SynapseConstants.SYNAPSE_AUDIT_CONFIGURATION,
                    auditConfigurable);
        }
    }

    public static void reportGlobalAudit(MessageContext synCtx) {
        
        AuditConfigurable auditConfigurable = (AuditConfigurable) synCtx.getProperty(
                SynapseConstants.SYNAPSE_AUDIT_CONFIGURATION);
        if (auditConfigurable != null) {
            StatisticsReporter.report(synCtx, auditConfigurable);
        }
    }
}
