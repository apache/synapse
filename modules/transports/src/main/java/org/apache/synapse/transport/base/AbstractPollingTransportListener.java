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

import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;

import java.util.TimerTask;
import java.util.Timer;
import java.util.Map;
import java.util.HashMap;

public abstract class AbstractPollingTransportListener extends AbstractTransportListener {

    /** the parameter in the services.xml that specifies the poll interval for a service */
    public static final String TRANSPORT_POLL_INTERVAL = "transport.PollInterval";
    /** the default poll interval */
    public static final int DEFAULT_POLL_INTERVAL = 5 * 60; // 5 mins by default

    /** default interval in ms before polls */
    protected int pollInterval = DEFAULT_POLL_INTERVAL;
    /** The main timer that runs as a daemon thread */
    protected final Timer timer = new Timer("PollTimer", true);
    /** is a poll already executing? */
    protected boolean pollInProgress = false;
    /** a lock to prevent concurrent execution of polling */
    protected final Object pollLock = new Object();
    /** a map that keeps track of services to the timer tasks created for them */
    protected Map serviceToTimerTaskMap = new HashMap();

    /**
     * Schedule a repeated poll at the specified interval for the given service
     * @param service the service to be polled
     * @param pollInterval the interval between successive polls in seconds
     */
    public void schedulePoll(AxisService service, long pollInterval) {
        pollInterval *= 1000; // convert to millisecs
        
        TimerTask task = (TimerTask) serviceToTimerTaskMap.get(service);

        // if a timer task exists, cancel it first and create a new one
        if (task != null) {
            task.cancel();
        }

        task = new TimerTask() {
            public void run() {
                if (pollInProgress) {
                    if (log.isDebugEnabled()) {
                        log.debug("Transport " + transportName +
                                " onPoll() trigger : already executing poll..");
                    }
                    return;
                }

                workerPool.execute(new Runnable() {
                    public void run() {
                        synchronized (pollLock) {
                            pollInProgress = true;
                            try {
                                onPoll();
                            } finally {
                                pollInProgress = false;
                            }
                        }
                    }
                });
            }
        };
        serviceToTimerTaskMap.put(service, task);
        timer.scheduleAtFixedRate(task, pollInterval, pollInterval);
    }

    /**
     * Cancel any pending timer tasks for the given service
     * @param service the service for which the timer task should be cancelled
     */
    public void cancelPoll(AxisService service) {
        TimerTask task = (TimerTask) serviceToTimerTaskMap.get(service);
        if (task != null) {
            task.cancel();
        }
    }

    public void onPoll() {}

    protected void startListeningForService(AxisService service) {
        Parameter param = service.getParameter(TRANSPORT_POLL_INTERVAL);
        long pollInterval = DEFAULT_POLL_INTERVAL;
        if (param != null && param.getValue() instanceof String) {
            try {
                pollInterval = Integer.parseInt(param.getValue().toString());
            } catch (NumberFormatException e) {
                log.error("Invalid poll interval : " + param.getValue() + " for service : " +
                    service.getName() + " Using defaults", e);
            }
        }
        schedulePoll(service, pollInterval);
    }

    protected void stopListeningForService(AxisService service) {
        cancelPoll(service);
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }
}
