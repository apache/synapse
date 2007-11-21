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

import java.util.Set;
import java.util.Stack;
import java.util.Map;

import javax.script.ScriptException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.bsf.xml.XMLHelper;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ScriptMessageContext decorates the Synapse MessageContext adding methods to use the
 * message payload XML in a way natural to the scripting languageS
 */
public class ScriptMessageContext implements MessageContext {

    /** The actual Synapse message context reference */
    private MessageContext mc;
    /** The OMElement to scripting language object converter for the selected language */
    private XMLHelper xmlHelper;

    public ScriptMessageContext(MessageContext mc, XMLHelper xmlHelper) {
        this.mc = mc;
        this.xmlHelper = xmlHelper;
    }

    /**
     * Get the XML representation of SOAP Body payload. 
     * The payload is the first element inside the SOAP <Body> tags
     * 
     * @return the XML SOAP Body
     * @throws ScriptException 
     * @throws OMException 
     */
    public Object getPayloadXML() throws OMException, ScriptException {
        return xmlHelper.toScriptXML(mc.getEnvelope().getBody().getFirstElement());
    }

    /**
     * Set the SOAP body payload from XML
     * 
     * @param payload
     * @throws ScriptException 
     * @throws OMException 
     * 
     */

    public void setPayloadXML(Object payload) throws OMException, ScriptException {
        OMElement firstChild = mc.getEnvelope().getBody().getFirstElement();
        firstChild.insertSiblingAfter(xmlHelper.toOMElement(payload));
        firstChild.detach();
    }

    /**
     * Get the XML representation of the complete SOAP envelope
     * @return return an object that represents the payload in the current scripting language
     * @throws ScriptException 
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

    public Map getContextEntries() {
        return mc.getContextEntries();
    }

    public void setContextEntries(Map entries) {
        mc.setContextEntries(entries);
    }

    public Object getProperty(String key) {
        return mc.getProperty(key);
    }

    public Object getEntry(String key) {
        return mc.getEntry(key);
    }

    public void setProperty(String key, Object value) {
        mc.setProperty(key, value);
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

    public Stack getFaultStack() {
        return mc.getFaultStack();
    }

    public void pushFaultHandler(FaultHandler fault) {
        mc.pushFaultHandler(fault);
    }

    public Log getServiceLog() {
        return LogFactory.getLog(ScriptMessageContext.class);
    }
}
