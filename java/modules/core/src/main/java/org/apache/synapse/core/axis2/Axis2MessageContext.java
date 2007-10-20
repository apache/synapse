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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.*;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.GetPropertyFunction;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.jaxen.JaxenException;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;

import java.util.*;

/**
 * This is the MessageContext implementation that synapse uses almost all the time because Synapse
 * is implemented on top of the Axis2
 */
public class Axis2MessageContext implements MessageContext {

    private static final Log log = LogFactory.getLog(Axis2MessageContext.class);

    /** Holds the reference to the Synapse Message Context */
    private SynapseConfiguration synCfg = null;

    /** Holds the environment on which synapse operates */
    private SynapseEnvironment synEnv = null;

    /** Synapse Message Context properties */
    private Map properties = new HashMap();

    /**
     * Local entries fetched from the configuration or from the registry for the transactional
     * resource access
     */
    private Map localEntries = new HashMap();

    /** Fault Handler stack which will be popped and called the handleFault in error states */
    private Stack faultStack = new Stack();

    /** The Axis2 MessageContext reference */
    private org.apache.axis2.context.MessageContext axis2MessageContext = null;

    /** Attribute of the MC specifying whether this is a response or not */
    private boolean response = false;

    /** Attribute specifying whether this MC corresponds to fault response or not */
    private boolean faultResponse = false;

    /** Attribute of MC stating the tracing state of the message */
    private int tracingState = SynapseConstants.TRACING_UNSET;

    /** The service log for this message */
    private Log serviceLog = null;

    public SynapseConfiguration getConfiguration() {
        return synCfg;
    }

    public void setConfiguration(SynapseConfiguration synCfg) {
        this.synCfg = synCfg;
    }

    public SynapseEnvironment getEnvironment() {
        return synEnv;
    }

    public void setEnvironment(SynapseEnvironment synEnv) {
        this.synEnv = synEnv;
    }

    public Mediator getMainSequence() {
        Object o = localEntries.get(SynapseConstants.MAIN_SEQUENCE_KEY);
        if (o != null && o instanceof Mediator) {
            return (Mediator) o;
        } else {
            Mediator main = getConfiguration().getMainSequence();
            localEntries.put(SynapseConstants.MAIN_SEQUENCE_KEY, main);
            return main;
        }
    }

    public Mediator getFaultSequence() {
        Object o = localEntries.get(SynapseConstants.FAULT_SEQUENCE_KEY);
        if (o != null && o instanceof Mediator) {
            return (Mediator) o;
        } else {
            Mediator fault = getConfiguration().getFaultSequence();
            localEntries.put(SynapseConstants.FAULT_SEQUENCE_KEY, fault);
            return fault;
        }
    }

    public Mediator getSequence(String key) {
        Object o = localEntries.get(key);
        if (o != null && o instanceof Mediator) {
            return (Mediator) o;
        } else {
            Mediator m = getConfiguration().getSequence(key);
            localEntries.put(key, m);
            return m;
        }
    }

    public Endpoint getEndpoint(String key) {
        Object o = localEntries.get(key);
        if (o != null && o instanceof Endpoint) {
            return (Endpoint) o;
        } else {
            Endpoint e = getConfiguration().getEndpoint(key);
            localEntries.put(key, e);
            return e;
        }
    }

    public Object getEntry(String key) {
        Object o = localEntries.get(key);
        if (o != null && o instanceof Entry) {
            return ((Entry) o).getValue();
        } else {
            Object e = getConfiguration().getEntry(key);
            if (e != null) {
                localEntries.put(key, e);
                return e;
            } else {
                getConfiguration().getEntryDefinition(key);
                return getConfiguration().getEntry(key);
            }
        }
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);

        // do not commit response by default in the server process
        if (SynapseConstants.RESPONSE.equals(key) &&
                getAxis2MessageContext().getOperationContext() != null) {
            getAxis2MessageContext().getOperationContext().setProperty(
                org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");
        }
    }

    public Set getPropertyKeySet() {
        return properties.keySet();
    }

    /**
     * Constructor for the Axis2MessageContext inside Synapse
     *
     * @param axisMsgCtx MessageContext representing the relevant Axis MC
     * @param synCfg SynapseConfiguraion describing Synapse
     * @param synEnv SynapseEnvironment describing the environment of Synapse
     */
    public Axis2MessageContext(org.apache.axis2.context.MessageContext axisMsgCtx,
                               SynapseConfiguration synCfg, SynapseEnvironment synEnv) {
        setAxis2MessageContext(axisMsgCtx);
        this.synCfg = synCfg;
        this.synEnv = synEnv;
        this.pushFaultHandler(new MediatorFaultHandler(synCfg.getFaultSequence()));
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
        axis2MessageContext.setProperty(SynapseConstants.ISRESPONSE_PROPERTY, Boolean.valueOf(b));
    }

    public boolean isResponse() {
        Object o = properties.get(SynapseConstants.RESPONSE);
        if (o != null && o instanceof String && ((String) o).equalsIgnoreCase("true")) {
            return true;
        }
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

    public Stack getFaultStack() {
        return this.faultStack;
    }

    public void pushFaultHandler(FaultHandler fault) {
        this.faultStack.push(fault);
    }

    /**
     * Return the service level Log for this message context or null
     * @return the service level Log for the message
     */
    public Log getServiceLog() {

        if (serviceLog != null) {
            return serviceLog;
        } else {
            String serviceName = (String) getProperty(SynapseConstants.PROXY_SERVICE);
            if (serviceName != null && synCfg.getProxyService(serviceName) != null) {
                serviceLog = LogFactory.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX + serviceName);
                return serviceLog;
            } else {
                serviceLog = LogFactory.getLog(
                    SynapseConstants.SERVICE_LOGGER_PREFIX.substring(0,
                    SynapseConstants.SERVICE_LOGGER_PREFIX.length()-1));
                return serviceLog;
            }
        }
    }

    /**
     * Set the service log
     * @param serviceLog
     */
    public void setServiceLog(Log serviceLog) {
        this.serviceLog = serviceLog;
    }

    public org.apache.axis2.context.MessageContext getAxis2MessageContext() {
        return axis2MessageContext;
    }

    public void setAxis2MessageContext(org.apache.axis2.context.MessageContext axisMsgCtx) {
        this.axis2MessageContext = axisMsgCtx;
        Boolean resp = (Boolean) axisMsgCtx.getProperty(SynapseConstants.ISRESPONSE_PROPERTY);
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

        synchronized(xpath) {

        if (xpath != null) {
            try {
                // create an instance of a synapse:get-property() function and set it to the xpath
                GetPropertyFunction getPropertyFunc = new GetPropertyFunction();
                getPropertyFunc.setSynCtx(synCtx);

                // set function context into XPath
                SimpleFunctionContext fc = new XPathFunctionContext();
                fc.registerFunction(SynapseConstants.SYNAPSE_NAMESPACE, "get-property", getPropertyFunc);
                fc.registerFunction(null, "get-property", getPropertyFunc);
                xpath.setFunctionContext(fc);

                // register namespace for XPath extension function
                xpath.addNamespace("synapse", SynapseConstants.SYNAPSE_NAMESPACE);
                xpath.addNamespace("syn", SynapseConstants.SYNAPSE_NAMESPACE);

            } catch (JaxenException je) {
                handleException("Error setting up the Synapse XPath " +
                    "extension function for XPath : " + xpath, je);
            }
            try {
                Object result = xpath.evaluate(synCtx.getEnvelope());
                if (result == null) {
                    return null;
                }
                StringBuffer textValue = new StringBuffer();
                if (result instanceof List) {
                    List list = (List) result;
                    Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        Object o = iter.next();
                        if (o == null && list.size() == 1) {
                            return null;
                        }
                        if (o instanceof OMTextImpl) {
                            textValue.append(((OMTextImpl) o).getText());
                        } else if (o instanceof OMElementImpl) {
                            String s = ((OMElementImpl) o).getText();
                            if (s.trim().length() == 0) {
                                s = o.toString();
                            }
                            textValue.append(s);
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

        SOAPHeader soapHeader = getEnvelope().getHeader();
        if (soapHeader != null) {
            sb.append(separator + "Headers : ");
            for (Iterator iter = soapHeader.examineAllHeaderBlocks(); iter.hasNext();) {
                Object o = iter.next();
                if (o instanceof SOAPHeaderBlock) {
                    SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) o;
                    sb.append(separator + headerBlock.getLocalName() + " : " + headerBlock.getText());
                } else if (o instanceof OMElement) {
                    OMElement headerElem = (OMElement) o;
                    sb.append(separator + headerElem.getLocalName() + " : " + headerElem.getText());
                }
            }
        }

        return sb.toString();
    }


}
