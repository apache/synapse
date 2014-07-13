/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.synapse.transport.amqp.ha;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.amqp.AMQPTransportException;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactory;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactoryManager;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Responsible for handling the shutdown signals gracefully. For example
 * this provides the functionality for reconnecting to broker if broker
 * when offline. The reconnection attempts happens in exponential back-off
 * fashion.
 */
public class AMQPTransportReconnectHandler implements Runnable {

    private BlockingQueue<AMQPTransportHAEntry> blockedTasks =
            new LinkedBlockingQueue<AMQPTransportHAEntry>();

    private ConcurrentMap<String, AMQPTransportHABrokerEntry> connectionMap =
            new ConcurrentHashMap<String, AMQPTransportHABrokerEntry>();

    private AMQPTransportConnectionFactoryManager connectionFactoryManager;

    private int initialReconnectDuration = 1000;

    private double reconnectionProgressionFactor = 2.0;

    private int maxReconnectionDuration = 1000 * 60 * 10;

    private ExecutorService es;

    public AMQPTransportReconnectHandler(ExecutorService es,
                                         int maxReconnectionDuration,
                                         double reconnectionProgressionFactor,
                                         int initialReconnectDuration,
                                         AMQPTransportConnectionFactoryManager
                                                 connectionFactoryManager) {
        this.es = es;
        this.maxReconnectionDuration = maxReconnectionDuration;
        this.reconnectionProgressionFactor = reconnectionProgressionFactor;
        this.initialReconnectDuration = initialReconnectDuration;
        this.connectionFactoryManager = connectionFactoryManager;
    }

    private static Log log = LogFactory.getLog(AMQPTransportReconnectHandler.class);

    public void run() {
        try {
            AMQPTransportHAEntry entry = blockedTasks.take();
            if (entry != null) {
                Map<String, String> params = connectionFactoryManager.
                        getConnectionFactory(entry.getConnectionFactoryName()).getParameters();
                int count = 1;
                long retryDuration = initialReconnectDuration;

                while (true) {
                    try {
                        Thread.sleep(initialReconnectDuration);
                        new AMQPTransportConnectionFactory(params, es);
                        log.info("The reconnection attempt '" + count + "' was successful");
                        break;
                    } catch (AMQPTransportException e) {
                        retryDuration = (long) (retryDuration * reconnectionProgressionFactor);
                        if (retryDuration > maxReconnectionDuration) {
                            retryDuration = initialReconnectDuration;
                            log.info("The retry duration exceeded the maximum reconnection duration." +
                                    " The retry duration is set to initial reconnection duration " +
                                    "value(" + initialReconnectDuration + "s)");
                        }
                        log.info("The reconnection attempt number '" + count++ + "' failed. Next " +
                                "re-try will be after '" + (retryDuration / 1000) + "' seconds");
                        try {
                            Thread.sleep(retryDuration);
                        } catch (InterruptedException ignore) {
                            // we need to block
                        }
                    }
                }

                ConcurrentHashMap<String, AMQPTransportConnectionFactory> allFac =
                        connectionFactoryManager.getAllFactories();

                for (Map.Entry me : allFac.entrySet()) {
                    String name = (String) me.getKey();
                    Map<String, String> param = ((AMQPTransportConnectionFactory)
                            me.getValue()).getParameters();
                    connectionFactoryManager.removeConnectionFactory(name);
                    connectionFactoryManager.addConnectionFactory(
                            name, new AMQPTransportConnectionFactory(param, es));
                    log.info("A new connection factory was created for -> '" + name + "'");
                }

                String conFacName = entry.getConnectionFactoryName();
                AMQPTransportConnectionFactory cf = connectionFactoryManager.
                        getConnectionFactory(conFacName);
                connectionMap.put(
                        entry.getKey(),
                        new AMQPTransportHABrokerEntry(cf.getChannel(), cf.getConnection()));
                entry.getLock().release();

                while (!blockedTasks.isEmpty()) {
                    entry = blockedTasks.take();
                    conFacName = entry.getConnectionFactoryName();
                    cf = connectionFactoryManager.
                            getConnectionFactory(conFacName);
                    connectionMap.put(
                            entry.getKey(),
                            new AMQPTransportHABrokerEntry(cf.getChannel(), cf.getConnection()));
                    if (log.isDebugEnabled()) {
                        log.info("The worker task with key '" + entry.getKey() + "' was combined with " +
                                "a new connection factory");
                    }
                    entry.getLock().release();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (AMQPTransportException e) {
            log.error("High Availability handler just died!. It's time to reboot the system.", e);
        }
    }

    public BlockingQueue<AMQPTransportHAEntry> getBlockedTasks() {
        return blockedTasks;
    }

    public ConcurrentMap<String, AMQPTransportHABrokerEntry> getConnectionMap() {
        return connectionMap;
    }
}