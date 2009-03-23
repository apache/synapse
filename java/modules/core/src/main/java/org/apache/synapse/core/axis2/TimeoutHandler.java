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

package org.apache.synapse.core.axis2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.statistics.StatisticsCleaner;
import org.apache.synapse.aspects.statistics.StatisticsCollector;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.endpoints.dispatch.SALSessions;

import java.util.Map;
import java.util.Stack;
import java.util.TimerTask;

/**
 * An object of this class is registered to be invoked in some predefined time intervals. This
 * checks the timeouts of callbacks stored in the SynapseCallbackReceiver and removes all expired
 * callbacks. Timeouts of the callbacks are stored as the time, not the duration. So that the
 * time or the interval of invoking this class does not affect the correctness of the timeouts,
 * although longer intervals would introduce larger error between the actual timeout and the
 * specified timeout.
 *
 * For each invocation this gets a time value to be compared against the timeouts of the callback
 * objects. This time is the System.currentTimeMillis() for Java 1.4 and System.nanoTime() for
 * Java 1.5 and later.
 */
public class TimeoutHandler extends TimerTask {

    private static final Log log = LogFactory.getLog(TimeoutHandler.class);

    /** The callback map - already a Collections.synchronized() hash map */
    private final Map callbackStore;
    /** a lock to prevent concurrent execution while ensuring least overhead */
    private final Object lock = new Object();
    private boolean alreadyExecuting = false;
    /*This is the timeout for otherwise non-expiring callbacks to ensure system stability over time */
    private long globalTimeout = SynapseConstants.DEFAULT_GLOBAL_TIMEOUT;
    private static final String SEND_TIMEOUT_MESSAGE = "Send timeout";
    private StatisticsCleaner statisticsCleaner;

    public TimeoutHandler(Map callbacks) {
        this.callbackStore = callbacks;
        this.globalTimeout = SynapseConfigUtils.getGlobalTimeoutInterval();
        log.info("This engine will expire all callbacks after : " + (globalTimeout / 1000) +
                " seconds, irrespective of the timeout action," +
                " after the specified or optional timeout");
    }

    /**
     * Checks if the timeout has expired for each callback in the callback store. If expired, removes
     * the callback. If specified sends a fault message to the client about the timeout.
     */
    public void run() {
        if (alreadyExecuting) return;

        synchronized(lock) {
            alreadyExecuting = true;
            try {
                processCallbacks();
            } catch (Exception ignore) {}
            alreadyExecuting = false;
        }
    }

    private void processCallbacks() {

        //clear the expired statistics
        if (statisticsCleaner == null) {
            StatisticsCollector collector = SynapseConfigUtils.getStatisticsCollector();
            if (collector != null) {
                statisticsCleaner = new StatisticsCleaner(collector);
            }
        }
        if (statisticsCleaner != null) {
            statisticsCleaner.clean();
        }

        //clear all the expired sessions
        SALSessions.getInstance().clearSessions();

        // checks if callback store contains at least one entry before proceeding. otherwise getting
        // the time for doing nothing would be a inefficient task.

        // we have to synchronize this on the callbackStore as iterators of thread safe collections
        // are not thread safe. callbackStore can be modified
        // concurrently by the SynapseCallbackReceiver.
        synchronized(callbackStore) {

            if (callbackStore.size() > 0) {

                long currentTime = currentTime();

                for (Object key : callbackStore.keySet()) {

                    AsyncCallback callback = (AsyncCallback) callbackStore.get(key);
                    if (callback == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("There is no callback for key :" + key);
                        }
                        continue;
                    }
                    if (callback.getTimeOutAction() != SynapseConstants.NONE) {

                        if (callback.getTimeOutOn() <= currentTime) {
                            callbackStore.remove(key);

                            if (callback.getTimeOutAction() == SynapseConstants.DISCARD_AND_FAULT) {

                                // actiavte the fault sequence of the current sequence mediator
                                MessageContext msgContext = callback.getSynapseOutMsgCtx();

                                // add an error code to the message context, so that error sequences
                                // can identify the cause of error
                                msgContext.setProperty(SynapseConstants.ERROR_CODE,
                                        SynapseConstants.HANDLER_TIME_OUT);
                                msgContext.setProperty(SynapseConstants.ERROR_MESSAGE,
                                        SEND_TIMEOUT_MESSAGE);

                                Stack faultStack = msgContext.getFaultStack();

                                for (int j = 0; j < faultStack.size(); j++) {
                                    Object o = faultStack.pop();
                                    if (o instanceof FaultHandler) {
                                        ((FaultHandler) o).handleFault(msgContext);
                                    }
                                }

                            }
                        }

                    } else if (currentTime > globalTimeout + callback.getTimeOutOn()) {
                        log.warn("Expiring message ID : " + key + "; dropping message after " +
                                "global timeout of : " + (globalTimeout / 1000) + " seconds");
                        callbackStore.remove(key);
                    }
                }
            }
        }
    }

    /**
     * Returns the current time.
     *
     * @return  System.currentTimeMillis() on Java 1.4
     *          System.nanoTime() on Java 1.5 (todo: implement)
     */
    private long currentTime() {
        return System.currentTimeMillis();
    }
}
