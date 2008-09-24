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

/**
 * Audit configuration
 * Currently contains only statistics configuration related things
 */
public class AuditConfiguration implements AuditConfigurable {

    /* Whether statistics enable */
    private boolean statisticsEnable = false;
    /* Identifier for a particular audit configuration */
    private String auditId;

    public AuditConfiguration(String auditId, boolean statisticsEnable) {
        this.statisticsEnable = statisticsEnable;
        this.auditId = auditId;
    }

    public AuditConfiguration(String auditId) {
        this.auditId = auditId;
    }

    public boolean isStatisticsEnable() {
        return statisticsEnable;
    }

    public void disableStatistics() {
        if (statisticsEnable) {
            this.statisticsEnable = false;
        }
    }

    public void enableStatistics() {
        if (!statisticsEnable) {
            statisticsEnable = true;
        }
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }
}
