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

package org.apache.synapse.endpoints;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.description.AxisOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.*;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.statistics.StatisticsReporter;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;

import java.util.List;
import java.util.Stack;

/**
 * An abstract base class for all Endpoint implementations
 */
public abstract class AbstractEndpoint extends FaultHandler implements Endpoint {

    protected Log log;
    protected static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /** Hold the logical name of an endpoint */
    private String endpointName = null;

    /** The parent endpoint for this endpoint */
    private Endpoint parentEndpoint = null;

    /** The child endpoints of this endpoint - if any */
    private List<Endpoint> children = null;

    /** The Endpoint definition for this endpoint - i.e. holds all static endpoint information */
    private EndpointDefinition definition = null;

    /** Has this endpoint been initialized ? */
    protected boolean initialized = false;

    /** The endpoint context - if applicable - that will hold the runtime state of the endpoint */
    private EndpointContext context = null;

    /** Is clustering enabled */
    protected Boolean isClusteringEnabled = null;

    /** The MBean managing the endpoint */
    EndpointView metricsMBean = null;

    /** The name of the file where this endpoint is defined */
    protected String fileName;

    protected boolean anonymous = false;

    protected AbstractEndpoint() {
        log = LogFactory.getLog(this.getClass());
    }

    //------------------------------- getters and setters ------------------------------------------

    public EndpointView getMetricsMBean() {
        return metricsMBean;
    }

    public EndpointContext getContext() {
        return context;
    }

    public String getName() {
        return endpointName;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public EndpointDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(EndpointDefinition definition) {
        this.definition = definition;
        definition.setLeafEndpoint(this);
    }

    public Endpoint getParentEndpoint() {
        return parentEndpoint;
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public List<Endpoint> getChildren() {
        return children;
    }

    public void setChildren(List<Endpoint> children) {
        this.children = children;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String toString() {
        if (endpointName != null) {
            return "Endpoint [" + endpointName + "]";
        }
        return SynapseConstants.ANONYMOUS_ENDPOINT;
    }

    public void setName(String endpointName) {
        this.endpointName = endpointName;
        metricsMBean = new EndpointView(endpointName, this);
        MBeanRegistrar.getInstance().registerMBean(metricsMBean, "Endpoint", endpointName);
    }

    //----------------------- default method implementations and common code -----------------------

    public void init(SynapseEnvironment synapseEnvironment) {
        ConfigurationContext cc =
                ((Axis2SynapseEnvironment) synapseEnvironment).getAxis2ConfigurationContext();
        if (!initialized) {
            // The check for clustering environment
            ClusteringAgent clusteringAgent = cc.getAxisConfiguration().getClusteringAgent();
            if (clusteringAgent != null && clusteringAgent.getStateManager() != null) {
                isClusteringEnabled = Boolean.TRUE;
            } else {
                isClusteringEnabled = Boolean.FALSE;
            }

            context = new EndpointContext(getName(), getDefinition(), isClusteringEnabled, cc);
        }
        initialized = true;

        if (children != null) {
            for (Endpoint e : children) {
                e.init(synapseEnvironment);
            }
        }
    }

    public boolean readyToSend() {
        return !initialized || context == null || context.readyToSend();
    }

    public void send(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        prepareForEndpointStatistics(synCtx);

        if (traceOrDebugOn) {
            String address = definition.getAddress();
            if (address == null && synCtx.getTo() != null && synCtx.getTo().getAddress() != null) {
                // compute address for the default endpoint only for logging purposes
                address = synCtx.getTo().getAddress();
            }

            traceOrDebug(traceOn, "Sending message through endpoint : " +
                    getName() + " resolving to address = " + address);
            traceOrDebug(traceOn, "SOAPAction: " + (synCtx.getSoapAction() != null ?
                    synCtx.getSoapAction() : "null"));
            traceOrDebug(traceOn, "WSA-Action: " + (synCtx.getWSAAction() != null ?
                    synCtx.getWSAAction() : "null"));
            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Envelope : \n" + synCtx.getEnvelope());
            }
        }

        // register this as the immediate fault handler for this message.
        synCtx.pushFaultHandler(this);
        // add this as the last endpoint to process this message - used by statistics counting code
        synCtx.setProperty(SynapseConstants.LAST_ENDPOINT, this);
        // set message level metrics collector
        ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(
            BaseConstants.METRICS_COLLECTOR, metricsMBean);
        // Send the message through this endpoint
        synCtx.getEnvironment().send(definition, synCtx);
    }

    /**
     * Is this a leaf level endpoint? or parent endpoint that has children?
     * @return true if there is no children - a leaf endpoint 
     */
    public boolean isLeafEndpoint() {
        return children == null || children.size() == 0;
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        // do nothing, the LB/FO endpoints will override this
    }

    /**
     * Is this [fault] message a timeout?
     * @param synCtx the current fault message
     * @return true if this is defined as a timeout
     */
    protected boolean isTimeout(MessageContext synCtx) {
        Integer errorCode = (Integer) synCtx.getProperty(SynapseConstants.ERROR_CODE);
        if (errorCode != null) {
            if (definition.getTimeoutErrorCodes().isEmpty()) {
                // if timeout codes are not defined, assume only HTTP timeout and connection close
                boolean isTimeout = SynapseConstants.NHTTP_CONNECTION_TIMEOUT == errorCode;
                boolean isClosed = SynapseConstants.NHTTP_CONNECTION_CLOSED == errorCode;
                
                if (isTimeout || isClosed) {

                    if (log.isDebugEnabled()) {
                        log.debug("Encountered a default HTTP connection " +
                                (isClosed ? "close" : "timeout") + " error : " + errorCode);
                    }
                    return true;
                }
            } else {
                if (definition.getTimeoutErrorCodes().contains(errorCode)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Encountered a mark for suspension error : " + errorCode
                                + " defined " + "error codes are : "
                                + definition.getTimeoutErrorCodes());    
                    }
                    return true;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Encountered a non-timeout error sending to " + this.toString() +
                ", error code : " + errorCode);
        }
        return false;
    }

    /**
     * Is this a fault that should put the endpoint on SUSPEND? or is this a fault to ignore?
     * @param synCtx the current fault message
     * @return true if this fault should suspend the endpoint
     */
    protected boolean isSuspendFault(MessageContext synCtx) {
        Integer errorCode = (Integer) synCtx.getProperty(SynapseConstants.ERROR_CODE);
        if (errorCode != null) {
            if (definition.getSuspendErrorCodes().isEmpty()) {
                // if suspend codes are not defined, any error will be fatal for the endpoint
                if (log.isDebugEnabled()) {
                    log.debug(this.toString() + " encountered a fatal error : " + errorCode);
                }
                return true;

            } else {
                if (definition.getSuspendErrorCodes().contains(errorCode)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Encountered a suspend error : " + errorCode +
                            " defined suspend codes are : " + definition.getSuspendErrorCodes());
                    }
                    return true;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Encountered a non-fatal error sending to " + this.toString()
                    + ", error code : " + errorCode
                    + ". Error will be handled, but endpoint will not fail");
        }
        return false;
    }

    /**
     * On a fault, propagate to parent if any, or call into the fault handler
     * @param synCtx the message at hand
     */
    public void onFault(MessageContext synCtx) {
        invokeNextFaultHandler(synCtx);
    }

    /**
     * The SynapseCallback Receiver notifies an endpoint, if a message was successfully processed
     * to give it a chance to clear up or reset its state to active
     */
    public void onSuccess() {
        // do nothing
    }


    /**
     * Should this mediator perform tracing? True if its explicitly asked to
     * trace, or its parent has been asked to trace and it does not reject it
     *
     * @param msgCtx the current message
     * @return true if tracing should be performed
     */
    protected boolean isTraceOn(MessageContext msgCtx) {
        return definition != null &&
               ((definition.getTraceState() == SynapseConstants.TRACING_ON) ||
                (definition.getTraceState() == SynapseConstants.TRACING_UNSET &&
                    msgCtx.getTracingState() == SynapseConstants.TRACING_ON));
    }

    /**
     * Is tracing or debug logging on?
     *
     * @param isTraceOn is tracing known to be on?
     * @return true, if either tracing or debug logging is on
     */
    protected boolean isTraceOrDebugOn(boolean isTraceOn) {
        return isTraceOn || log.isDebugEnabled();
    }

    /**
     * Perform Trace and Debug logging of a message @INFO (trace) and DEBUG (log)
     *
     * @param traceOn is runtime trace on for this message?
     * @param msg     the message to log/trace
     */
    protected void traceOrDebug(boolean traceOn, String msg) {
        if (traceOn) {
            trace.info(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    /**
     * Process statistics for this message
     * @param synCtx the current message
     */
    protected void prepareForEndpointStatistics(MessageContext synCtx) {
        // Setting Required property to reportForComponent the End Point aspects
        if (definition != null && definition.isStatisticsEnable()) {
            String opName = null;

            if (synCtx.getProperty(SynapseConstants.ENDPOINT_OPERATION) != null) {
                opName = synCtx.getProperty(SynapseConstants.ENDPOINT_OPERATION).toString();
            } else if (synCtx instanceof Axis2MessageContext) {
                AxisOperation operation
                        = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getAxisOperation();
                if (operation != null) {
                    opName = operation.getName().getLocalPart();
                }
                if (opName == null ||
                        SynapseConstants.SYNAPSE_OPERATION_NAME.getLocalPart().equals(opName)) {
                    String soapAction = synCtx.getSoapAction();
                    opName = null;
                    if (soapAction != null) {
                        int index = soapAction.indexOf("urn:");
                        if (index >= 0) {
                            opName = soapAction.substring("urn:".length());
                        } else {
                            opName = soapAction;
                        }
                    }
                }
            }

            AspectConfiguration oldConfiguration = definition.getAspectConfiguration();
            if (opName != null) {
                if (oldConfiguration.isStatisticsEnable()) {
                    AspectConfiguration newConfiguration = new AspectConfiguration(
                            oldConfiguration.getId() + SynapseConstants.STATISTICS_KEY_SEPARATOR +
                                    opName);
                    newConfiguration.enableStatistics();
                    StatisticsReporter.reportForComponent(synCtx, newConfiguration,
                            ComponentType.ENDPOINT);
                }
            } else {
                StatisticsReporter.reportForComponent(synCtx, oldConfiguration,
                        ComponentType.ENDPOINT);
            }
        }
    }

    /**
     * Helper methods to handle errors.
     *
     * @param msg The error message
     */
    protected void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Helper methods to handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    protected void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    protected void logOnChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        if (log.isDebugEnabled()) {
            log.debug(this.toString() + " detected a failure in a child endpoint : " + endpoint);
            log.debug(this.toString() + " retrying request [[Message ID : "
                    + synMessageContext.getMessageID() + "], [To : "
                    + synMessageContext.getTo() + "]]");
        }
    }

    protected void informFailure(MessageContext synCtx, int errorCode, String errorMsg) {

        if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {
            setErrorOnMessage(synCtx, String.valueOf(errorCode), errorMsg);
        }
        invokeNextFaultHandler(synCtx);
    }


    protected void setErrorOnMessage(MessageContext synCtx, String errorCode, String errorMsg) {

        synCtx.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, errorMsg);
        synCtx.setProperty(SynapseConstants.ERROR_DETAIL, errorMsg);
    }

    private void invokeNextFaultHandler(MessageContext synCtx) {

        Stack faultStack = synCtx.getFaultStack();
        if (!faultStack.isEmpty()) {
            Object faultHandler = faultStack.pop();
            if (faultHandler instanceof Endpoint) {
                // This is the parent . need to inform parent with fault child
                ((Endpoint) faultHandler).onChildEndpointFail(this, synCtx);
            } else {
                ((FaultHandler) faultHandler).handleFault(synCtx);
            }
        }
    }

    public void destroy() {
        MBeanRegistrar.getInstance().unRegisterMBean("Endpoint", endpointName);
        this.initialized = false;
    }
}
