/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.axis2.transport.niohttp;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.niohttp.impl.*;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.util.threadpool.DefaultThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import java.io.OutputStreamWriter;
import java.io.IOException;

public class NHttpListener implements TransportListener, HttpService {

    private static final Log log = LogFactory.getLog(NHttpListener.class);

    private static final int WORKERS_MAX_THREADS = 40;
    private static final long WORKER_KEEP_ALIVE = 100L;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private ConfigurationContext cfgCtx;
    private String serviceEPRPrefix;
    private Reactor _reactor;
    private Executor workerPool = null;

    public NHttpListener() {}
    
    public NHttpListener(ConfigurationContext cfgCtx, int port) throws AxisFault {

        this.cfgCtx = cfgCtx;
        TransportInDescription httpDescription = new TransportInDescription(
            new QName(org.apache.axis2.Constants.TRANSPORT_HTTP));
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
        try {
            int port = 8080;
            String host;
            Parameter param = transprtIn.getParameter(PARAM_PORT);
            if (param != null)
                port = Integer.parseInt((String) param.getValue());

            param = transprtIn.getParameter(HOST_ADDRESS);
            if (param != null)
                host = ((String) param.getValue()).trim();
            else
                host = java.net.InetAddress.getLocalHost().getHostName();

            serviceEPRPrefix = "http://" + host + (port == 80 ? "" : ":" + port) +
                cfgCtx.getServiceContextPath() + "/";

            _reactor = Reactor.createReactor(host, port, false, this);

            workerPool = new ThreadPoolExecutor(
            1,
            WORKERS_MAX_THREADS, WORKER_KEEP_ALIVE, TIME_UNIT,
            new LinkedBlockingQueue(),
            new DefaultThreadFactory(
                    new ThreadGroup("HTTP Worker thread group"),
                    "HTTPWorker"));

        } catch (Exception e) {
            throw new AxisFault(e);
        }
    }

    public void start() throws AxisFault {
        log.debug("Starting IO Reactor...");
        new Thread(_reactor).start();
        log.info("IO Reactor started, accepting connections...");
    }

    public void stop() throws AxisFault {
        _reactor.setShutdownRequested(true);
        log.info("IO Reactor shut down");
    }

    public EndpointReference getEPRForService(String serviceName, String ip) throws AxisFault {
        return new EndpointReference(serviceEPRPrefix + serviceName);
    }

    //TODO This should handle other endpoints too. Ex: If a rest call has made
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        EndpointReference[] endpointReferences = new EndpointReference[1];
        endpointReferences[0] = new EndpointReference(serviceEPRPrefix + serviceName);
        return endpointReferences;
    }

    public void handleRequest(HttpRequest request) {

        log.debug("@@@@ Got new HTTP request : " + request.toStringLine());

        MessageContext msgContext = new MessageContext();
        msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTP);
        try {
            TransportOutDescription transportOut = cfgCtx.getAxisConfiguration()
                .getTransportOut(new QName(Constants.TRANSPORT_HTTP));
            TransportInDescription transportIn = cfgCtx.getAxisConfiguration()
                .getTransportIn(new QName(Constants.TRANSPORT_HTTP));

            msgContext.setConfigurationContext(cfgCtx);

            /* TODO session handling
            String sessionKey = request.getSession(true).getId();
            if (cfgCtx.getAxisConfiguration().isManageTransportSession()) {
                SessionContext sessionContext = sessionManager.getSessionContext(sessionKey);
                msgContext.setSessionContext(sessionContext);
            }*/

            msgContext.setTransportIn(transportIn);
            msgContext.setTransportOut(transportOut);
            msgContext.setServiceGroupContextId(UUIDGenerator.getUUID());
            msgContext.setServerSide(true);
            msgContext.setProperty(Constants.Configuration.TRANSPORT_IN_URL, request.getPath());

            msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, request.getHeaders());
            msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, request);

            workerPool.execute(new Worker(cfgCtx, msgContext, request));

        } catch (AxisFault e) {
            HttpResponse response = request.createResponse();

            try {
                AxisEngine engine = new AxisEngine(cfgCtx);
                msgContext.setProperty(MessageContext.TRANSPORT_OUT, response.getOutputStream());
                msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, response.getOutputStream());

                MessageContext faultContext = engine.createFaultMessageContext(msgContext, e);
                engine.sendFault(faultContext);

                response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);

            } catch (Exception ex) {
                response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
                response.addHeader(org.apache.axis2.transport.niohttp.impl.Constants.CONTENT_TYPE,
                    org.apache.axis2.transport.niohttp.impl.Constants.TEXT_PLAIN);
                OutputStreamWriter out = new OutputStreamWriter(
                    response.getOutputStream());
                try {
                    out.write(ex.getMessage());
                    out.close();
                } catch (IOException ee) {
                }
            }
            response.commit();
            return;
        }
    }

    public void handleResponse(HttpResponse response, Runnable callback) {

        log.debug("@@@@ Got new HTTP response : " + response.toStringLine());
        //callback.setResponse(response);
        callback.run();
    }

}
