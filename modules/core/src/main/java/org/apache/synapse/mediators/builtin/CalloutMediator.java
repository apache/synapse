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

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2BlockingClient;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.DefaultEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.List;

/**
 * <callout [serviceURL="string"] [action="string"][passHeaders="true|false"] [initAxis2ClientOptions="true|false"]>
 * <configuration [axis2xml="string"] [repository="string"]/>?
 * <endpoint/>?
 * <source xpath="expression" | key="string">? <!-- key can be a MC property or entry key -->
 * <target xpath="expression" | key="string"/>?
 * <enableSec policy="string" | outboundPolicy="String" | inboundPolicy="String"/>?
 * </callout>
 */
public class CalloutMediator extends AbstractMediator implements ManagedLifecycle {

    private String serviceURL = null;

    private String action = null;

    private String requestKey = null;

    private SynapseXPath requestXPath = null;

    private SynapseXPath targetXPath = null;

    private String targetKey = null;

    private String clientRepository = null;

    private String axis2xml = null;

    private boolean passHeaders = false;

    private boolean initClientOptions = true;

    private boolean securityOn = false;  //Should messages be sent using WS-Security?

    private String wsSecPolicyKey = null;

    private String inboundWsSecPolicyKey = null;

    private String outboundWsSecPolicyKey = null;

    private Endpoint endpoint = null;

    private boolean isWrappingEndpointCreated = false;

    private Axis2BlockingClient blockingMsgSender = null;

    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Callout mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        try {

            if (synLog.isTraceOrDebugEnabled()) {
                if (!isWrappingEndpointCreated) {
                    synLog.traceOrDebug("Using the defined endpoint : " + endpoint.getName());
                } else {
                    if (serviceURL != null) {
                        synLog.traceOrDebug("Using the serviceURL : " + serviceURL);
                    } else {
                        synLog.traceOrDebug("Using the To header as the EPR ");
                    }
                    if (securityOn) {
                        synLog.traceOrDebug("Security enabled within the Callout Mediator config");
                        if (wsSecPolicyKey != null) {
                            synLog.traceOrDebug("Using security policy key : " + wsSecPolicyKey);
                        } else {
                            if (inboundWsSecPolicyKey != null) {
                                synLog.traceOrDebug("Using inbound security policy key : " + inboundWsSecPolicyKey);
                            }
                            if (outboundWsSecPolicyKey != null) {
                                synLog.traceOrDebug("Using outbound security policy key : " + outboundWsSecPolicyKey);
                            }
                        }
                    }
                }
            }

            org.apache.axis2.context.MessageContext axis2MsgCtx =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            if (Constants.VALUE_TRUE.equals(axis2MsgCtx.getProperty(Constants.Configuration.ENABLE_MTOM))) {
                ((AbstractEndpoint) endpoint).getDefinition().setUseMTOM(true);
            }

            MessageContext synapseOutMsgCtx = MessageHelper.cloneMessageContext(synCtx);

            if (action != null) {
                synapseOutMsgCtx.setSoapAction(action);
            }

            if (requestKey != null || requestXPath != null) {
                SOAPBody soapBody = synapseOutMsgCtx.getEnvelope().getBody();
                soapBody.removeChildren();
                soapBody.addChild(getRequestPayload(synCtx));
                if (!passHeaders) {
                    SOAPHeader soapHeader = synapseOutMsgCtx.getEnvelope().getHeader();
                    soapHeader.removeChildren();
                }
            }

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("About to invoke the service");
                if (synLog.isTraceTraceEnabled()) {
                    synLog.traceTrace("Request message payload : " + synapseOutMsgCtx.getEnvelope());
                }
            }

            MessageContext resultMsgCtx = null;
            try {
                if ("true".equals(synCtx.getProperty(SynapseConstants.OUT_ONLY))) {
                    blockingMsgSender.send(endpoint, synapseOutMsgCtx);
                } else {
                    resultMsgCtx = blockingMsgSender.send(endpoint, synapseOutMsgCtx);
                    if ("true".equals(resultMsgCtx.getProperty(SynapseConstants.BLOCKING_CLIENT_ERROR))) {
                        handleFault(synCtx, (Exception) synCtx.getProperty(SynapseConstants.ERROR_EXCEPTION));
                    }
                }
            } catch (Exception ex) {
                handleFault(synCtx, ex);
            }

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Response payload received : " + resultMsgCtx.getEnvelope());
            }

            if (resultMsgCtx != null) {
                if (targetXPath != null) {
                    Object o = targetXPath.evaluate(synCtx);
                    OMElement result = resultMsgCtx.getEnvelope().getBody().getFirstElement();
                    if (o != null && o instanceof OMElement) {
                        OMNode tgtNode = (OMElement) o;
                        tgtNode.insertSiblingAfter(result);
                        tgtNode.detach();
                    } else if (o != null && o instanceof List && !((List) o).isEmpty()) {
                        // Always fetches *only* the first
                        OMNode tgtNode = (OMElement) ((List) o).get(0);
                        tgtNode.insertSiblingAfter(result);
                        tgtNode.detach();
                    } else {
                        handleException("Evaluation of target XPath expression : " +
                                        targetXPath.toString() + " did not yeild an OMNode", synCtx);
                    }
                } else if (targetKey != null) {
                    OMElement result = resultMsgCtx.getEnvelope().getBody().getFirstElement();
                    synCtx.setProperty(targetKey, result);
                } else {
                    synCtx.setEnvelope(resultMsgCtx.getEnvelope());
                }
            } else {
                synLog.traceOrDebug("Service returned a null response");
            }

        } catch (AxisFault e) {
            handleException("Error invoking service : " + serviceURL +
                            (action != null ? " with action : " + action : ""), e, synCtx);
        } catch (JaxenException e) {
            handleException("Error while evaluating the XPath expression: " + targetXPath,
                            e, synCtx);
        }

        synLog.traceOrDebug("End : Callout mediator");
        return true;
    }

    private void handleFault(MessageContext synCtx, Exception ex) {
        synCtx.setProperty(SynapseConstants.SENDING_FAULT, Boolean.TRUE);

        if (ex instanceof AxisFault) {
            AxisFault axisFault = (AxisFault) ex;

            if (axisFault.getFaultCodeElement() != null) {
                synCtx.setProperty(SynapseConstants.ERROR_CODE,
                                   axisFault.getFaultCodeElement().getText());
            } else {
                synCtx.setProperty(SynapseConstants.ERROR_CODE,
                                   SynapseConstants.CALLOUT_OPERATION_FAILED);
            }

            if (axisFault.getMessage() != null) {
                synCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                                   axisFault.getMessage());
            } else {
                synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, "Error while performing " +
                                                                   "the callout operation");
            }

            if (axisFault.getFaultDetailElement() != null) {
                if (axisFault.getFaultDetailElement().getFirstElement() != null) {
                    synCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                                       axisFault.getFaultDetailElement().getFirstElement());
                } else {
                    synCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                                       axisFault.getFaultDetailElement().getText());
                }
            }
        }

        synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, ex);
        throw new SynapseException("Error while performing the callout operation", ex);
    }

    private OMElement getRequestPayload(MessageContext synCtx) throws AxisFault {

        if (requestKey != null) {
            Object request = synCtx.getProperty(requestKey);
            if (request == null) {
                request = synCtx.getEntry(requestKey);
            }
            if (request != null && request instanceof OMElement) {
                return (OMElement) request;
            } else {
                handleException("The property : " + requestKey + " is not an OMElement", synCtx);
            }
        } else if (requestXPath != null) {
            try {
                Object o = requestXPath.evaluate(MessageHelper.cloneMessageContext(synCtx));

                if (o instanceof OMElement) {
                    return (OMElement) o;
                } else if (o instanceof List && !((List) o).isEmpty()) {
                    return (OMElement) ((List) o).get(0);  // Always fetches *only* the first
                } else {
                    handleException("The evaluation of the XPath expression : "
                        + requestXPath.toString() + " did not result in an OMElement", synCtx);
                }
            } catch (JaxenException e) {
                handleException("Error evaluating XPath expression : "
                        + requestXPath.toString(), e, synCtx);
            }
        }
        return null;
    }

    public void init(SynapseEnvironment synEnv) {
        blockingMsgSender = new Axis2BlockingClient(clientRepository, axis2xml);
        blockingMsgSender.setInitClientOptions(initClientOptions);

        EndpointDefinition endpointDefinition = null;

        if (serviceURL != null) {
            // If Service URL is specified, it is given the highest priority
            endpoint = new AddressEndpoint();
            endpointDefinition = new EndpointDefinition();
            endpointDefinition.setAddress(serviceURL);
            ((AddressEndpoint) endpoint).setDefinition(endpointDefinition);
            isWrappingEndpointCreated = true;
        } else if (endpoint == null) {
            // Use a default endpoint in this case - i.e. the To header
            endpoint = new DefaultEndpoint();
            endpointDefinition = new EndpointDefinition();
            ((DefaultEndpoint) endpoint).setDefinition(endpointDefinition);
            isWrappingEndpointCreated = true;
        } else {
            endpoint.init(synEnv);
        }
        // If the endpoint is specified, we'll look it up at mediation time.

        if (endpointDefinition != null && isSecurityOn()) {
            endpointDefinition.setSecurityOn(true);
            if (wsSecPolicyKey != null) {
                endpointDefinition.setWsSecPolicyKey(wsSecPolicyKey);
            } else {
                if (inboundWsSecPolicyKey != null) {
                    endpointDefinition.setInboundWsSecPolicyKey(inboundWsSecPolicyKey);
                }
                if (outboundWsSecPolicyKey != null) {
                    endpointDefinition.setOutboundWsSecPolicyKey(outboundWsSecPolicyKey);
                }
            }
        }
    }

    public void destroy() {
        if (!isWrappingEndpointCreated) {
            endpoint.destroy();
        }
        try {
            blockingMsgSender.cleanup();
        } catch (AxisFault ignore) {}
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public void setRequestXPath(SynapseXPath requestXPath) throws JaxenException {
        this.requestXPath = requestXPath;
    }

    public void setTargetXPath(SynapseXPath targetXPath) throws JaxenException {
        this.targetXPath = targetXPath;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }

    public SynapseXPath getRequestXPath() {
        return requestXPath;
    }

    public SynapseXPath getTargetXPath() {
        return targetXPath;
    }

    public String getClientRepository() {
        return clientRepository;
    }

    public void setClientRepository(String clientRepository) {
        this.clientRepository = clientRepository;
    }

    public String getAxis2xml() {
        return axis2xml;
    }

    public void setAxis2xml(String axis2xml) {
        this.axis2xml = axis2xml;
    }

    public boolean isPassHeaders() {
        return passHeaders;
    }

    public void setPassHeaders(boolean passHeaders) {
        this.passHeaders = passHeaders;
    }

    public boolean isInitClientOptions() {
        return initClientOptions;
    }

    public void setInitClientOptions(boolean initClientOptions) {
        this.initClientOptions = initClientOptions;
    }

    /**
     * Is WS-Security turned on?
     *
     * @return true if on
     */
    public boolean isSecurityOn() {
        return securityOn;
    }

    /**
     * Request that WS-Sec be turned on/off
     *
     * @param securityOn a boolean flag indicating security is on or not
     */
    public void setSecurityOn(boolean securityOn) {
        this.securityOn = securityOn;
    }

    /**
     * Return the Rampart Security configuration policy's 'key' to be used
     *
     * @return the Rampart Security configuration policy's 'key' to be used
     */
    public String getWsSecPolicyKey() {
        return wsSecPolicyKey;
    }

    /**
     * Set the Rampart Security configuration policy's 'key' to be used
     *
     * @param wsSecPolicyKey the Rampart Security configuration policy's 'key' to be used
     */
    public void setWsSecPolicyKey(String wsSecPolicyKey) {
        this.wsSecPolicyKey = wsSecPolicyKey;
    }

    /**
     * Get the outbound security policy key. This is used when we specify different policies for
     * inbound and outbound.
     *
     * @return outbound security policy key
     */
    public String getOutboundWsSecPolicyKey() {
        return outboundWsSecPolicyKey;
    }

    /**
     * Set the outbound security policy key.This is used when we specify different policies for
     * inbound and outbound.
     *
     * @param outboundWsSecPolicyKey outbound security policy key.
     */
    public void setOutboundWsSecPolicyKey(String outboundWsSecPolicyKey) {
        this.outboundWsSecPolicyKey = outboundWsSecPolicyKey;
    }

    /**
     * Get the inbound security policy key. This is used when we specify different policies for
     * inbound and outbound.
     *
     * @return inbound security policy key
     */
    public String getInboundWsSecPolicyKey() {
        return inboundWsSecPolicyKey;
    }

    /**
     * Set the inbound security policy key. This is used when we specify different policies for
     * inbound and outbound.
     *
     * @param inboundWsSecPolicyKey inbound security policy key.
     */
    public void setInboundWsSecPolicyKey(String inboundWsSecPolicyKey) {
        this.inboundWsSecPolicyKey = inboundWsSecPolicyKey;
    }

    /**
     * Get the defined endpoint
     *
     * @return endpoint
     */
    public Endpoint getEndpoint() {
        if (!isWrappingEndpointCreated) {
            return endpoint;
        }
        return null;
    }

    /**
     * Set the defined endpoint
     *
     * @param endpoint defined endpoint
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

}
