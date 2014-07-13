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
package org.apache.axis2.transport.nhttp;

import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.safehaus.asyncweb.container.basic.BasicServiceContainer;
import org.safehaus.asyncweb.container.ContainerLifecycleException;
import org.safehaus.asyncweb.transport.nio.HttpIOHandler;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class AsyncHTTPListener implements TransportListener {

    private static final Log log = LogFactory.getLog(AsyncHTTPListener.class);

    private ConfigurationContext cfgCtx = null;
    private BasicServiceContainer svcCont = null;
    private int port = 8080;
    private int ioWorkerCount = 2;
    private int maxKeepAlives = 100;
    private int readIdleTime = 300;
    private String hostAddress = null;
    private String serviceContextPath;

    public AsyncHTTPListener() {
    }

    public AsyncHTTPListener(ConfigurationContext cfgCtx, int port) throws AxisFault {
        this.cfgCtx = cfgCtx;
        this.port = port;
        TransportInDescription httpDescription = new TransportInDescription(
                new QName(Constants.TRANSPORT_HTTP));
        httpDescription.setReceiver(this);

        ListenerManager listenerManager = cfgCtx.getListenerManager();
        if (listenerManager == null) {
            listenerManager = new ListenerManager();
            listenerManager.init(cfgCtx);
        }
        cfgCtx.getListenerManager().addListener(httpDescription, true);
    }

    public void init(ConfigurationContext cfgCtx, TransportInDescription transprtIn)
            throws AxisFault {
        this.cfgCtx = cfgCtx;
        serviceContextPath = cfgCtx.getServiceContextPath();

        try {
            Parameter param = transprtIn.getParameter(PARAM_PORT);
            if (param != null)
                port = Integer.parseInt((String) param.getValue());

            param = transprtIn.getParameter(HOST_ADDRESS);
            if (param != null)
                hostAddress = ((String) param.getValue()).trim();
            else
                hostAddress = java.net.InetAddress.getLocalHost().getHostName();

        } catch (Exception e1) {
            throw new AxisFault(e1);
        }
    }

    public void start() throws AxisFault {
        List svcHandlers = new ArrayList();
        svcHandlers.add(
                new org.apache.axis2.transport.nhttp.HttpServiceHandler(cfgCtx, port));

        NIOTransport nioTransport = new NIOTransport();
        nioTransport.setPort(port);
        nioTransport.setIoWorkerCount(ioWorkerCount);
        HttpIOHandler httpIOHandler = new HttpIOHandler();
        httpIOHandler.setMaxKeepAlives(maxKeepAlives);
        httpIOHandler.setReadIdleTime(readIdleTime);

        nioTransport.setHttpIOHandler(httpIOHandler);
        List transports = new ArrayList();
        transports.add(nioTransport);

        svcCont = new BasicServiceContainer();
        svcCont.setServiceHandlers(svcHandlers);
        svcCont.setTransports(transports);

        try {
            log.debug("Starting AsyncHTTPListener on port : " + port + "...");
            svcCont.start();
            log.info("Started AsyncHTTPListener on port : " + port);
        } catch (ContainerLifecycleException e) {
            throw new AxisFault("Error starting Async HTTP listener on port : "
                                + port + " : " + e.getMessage(), e);
        }
    }

    public void stop() throws AxisFault {
        svcCont.stop();
        log.info("Async HTTP protocol listener shut down");
    }

    public EndpointReference getEPRForService(String serviceName, String ip) throws AxisFault {
        return new EndpointReference(
                "http://" + hostAddress + ":" + port + serviceContextPath + "/" + serviceName);
    }

    //TODO This should handle other endpoints too. Ex: If a rest call has made 
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        EndpointReference[] endpointReferences = new EndpointReference[1];
        endpointReferences[0] = new EndpointReference(
                "http://" + hostAddress + ":" + port + "/" + serviceContextPath + "/" + serviceName);
        return endpointReferences;
    }
}
