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

package org.apache.synapse.libraries.eip;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.synapse.*;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.Iterator;
import java.util.List;

/*
 *This pattern blocks external service invocation during mediation.The list of endpoints through
 *which the message should pass will be given at the design time via callout blocks.
 *Each time the message returns from an endpoint, it will proceed to next callout block defined.
 */

public class CalloutBlockMediator extends AbstractMediator implements ManagedLifecycle {

    private ThreadLocal<String> serviceURL = new ThreadLocal<String>();
    private ThreadLocal<String> action = new ThreadLocal<String>();
    private ThreadLocal<String> requestKey = new ThreadLocal<String>();
    private ThreadLocal<SynapseXPath> requestXPath = new ThreadLocal<SynapseXPath>();
    private ThreadLocal<SynapseXPath> targetXPath = new ThreadLocal<SynapseXPath>();
    private ThreadLocal<String> targetKey = new ThreadLocal<String>();

    private ConfigurationContext configCtx = null;
    private String clientRepository = null;
    private String axis2xml = null;
    private boolean passHeaders = false;
    public final static String DEFAULT_CLIENT_REPO = "./samples/axis2Client/client_repo";
    public final static String DEFAULT_AXIS2_XML = "./samples/axis2Client/client_repo/conf/axis2.xml";

    /**
     * Blocks external service invocation
     *
     * @param synCtx the current message for mediation
     * @return true
     */
    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Callout mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        initParams(synCtx);


        try {
            ServiceClient sc = new ServiceClient(configCtx, null);
            Options options = new Options();
            options.setTo(new EndpointReference(getServiceURL()));

            if (getAction() != null) {
                options.setAction(getAction());
            } else {
                if (synCtx.isSOAP11()) {
                    options.setProperty(Constants.Configuration.DISABLE_SOAP_ACTION, true);
                } else {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx =
                            axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.getTransportOut().addParameter(
                            new Parameter(HTTPConstants.OMIT_SOAP_12_ACTION, true));
                }
            }

            if (passHeaders) {
                SOAPHeader header = synCtx.getEnvelope().getHeader();
                if (header != null) {
                    Iterator headerElements = header.cloneOMElement().getChildElements();
                    while (headerElements.hasNext()) {
                        sc.addHeader((OMElement) headerElements.next());
                    }
                }
            }

            options.setProperty(
                    AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
            sc.setOptions(options);

            OMElement request = getRequestPayload(synCtx);
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("About to invoke service : " + getServiceURL() + (getAction() != null ?
                        " with action : " + getAction() : ""));
                if (synLog.isTraceTraceEnabled()) {
                    synLog.traceTrace("Request message payload : " + request);
                }
            }

            OMElement result = null;
            try {
                options.setCallTransportCleanup(true);
                result = sc.sendReceive(request);
            } catch (AxisFault axisFault) {
                handleFault(synCtx, axisFault);
            }

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Response payload received : " + result);
            }

            if (result != null) {
                if (getTargetXPath() != null) {
                    Object o = getTargetXPath().evaluate(synCtx);

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
                                getTargetXPath().toString() + " did not yeild an OMNode", synCtx);
                    }
                }
                if (getTargetKey() != null) {
                    synCtx.setProperty(getTargetKey(), result);
                }
            } else {
                synLog.traceOrDebug("Service returned a null response");
            }

        } catch (AxisFault e) {
            handleException("Error invoking service : " + getServiceURL() +
                    (getAction() != null ? " with action : " + getAction() : ""), e, synCtx);
        } catch (JaxenException e) {
            handleException("Error while evaluating the XPath expression: " + getTargetXPath(),
                    e, synCtx);
        }

        synLog.traceOrDebug("End : Callout mediator");
        return true;
    }

    /**
     * Initialize parameters via the values fetched from templates
     * @param synCtx MessageContext
     */
    private void initParams(MessageContext synCtx) {

        String serURL = (String) EIPUtils.lookupFunctionParam(synCtx, "service_URL");

        if (serURL != null && !serURL.trim().equals("")) {
            setServiceURL(serURL);
        } else {
            handleException("The 'serviceURL' attribute is required for the Callout mediator", synCtx);
        }

        String soapAction = (String) EIPUtils.lookupFunctionParam(synCtx, "action");

        if (soapAction != null && !soapAction.trim().equals("")) {
            setAction(soapAction);
        }

        Object sXpath = EIPUtils.lookupFunctionParam(synCtx, "source_xpath");
        Object sKey = EIPUtils.lookupFunctionParam(synCtx, "source_key");

        if (sXpath != null && sXpath instanceof SynapseXPath) {
            try {
                setRequestXPath((SynapseXPath) sXpath);
            } catch (JaxenException e) {
                handleException("Invalid source XPath  ",synCtx);
            }
        } else if (sKey != null) {
            setRequestKey((String) sKey);
        } else {
            handleException("The message 'source' must be specified for a Callout mediator", synCtx);
        }

        Object tXpath = EIPUtils.lookupFunctionParam(synCtx, "target_xpath");
        Object tKey = EIPUtils.lookupFunctionParam(synCtx, "target_key");

        if (tXpath != null && tXpath instanceof SynapseXPath) {
            try {
                setTargetXPath((SynapseXPath) tXpath);
            } catch (JaxenException e) {
                handleException("Invalid target XPath  ",synCtx);
            }
        } else if (tKey != null) {
            setTargetKey((String) tKey);
        } else {
            handleException("The message 'target' must be specified for a Callout mediator", synCtx);
        }


    }


    /**
     * Fault Handler
     * @param synCtx MessageContext
     * @param axisFault AxisFault
     */
    private void handleFault(MessageContext synCtx, AxisFault axisFault) {
        synCtx.setProperty(SynapseConstants.SENDING_FAULT, Boolean.TRUE);
        if (axisFault.getFaultCodeElement() != null) {
            synCtx.setProperty(SynapseConstants.ERROR_CODE,
                    axisFault.getFaultCodeElement().getText());
        } else {
            synCtx.setProperty(SynapseConstants.ERROR_CODE,
                    SynapseConstants.CALLOUT_OPERATION_FAILED);
        }

        if (axisFault.getFaultReasonElement() != null) {
            synCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                    axisFault.getFaultReasonElement().getText());
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

        synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, axisFault);
        throw new SynapseException("Error while performing the callout operation", axisFault);
    }

    /**
     *
     * @param synCtx MessageContext
     * @return null
     * @throws AxisFault
     */
    private OMElement getRequestPayload(MessageContext synCtx) throws AxisFault {

        if (getRequestKey() != null) {
            Object request = synCtx.getProperty(getRequestKey());
            if (request == null) {
                request = synCtx.getEntry(getRequestKey());
            }
            if (request != null && request instanceof OMElement) {
                return (OMElement) request;
            } else {
                handleException("The property : " + getRequestKey() + " is not an OMElement", synCtx);
            }
        } else if (getRequestXPath() != null) {
            try {
                Object o = getRequestXPath().evaluate(MessageHelper.cloneMessageContext(synCtx));

                if (o instanceof OMElement) {
                    return (OMElement) o;
                } else if (o instanceof List && !((List) o).isEmpty()) {
                    return (OMElement) ((List) o).get(0);  // Always fetches *only* the first
                } else {
                    handleException("The evaluation of the XPath expression : "
                            + getRequestXPath().toString() + " did not result in an OMElement", synCtx);
                }
            } catch (JaxenException e) {
                handleException("Error evaluating XPath expression : "
                        + getRequestXPath().toString(), e, synCtx);
            }
        }
        return null;
    }

    /**
     * Initialize synapse environment
     * @param synEnv SynapseEnvironment
     */
    public void init(SynapseEnvironment synEnv) {
        try {
            configCtx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    clientRepository != null ? clientRepository : DEFAULT_CLIENT_REPO,
                    axis2xml != null ? axis2xml : DEFAULT_AXIS2_XML);
        } catch (AxisFault e) {
            String msg = "Error initializing callout mediator : " + e.getMessage();
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    public void destroy() {
        try {
            configCtx.terminate();
        } catch (AxisFault ignore) {
        }
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL.set(serviceURL);
    }

    public void setAction(String action) {
        this.action.set(action);
    }

    public void setRequestKey(String requestKey) {
        this.requestKey.set(requestKey);
    }

    public void setRequestXPath(SynapseXPath requestXPath) throws JaxenException {
        this.requestXPath.set(requestXPath);
    }

    public void setTargetXPath(SynapseXPath targetXPath) throws JaxenException {
        this.targetXPath.set(targetXPath);
    }

    public void setTargetKey(String targetKey) {
        this.targetKey.set(targetKey);
    }

    public String getServiceURL() {
        return serviceURL.get();
    }

    public String getAction() {
        return action.get();
    }

    public String getRequestKey() {
        return requestKey.get();
    }

    public SynapseXPath getRequestXPath() {
        return requestXPath.get();
    }

    public SynapseXPath getTargetXPath() {
        return targetXPath.get();
    }

    public String getTargetKey() {
        return targetKey.get();
    }
}