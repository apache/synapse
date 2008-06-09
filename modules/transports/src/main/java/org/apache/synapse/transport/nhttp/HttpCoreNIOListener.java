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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.management.*;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.SSLIOSessionHandler;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.synapse.transport.base.ManagementSupport;
import org.apache.synapse.transport.base.MetricsCollector;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.TransportView;

/**
 * NIO transport listener for Axis2 based on HttpCore and NIO extensions
 */
public class HttpCoreNIOListener implements TransportListener, ManagementSupport {

    private static final Log log = LogFactory.getLog(HttpCoreNIOListener.class);

    /** The Axis2 configuration context */
    private ConfigurationContext cfgCtx;
    /** The IOReactor */
    private DefaultListeningIOReactor ioReactor = null;

    /** The EPR prefix for services available over this transport */
    private String serviceEPRPrefix;
    /** The port to listen on, defaults to 8280 */
    private int port = 8280;
    /** The hostname to use, defaults to localhost */
    private String host = "localhost";
    /** The bind addresses as (address, port) pairs */
    private String bindAddress = null;
    /** SSLContext if this listener is a SSL listener */
    private SSLContext sslContext = null;
    /** The SSL session handler that manages client authentication etc */
    private SSLIOSessionHandler sslIOSessionHandler = null;
    /** Metrics collector for this transport */
    private MetricsCollector metrics = new MetricsCollector();
    /** state of the listener */
    private int state = BaseConstants.STOPPED;
    /** The ServerHandler */
    private ServerHandler handler = null;

    /**
     * configure and start the IO reactor on the specified port
     * @param port port to start the listener on
     */
    private void startServerEngine(int port) {
        HttpParams params = getServerParameters();
        try {
            ioReactor = new DefaultListeningIOReactor(
                NHttpConfiguration.getInstance().getServerIOWorkers(), params);

            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: IOReactor encountered a checked exception : " +
                        ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: IOReactor encountered a runtime exception : " +
                        runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });
        } catch (IOException e) {
            log.error("Error starting the IOReactor", e);
        }

        handler = new ServerHandler(cfgCtx, params, sslContext != null, metrics);
        IOEventDispatch ioEventDispatch = getEventDispatch(
            handler, sslContext, sslIOSessionHandler, params);
        state = BaseConstants.STARTED;
        try {
            if (bindAddress == null) {
                ioReactor.listen(new InetSocketAddress(port));
            } else {
                ioReactor.listen(new InetSocketAddress(
                    InetAddress.getByName(bindAddress), port));
            }
            ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
            log.fatal("Reactor Interrupted");
        } catch (IOException e) {
            log.fatal("Encountered an I/O error: " + e.getMessage(), e);
        }
        log.info((sslContext == null ? "HTTP" : "HTTPS") + " Listener Shutdown");
    }

    protected IOEventDispatch getEventDispatch(
        NHttpServiceHandler handler, SSLContext sslContext, 
        SSLIOSessionHandler sslioSessionHandler, HttpParams params) {
        return new PlainServerIOEventDispatch(handler, params);
    }

    /**
     * get HTTP protocol parameters to which the listener must adhere to
     * @return the applicable HTTP protocol parameters
     */
    private HttpParams getServerParameters() {
        HttpParams params = new BasicHttpParams();
        NHttpConfiguration cfg = NHttpConfiguration.getInstance();
        params
            .setIntParameter(HttpConnectionParams.SO_TIMEOUT,
                cfg.getProperty(HttpConnectionParams.SO_TIMEOUT, 60000))
            .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                cfg.getProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024))
            .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK,
                cfg.getProperty(HttpConnectionParams.STALE_CONNECTION_CHECK, 0) == 1)
            .setBooleanParameter(HttpConnectionParams.TCP_NODELAY,
                cfg.getProperty(HttpConnectionParams.TCP_NODELAY, 1) == 1)
            .setParameter(HttpProtocolParams.ORIGIN_SERVER, "Synapse-HttpComponents-NIO");
        return params;
    }

    /**
     * Initialize the transport listener, and execute reactor in new seperate thread
     * @param cfgCtx the Axis2 configuration context
     * @param transprtIn the description of the http/s transport from Axis2 configuration
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportInDescription transprtIn) throws AxisFault {

        this.cfgCtx = cfgCtx;
        Parameter param = transprtIn.getParameter(PARAM_PORT);
        if (param != null) {
            port = Integer.parseInt((String) param.getValue());
        }

        param = transprtIn.getParameter(NhttpConstants.BIND_ADDRESS);
        if (param != null) {
            bindAddress = ((String) param.getValue()).trim();
        }

        param = transprtIn.getParameter(HOST_ADDRESS);
        if (param != null) {
            host = ((String) param.getValue()).trim();
        } else {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("Unable to lookup local host name, using 'localhost'");
            }
        }

        // is this an SSL listener?
        sslContext = getSSLContext(transprtIn);
        sslIOSessionHandler = getSSLIOSessionHandler(transprtIn);

        param = transprtIn.getParameter(NhttpConstants.WSDL_EPR_PREFIX);
        if (param != null) {
            serviceEPRPrefix = getServiceEPRPrefix(cfgCtx, (String) param.getValue());
        } else {
            serviceEPRPrefix = getServiceEPRPrefix(cfgCtx, host, port);
        }

        // register with JMX
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String jmxAgentName = System.getProperty("jmx.agent.name");
        if (jmxAgentName == null || "".equals(jmxAgentName)) {
            jmxAgentName = "org.apache.synapse";
        }
        String name;
        try {
            name = jmxAgentName + ":Type=Transport,ConnectorName=" +
                "nio-http" + (sslContext == null ? "" : "s") + "-listener";
            TransportView tBean = new TransportView(this, null);
            registerMBean(mbs, tBean, name);
        } catch (Exception e) {
            log.warn("Error registering the non-blocking http" +
                (sslContext == null ? "" : "s") + " transport for JMX management", e);
        }
    }

    /**
     * Return the EPR prefix for services made available over this transport
     * @return
     */
    protected String getServiceEPRPrefix(ConfigurationContext cfgCtx, String host, int port) {
        return "http://" + host + (port == 80 ? "" : ":" + port) +
            (!cfgCtx.getServiceContextPath().startsWith("/") ? "/" : "") +
            cfgCtx.getServiceContextPath() +
            (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
    }

    /**
     * Return the EPR prefix for services made available over this transport
     * @return
     */
    protected String getServiceEPRPrefix(ConfigurationContext cfgCtx, String wsdlEPRPrefix) {
        return wsdlEPRPrefix +
            (!cfgCtx.getServiceContextPath().startsWith("/") ? "/" : "") +
            cfgCtx.getServiceContextPath() +
            (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
    }


    /**
     * Create the SSLContext to be used by this listener
     * @param transportIn
     * @return always null
     */
    protected SSLContext getSSLContext(TransportInDescription transportIn) throws AxisFault {
        return null;
    }

    /**
     * Create the SSL IO Session handler to be used by this listener
     * @param transportIn
     * @return always null
     */
    protected SSLIOSessionHandler getSSLIOSessionHandler(TransportInDescription transportIn)
        throws AxisFault {
        return null;
    }

    /**
     * Start the transport listener on a new thread
     * @throws AxisFault
     */
    public void start() throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Starting Listener...");
        }
        // start the Listener in a new seperate thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    startServerEngine(port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "HttpCoreNIOListener");

        t.start();
        log.info((sslContext == null ? "HTTP" : "HTTPS") + " Listener starting on" +
            (bindAddress != null ? " address : " + bindAddress : "") + " port : " + port);
    }

    /**
     * Stop the listener
     * @throws AxisFault on error
     */
    public void stop() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            ioReactor.shutdown();
            state = BaseConstants.STOPPED;
        } catch (IOException e) {
            handleException("Error shutting down IOReactor", e);
        }
    }

    /**
     * Pause the listener - Stops accepting new connections, but continues processing existing
     * connections until they complete. This helps bring an instance into a maintenence mode
     * @throws AxisFault
     */
    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            ioReactor.pause();
            state = BaseConstants.PAUSED;
            log.info("Listener paused");
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
            log.info("Listener resumed");
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
     * Returns the number of requestes queued in the thread pool
     * @return queue size
     */
    public int getQueueSize() {
        return handler.getQueueSize();
    }

    /**
     * Stop accepting new connections, and wait the maximum specified time for in-flight
     * requests to complete before a controlled shutdown for maintenence
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
            handleException("Error shutting down the IOReactor for maintenence", e);
        }
    }


    /**
     * Return the EPR for the given service (implements deprecated method temporarily)
     */
    public EndpointReference getEPRForService(String serviceName, String ip) throws AxisFault {
        return new EndpointReference(serviceEPRPrefix + serviceName);
    }

    /**
     * Return the EPRs for the given service over this transport
     * @param serviceName name of the service
     * @param ip IP address
     * @return the EndpointReferences for this service over the transport
     * @throws AxisFault on error
     */
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        EndpointReference[] endpointReferences = new EndpointReference[1];
        endpointReferences[0] = new EndpointReference(serviceEPRPrefix + serviceName);
        return endpointReferences;
    }

    /**
     * TODO: Return session context from transport, this is an improvement in axis2 1.2 and
     * is not currently supported
     * @param messageContext
     * @return
     */
    public SessionContext getSessionContext(MessageContext messageContext) {
        return null;
    }

    public void destroy() {
        ioReactor = null;
    }

    // -------------- utility methods -------------
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
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

    private void registerMBean(MBeanServer mbs, Object mbeanInstance, String objectName) {
        try {
            ObjectName name = new ObjectName(objectName);
            Set set = mbs.queryNames(name, null);
            if (set != null && set.isEmpty()) {
                mbs.registerMBean(mbeanInstance, name);
            } else {
                mbs.unregisterMBean(name);
                mbs.registerMBean(mbeanInstance, name);
            }
        } catch (Exception e) {
            log.warn("Error registering a MBean with objectname ' " + objectName +
                " ' for JMX management", e);
        }
    }
}
