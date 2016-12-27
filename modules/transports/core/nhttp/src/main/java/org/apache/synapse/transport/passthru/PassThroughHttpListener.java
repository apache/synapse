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

package org.apache.synapse.transport.passthru;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.transport.base.threads.NativeThreadFactory;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.jmx.TransportView;
import org.apache.synapse.transport.utils.conn.logging.LoggingUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the TransportListener listening for incoming connections. This class start the IOReactor
 * and registers the InRequest Handlers on it.
 */
public class PassThroughHttpListener implements TransportListener {

    protected Log log = LogFactory.getLog(this.getClass());

    /** The reactor being used */
    private DefaultListeningIOReactor ioReactor;

    /** The configuration of the listener */
    private SourceConfiguration sourceConfiguration = null;

    /** SSLContext if this listener is a SSL listener */
    private SSLContext sslContext = null;

    /** The SSL session handler that manages client authentication etc */
    private SSLSetupHandler sslSetupHandler = null;

    /** The custom URI map for the services if there are any */
    private Map<String, String> serviceNameToEPRMap = new HashMap<String, String>();

    /** The service name map for the custom URI if there are any */
    private Map<String, String> eprToServiceNameMap = new HashMap<String, String>();

    /** the axis observer that gets notified of service life cycle events*/
    private final AxisObserver axisObserver = new GenericAxisObserver();

    private volatile int state = BaseConstants.STOPPED;

    private String namePrefix;

    public void init(ConfigurationContext cfgCtx, TransportInDescription transportInDescription)
            throws AxisFault {

        if (log.isDebugEnabled()) {
            log.debug("Initializing pass-through HTTP/S Listener...");
        }

        int portOffset = Integer.parseInt(System.getProperty("portOffset", "0"));
        Parameter portParam = transportInDescription.getParameter("port");
        int port = Integer.parseInt(portParam.getValue().toString());
        port = port + portOffset;
        portParam.setValue(String.valueOf(port));
        portParam.getParameterElement().setText(String.valueOf(port));

        Object obj = cfgCtx.getProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL);
        WorkerPool workerPool = null;
        if (obj != null) {
            workerPool = (WorkerPool) obj;
        }

        // is this a SSL listener?
        sslContext = getSSLContext(transportInDescription);
        sslSetupHandler = getSSLSetupHandler(transportInDescription);
        namePrefix = (sslContext == null) ? "HTTP" : "HTTPS";

        sourceConfiguration = new SourceConfiguration(cfgCtx, transportInDescription,
                workerPool, sslContext != null);

        // register to receive updates on services for lifetime management
        cfgCtx.getAxisConfiguration().addObservers(axisObserver);
        cfgCtx.setProperty(PassThroughConstants.EPR_TO_SERVICE_NAME_MAP, eprToServiceNameMap);

        cfgCtx.setProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL,
                sourceConfiguration.getWorkerPool());

        PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(
                                                            true, sslContext != null);

        MBeanRegistrar.getInstance().registerMBean(
                new TransportView(this, null, metrics, null), "Transport",
                "passthru-" + namePrefix.toLowerCase() + "-receiver");
        sourceConfiguration.setMetrics(metrics);
    }

    public void start() throws AxisFault {
        log.info("Starting pass-through " + namePrefix + " listener...");

        try {
            String prefix = namePrefix + "-PT-Listener I/O Dispatcher";
            ioReactor = new DefaultListeningIOReactor(
                            sourceConfiguration.getReactorConfig(true),
                            new NativeThreadFactory(new ThreadGroup(prefix + " Thread Group"), prefix));
            
            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {

                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: " + namePrefix +
                            " ListeningIOReactor encountered a checked exception." , ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: " + namePrefix +
                            " ListeningIOReactor encountered a runtime exception.", runtimeException);
                    return true;
                }
            });

        } catch (IOReactorException e) {
            handleException("Error starting " + namePrefix + " ListeningIOReactor", e);
        }

        SourceHandler handler = new SourceHandler(sourceConfiguration);
        final IOEventDispatch ioEventDispatch = getEventDispatch(handler, sslContext,
                sslSetupHandler, sourceConfiguration.getConnectionConfig());

        ListenerEndpoint endpoint;
        if (sourceConfiguration.getBindAddress() != null) {
            try {
                endpoint = ioReactor.listen(new InetSocketAddress(
                        InetAddress.getByName(sourceConfiguration.getBindAddress()),
                        sourceConfiguration.getPort()));
            } catch (UnknownHostException e) {
                handleException("Failed to resolve the bind address: " +
                        sourceConfiguration.getBindAddress(), e);
                return;
            }
        } else {
            endpoint = ioReactor.listen(new InetSocketAddress(sourceConfiguration.getPort()));
        }
        HttpGetRequestProcessor getProcessor = sourceConfiguration.getHttpGetRequestProcessor();
        if (getProcessor != null){
           getProcessor.init(sourceConfiguration.getConfigurationContext(), handler);
        }

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (Exception e) {
                    log.fatal("Exception encountered in the " + namePrefix + " listener. " +
                            "No more connections will be accepted by this transport.", e);
                }
                log.info(namePrefix + " listener shutdown.");
            }
        }, "PassThrough" + namePrefix + "Listener");
        t.start();

        try {
            endpoint.waitFor();
        } catch (InterruptedException e) {
            log.warn("Pass-through " + namePrefix + " listener startup was interrupted", e);
        }

        state = BaseConstants.STARTED;
        log.info("Pass-through " + namePrefix + " listener " + "started on port: " +
                sourceConfiguration.getPort());
    }

    private void handleException(String s, Exception e) throws AxisFault {
        log.error(s, e);
        throw new AxisFault(s, e);
    }

    /**
     * Return the EPRs for the given service over this transport
     * @param serviceName name of the service
     * @param ip IP address
     * @return the EndpointReferences for this service over the transport
     * @throws AxisFault on error
     */
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        String trailer = "";
        //Strip out the operation name
        if (serviceName.indexOf('/') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("/"));
            serviceName = serviceName.substring(0, serviceName.indexOf('/'));
        }
        // strip out the endpoint name if present
        if (serviceName.indexOf('.') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("."));
            serviceName = serviceName.substring(0, serviceName.indexOf('.'));
        }

        EndpointReference[] endpointReferences = new EndpointReference[1];
        if (serviceNameToEPRMap.containsKey(serviceName)) {
            endpointReferences[0] = new EndpointReference(
                    sourceConfiguration.getCustomEPRPrefix() +
                            serviceNameToEPRMap.get(serviceName) + trailer);
        } else {
            endpointReferences[0]
                    = new EndpointReference(sourceConfiguration.getServiceEPRPrefix() +
                    serviceName + trailer);
        }
        return endpointReferences;
    }

    public SessionContext getSessionContext(MessageContext messageContext) {
        return null;
    }

    public void stop() throws AxisFault {
        log.info("Stopping pass-through " + namePrefix + " listener..");
        try {
            ioReactor.shutdown();
        } catch (IOException e) {
            handleException("Error shutting down " + namePrefix + " listening IO reactor", e);
        }
    }

    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying pass-through " + namePrefix + " listener");
        }
        ioReactor = null;
        sourceConfiguration.getConfigurationContext().
                getAxisConfiguration().getObserversList().remove(axisObserver);

        MBeanRegistrar.getInstance().unRegisterMBean("Transport",
                "passthru-" + namePrefix.toLowerCase() + "-receiver");
        sourceConfiguration.getMetrics().destroy();
    }

    /**
     * Pause the listener - Stops accepting new connections, but continues processing existing
     * connections until they complete. This helps bring an instance into a maintenance mode
     *
     * @throws AxisFault if pausing fails
     */
    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            ioReactor.pause();

            state = BaseConstants.PAUSED;
            log.info(namePrefix + " Listener Paused");
        } catch (IOException e) {
            handleException("Error pausing IOReactor", e);
        }
    }

    /**
     * Resume the lister - Brings the lister into active mode back from a paused state
     *
     * @throws AxisFault if the resume fails
     */
    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) return;
        try {
            ioReactor.resume();
            state = BaseConstants.STARTED;
            log.info((sslContext == null ? "HTTP" : "HTTPS") + "Listener Resumed");
        } catch (IOException e) {
            handleException("Error resuming IOReactor", e);
        }
    }

    /**
     * Stop accepting new connections, and wait the maximum specified time for in-flight
     * requests to complete before a controlled shutdown for maintenance
     *
     * @param milliSecs number of milliseconds to wait until pending requests complete
     * @throws AxisFault if the shutdown fails
     */
    public void maintenanceShutdown(long milliSecs) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            ioReactor.pause();
            ioReactor.shutdown(milliSecs);
            state = BaseConstants.STOPPED;
            log.info("Listener shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            handleException("Error shutting down the IOReactor for maintenance", e);
        }
    }

    /**
     * An AxisObserver which will start listening for newly deployed or started services,
     * and stop listening when services are un-deployed or stopped.
     */
    private class GenericAxisObserver implements AxisObserver {
        public void init(AxisConfiguration axisConfig) {}

        public void serviceUpdate(AxisEvent event, AxisService service) {
            if (!ignoreService(service) && BaseUtils.isUsingTransport(
                    service, sourceConfiguration.getTransportName())) {
                switch (event.getEventType()) {
                    case AxisEvent.SERVICE_DEPLOY :
                        addToServiceURIMap(service);
                        break;
                    case AxisEvent.SERVICE_REMOVE :
                        removeServiceFromURIMap(service);
                        break;
                    case AxisEvent.SERVICE_START  :
                        addToServiceURIMap(service);
                        break;
                    case AxisEvent.SERVICE_STOP   :
                        removeServiceFromURIMap(service);
                        break;
                }
            }
        }

        public void moduleUpdate(AxisEvent event, AxisModule module) {}
        public void addParameter(Parameter parameter) throws AxisFault {}
        public void removeParameter(Parameter parameter) throws AxisFault {}
        public void deserializeParameters(OMElement parameterElement) throws AxisFault {}
        public Parameter getParameter(String name) { return null; }
        public ArrayList<Parameter> getParameters() { return null; }
        public boolean isParameterLocked(String parameterName) { return false; }
        public void serviceGroupUpdate(AxisEvent event, AxisServiceGroup serviceGroup) {}
    }

    private boolean ignoreService(AxisService service) {
        // these are "private" services
        return service.getName().startsWith("__") || JavaUtils.isTrueExplicitly(
                service.getParameter(PassThroughConstants.HIDDEN_SERVICE_PARAM_NAME));
    }

    private void addToServiceURIMap(AxisService service) {
        Parameter param = service.getParameter(PassThroughConstants.SERVICE_URI_LOCATION);
        if (param != null) {
            String uriLocation = param.getValue().toString();
            if (uriLocation.startsWith("/")) {
                uriLocation = uriLocation.substring(1);
            }
            serviceNameToEPRMap.put(service.getName(), uriLocation);
            eprToServiceNameMap.put(uriLocation, service.getName());
        }
    }

    private void removeServiceFromURIMap(AxisService service) {
        eprToServiceNameMap.remove(serviceNameToEPRMap.get(service.getName()));
        serviceNameToEPRMap.remove(service.getName());
    }

    protected IOEventDispatch getEventDispatch(
            NHttpServerEventHandler handler, SSLContext sslContext,
            SSLSetupHandler sslSetupHandler, ConnectionConfig config) {
        return LoggingUtils.getServerIODispatch(handler, config);
    }

    /**
     * Create the SSLContext to be used by this listener
     * @param transportIn transport in description
     * @return always null
     * @throws AxisFault never thrown
     */
    protected SSLContext getSSLContext(
            TransportInDescription transportIn) throws AxisFault {
        return null;
    }

    /**
     * Create the SSL IO Session handler to be used by this listener
     *
     * @param transportOut Transport out description
     * @return always null
     * @throws AxisFault on error
     */
    protected SSLSetupHandler getSSLSetupHandler(TransportInDescription transportOut)
            throws AxisFault {
        return null;
    }

}
