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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.clustering.ClusterManager;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.utils.EndpointDefinition;
import org.apache.synapse.statistics.impl.EndPointStatisticsStack;

import java.util.Stack;

/**
 * WSDLEndpoint represents the endpoints built using a wsdl document. It stores the details about
 * the endpoint in a EndpointDefinition object. Once the WSDLEndpoint object is constructed, it
 * should not access the wsdl document at runtime to obtain endpoint information. If it is necessary
 * to create an endpoint using a dynamic wsdl, store the endpoint configuration in the registry and
 * create a dynamic wsdl endpoint using that registry key.
 * <p/>
 * TODO: This should allow various policies to be applied on fine grained level (e.g. operations).
 */
public class WSDLEndpoint extends FaultHandler implements Endpoint {

    private static final Log log = LogFactory.getLog(WSDLEndpoint.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    private String name = null;
    private String wsdlURI;
    private OMElement wsdlDoc;
    private String serviceName;
    private String portName;

    private Endpoint parentEndpoint = null;
    private EndpointDefinition endpoint = null;

    /**
     * The endpoint context , place holder for keep any runtime states related to the endpoint
     */
    private final EndpointContext endpointContext = new EndpointContext();

    /**
     * Sends the message through this endpoint. This method just handles statistics related
     * functions and gives the message to the Synapse environment to send. It does not add any
     * endpoint specific details to the message context. These details are added only to the cloned
     * message context by the Axis2FlexibleMepClient. So that we can reuse the original message
     * context for resending through different endpoints.
     *
     * @param synCtx MessageContext sent by client to Synapse
     */
    public void send(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : WSDL Endpoint");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        if (endpoint.getAddress() != null) {

            String eprAddress = endpoint.getAddress();
            boolean isClusteringEnable = false;
            // get Axis2 MessageContext and ConfigurationContext
            org.apache.axis2.context.MessageContext axisMC =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            ConfigurationContext cc = axisMC.getConfigurationContext();

            //The check for clustering environment
            ClusterManager clusterManager = cc.getAxisConfiguration().getClusterManager();
            if (clusterManager != null &&
                    clusterManager.getContextManager() != null) {
                isClusteringEnable = true;
            }

            String endPointName = this.getName();
            if (endPointName == null) {

                if (traceOrDebugOn && isClusteringEnable) {
                    log.warn("In a clustering environment , the endpoint  name should be " +
                            "specified even for anonymous endpoints. Otherwise, the clustering " +
                            "would not be functioned correctly if there are more than one " +
                            "anonymous endpoints. ");
                }
                endPointName = SynapseConstants.ANONYMOUS_ENDPOINT;
            }

            if (isClusteringEnable) {
                // if this is a cluster environment , then set configuration context
                // to endpoint context
                if (endpointContext.getConfigurationContext() == null) {
                    endpointContext.setConfigurationContext(cc);
                    endpointContext.setContextID(endPointName);
                }
            }

            // Setting Required property to collect the End Point statistics
            boolean statisticsEnable =
                    (SynapseConstants.STATISTICS_ON == endpoint.getStatisticsState());

            if (statisticsEnable) {
                EndPointStatisticsStack endPointStatisticsStack = null;
                Object statisticsStackObj =
                        synCtx.getProperty(org.apache.synapse.SynapseConstants.ENDPOINT_STATS);
                if (statisticsStackObj == null) {
                    endPointStatisticsStack = new EndPointStatisticsStack();
                    synCtx.setProperty(org.apache.synapse.SynapseConstants.ENDPOINT_STATS,
                            endPointStatisticsStack);
                } else if (statisticsStackObj instanceof EndPointStatisticsStack) {
                    endPointStatisticsStack = (EndPointStatisticsStack) statisticsStackObj;
                }
                if (endPointStatisticsStack != null) {
                    boolean isFault = synCtx.getEnvelope().getBody().hasFault();
                    endPointStatisticsStack.put(endPointName, System.currentTimeMillis(),
                            !synCtx.isResponse(), statisticsEnable, isFault);
                }
            }
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Sending message to WSDL endpoint : " +
                        endPointName + " resolves to address = " + eprAddress);
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

            // add this as the last endpoint to process this message. it is used by statistics code.
            synCtx.setProperty(SynapseConstants.PROCESSED_ENDPOINT, this);

            synCtx.getEnvironment().send(endpoint, synCtx);
        }
    }

    public void onFault(MessageContext synCtx) {
        // perform retries here

        // if this endpoint has actually failed, inform the parent.
        setActive(false, synCtx);

        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synCtx);
        } else {
            Stack faultStack = synCtx.getFaultStack();
            if (!faultStack.isEmpty()) {
                ((FaultHandler) faultStack.pop()).handleFault(synCtx);
            }
        }
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        // WSDLEndpoint does not contain any child endpoints. So this method will never be called.
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getWsdlURI() {
        return wsdlURI;
    }

    public void setWsdlURI(String wsdlURI) {
        this.wsdlURI = wsdlURI;
    }

    public OMElement getWsdlDoc() {
        return wsdlDoc;
    }

    public void setWsdlDoc(OMElement wsdlDoc) {
        this.wsdlDoc = wsdlDoc;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    /**
     * Checks if the endpoint is active (failed or not). If endpoint is in failed state and
     * suspendOnFailDuration has elapsed, it will be set to active.
     *
     * @param synMessageContext MessageContext of the current message. This is not used here.
     * @return true if endpoint is active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {
        boolean active = endpointContext.isActive();
        if (!active) {
            long recoverOn = endpointContext.getRecoverOn();
            if (System.currentTimeMillis() > recoverOn) {
                active = true;
                endpointContext.setActive(true);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("WSDLEndpoint with name '" + name + "' is in "
                    + (active ? "active" : "inactive") + " state");
        }

        return active;
    }

    /**
     * Sets if endpoint active or not. if endpoint is set as failed (active = false), the recover on
     * time is calculated so that it will be activated after the recover on time.
     *
     * @param active            true if active. false otherwise.
     * @param synMessageContext MessageContext of the current message. This is not used here.
     */
    public void setActive(boolean active, MessageContext synMessageContext) {

        if (!active) {
            if (endpoint.getSuspendOnFailDuration() != -1) {
                // Calculating a new value by adding suspendOnFailDuration to current time.
                // as the endpoint is set as failed
                endpointContext.setRecoverOn(
                        System.currentTimeMillis() + endpoint.getSuspendOnFailDuration());
            } else {
                endpointContext.setRecoverOn(Long.MAX_VALUE);
            }
        }

        endpointContext.setActive(true);
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public EndpointDefinition getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointDefinition endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Should this mediator perform tracing? True if its explicitly asked to
     * trace, or its parent has been asked to trace and it does not reject it
     *
     * @param msgCtx the current message
     * @return true if tracing should be performed
     */
    protected boolean isTraceOn(MessageContext msgCtx) {
        return
                (endpoint.getTraceState() == SynapseConstants.TRACING_ON) ||
                        (endpoint.getTraceState() == SynapseConstants.TRACING_UNSET &&
                                msgCtx.getTracingState() == SynapseConstants.TRACING_ON);
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
}
