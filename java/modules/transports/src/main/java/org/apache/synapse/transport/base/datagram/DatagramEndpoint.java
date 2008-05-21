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
package org.apache.synapse.transport.base.datagram;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.base.MetricsCollector;

/**
 * Endpoint description.
 * This class is used by the transport to store information
 * about an endpoint, e.g. the Axis service it is bound to.
 * Transports extend this abstract class to store additional
 * transport specific information, such as the port number
 * the transport listens on.
 */
public abstract class DatagramEndpoint {
    private AbstractDatagramTransportListener listener;
    private String contentType;
    private AxisService service;
    private MetricsCollector metrics;

    public AbstractDatagramTransportListener getListener() {
        return listener;
    }

    public void setListener(AbstractDatagramTransportListener listener) {
		this.listener = listener;
	}

	public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public AxisService getService() {
        return service;
    }

    public void setService(AxisService service) {
		this.service = service;
	}

	public MetricsCollector getMetrics() {
        return metrics;
    }

	public void setMetrics(MetricsCollector metrics) {
		this.metrics = metrics;
	}
	
	public abstract EndpointReference getEndpointReference(String ip);
}
