/*
 *
 * Copyright ©2002-2005 Infravio, Inc. All rights reserved.
 *
 * Infravio PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *   This software is the confidential and proprietary information of Infravio, Inc
 *   ("Confidential Information").  You shall not disclose such Confidential 
 *   Information and shall use it only in accordance with the terms of the license  
 *   agreement you entered into with Infravio.
 *
 *
 */

package org.apache.synapse.mediators.failover;

public class FailoverRule {

    public FailoverRule() {
    }

    private String service;
    private String active;
    private String primary;
    private long timeout;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(1024);
        buf.append("Rule:").append("\n");
        buf.append("Service : ").append(getService()).append("\n");
        buf.append("Active : ").append(getActive()).append("\n");
        buf.append("Primary : ").append(getPrimary()).append("\n");
        buf.append("Timeout : ").append(getTimeout()).append("\n");
        return buf.toString();
    }

}
