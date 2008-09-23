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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.endpoints.Endpoint;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class StatisticsRecord {

    private static final Log log = LogFactory.getLog(StatisticsRecord.class);
    private String id;

    private final Map<String, StatisticsLog> endPointsStatisticsRecordMap =
            new HashMap<String, StatisticsLog>();
    private final Map<String, StatisticsLog> mediatorsStatisticsRecordMap =
            new HashMap<String, StatisticsLog>();
    private final Map<String, StatisticsLog> sequencesStatisticsRecordMap =
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

    public void collect(AuditConfigurable auditConfigurable) {
     
        if (isValid(auditConfigurable)) {

            String auditID = auditConfigurable.getAuditId();             
            if (auditConfigurable instanceof Endpoint) {
                endPointsStatisticsRecordMap.put(auditID, new StatisticsLog(auditID));
            }

        }
    }

    public void commit(AuditConfigurable auditConfigurable) {

        if (isValid(auditConfigurable)) {

            String auditID = auditConfigurable.getAuditId();
            if (auditConfigurable instanceof Endpoint) {
                StatisticsLog log = endPointsStatisticsRecordMap.get(auditID);
                if (log != null) {
                    log.setEndTime(System.currentTimeMillis());
                }
            }
        }
    }

    private boolean isValid(AuditConfigurable auditConfigurable) {

        if (auditConfigurable == null) {
            if(log.isDebugEnabled()){
                    log.debug("TODO");
                }
            return false;
        }

        if (auditConfigurable.isStatisticsEnable()) {
            String auditID = auditConfigurable.getAuditId();
            if (auditID == null || "".equals(auditID)) {
                if(log.isDebugEnabled()){
                    log.debug("TODO");
                }
                return false;
            }
            return true;
        }
        return false;
    }
    
    public StatisticsLog getEndpointStatisticsRecord(String name){
        return endPointsStatisticsRecordMap.get(name);
    }
}
