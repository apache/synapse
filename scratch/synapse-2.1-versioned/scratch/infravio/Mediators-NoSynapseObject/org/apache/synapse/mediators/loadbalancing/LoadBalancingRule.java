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

package org.apache.synapse.mediators.loadbalancing;

public class LoadBalancingRule {

    public LoadBalancingRule() {
    }

    private String service;
    private String active;
    private long avgResponseTime;
    private long lastResponseTime;
    private long requestCount;

    public long getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(long avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public long getLastResponseTime() {
        return lastResponseTime;
    }

    public void setLastResponseTime(long lastResponseTime) {
        this.lastResponseTime = lastResponseTime;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }

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

    public void updateValues(long responseTime){

        this.setLastResponseTime(responseTime);
        long count = this.getRequestCount();
        long newAverage = (count*(this.getAvgResponseTime()) + responseTime)/count;
        this.setRequestCount(++count);
        this.setAvgResponseTime(newAverage);
    }


    public String toString() {
        StringBuffer buf = new StringBuffer(1024);
        buf.append("Rule:").append("\n");
        buf.append("Service : ").append(getService()).append("\n");
        buf.append("Active : ").append(getActive()).append("\n");
        buf.append("AvgResponseTime : ").append(getAvgResponseTime()).append("\n");
        buf.append("LastResponseTime : ").append(getLastResponseTime()).append("\n");
        buf.append("Requests : ").append(getRequestCount()).append("\n");
        return buf.toString();
    }

}
