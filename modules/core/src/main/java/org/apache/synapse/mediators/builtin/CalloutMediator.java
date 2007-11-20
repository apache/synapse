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

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

import java.util.List;

/**
 * <callout serviceURL="string" [action="string"]>
 *      <source xpath="expression" | key="string"> <!-- key can be a MC property or entry key -->
 *      <target xpath="expression" | key="string"/>
 * </callout>
 */
public class CalloutMediator extends AbstractMediator implements ManagedLifecycle {

    private ServiceClient sc = null;
    private String serviceURL = null;
    private String action = null;
    private String requestKey = null;
    private String requestXPathString = null;
    private AXIOMXPath requestXPath = null;
    private String targetXPathString = null;
    private AXIOMXPath targetXPath = null;
    private String targetKey = null;

    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Callout mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        try {
            Options options = new Options();
            options.setTo(new EndpointReference(serviceURL));
            options.setAction(action);
            options.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
            sc.setOptions(options);

            OMElement request = getRequestPayload(synCtx);
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "About to invoke service : " + serviceURL +
                    " with action : " + action);
                if (traceOn && trace.isTraceEnabled()) {
                    trace.trace("Request message payload : " + request);
                }
            }

            OMElement result = sc.sendReceive(request);

            if (traceOrDebugOn) {
                if (traceOn && trace.isTraceEnabled()) {
                    trace.trace("Response payload received : " + result);
                }
            }

            if (result != null) {
                if (targetXPath != null) {
                    Object o = targetXPath.evaluate(synCtx.getEnvelope());

                    if (o != null && o instanceof OMElement) {
                        OMNode tgtNode = (OMElement) o;
                        tgtNode.insertSiblingAfter(result);
                        tgtNode.detach();
                    } else if (o != null && o instanceof List && !((List) o).isEmpty()) {
                        OMNode tgtNode = (OMElement) ((List) o).get(0);  // Always fetches *only* the first
                        tgtNode.insertSiblingAfter(result);
                        tgtNode.detach();
                    } else {
                        handleException("Evaluation of target XPath expression : " +
                            targetXPathString + " did not yeild an OMNode", synCtx);
                    }
                } if (targetKey != null) {
                    synCtx.setProperty(targetKey, result);
                }
            } else {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Service returned a null response");
                }
            }

        } catch (Exception e) {
            handleException("Error invoking service : " + serviceURL + " with action : " + action, e, synCtx);
        }

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Callout mediator");
        }
        return true;
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
                Object o = null;
                o = requestXPath.evaluate(
                    MessageHelper.cloneMessageContext(synCtx).getEnvelope());

                if (o instanceof OMElement) {
                    return (OMElement) o;
                } else if (o instanceof List && !((List) o).isEmpty()) {
                    return (OMElement) ((List) o).get(0);  // Always fetches *only* the first
                } else {
                    handleException("The evaluation of the XPath expression : "
                        + requestXPathString + " did not result in an OMElement", synCtx);
                }
            } catch (JaxenException e) {
                handleException("Error evaluating XPath expression : " + requestXPathString, e, synCtx);
            }
        }
        return null;
    }

    public void init(SynapseEnvironment synEnv) {
        try {
            ConfigurationContext cfgCtx =
                ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    "./samples/axis2Client/client_repo", "./samples/axis2Client/client_repo/conf/axis2.xml");
            sc = new ServiceClient(cfgCtx, null);
        } catch (AxisFault e) {
            String msg = "Error initializing callout mediator : " + e.getMessage();
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    public void destroy() {
        try {
            sc.cleanup();
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

    public String getRequestXPathString() {
        return requestXPathString;
    }

    public void setRequestXPathString(String requestXPathString) throws JaxenException {
        this.requestXPathString = requestXPathString;
        this.requestXPath = new AXIOMXPath(requestXPathString);
    }

    public String getTargetXPathString() {
        return targetXPathString;
    }

    public void setTargetXPathString(String targetXPathString) throws JaxenException {
        this.targetXPathString = targetXPathString;
        this.targetXPath = new AXIOMXPath(targetXPathString);
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }

    public AXIOMXPath getRequestXPath() {
        return requestXPath;
    }

    public AXIOMXPath getTargetXPath() {
        return targetXPath;
    }
}
