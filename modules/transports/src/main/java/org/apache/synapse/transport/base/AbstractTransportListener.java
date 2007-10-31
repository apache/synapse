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

import java.util.*;

public abstract class AbstractTransportListener implements TransportListener {

    /** the reference to the actual commons logger to be used for log messages */
    protected Log log = null;

    /** the name of the transport */
    protected String transportName = null;
    /** the axis2 configuration context */
    protected ConfigurationContext cfgCtx = null;
    /** an axis2 engine over the above configuration context to process messages */
    protected AxisEngine engine = null;

    /** transport in description */
    private TransportInDescription  transportIn  = null;
    /** transport out description */
    private TransportOutDescription transportOut = null;
    /** is this transport started? */
    protected boolean started = false;
    /** is this transport non-blocking? */
    protected boolean isNonBlocking = false;
    /** the axis observer that gets notified of service life cycle events*/
    private final AxisObserver axisObserver = new GenericAxisObserver();

    /** the thread pool to execute actual poll invocations */
    protected WorkerPool workerPool = null;
    /** use the thread pool available in the axis2 configuration context */
    protected boolean useAxis2ThreadPool = false;

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
        this.engine = new AxisEngine(cfgCtx);
        this.transportIn  = transportIn;
        this.transportOut = cfgCtx.getAxisConfiguration().getTransportOut(transportName);

        if (useAxis2ThreadPool) {
            //this.workerPool = cfgCtx.getThreadPool(); not yet implemented
            throw new AxisFault("Unsupported thread pool for task execution - Axis2 thread pool");
        } else {
            this.workerPool = WorkerPoolFactory.getWorkerPool(
            10, 20, 5, -1, transportName + "Server Worker thread group", transportName + "-Worker");
        }

        // register to receive updates on services for lifetime management
        cfgCtx.getAxisConfiguration().addObservers(axisObserver);
    }

    public void destroy() {
        try {
            if (started) {
                try {
                    stop();
                } catch (AxisFault ignore) {
                    log.warn("Error stopping the transport : " + transportName);
                }
            }
        } finally {
            started = false;
        }
    }

    public void stop() throws AxisFault {
        if (started) {
            started = false;
            // cancel receipt of service lifecycle events
            cfgCtx.getAxisConfiguration().getObserversList().remove(axisObserver);
        }
    }

    public void start() throws AxisFault {
        if (!started) {
            started = true;
            // register to receive updates on services for lifetime management
            cfgCtx.getAxisConfiguration().addObservers(axisObserver);
        }

        // iterate through deployed services and start
        Iterator services = cfgCtx.getAxisConfiguration().getServices().values().iterator();

        while (services.hasNext()) {
            AxisService service = (AxisService) services.next();
            if (BaseUtils.isUsingTransport(service, transportName)) {
                startListeningForService(service);
            }
        }
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

        msgCtx.setIncomingTransportName(transportName);
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
        String soapAction, String contentType) {

        // set the soapaction if one is available via a transport header
        if (soapAction != null) {
            msgCtx.setSoapAction(soapAction);
        }

        // set the transport headers to the message context
        msgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, trpHeaders);

        // send the message context through the axis engine
        try {
                try {
                    engine.receive(msgCtx);
                } catch (AxisFault e) {
                    e.printStackTrace();
                    if (log.isDebugEnabled()) {
                        log.debug("Error receiving message", e);
                    }
                    if (msgCtx.isServerSide()) {
                        engine.sendFault(MessageContextBuilder.createFaultMessageContext(msgCtx, e));
                    }
                }
        } catch (AxisFault axisFault) {
            logException("Error processing received message", axisFault);
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
        return transportName;
    }

    public void setTransportName(String transportName) {
        this.transportName = transportName;
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

            if (service.getName().startsWith("__")) {
                return; // these are "private" services
            }

            if (BaseUtils.isUsingTransport(service, transportName)) {
                switch (event.getEventType()) {
                    case AxisEvent.SERVICE_DEPLOY :
                        startListeningForService(service);
                        break;
                    case AxisEvent.SERVICE_REMOVE :
                        stopListeningForService(service);
                        break;
                    case AxisEvent.SERVICE_START  :
                        startListeningForService(service);
                        break;
                    case AxisEvent.SERVICE_STOP   :
                        stopListeningForService(service);
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

}
