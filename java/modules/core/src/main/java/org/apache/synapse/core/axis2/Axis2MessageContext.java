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

package org.apache.synapse.core.axis2;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.GetPropertyFunction;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.JaxenException;

import java.util.*;

public class Axis2MessageContext implements MessageContext {

    private static final Log log = LogFactory.getLog(Axis2MessageContext.class);

    private SynapseConfiguration cfg = null;
    private SynapseEnvironment   env = null;
    private Map properties = new HashMap();
    private Map correlationProperties = new HashMap();

    /** The Axis2 MessageContext reference */
    private org.apache.axis2.context.MessageContext axis2MessageContext = null;

    private boolean response = false;

    private boolean faultResponse = false;

    private int tracingState = Constants.TRACING_UNSET;

    public SynapseConfiguration getConfiguration() {
        return cfg;
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        this.cfg = cfg;
    }

    public SynapseEnvironment getEnvironment() {
        return env;
    }

    public void setEnvironment(SynapseEnvironment env) {
        this.env = env;
    }

    public Object getProperty(String key) {
        Object ret = properties.get(key);
        if (ret != null) {
            return ret;
        } else if (correlationProperties.get(key) != null) {
            return correlationProperties.get(key);
        } else if (getConfiguration() != null) {
            return getConfiguration().getProperty(key);
        } else {
            return null;
        }
    }

    public Object getCorrelationProperty(String key) {
        return correlationProperties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public void setCorrelationProperty(String key, Object value) {
        correlationProperties.put(key, value);
    }

    public Set getPropertyKeySet() {
        return properties.keySet();
    }

    public Set getCorrelationPropertyKeySet() {
        return correlationProperties.keySet();
    }

    //--------------------
    public Axis2MessageContext(org.apache.axis2.context.MessageContext axisMsgCtx,
                               SynapseConfiguration synCfg, SynapseEnvironment synEnv) {
        setAxis2MessageContext(axisMsgCtx);
        cfg = synCfg;
        env = synEnv;
    }

    public EndpointReference getFaultTo() {
        return axis2MessageContext.getFaultTo();
    }

    public void setFaultTo(EndpointReference reference) {
        axis2MessageContext.setFaultTo(reference);
    }

    public EndpointReference getFrom() {
        return axis2MessageContext.getFrom();
    }

    public void setFrom(EndpointReference reference) {
        axis2MessageContext.setFrom(reference);
    }

    public SOAPEnvelope getEnvelope() {
        return axis2MessageContext.getEnvelope();
    }

    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        axis2MessageContext.setEnvelope(envelope);
    }

    public String getMessageID() {
        return axis2MessageContext.getMessageID();
    }

    public void setMessageID(String string) {
        axis2MessageContext.setMessageID(string);
    }

    public RelatesTo getRelatesTo() {
        return axis2MessageContext.getRelatesTo();
    }

    public void setRelatesTo(RelatesTo[] reference) {
        axis2MessageContext.setRelationships(reference);
    }

    public EndpointReference getReplyTo() {
        return axis2MessageContext.getReplyTo();
    }

    public void setReplyTo(EndpointReference reference) {
        axis2MessageContext.setReplyTo(reference);
    }

    public EndpointReference getTo() {
        return axis2MessageContext.getTo();
    }

    public void setTo(EndpointReference reference) {
        axis2MessageContext.setTo(reference);
    }

    public void setWSAAction(String actionURI) {
        axis2MessageContext.setWSAAction(actionURI);
    }

    public String getWSAAction() {
        return axis2MessageContext.getWSAAction();
    }

    public void setWSAMessageID(String messageID) {
        axis2MessageContext.setWSAMessageId(messageID);
    }

    public String getWSAMessageID() {
        return axis2MessageContext.getMessageID();
    }

    public String getSoapAction() {
        return axis2MessageContext.getSoapAction();
    }

    public void setSoapAction(String string) {
        axis2MessageContext.setSoapAction(string);
    }

    public boolean isDoingMTOM() {
        return axis2MessageContext.isDoingMTOM();
    }

    public boolean isDoingSWA() {
        return axis2MessageContext.isDoingSwA();
    }

    public void setDoingMTOM(boolean b) {
        axis2MessageContext.setDoingMTOM(b);
    }

    public void setDoingSWA(boolean b) {
        axis2MessageContext.setDoingSwA(b);
    }

    public boolean isDoingPOX() {
        return axis2MessageContext.isDoingREST();
    }

    public void setDoingPOX(boolean b) {
        axis2MessageContext.setDoingREST(b);
    }

    public boolean isSOAP11() {
        return axis2MessageContext.isSOAP11();
    }

    public void setResponse(boolean b) {
        response = b;
        axis2MessageContext.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.valueOf(b));
    }

    public boolean isResponse() {
        return response;
    }

    public void setFaultResponse(boolean b) {
        this.faultResponse = b;
    }

    public boolean isFaultResponse() {
        return this.faultResponse;
    }

    public int getTracingState() {
        return tracingState;
    }

    public void setTracingState(int tracingState) {
        this.tracingState= tracingState;
    }

    public org.apache.axis2.context.MessageContext getAxis2MessageContext() {
        return axis2MessageContext;
    }

    public void setAxis2MessageContext(org.apache.axis2.context.MessageContext axisMsgCtx) {
        this.axis2MessageContext = axisMsgCtx;
        Boolean resp = (Boolean) axisMsgCtx.getProperty(Constants.ISRESPONSE_PROPERTY);
        if (resp != null)
            response = resp.booleanValue();
    }

    public void setPaused(boolean value) {
        axis2MessageContext.setPaused(value);
    }

    public boolean isPaused() {
        return axis2MessageContext.isPaused();
    }

    public boolean isServerSide() {
        return axis2MessageContext.isServerSide();
    }

    public void setServerSide(boolean value) {
        axis2MessageContext.setServerSide(value);
    }

    /**
     * Evaluates the given XPath expression against the SOAPEnvelope of the
     * current message and returns a String representation of the result
     * @param xpath the expression to evaluate
     * @param synCtx the source message which holds the SOAP envelope
     * @return a String representation of the result of evaluation
     */
    public static String getStringValue(AXIOMXPath xpath, MessageContext synCtx) {

        if (xpath != null) {
            try {
                // create an instance of a synapse:get-property() function and set it to the xpath
                GetPropertyFunction getPropertyFunc = new GetPropertyFunction();
                getPropertyFunc.setSynCtx(synCtx);

                // set function context into XPath
                SimpleFunctionContext fc = new XPathFunctionContext();
                fc.registerFunction(Constants.SYNAPSE_NAMESPACE, "get-property", getPropertyFunc);
                xpath.setFunctionContext(fc);

                // register namespace for XPath extension function
                xpath.addNamespace("synapse", Constants.SYNAPSE_NAMESPACE);

            } catch (JaxenException je) {
                handleException("Error setting up the Synapse XPath " +
                    "extension function for XPath : " + xpath, je);
            }

            try {
                Object result = xpath.evaluate(synCtx.getEnvelope());
                StringBuffer textValue = new StringBuffer();

                if (result instanceof List) {
                    Iterator iter = ((List) result).iterator();
                    while (iter.hasNext()) {
                        Object o = iter.next();
                        if (o instanceof OMTextImpl) {
                            textValue.append(((OMTextImpl) o).getText());
                        } else if (o instanceof OMElementImpl) {
                            textValue.append(((OMElementImpl) o).getText());
                        }
                    }
                } else {
                    textValue.append(result.toString());
                }
                return textValue.toString();

            } catch (JaxenException je) {
                handleException("Evaluation of the XPath expression " + xpath.toString() +
                    " resulted in an error", je);
            }
        } else {
            handleException("Invalid (null) XPath expression");
        }
        return null;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        String separator = "\n";

        if (getTo() != null)
            sb.append("To: " + getTo().getAddress());
        else
            sb.append("To: ");
        if (getFrom() != null)
            sb.append(separator + "From: " + getFrom().getAddress());
        if (getWSAAction() != null)
            sb.append(separator + "WSAction: " + getWSAAction());
        if (getSoapAction() != null)
            sb.append(separator + "SOAPAction: " + getSoapAction());
        if (getReplyTo() != null)
            sb.append(separator + "ReplyTo: " + getReplyTo().getAddress());
        if (getMessageID() != null)
            sb.append(separator + "MessageID: " + getMessageID());

        Iterator iter = getEnvelope().getHeader().examineAllHeaderBlocks();
        if (iter.hasNext()) {
            sb.append(separator + "Headers : ");
            while (iter.hasNext()) {
                SOAPHeader header = (SOAPHeader) iter.next();
                sb.append(separator + header.getLocalName() + " : " + header.getText());
            }
        }

        return sb.toString();
    }

    public static void setErrorInformation(MessageContext synCtx, SynapseException e) {
        synCtx.setProperty(Constants.ERROR_CODE, "00000"); //TODO not yet defined
        synCtx.setProperty(Constants.ERROR_MESSAGE, e.getMessage());
        synCtx.setProperty(Constants.ERROR_DETAIL, e.getStackTrace().toString());
    }
}
