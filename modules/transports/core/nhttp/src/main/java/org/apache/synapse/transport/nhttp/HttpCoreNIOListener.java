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

package org.apache.synapse.transport.nhttp;

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
import org.apache.axis2.transport.base.*;
import org.apache.axis2.transport.base.threads.NativeThreadFactory;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.synapse.transport.nhttp.util.NhttpMetricsCollector;
import org.apache.synapse.transport.utils.conn.logging.LoggingUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * NIO transport listener for Axis2 based on HttpCore and NIO extensions
 */
public class HttpCoreNIOListener implements TransportListener, ManagementSupport {

    private static final Log log = LogFactory.getLog(HttpCoreNIOListener.class);
    /** The IOReactor */
    private DefaultListeningIOReactor ioReactor = null;

    /** The EPR prefix for services available over this transport */
    private String serviceEPRPrefix;
    /** The EPR prefix for services with custom URI available over this transport */
    private String customEPRPrefix;
    /** The custom URI map for the services if there are any */
    private Map<String, String> serviceNameToEPRMap = new HashMap<String, String>();
    /** The service name map for the custom URI if there are any */
    private Map<String, String> eprToServiceNameMap = new HashMap<String, String>();
    /** the axis observer that gets notified of service life cycle events*/
    private final AxisObserver axisObserver = new GenericAxisObserver();
    /** SSLContext if this listener is a SSL listener */
    private SSLContext sslContext = null;
    /** The SSL session handler that manages client authentication etc */
    private SSLSetupHandler sslSetupHandler = null;
    /** JMX support */
    private TransportMBeanSupport mbeanSupport;
    /** state of the listener */
    private volatile int state = BaseConstants.STOPPED;
    /** The ServerHandler */
    private ServerHandler handler = null;
    /** Listener configurations */
    private ListenerContext listenerContext;
    /** Metrics */
    private NhttpMetricsCollector metrics = null;

    protected IOEventDispatch getEventDispatch(
        NHttpServerEventHandler handler, SSLContext sslContext,
        SSLSetupHandler setupHandler, ConnectionConfig config) {
        return LoggingUtils.getServerIODispatch(handler, config);
    }

    /**
     * Initialize the transport listener, and execute reactor in new separate thread
     * @param cfgCtx the Axis2 configuration context
     * @param transportIn the description of the http/s transport from Axis2 configuration
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportInDescription transportIn)
            throws AxisFault {

        cfgCtx.setProperty(NhttpConstants.EPR_TO_SERVICE_NAME_MAP, eprToServiceNameMap);

        // is this an SSL listener?
        sslContext = getSSLContext(transportIn);
        sslSetupHandler = getSSLIOSessionHandler(transportIn);

        listenerContext = new ListenerContext(cfgCtx, transportIn, sslContext != null);
        listenerContext.build();

        metrics = listenerContext.getMetrics();

        Parameter param = transportIn.getParameter(NhttpConstants.WSDL_EPR_PREFIX);
        if (param != null) {
            serviceEPRPrefix = getServiceEPRPrefix(cfgCtx, (String) param.getValue());
            customEPRPrefix = (String) param.getValue();
        } else {
            serviceEPRPrefix = getServiceEPRPrefix(cfgCtx, listenerContext.getHost(), listenerContext.getPort());
            customEPRPrefix = transportIn.getName() + "://" + listenerContext.getHost() +
                    ":" + (listenerContext.getPort() == 80 ? "" : listenerContext.getPort()) + "/";
        }

        // register to receive updates on services for lifetime management
        cfgCtx.getAxisConfiguration().addObservers(axisObserver);

        // register with JMX
        mbeanSupport
            = new TransportMBeanSupport(this, "nio-" + transportIn.getName());
        mbeanSupport.register();
    }

    public int getActiveConnectionsSize() {
        return handler.getActiveConnectionsSize();
    }


    /**
     * Return the EPR prefix for services made available over this transport
     * @param cfgCtx configuration context to retrieve the service context path
     * @param host name of the host
     * @param port listening port
     * @return wsdlEPRPrefix for the listener
     */
    protected String getServiceEPRPrefix(ConfigurationContext cfgCtx, String host, int port) {
        return "http://" + host + (port == 80 ? "" : ":" + port) +
            (!cfgCtx.getServiceContextPath().startsWith("/") ? "/" : "") +
            cfgCtx.getServiceContextPath() +
            (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
    }

    /**
     * Return the EPR prefix for services made available over this transport
     * @param cfgCtx configuration context to retrieve the service context path
     * @param wsdlEPRPrefix specified wsdlPrefix
     * @return wsdlEPRPrefix for the listener
     */
    protected String getServiceEPRPrefix(ConfigurationContext cfgCtx, String wsdlEPRPrefix) {
        return wsdlEPRPrefix +
            (!cfgCtx.getServiceContextPath().startsWith("/") ? "/" : "") +
            cfgCtx.getServiceContextPath() +
            (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
    }


    /**
     * Create the SSLContext to be used by this listener
     * @param transportIn transport in description
     * @return always null
     * @throws AxisFault never thrown
     */
    protected SSLContext getSSLContext(TransportInDescription transportIn) throws AxisFault {
        return null;
    }

    /**
     * Create the SSL IO Session handler to be used by this listener
     * @param transportIn transport in descritption
     * @return always null
     * @throws AxisFault never thrown
     */
    protected SSLSetupHandler getSSLIOSessionHandler(TransportInDescription transportIn)
        throws AxisFault {
        return null;
    }

    /**
     * Start the transport listener. This method returns when the listener is ready to
     * accept connections.
     * @throws AxisFault
     */
    public void start() throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Starting Listener...");
        }
        
        // configure the IO reactor on the specified port
        try {
            String prefix = (sslContext == null ? "http" : "https") + "-Listener I/O dispatcher";
            ioReactor = new DefaultListeningIOReactor(
                listenerContext.getReactorConfig(),
                new NativeThreadFactory(new ThreadGroup(prefix + " thread group"), prefix));

            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: IOReactor encountered a checked exception : "
                            + ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: IOReactor encountered a runtime exception : "
                            + runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });
        } catch (IOException e) {
            handleException("Error starting the IOReactor", e);
        }

        ConfigurationContext cfgCtx = listenerContext.getCfgCtx();

        for (Object obj : cfgCtx.getAxisConfiguration().getServices().values()) {
            addToServiceURIMap((AxisService) obj);
        }

        handler = new ServerHandler(listenerContext);
        final IOEventDispatch ioEventDispatch = getEventDispatch(handler,
                sslContext, sslSetupHandler, listenerContext.getConnectionConfig());
        state = BaseConstants.STARTED;

        listenerContext.getHttpGetRequestProcessor().init(cfgCtx, handler);

        ListenerEndpoint endpoint;
        try {
            if (listenerContext.getBindAddress() == null) {
                endpoint = ioReactor.listen(new InetSocketAddress(listenerContext.getPort()));
            } else {
                endpoint = ioReactor.listen(new InetSocketAddress(
                    InetAddress.getByName(listenerContext.getBindAddress()), listenerContext.getPort()));
            }
        } catch (IOException e) {
            handleException("Encountered an I/O error: " + e.getMessage(), e);
            return;
        }
        
        // start the IO reactor in a new separate thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (InterruptedIOException ex) {
                    log.fatal("Reactor Interrupted", ex);
                } catch (IOException e) {
                    log.fatal("Encountered an I/O error: " + e.getMessage(), e);
                } catch (Exception e) {
                    log.fatal("Unexpected exception in I/O reactor", e);
                }
                log.info((sslContext == null ? "HTTP" : "HTTPS") + " Listener Shutdown");
            }
        }, "HttpCoreNIOListener");

        t.start();
        
        // Wait for the endpoint to become ready, i.e. for the listener to start accepting
        // requests.
        try {
            endpoint.waitFor();
        } catch (InterruptedException e) {
            log.warn("HttpCoreNIOListener#start() was interrupted");
        }

        log.info((sslContext == null ? "HTTP" : "HTTPS") + " Listener started on" +
                (listenerContext.getBindAddress() != null ? " address : " + listenerContext.getBindAddress() : "") +
                " port : " + listenerContext.getPort());
    }

    private void addToServiceURIMap(AxisService service) {
        Parameter param = service.getParameter(NhttpConstants.SERVICE_URI_LOCATION);
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

    /**
     * Stop the listener
     * @throws AxisFault on error
     */
    public void stop() throws AxisFault {
        if (state == BaseConstants.STOPPED) return;
        try {
            ioReactor.shutdown();
            handler.stop();
            state = BaseConstants.STOPPED;
            for (Object obj : listenerContext.getCfgCtx().getAxisConfiguration().getServices().values()) {
                removeServiceFromURIMap((AxisService) obj);
            }
        } catch (IOException e) {
            handleException("Error shutting down IOReactor", e);
        }
    }

    /**
     * Pause the listener - Stops accepting new connections, but continues processing existing
     * connections until they complete. This helps bring an instance into a maintenance mode
     * @throws AxisFault
     */
    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            ioReactor.pause();
            handler.markActiveConnectionsToBeClosed();
            state = BaseConstants.PAUSED;
            log.info((sslContext == null ? "HTTP" : "HTTPS") + " Listener Paused");
        } catch (IOException e) {
            handleException("Error pausing IOReactor", e);
        }
    }

    /**
     * Resume the lister - Brings the lister into active mode back from a paused state
     * @throws AxisFault
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
     * Returns the number of active threads processing messages
     * @return number of active threads processing messages
     */
    public int getActiveThreadCount() {
        return handler.getActiveCount();
    }

    /**
     * Returns the number of requests queued in the thread pool
     * @return queue size
     */
    public int getQueueSize() {
        return handler.getQueueSize();
    }

    /**
     * Stop accepting new connections, and wait the maximum specified time for in-flight
     * requests to complete before a controlled shutdown for maintenance
     *
     * @param millis a number of milliseconds to wait until pending requests are allowed to complete
     * @throws AxisFault
     */
    public void maintenenceShutdown(long millis) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            ioReactor.pause();
            ioReactor.shutdown(millis);
            state = BaseConstants.STOPPED;
            log.info("Listener shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            handleException("Error shutting down the IOReactor for maintenance", e);
        }
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
                    customEPRPrefix + serviceNameToEPRMap.get(serviceName) + trailer);
        } else {
            if (serviceEPRPrefix == null) {
                return null;
            }
            endpointReferences[0] = new EndpointReference(serviceEPRPrefix + serviceName + trailer);
        }
        return endpointReferences;
    }

    /**
     * TODO: Return session context from transport, this is an improvement in axis2 1.2 and
     * is not currently supported
     * @param messageContext context to be used
     * @return always null
     */
    public SessionContext getSessionContext(MessageContext messageContext) {
        return null;
    }

    public void destroy() {
        ioReactor = null;
        listenerContext.getCfgCtx().getAxisConfiguration().getObserversList().remove(axisObserver);
        mbeanSupport.unregister();
        metrics.destroy();
    }

    /**
     * An AxisObserver which will start listening for newly deployed or started services,
     * and stop listening when services are undeployed or stopped.
     */
    class GenericAxisObserver implements AxisObserver {

        // The initilization code will go here
        public void init(AxisConfiguration axisConfig) {
        }

        public void serviceUpdate(AxisEvent event, AxisService service) {

            if (!ignoreService(service)
                    && BaseUtils.isUsingTransport(service, listenerContext.getTransportIn().getName())) {
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
        public void addParameter(Parameter param) throws AxisFault {}
        public void removeParameter(Parameter param) throws AxisFault {}
        public void deserializeParameters(OMElement parameterElement) throws AxisFault {}
        public Parameter getParameter(String name) { return null; }
        public ArrayList<Parameter> getParameters() { return null; }
        public boolean isParameterLocked(String parameterName) { return false; }
        public void serviceGroupUpdate(AxisEvent event, AxisServiceGroup serviceGroup) {}
    }

    private boolean ignoreService(AxisService service) {
        // these are "private" services
        return service.getName().startsWith("__") || JavaUtils.isTrueExplicitly(
                service.getParameter(NhttpConstants.HIDDEN_SERVICE_PARAM_NAME));
    }

    // -------------- utility methods -------------
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    // -- jmx/management methods--
    public long getMessagesReceived() {
        if (metrics != null) {
            return metrics.getMessagesReceived();
        }
        return -1;
    }

    public long getFaultsReceiving() {
        if (metrics != null) {
            return metrics.getFaultsReceiving();
        }
        return -1;
    }

    public long getBytesReceived() {
        if (metrics != null) {
            return metrics.getBytesReceived();
        }
        return -1;
    }

    public long getMessagesSent() {
        if (metrics != null) {
            return metrics.getMessagesSent();
        }
        return -1;
    }

    public long getFaultsSending() {
        if (metrics != null) {
            return metrics.getFaultsSending();
        }
        return -1;
    }

    public long getBytesSent() {
        if (metrics != null) {
            return metrics.getBytesSent();
        }
        return -1;
    }

    public long getTimeoutsReceiving() {
        if (metrics != null) {
            return metrics.getTimeoutsReceiving();
        }
        return -1;
    }

    public long getTimeoutsSending() {
        if (metrics != null) {
            return metrics.getTimeoutsSending();
        }
        return -1;
    }

    public long getMinSizeReceived() {
        if (metrics != null) {
            return metrics.getMinSizeReceived();
        }
        return -1;
    }

    public long getMaxSizeReceived() {
        if (metrics != null) {
            return metrics.getMaxSizeReceived();
        }
        return -1;
    }

    public double getAvgSizeReceived() {
        if (metrics != null) {
            return metrics.getAvgSizeReceived();
        }
        return -1;
    }

    public long getMinSizeSent() {
        if (metrics != null) {
            return metrics.getMinSizeSent();
        }
        return -1;
    }

    public long getMaxSizeSent() {
        if (metrics != null) {
            return metrics.getMaxSizeSent();
        }
        return -1;
    }

    public double getAvgSizeSent() {
        if (metrics != null) {
            return metrics.getAvgSizeSent();
        }
        return -1;
    }

    public Map getResponseCodeTable() {
        if (metrics != null) {
            return metrics.getResponseCodeTable();
        }
        return null;
    }

    public void resetStatistics() {
        if (metrics != null) {
            metrics.reset();
        }
    }

    public long getLastResetTime() {
        if (metrics != null) {
            return metrics.getLastResetTime();
        }
        return -1;
    }

    public long getMetricsWindow() {
        if (metrics != null) {
            return System.currentTimeMillis() - metrics.getLastResetTime();
        }
        return -1;
    }
}
