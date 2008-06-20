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
package org.apache.synapse.transport.base;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.synapse.transport.base.threads.WorkerPool;
import org.apache.synapse.transport.base.threads.WorkerPoolFactory;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.AxisObserver;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.om.OMElement;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.*;
import java.lang.management.ManagementFactory;

public abstract class AbstractTransportListener implements TransportListener {

    /** the reference to the actual commons logger to be used for log messages */
    protected Log log = null;

    /** the axis2 configuration context */
    protected ConfigurationContext cfgCtx = null;

    /** transport in description */
    private TransportInDescription  transportIn  = null;
    /** transport out description */
    private TransportOutDescription transportOut = null;
    /** state of the listener */
    protected int state = BaseConstants.STOPPED;
    /** is this transport non-blocking? */
    protected boolean isNonBlocking = false;
    /** the axis observer that gets notified of service life cycle events*/
    private final AxisObserver axisObserver = new GenericAxisObserver();

    /** the thread pool to execute actual poll invocations */
    protected WorkerPool workerPool = null;
    /** use the thread pool available in the axis2 configuration context */
    protected boolean useAxis2ThreadPool = false;
    /** JMX support */
    private TransportMBeanSupport mbeanSupport;
    /** Metrics collector for this transport */
    protected MetricsCollector metrics = new MetricsCollector();

    /**
     * A constructor that makes subclasses pick up the correct logger
     */
    protected AbstractTransportListener() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * Initialize the generic transport. Sets up the transport and the thread pool to be used
     * for message processing. Also creates an AxisObserver that gets notified of service
     * life cycle events for the transport to act on
     * @param cfgCtx the axis configuration context
     * @param transportIn the transport-in description
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportInDescription transportIn)
        throws AxisFault {
        
        this.cfgCtx = cfgCtx;
        this.transportIn  = transportIn;
        this.transportOut = cfgCtx.getAxisConfiguration().getTransportOut(getTransportName());

        if (useAxis2ThreadPool) {
            //this.workerPool = cfgCtx.getThreadPool(); not yet implemented
            throw new AxisFault("Unsupported thread pool for task execution - Axis2 thread pool");
        } else {
            this.workerPool = WorkerPoolFactory.getWorkerPool(
            10, 20, 5, -1, getTransportName() + "Server Worker thread group", getTransportName() + "-Worker");
        }

        // register to receive updates on services for lifetime management
        cfgCtx.getAxisConfiguration().addObservers(axisObserver);

        // register with JMX
        mbeanSupport = new TransportMBeanSupport(this, getTransportName());
        mbeanSupport.register();
    }

    public void destroy() {
        try {
            if (state == BaseConstants.STARTED) {
                try {
                    stop();
                } catch (AxisFault ignore) {
                    log.warn("Error stopping the transport : " + getTransportName());
                }
            }
        } finally {
            state = BaseConstants.STOPPED;
            mbeanSupport.unregister();
        }
        try {
            workerPool.shutdown(10000);
        } catch (InterruptedException ex) {
            log.warn("Thread interrupted while waiting for worker pool to shut down");
        }
    }

    public void stop() throws AxisFault {
        if (state == BaseConstants.STARTED) {
            state = BaseConstants.STOPPED;
            // cancel receipt of service lifecycle events
            cfgCtx.getAxisConfiguration().getObserversList().remove(axisObserver);
            log.info(getTransportName().toUpperCase() + " Listener Shutdown");
            for (AxisService service : getListeningServices()) {
                internalStopListeningForService(service);
            }
        }
    }

    public void start() throws AxisFault {
        if (state != BaseConstants.STARTED) {
            state = BaseConstants.STARTED;
            // register to receive updates on services for lifetime management
            // cfgCtx.getAxisConfiguration().addObservers(axisObserver);
            log.info(getTransportName().toUpperCase() + " Listener started");
            // iterate through deployed services and start
            for (AxisService service : getListeningServices()) {
                internalStartListeningForService(service);
            }
        }
    }
    
    /**
     * Get the list of services that are listening on this transport, i.e. that are
     * configured to use this transport.
     * 
     * @return the list of listening services
     */
    private List<AxisService> getListeningServices() {
        List<AxisService> result = new LinkedList<AxisService>();
        Iterator services = cfgCtx.getAxisConfiguration().getServices().values().iterator();
        while (services.hasNext()) {
            AxisService service = (AxisService) services.next();
            if (!ignoreService(service)
                    && BaseUtils.isUsingTransport(service, getTransportName())) {
                result.add(service);
            }
        }
        return result;
    }
    
    private boolean ignoreService(AxisService service) {
        return service.getName().startsWith("__"); // these are "private" services
    }
    
    public void disableTransportForService(AxisService service) {

        log.warn("Disabling the " + getTransportName() + " transport for the service "
                + service.getName() + ", because it is not configured properly for the service");

        if (service.isEnableAllTransports()) {
            ArrayList<String> exposedTransports = new ArrayList<String>();
            for(Object obj: cfgCtx.getAxisConfiguration().getTransportsIn().values()) {
                String transportName = ((TransportInDescription) obj).getName();
                if (!transportName.equals(getTransportName())) {
                    exposedTransports.add(transportName);
                }
            }
            service.setExposedTransports(exposedTransports);
        } else {
            service.removeExposedTransport(getTransportName());
        }
    }

    private void internalStartListeningForService(AxisService service) {
        startListeningForService(service);
        String serviceName = service.getName();
        registerMBean(new TransportListenerEndpointView(this, serviceName),
                      getEndpointMBeanName(serviceName));
    }

    private void internalStopListeningForService(AxisService service) {
        unregisterMBean(getEndpointMBeanName(service.getName()));
        stopListeningForService(service);
    }
    
    protected abstract void startListeningForService(AxisService service);

    protected abstract void stopListeningForService(AxisService service);

    /**
     * This is a deprecated method in Axis2 and this default implementation returns the first
     * result from the getEPRsForService() method
     */
    public EndpointReference getEPRForService(String serviceName, String ip) throws AxisFault {
        return getEPRsForService(serviceName, ip)[0];
    }

    public SessionContext getSessionContext(MessageContext messageContext) {
        return null;
    }

    /**
     * Create a new axis MessageContext for an incoming message through this transport
     * @return the newly created message context
     */
    public MessageContext createMessageContext() {
        MessageContext msgCtx = new MessageContext();
        msgCtx.setConfigurationContext(cfgCtx);

        msgCtx.setIncomingTransportName(getTransportName());
        msgCtx.setTransportOut(transportOut);
        msgCtx.setTransportIn(transportIn);
        msgCtx.setServerSide(true);
        msgCtx.setMessageID(UUIDGenerator.getUUID());

        // There is a discrepency in what I thought, Axis2 spawns a nes threads to
        // send a message is this is TRUE - and I want it to be the other way
        msgCtx.setProperty(MessageContext.TRANSPORT_NON_BLOCKING, Boolean.valueOf(!isNonBlocking));

        // are these relevant?
        //msgCtx.setServiceGroupContextId(UUIDGenerator.getUUID());
        // this is required to support Sandesha 2
        //msgContext.setProperty(RequestResponseTransport.TRANSPORT_CONTROL,
        //        new HttpCoreRequestResponseTransport(msgContext));

        return msgCtx;
    }

    /**
     * Process a new incoming message through the axis engine
     * @param msgCtx the axis MessageContext
     * @param trpHeaders the map containing transport level message headers
     * @param soapAction the optional soap action or null
     * @param contentType the optional content-type for the message
     */
    public void handleIncomingMessage(
        MessageContext msgCtx, Map trpHeaders,
        String soapAction, String contentType) throws AxisFault {

        // set the soapaction if one is available via a transport header
        if (soapAction != null) {
            msgCtx.setSoapAction(soapAction);
        }

        // set the transport headers to the message context
        msgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, trpHeaders);

        // send the message context through the axis engine
        try {
                try {
                    AxisEngine.receive(msgCtx);
                } catch (AxisFault e) {
                    e.printStackTrace();
                    if (log.isDebugEnabled()) {
                        log.debug("Error receiving message", e);
                    }
                    if (msgCtx.isServerSide()) {
                        AxisEngine.sendFault(MessageContextBuilder.createFaultMessageContext(msgCtx, e));
                    }
                }
        } catch (AxisFault axisFault) {
            logException("Error processing received message", axisFault);
            throw axisFault;
        }
    }

    protected void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    protected void logException(String msg, Exception e) {
        log.error(msg, e);
    }

    public String getTransportName() {
        return transportIn.getName();
    }

    public MetricsCollector getMetricsCollector() {
        return metrics;
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
                    && BaseUtils.isUsingTransport(service, getTransportName())) {
                switch (event.getEventType()) {
                    case AxisEvent.SERVICE_DEPLOY :
                        internalStartListeningForService(service);
                        break;
                    case AxisEvent.SERVICE_REMOVE :
                        internalStopListeningForService(service);
                        break;
                    case AxisEvent.SERVICE_START  :
                        internalStartListeningForService(service);
                        break;
                    case AxisEvent.SERVICE_STOP   :
                        internalStopListeningForService(service);
                        break;
                }
            }
        }

        public void moduleUpdate(AxisEvent event, AxisModule module) {}
        public void addParameter(Parameter param) throws AxisFault {}
        public void removeParameter(Parameter param) throws AxisFault {}
        public void deserializeParameters(OMElement parameterElement) throws AxisFault {}
        public Parameter getParameter(String name) { return null; }
        public ArrayList getParameters() { return null; }
        public boolean isParameterLocked(String parameterName) { return false; }
        public void serviceGroupUpdate(AxisEvent event, AxisServiceGroup serviceGroup) {}
    }

    // -- jmx/management methods--
    /**
     * Pause the listener - Stop accepting/processing new messages, but continues processing existing
     * messages until they complete. This helps bring an instance into a maintenence mode
     * @throws AxisFault on error
     */
    public void pause() throws AxisFault {}
    /**
     * Resume the lister - Brings the lister into active mode back from a paused state
     * @throws AxisFault on error
     */
    public void resume() throws AxisFault {}
    
    /**
     * Stop processing new messages, and wait the specified maximum time for in-flight
     * requests to complete before a controlled shutdown for maintenence
     *
     * @param millis a number of milliseconds to wait until pending requests are allowed to complete
     * @throws AxisFault on error
     */
    public void maintenenceShutdown(long millis) throws AxisFault {}

    /**
     * Returns the number of active threads processing messages
     * @return number of active threads processing messages
     */
    public int getActiveThreadCount() {
        return workerPool.getActiveCount();
    }

    /**
     * Return the number of requests queued in the thread pool
     * @return queue size
     */
    public int getQueueSize() {
        return workerPool.getQueueSize();
    }

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

    private String getEndpointMBeanName(String serviceName) {
        return mbeanSupport.getMBeanName() + ",Group=Services,Service=" + serviceName;
    }
    
    /**
     * Utility method to allow transports to register MBeans
     * @param mbeanInstance bean instance
     * @param objectName name
     */
    private void registerMBean(Object mbeanInstance, String objectName) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
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
    
    private void unregisterMBean(String objectName) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName objName = new ObjectName(objectName);
            if (mbs.isRegistered(objName)) {
                mbs.unregisterMBean(objName);
            }
        } catch (Exception e) {
            log.warn("Error un-registering a MBean with objectname ' " + objectName +
                " ' for JMX management", e);
        }
    }
}
