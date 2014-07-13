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
package org.apache.synapse.transport.udp;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.base.MetricsCollector;

/**
 * UDP endpoint description.
 * This class is used by the transport to store information
 * about an endpoint, i.e. a UDP port the transport listens on and that is
 * bound to a given Axis service.
 */
public class Endpoint {
    private final UDPListener listener;
    private final int port;
    private final String contentType;
    private final int maxPacketSize;
    private final AxisService service;
    private final MetricsCollector metrics;
    
    public Endpoint(UDPListener listener, int port, String contentType, int maxPacketSize, AxisService service, MetricsCollector metrics) {
        this.listener = listener;
        this.port = port;
        this.contentType = contentType;
        this.maxPacketSize = maxPacketSize;
        this.service = service;
        this.metrics = metrics;
    }

    public UDPListener getListener() {
        return listener;
    }

    public int getPort() {
        return port;
    }
    
    public String getContentType() {
        return contentType;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public AxisService getService() {
        return service;
    }

    public MetricsCollector getMetrics() {
        return metrics;
    }
    
    public EndpointReference getEndpointReference(String ip) {
        return new EndpointReference("udp://" + ip + ":" + getPort() + "?contentType=" + contentType);
    }
}
