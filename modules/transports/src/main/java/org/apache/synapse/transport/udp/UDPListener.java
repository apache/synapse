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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.base.AbstractTransportListener;
import org.apache.synapse.transport.base.ManagementSupport;

/**
 * Transport listener for the UDP protocol.
 * Services accepting messages using this transport must be configured with the
 * following parameters:
 * <dl>
 *   <dt>transport.udp.port</dt>
 *   <dd>The UDP port to listen to (required).</dd>
 *   <dt>transport.udp.contentType</dt>
 *   <dd>The content type of the messages received (required). This setting
 *       is used to select the appropriate message builder.</dd>
 *   <dt>transport.udp.maxPacketSize</dt>
 *   <dd>The maximum packet size (optional; default 1024). Packets longer
 *       than the specified length will be truncated.</dd>
 * </dl>
 * 
 * @see org.apache.synapse.transport.udp
 */
public class UDPListener extends AbstractTransportListener implements ManagementSupport {
    private final Map<String,Endpoint> endpoints = new HashMap<String,Endpoint>();
    
    private IODispatcher dispatcher;
    
    @Override
    public void init(ConfigurationContext cfgCtx, TransportInDescription transportIn) throws AxisFault {
        setTransportName(UDPConstants.TRANSPORT_NAME);
        super.init(cfgCtx, transportIn);
        try {
            dispatcher = new IODispatcher(workerPool);
        } catch (IOException ex) {
            throw new AxisFault("Unable to create selector", ex);
        }
        // Start a new thread for the I/O dispatcher
        new Thread(dispatcher, getTransportName() + "-dispatcher").start();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            dispatcher.stop();
        } catch (IOException ex) {
            log.error("Failed to stop dispatcher", ex);
        }
    }

    @Override
    protected void startListeningForService(AxisService service) {
        Parameter param;
        
        int port;
        param = service.getParameter(UDPConstants.PORT_KEY);
        if (param == null) {
            log.info("No UDP port number specified for service " + service.getName() + "; disabling transport for this service");
            disableTransportForService(service);
            return;
        } else {
            try {
                port = Integer.parseInt(param.getValue().toString());
            }
            catch (NumberFormatException ex) {
                log.error("Invalid port number " + param.getValue() + " for service " + service.getName());
                disableTransportForService(service);
                return;
            }
        }
        
        int maxPacketSize = UDPConstants.DEFAULT_MAX_PACKET_SIZE;
        param = service.getParameter(UDPConstants.MAX_PACKET_SIZE_KEY);
        if (param != null) {
            try {
                maxPacketSize = Integer.parseInt(param.getValue().toString());
            }
            catch (NumberFormatException ex) {
                log.warn("Invalid maximum packet size; falling back to default value " + maxPacketSize);
            }
        }
        
        String contentType;
        param = service.getParameter(UDPConstants.CONTENT_TYPE_KEY);
        if (param == null) {
            log.info("No content type specified for service " + service.getName() + "; disabling transport for this service");
            disableTransportForService(service);
            return;
        } else {
            contentType = (String)param.getValue();
        }
        
        Endpoint endpoint = new Endpoint(this, port, contentType, maxPacketSize, service, metrics);
        try {
            dispatcher.addEndpoint(endpoint);
        } catch (IOException ex) {
            log.error("Unable to listen on port " + port, ex);
            disableTransportForService(service);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Started listening on port " + port + " [contentType=" + contentType + "; maxPacketSize=" + maxPacketSize + "; service=" + service.getName() + "]");
        }
        endpoints.put(service.getName(), endpoint);
    }

    @Override
    protected void stopListeningForService(AxisService service) {
        try {
            dispatcher.removeEndpoint(service.getName());
        } catch (IOException ex) {
            log.error("I/O exception while stopping listener for service " + service.getName(), ex);
        }
    }

    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        Endpoint endpoint = endpoints.get(serviceName);
        if (endpoint == null) {
            return null;
        } else {
            return new EndpointReference[] { endpoint.getEndpointReference(ip) };
        }
    }
}
