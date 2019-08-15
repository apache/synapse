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

package org.apache.synapse.mediators.bsf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Map;

import javax.script.ScriptException;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.bsf.xml.XMLHelper;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.xml.XMLObject;

/**
 * ScriptMessageContext decorates the Synapse MessageContext adding methods to use the
 * message payload XML in a way natural to the scripting languageS
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ScriptMessageContext implements MessageContext {

    /** The actual Synapse message context reference */
    private final MessageContext mc;
    /** The OMElement to scripting language object converter for the selected language */
    private final XMLHelper xmlHelper;

    public ScriptMessageContext(MessageContext mc, XMLHelper xmlHelper) {
        this.mc = mc;
        this.xmlHelper = xmlHelper;
    }

    /**
     * Get the XML representation of SOAP Body payload.
     * The payload is the first element inside the SOAP <Body> tags
     *
     * @return the XML SOAP Body
     * @throws ScriptException in-case of an error in getting
     * the XML representation of SOAP Body payload
     */
    public Object getPayloadXML() throws ScriptException {
        return xmlHelper.toScriptXML(mc.getEnvelope().getBody().getFirstElement());
    }

    /**
     * Set the SOAP body payload from XML
     *
     * @param payload Message payload
     * @throws ScriptException For errors in converting xml To OM
     * @throws OMException     For errors in OM manipulation
     */

    public void setPayloadXML(Object payload) throws OMException, ScriptException {
        SOAPBody body = mc.getEnvelope().getBody();
        OMElement firstChild = body.getFirstElement();
        OMElement omElement = xmlHelper.toOMElement(payload);
        if (firstChild == null) {
            body.addChild(omElement);
        } else {
            firstChild.insertSiblingAfter(omElement);
            firstChild.detach();
        }
    }

    /**
     * Add a new SOAP header to the message.
     * 
     * @param mustUnderstand the value for the <code>soapenv:mustUnderstand</code> attribute
     * @param content the XML for the new header
     * @throws ScriptException if an error occurs when converting the XML to OM
     */
    public void addHeader(boolean mustUnderstand, Object content) throws ScriptException {
        SOAPHeader header = mc.getEnvelope().getOrCreateHeader();
        OMElement element = xmlHelper.toOMElement(content);
        // We can't add the element directly to the SOAPHeader. Instead, we need to copy the
        // information over to a SOAPHeaderBlock.
        SOAPHeaderBlock headerBlock = header.addHeaderBlock(element.getLocalName(),
                element.getNamespace());
        for (Iterator it = element.getAllAttributes(); it.hasNext(); ) {
            headerBlock.addAttribute((OMAttribute)it.next());
        }
        headerBlock.setMustUnderstand(mustUnderstand);
        OMNode child = element.getFirstOMChild();
        while (child != null) {
            // Get the next child before addChild will detach the node from its original place. 
            OMNode next = child.getNextOMSibling();
            headerBlock.addChild(child);
            child = next;
        }
    }
    
    /**
     * Get the XML representation of the complete SOAP envelope
     * @return return an object that represents the payload in the current scripting language
     * @throws ScriptException in-case of an error in getting
     * the XML representation of SOAP envelope
     */
    public Object getEnvelopeXML() throws ScriptException {
        return xmlHelper.toScriptXML(mc.getEnvelope());
    }

    // helpers to set EPRs from a script string
    public void setTo(String reference) {
        mc.setTo(new EndpointReference(reference));
    }

    public void setFaultTo(String reference) {
        mc.setFaultTo(new EndpointReference(reference));
    }

    public void setFrom(String reference) {
        mc.setFrom(new EndpointReference(reference));
    }

    public void setReplyTo(String reference) {
        mc.setReplyTo(new EndpointReference(reference));
    }

    // -- all the remainder just use the underlying MessageContext
    public SynapseConfiguration getConfiguration() {
        return mc.getConfiguration();
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        mc.setConfiguration(cfg);
    }

    public SynapseEnvironment getEnvironment() {
        return mc.getEnvironment();
    }

    public void setEnvironment(SynapseEnvironment se) {
        mc.setEnvironment(se);
    }

    public Map<String, Object> getContextEntries() {
        return mc.getContextEntries();
    }

    public void setContextEntries(Map<String, Object> entries) {
        mc.setContextEntries(entries);
    }

    public Object getProperty(String key) {
        return mc.getProperty(key);
    }

    public Object getEntry(String key) {
        return mc.getEntry(key);
    }

    public Object getLocalEntry(String key) {
        return mc.getLocalEntry(key);
    }

    public void setProperty(String key, Object value) {
        if (value instanceof XMLObject) {
            OMElement omElement = null;
            try {
                omElement = xmlHelper.toOMElement(value);
            } catch (ScriptException e) {
                mc.setProperty(key, value);
            }
            if (omElement != null) {
                mc.setProperty(key, omElement);
            }
        } else {
            mc.setProperty(key, value);
        }
    }

    /**
     * Set Property to the message context
     *
     * @param key   property name
     * @param value property value
     * @param scope scope of the property
     */
    public void setProperty(String key, Object value, String scope) {
        if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
            // Setting property into default scope
            setProperty(key, value);
        } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)) {
            // Setting property into the  Axis2 Message Context
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            axis2MessageCtx.setProperty(key, value);
        } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)) {
            // Setting Transport Headers
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();

            Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers == null) {
                Map headersMap = new HashMap();
                headersMap.put(key, value);
                axis2MessageCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
            } else if (headers instanceof Map) {
                Map headersMap = (Map) headers;
                headersMap.put(key, value);
            }

        }
    }

    /**
     * Remove property from message context
     * Scope: Default
     *
     * @param key Property name
     */
    public void removeProperty(String key) {
        Set pros = mc.getPropertyKeySet();
        if (pros != null) {
            pros.remove(key);
        }
    }

    /**
     * Remove property from message context
     *
     * @param key   Property name
     * @param scope Property scope
     */
    public void removeProperty(String key, String scope) {
        if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
            // Removing property from default scope
            removeProperty(key);
        } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)) {
            // Removing property from the Axis2 Message Context
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            axis2MessageCtx.removeProperty(key);

        } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)) {
            // Removing transport headers
            Axis2MessageContext axis2smc = (Axis2MessageContext) mc;
            org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
            Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null && headers instanceof Map) {
                Map headersMap = (Map) headers;
                headersMap.remove(key);
            }
        }
    }

    public Set getPropertyKeySet() {
        return mc.getPropertyKeySet();
    }

    public Mediator getMainSequence() {
        return mc.getMainSequence();
    }

    public Mediator getFaultSequence() {
        return mc.getFaultSequence();
    }

    public Mediator getSequence(String key) {
        return mc.getSequence(key);
    }

    public Endpoint getEndpoint(String key) {
        return mc.getEndpoint(key);
    }

    public SOAPEnvelope getEnvelope() {
        return mc.getEnvelope();
    }

    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        mc.setEnvelope(envelope);
    }

    public EndpointReference getFaultTo() {
        return mc.getFaultTo();
    }

    public void setFaultTo(EndpointReference reference) {
        mc.setFaultTo(reference);
    }

    public EndpointReference getFrom() {
        return mc.getFrom();
    }

    public void setFrom(EndpointReference reference) {
        mc.setFrom(reference);
    }

    public String getMessageID() {
        return mc.getMessageID();
    }

    public void setMessageID(String string) {
        mc.setMessageID(string);
    }

    public RelatesTo getRelatesTo() {
        return mc.getRelatesTo();
    }

    public void setRelatesTo(RelatesTo[] reference) {
        mc.setRelatesTo(reference);
    }

    public EndpointReference getReplyTo() {
        return mc.getReplyTo();
    }

    public void setReplyTo(EndpointReference reference) {
        mc.setReplyTo(reference);
    }

    public EndpointReference getTo() {
        return mc.getTo();
    }

    public void setTo(EndpointReference reference) {
        mc.setTo(reference);
    }

    public void setWSAAction(String actionURI) {
        mc.setWSAAction(actionURI);
    }

    public String getWSAAction() {
        return mc.getWSAAction();
    }

    public String getSoapAction() {
        return mc.getSoapAction();
    }

    public void setSoapAction(String string) {
        mc.setSoapAction(string);
    }

    public void setWSAMessageID(String messageID) {
        mc.setWSAMessageID(messageID);
    }

    public String getWSAMessageID() {
        return mc.getWSAMessageID();
    }

    public boolean isDoingMTOM() {
        return mc.isDoingMTOM();
    }

    public boolean isDoingSWA() {
        return mc.isDoingSWA();
    }

    public void setDoingMTOM(boolean b) {
        mc.setDoingMTOM(b);
    }

    public void setDoingSWA(boolean b) {
        mc.setDoingSWA(b);
    }

    public boolean isDoingPOX() {
        return mc.isDoingPOX();
    }

    public void setDoingPOX(boolean b) {
        mc.setDoingPOX(b);
    }

    public boolean isDoingGET() {
        return mc.isDoingGET();
    }

    public void setDoingGET(boolean b) {
        mc.setDoingGET(b);
    }

    public boolean isSOAP11() {
        return mc.isSOAP11();
    }

    public void setResponse(boolean b) {
        mc.setResponse(b);
    }

    public boolean isResponse() {
        return mc.isResponse();
    }

    public void setFaultResponse(boolean b) {
        mc.setFaultResponse(b);
    }

    public boolean isFaultResponse() {
        return mc.isFaultResponse();
    }

    public int getTracingState() {
        return mc.getTracingState();
    }

    public void setTracingState(int tracingState) {
        mc.setTracingState(tracingState);
    }

    public Stack<FaultHandler> getFaultStack() {
        return mc.getFaultStack();
    }

    public void pushFaultHandler(FaultHandler fault) {
        mc.pushFaultHandler(fault);
    }

    public Log getServiceLog() {
        return LogFactory.getLog(ScriptMessageContext.class);
    }

    /**
     * Get the sequence template from the key
     * @param key the sequence key to be looked up
     * @return the sequence template
     */
    public Mediator getSequenceTemplate(String key) {
        return mc.getSequenceTemplate(key);
    }
}
