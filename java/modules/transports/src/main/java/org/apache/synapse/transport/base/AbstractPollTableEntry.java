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

package org.apache.synapse.transport.base;

import java.util.TimerTask;

import org.apache.axis2.addressing.EndpointReference;

public abstract class AbstractPollTableEntry {
    // status of last scan
    public static final int SUCCSESSFUL = 0;
    public static final int WITH_ERRORS = 1;
    public static final int FAILED      = 2;
    public static final int NONE        = 3;

    /** Axis2 service name */
    private String serviceName;
    /** next poll time */
    private long nextPollTime;
    /** last poll performed at */
    private long lastPollTime;
    /** duration in ms between successive polls */
    private long pollInterval;
    /** state of the last poll */
    private int lastPollState;
    /** The timer task that will trigger the next poll */
    TimerTask timerTask;
    /** Flag indicating whether polling has been canceled. */
    boolean canceled;
    
    public String getServiceName() {
        return serviceName;
    }

    void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public abstract EndpointReference getEndpointReference();

    public long getNextPollTime() {
        return nextPollTime;
    }

    public void setNextPollTime(long nextPollTime) {
        this.nextPollTime = nextPollTime;
    }

    public long getLastPollTime() {
        return lastPollTime;
    }

    public void setLastPollTime(long lastPollTime) {
        this.lastPollTime = lastPollTime;
    }

    public long getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(long pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getLastPollState() {
        return lastPollState;
    }

    public void setLastPollState(int lastPollState) {
        this.lastPollState = lastPollState;
    }
}
