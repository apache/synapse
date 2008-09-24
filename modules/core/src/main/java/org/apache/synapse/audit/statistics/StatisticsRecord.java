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
import org.apache.synapse.Mediator;
import org.apache.synapse.audit.AuditConfigurable;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds a record for statistics for current message
 */
public class StatisticsRecord {

    private static final Log log = LogFactory.getLog(StatisticsRecord.class);
    private String id;

    private final Map<String, StatisticsLog> endPointsStatisticsRecordMap =
            new HashMap<String, StatisticsLog>();
    private final Map<String, StatisticsLog> mediatorsStatisticsRecordMap =
            new HashMap<String, StatisticsLog>();
    private final Map<String, StatisticsLog> proxyServicesStatisticsRecordMap =
            new HashMap<String, StatisticsLog>();

    private boolean isFaultResponse;
    private String clientIP;
    private String clientHost;

    public StatisticsRecord(String id, String clientIP, String clientHost) {
        this.id = id;
        this.clientIP = clientIP;
        this.clientHost = clientHost;
    }

    public String getId() {
        return id;
    }

    public boolean isFaultResponse() {
        return isFaultResponse;
    }

    public String getClientIP() {
        return clientIP;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setFaultResponse(boolean faultResponse) {
        isFaultResponse = faultResponse;
    }

    /**
     * Collecting statistics for a particular component
     *
     * @param auditConfigurable audit configurable component
     */
    public void collect(AuditConfigurable auditConfigurable) {

        if (isValid(auditConfigurable)) {

            String auditID = auditConfigurable.getAuditId();
            if (log.isDebugEnabled()) {
                log.debug("Start to collect statistics for : " + auditID);
            }
            if (auditConfigurable instanceof Endpoint) {
                endPointsStatisticsRecordMap.put(auditID, new StatisticsLog(auditID));
            } else if (auditConfigurable instanceof ProxyService) {
                proxyServicesStatisticsRecordMap.put(auditID, new StatisticsLog(auditID));
            } else if (auditConfigurable instanceof Mediator) {
                mediatorsStatisticsRecordMap.put(auditID, new StatisticsLog(auditID));
            }
        }
    }

    /**
     * Reporting statistics for a particular component
     *
     * @param auditConfigurable audit configurable component
     */
    public void commit(AuditConfigurable auditConfigurable) {

        if (isValid(auditConfigurable)) {

            String auditID = auditConfigurable.getAuditId();
            if (log.isDebugEnabled()) {
                log.debug("Reporting statistics for : " + auditID);
            }
            if (auditConfigurable instanceof Endpoint) {
                commit(auditID, endPointsStatisticsRecordMap);
            } else if (auditConfigurable instanceof ProxyService) {
                commit(auditID, proxyServicesStatisticsRecordMap);
            } else if (auditConfigurable instanceof Mediator) {
                commit(auditID, mediatorsStatisticsRecordMap);
            }
        }
    }

    private boolean isValid(AuditConfigurable auditConfigurable) {

        if (auditConfigurable == null) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid audit configuration , It is null.");
            }
            return false;
        }

        if (auditConfigurable.isStatisticsEnable()) {
            String auditID = auditConfigurable.getAuditId();
            if (auditID == null || "".equals(auditID)) {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid audit configuration , Audit name is null.");
                }
                return false;
            }
            return true;
        }
        return false;
    }

    private void commit(String auditID, Map<String, StatisticsLog> map) {
        StatisticsLog log = map.get(auditID);
        if (log != null) {
            log.setEndTime(System.currentTimeMillis());
        }
    }

    public StatisticsLog getEndpointStatisticsRecord(String name) {
        return endPointsStatisticsRecordMap.get(name);
    }

    public StatisticsLog getMediatorStatisticsRecord(String name) {
        return mediatorsStatisticsRecordMap.get(name);
    }

    public StatisticsLog getProxyServiceStatisticsRecord(String name) {
        return proxyServicesStatisticsRecordMap.get(name);
    }

    public Map<String, StatisticsLog> getAllEndpointStatisticsRecords() {
        return endPointsStatisticsRecordMap;
    }

    public Map<String, StatisticsLog> getAllMediatorStatisticsRecords() {
        return mediatorsStatisticsRecordMap;
    }

    public Map<String, StatisticsLog> getAllProxyServiceStatisticsRecords() {
        return proxyServicesStatisticsRecordMap;
    }

    public String toString() {
        return new StringBuffer()
                .append("[Message id : ").append(id).append(" ]")
                .append("[Remote  IP : ").append(clientIP).append(" ]")
                .append("[Remote host : ").append(clientHost).append(" ]")
                .toString();
    }
}
