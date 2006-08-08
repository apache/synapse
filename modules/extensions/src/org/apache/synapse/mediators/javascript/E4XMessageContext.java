/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.mediators.javascript;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.xmlbeans.XmlObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.util.Set;

/**
 * E4XMessageContext decorates the Synapse MessageContext adding methods to use the 
 * message XML as E4X XML objects.
 */
public class E4XMessageContext implements MessageContext {

    private MessageContext mc;

    private Scriptable scope;

    public E4XMessageContext(MessageContext mc, Scriptable scope) {
        this.mc = mc;
        this.scope = scope;
    }

    /**
     * Get the SOAP Envelope as an E4X XML object
     * 
     * @return the XML SOAP Body
     */
    public Scriptable getEnvelopeXML() {
        byte[] xmlBytes = mc.getEnvelope().toString().getBytes();
        Scriptable jsXML = toE4XXML(xmlBytes);
        return jsXML;
    }

    /**
     * Set the SOAP envelope from an E4X XML object
     * 
     * @param envelope
     */
    public void setEnvelopeXML(Object envelope) {
        // TODO
    }

    /**
     * Get the SOAP Body payload as an E4X XML object
     * 
     * @return the XML SOAP Body
     */
    public Scriptable getPayloadXML() {
        byte[] xmlBytes = mc.getEnvelope().getBody().getFirstElement().toString().getBytes();
        Scriptable jsXML = toE4XXML(xmlBytes);
        return jsXML;
    }

    /**
     * Set the SOAP body payload from an E4X XML object
     * 
     * @param payload
     * @throws XMLStreamException 
     */
    public void setPayloadXML(Object payload) throws XMLStreamException {
        byte[] xmlBytes = payload.toString().getBytes();
        StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(xmlBytes));
        OMElement omElement = builder.getDocumentElement();
        mc.getEnvelope().getBody().setFirstChild(omElement);
    }

    private Scriptable toE4XXML(byte[] xmlBytes) {
        XmlObject xml;
        try {
            xml = XmlObject.Factory.parse(new ByteArrayInputStream(xmlBytes));
        } catch (Exception e) {
            throw new SynapseException("exception getting message XML: " + e);
        }

        Context cx = Context.enter();
        try {

            Object wrappedXML = cx.getWrapFactory().wrap(cx, scope, xml, XmlObject.class);
            Scriptable jsXML = cx.newObject(scope, "XML", new Object[] { wrappedXML });

            return jsXML;

        } finally {
            Context.exit();
        }
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

    public Object getProperty(String key) {
        return mc.getConfiguration();
    }

    public void setProperty(String key, Object value) {
        mc.setProperty(key, value);
    }

    public Set getPropertyKeySet() {
        return mc.getPropertyKeySet();
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

    public void setMessageId(String messageID) {
        mc.setMessageId(messageID);
    }

    public String getMessageId() {
        return mc.getMessageId();
    }

    public boolean isDoingMTOM() {
        return mc.isDoingMTOM();
    }

    public void setDoingMTOM(boolean b) {
        mc.setDoingMTOM(b);
    }

    public boolean isDoingREST() {
        return mc.isDoingREST();
    }

    public void setDoingREST(boolean b) {
        mc.setDoingREST(b);
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

}