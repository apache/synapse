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

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

/**
 * This delegates all method calls to thread specific MessageContext.
 * 
 * This is required for the InlineScriptMediator to enable concurrent requests to run against the same inline script. As there is a single BSFEngine
 * holding the inline script fragment and the MessageContext is pre-registered in that BSFEngine means there is a single MessageContext instance
 * shared over all requests. Using this class as the single pre-registered MessageContext enables delegating all the method calls to a thread specific
 * MessageContext instance that is registerd on the thread just prior to the script being invoked.
 */
public class ThreadLocalMessageContext implements MessageContext {

    private static ThreadLocal threadLocalMC = new ThreadLocal();

    public static void setMC(ScriptMessageContext mc) {
        threadLocalMC.set(mc);
    }

    public ScriptMessageContext getMC() {
        return (ScriptMessageContext) threadLocalMC.get();
    }

    // non-MessageContext helpers on the ScriptMessageContext class

    public Object getPayloadXML() {
        return getMC().getPayloadXML();
    }

    public void setPayloadXML(Object payload) {
        getMC().setPayloadXML(payload);
    }

    public Object getEnvelopeXML() {
        return getMC().getEnvelopeXML();
    }

    public void setTo(String reference) {
        getMC().setTo(reference);
    }

    public void setFaultTo(String reference) {
        getMC().setFaultTo(reference);
    }

    public void setFrom(String reference) {
        getMC().setFrom(reference);
    }

    public void setReplyTo(String reference) {
        getMC().setReplyTo(reference);
    }

    // -- all the MessageContext interface methods

    public SynapseConfiguration getConfiguration() {
        return getMC().getConfiguration();
    }

    public SOAPEnvelope getEnvelope() {
        return getMC().getEnvelope();
    }

    public SynapseEnvironment getEnvironment() {
        return getMC().getEnvironment();
    }

    public EndpointReference getFaultTo() {
        return getMC().getFaultTo();
    }

    public EndpointReference getFrom() {
        return getMC().getFrom();
    }

    public String getMessageID() {
        return getMC().getMessageID();
    }

    public String getWSAMessageID() {
        return getMC().getWSAMessageID();
    }

    public Object getProperty(String key) {
        return getMC().getProperty(key);
    }

    public Object getCorrelationProperty(String key) {
        return getMC().getCorrelationProperty(key);
    }

    public Set getPropertyKeySet() {
        return getMC().getPropertyKeySet();
    }

    public RelatesTo getRelatesTo() {
        return getMC().getRelatesTo();
    }

    public EndpointReference getReplyTo() {
        return getMC().getReplyTo();
    }

    public String getSoapAction() {
        return getMC().getSoapAction();
    }

    public EndpointReference getTo() {
        return getMC().getTo();
    }

    public String getWSAAction() {
        return getMC().getWSAAction();
    }

    public boolean isDoingMTOM() {
        return getMC().isDoingMTOM();
    }

    public boolean isDoingSWA() {
        return getMC().isDoingSWA();
    }

    public boolean isDoingPOX() {
        return getMC().isDoingPOX();
    }

    public boolean isFaultResponse() {
        return getMC().isFaultResponse();
    }

    public int getTracingState() {
        return getMC().getTracingState();
    }

    public boolean isResponse() {
        return getMC().isResponse();
    }

    public boolean isSOAP11() {
        return getMC().isSOAP11();
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        getMC().setConfiguration(cfg);
    }

    public void setTracingState(int tracingState) {
        getMC().setTracingState(tracingState);
    }

    public void setDoingMTOM(boolean b) {
        getMC().setDoingMTOM(b);
    }

    public void setDoingSWA(boolean b) {
        getMC().setDoingSWA(b);
    }

    public void setDoingPOX(boolean b) {
        getMC().setDoingPOX(b);
    }

    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        getMC().setEnvelope(envelope);
    }

    public void setEnvironment(SynapseEnvironment se) {
        getMC().setEnvironment(se);
    }

    public void setFaultResponse(boolean b) {
        getMC().setFaultResponse(b);
    }

    public void setFaultTo(EndpointReference reference) {
        getMC().setFaultTo(reference);
    }

    public void setFrom(EndpointReference reference) {
        getMC().setFrom(reference);
    }

    public void setMessageID(String string) {
        getMC().setMessageID(string);
    }

    public void setWSAMessageID(String messageID) {
        getMC().setWSAMessageID(messageID);
    }

    public void setProperty(String key, Object value) {
        getMC().setProperty(key, value);
    }

    public void setCorrelationProperty(String key, Object value) {
        getMC().setCorrelationProperty(key, value);
    }

    public Set getCorrelationPropertyKeySet() {
        return getMC().getCorrelationPropertyKeySet();
    }

    public void setRelatesTo(RelatesTo[] reference) {
        getMC().setRelatesTo(reference);
    }

    public void setReplyTo(EndpointReference reference) {
        getMC().setReplyTo(reference);
    }

    public void setResponse(boolean b) {
        getMC().setResponse(b);
    }

    public void setSoapAction(String string) {
        getMC().setSoapAction(string);
    }

    public void setTo(EndpointReference reference) {
        getMC().setTo(reference);
    }

    public void setWSAAction(String actionURI) {
        getMC().setWSAAction(actionURI);
    }

}
