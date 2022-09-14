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

package org.apache.synapse.transport.passthru.jmx;

import org.apache.axis2.AxisFault;
import org.apache.synapse.transport.passthru.PassThroughHttpListener;
import org.apache.synapse.transport.passthru.PassThroughHttpSender;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class TransportView implements TransportViewMBean {

    private PassThroughHttpListener listener = null;

    private PassThroughHttpSender sender = null;

    private PassThroughTransportMetricsCollector metrics = null;

    private ThreadPoolExecutor threadPool = null;

    public TransportView(PassThroughHttpListener listener,
                         PassThroughHttpSender sender,
                         PassThroughTransportMetricsCollector metrics,
                         ThreadPoolExecutor threadPool) throws AxisFault {
        this.listener = listener;
        this.metrics = metrics;
        this.threadPool = threadPool;
        this.sender = sender;
    }

    @Override
    public void pause() throws AxisFault {
        if (listener != null) {
            listener.pause();
        } else if (sender != null) {
            sender.pause();
        }
    }

    @Override
    public void resume() throws AxisFault {
        if (listener != null) {
            listener.resume();
        } else if (sender != null) {
            sender.resume();
        }
    }

    @Override
    public void maintenenceShutdown(long l) throws AxisFault {
        if (listener != null) {
            listener.maintenanceShutdown(l);
        } else if (sender != null) {
            sender.maintenanceShutdown(l);
        }
    }

    @Override
    public int getActiveThreadCount() {
        if (threadPool != null) {
            return threadPool.getActiveCount();
        }
        return 0;
    }

    @Override
    public int getQueueSize() {
        if (threadPool != null && threadPool.getQueue() != null) {
            return threadPool.getQueue().size();
        }
        return 0;
    }

    @Override
    public long getMessagesReceived() {
        if (metrics != null) {
            return metrics.getMessagesReceived();
        }
        return -1;
    }

    @Override
    public long getFaultsReceiving() {
        if (metrics != null) {
            return metrics.getFaultsReceiving();
        }
        return -1;
    }

    @Override
    public long getBytesReceived() {
        if (metrics != null) {
            return metrics.getBytesReceived();
        }
        return -1;
    }

    @Override
    public long getMessagesSent() {
        if (metrics != null) {
            return metrics.getMessagesSent();
        }
        return -1;
    }

    @Override
    public long getFaultsSending() {
        if (metrics != null) {
            return metrics.getFaultsSending();
        }
        return -1;
    }

    @Override
    public long getBytesSent() {
        if (metrics != null) {
            return metrics.getBytesSent();
        }
        return -1;
    }

    @Override
    public long getTimeoutsReceiving() {
        if (metrics != null) {
            return metrics.getTimeoutsReceiving();
        }
        return -1;
    }

    @Override
    public long getTimeoutsSending() {
        if (metrics != null) {
            return metrics.getTimeoutsSending();
        }
        return -1;
    }

    @Override
    public long getMinSizeReceived() {
        if (metrics != null) {
            return metrics.getMinSizeReceived();
        }
        return -1;
    }

    @Override
    public long getMaxSizeReceived() {
        if (metrics != null) {
            return metrics.getMaxSizeReceived();
        }
        return -1;
    }

    @Override
    public double getAvgSizeReceived() {
        if (metrics != null) {
            return metrics.getAvgSizeReceived();
        }
        return -1;
    }

    @Override
    public long getMinSizeSent() {
        if (metrics != null) {
            return metrics.getMinSizeSent();
        }
        return -1;
    }

    @Override
    public long getMaxSizeSent() {
        if (metrics != null) {
            return metrics.getMaxSizeSent();
        }
        return -1;
    }

    @Override
    public double getAvgSizeSent() {
        if (metrics != null) {
            return metrics.getAvgSizeSent();
        }
        return -1;
    }

    @Override
    public Map getResponseCodeTable() {
        if (metrics != null) {
            return metrics.getResponseCodeTable();
        }
        return null;
    }

    @Override
    public void start() throws Exception {
        if (listener != null) {
            listener.start();
        }
    }

    @Override
    public void stop() throws Exception {
        if (listener != null) {
            listener.stop();
        }
    }

    @Override
    public void resetStatistics() {
        if (metrics != null) {
            metrics.reset();
        }
    }

    @Override
    public long getLastResetTime() {
        if (metrics != null) {
            return metrics.getLastResetTime();
        }
        return -1;
    }

    @Override
    public long getMetricsWindow() {
        if (metrics != null) {
            return System.currentTimeMillis() - metrics.getLastResetTime();
        }
        return -1;
    }
}