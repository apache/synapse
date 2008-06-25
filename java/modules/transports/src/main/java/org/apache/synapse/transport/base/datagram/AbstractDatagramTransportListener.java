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

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.base.AbstractTransportListener;
import org.apache.synapse.transport.base.ParamUtils;

public abstract class AbstractDatagramTransportListener<E extends DatagramEndpoint>
        extends AbstractTransportListener {
    
    private final Map<String,E> endpoints = new HashMap<String,E>();
	private DatagramDispatcher<E> dispatcher;
    private String defaultIp;
	
	@Override
    public void init(ConfigurationContext cfgCtx, TransportInDescription transportIn)
            throws AxisFault {
        
        super.init(cfgCtx, transportIn);
        DatagramDispatcherCallback callback = new DatagramDispatcherCallback() {
            public void receive(DatagramEndpoint endpoint, byte[] data, int length) {
                workerPool.execute(new ProcessPacketTask(endpoint, data, length));
            }
        };
        try {
            dispatcher = createDispatcher(callback);
        } catch (IOException ex) {
            throw new AxisFault("Unable to create selector", ex);
        }
        try {
            defaultIp = org.apache.axis2.util.Utils.getIpAddress(cfgCtx.getAxisConfiguration());
        } catch (SocketException ex) {
            throw new AxisFault("Unable to determine the host's IP address", ex);
        }
    }
	
    @Override
    protected void startListeningForService(AxisService service) {
        E endpoint;
        try {
        	endpoint = createEndpoint(service);
            endpoint.setListener(this);
            endpoint.setService(service);
            endpoint.setContentType(ParamUtils.getRequiredParam(
                    service, "transport." + getTransportName() + ".contentType"));
            endpoint.setMetrics(metrics);
        } catch (AxisFault ex) {
            log.warn("Error configuring the " + getTransportName()
                    + " transport for service '" + service.getName() + "': " + ex.getMessage());
            disableTransportForService(service);
            return;
        }
        
        try {
            dispatcher.addEndpoint(endpoint);
        } catch (IOException ex) {
            log.error("Unable to listen on endpoint "
                    + endpoint.getEndpointReference(defaultIp), ex);
            disableTransportForService(service);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Started listening on endpoint " + endpoint.getEndpointReference(defaultIp)
                    + " [contentType=" + endpoint.getContentType()
                    + "; service=" + service.getName() + "]");
        }
        endpoints.put(service.getName(), endpoint);
    }
    
    @Override
    protected void stopListeningForService(AxisService service) {
        try {
            dispatcher.removeEndpoint(endpoints.get(service.getName()));
        } catch (IOException ex) {
            log.error("I/O exception while stopping listener for service " + service.getName(), ex);
        }
        endpoints.remove(service.getName());
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

    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        E endpoint = endpoints.get(serviceName);
        if (endpoint == null) {
            return null;
        } else {
            return new EndpointReference[] {
                    endpoint.getEndpointReference(ip == null ? defaultIp : ip) };
        }
    }
    
	protected abstract DatagramDispatcher<E> createDispatcher(DatagramDispatcherCallback callback)
            throws IOException;
    
    protected abstract E createEndpoint(AxisService service) throws AxisFault;
}
