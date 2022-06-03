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

package org.apache.synapse;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.mediators.template.TemplateMediator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class TestMessageContext implements MessageContext {

    private Map properties = new HashMap();

    private Map<String, Object> localEntries = new HashMap<String, Object>();

    private Stack<FaultHandler> faultStack = new Stack<FaultHandler>();

    private SynapseConfiguration synCfg = null;

    private SynapseEnvironment synEnv;

    SOAPEnvelope envelope = null;

    private EndpointReference to = null;

    private String soapAction = null;
    
    @Override
    public SynapseConfiguration getConfiguration() {
        return synCfg;
    }

    @Override
    public void setConfiguration(SynapseConfiguration cfg) {
        this.synCfg = cfg;
    }

    @Override
    public SynapseEnvironment getEnvironment() {
        return synEnv;
    }

    @Override
    public void setEnvironment(SynapseEnvironment se) {
        synEnv = se;
    }

    @Override
    public Map<String, Object> getContextEntries() {
        return localEntries;
    }

    @Override
    public void setContextEntries(Map<String, Object> entries) {
        this.localEntries = entries;
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public Object getEntry(String key) {
        Object ret = properties.get(key);
        if (ret != null) {
            return ret;
        } else if (getConfiguration() != null) {
            return getConfiguration().getEntry(key);
        } else {
            return null;
        }
    }

    @Override
    public Object getLocalEntry(String key) {
        Object ret = properties.get(key);
        if (ret != null) {
            return ret;
        } else if (getConfiguration() != null) {
            return getConfiguration().getLocalRegistryEntry(key);
        } else {
            return null;
        }
    }

    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    @Override
    public Set getPropertyKeySet() {
        return properties.keySet();
    }

    @Override
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

    @Override
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

    @Override
    public Mediator getSequence(String key) {
        Object o = localEntries.get(key);
        if (o != null && o instanceof Mediator) {
            return (Mediator) o;
        } else {
            Mediator m = getConfiguration().getSequence(key);
            if (m instanceof SequenceMediator) {
                SequenceMediator seqMediator = (SequenceMediator) m;
                synchronized (m) {
                    if (!seqMediator.isInitialized()) {
                        seqMediator.init(synEnv);
                    }
                }
            }
            localEntries.put(key, m);
            return m;
        }
    }

    @Override
    public Endpoint getEndpoint(String key) {
        Object o = localEntries.get(key);
        if (o != null && o instanceof Endpoint) {
            return (Endpoint) o;
        } else {
            Endpoint e = getConfiguration().getEndpoint(key);
            synchronized (e) {
                if (!e.isInitialized()) {
                    e.init(synEnv);
                }
            }
            localEntries.put(key, e);
            return e;
        }
    }

    //---------
    @Override
    public SOAPEnvelope getEnvelope() {
        if (envelope == null)
            return OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        else
            return envelope;
    }

    @Override
    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        this.envelope = envelope;
    }

    @Override
    public EndpointReference getFaultTo() {
        return null;
    }

    @Override
    public void setFaultTo(EndpointReference reference) {
    }

    @Override
    public EndpointReference getFrom() {
        return null;
    }

    @Override
    public void setFrom(EndpointReference reference) {
    }

    @Override
    public String getMessageID() {
        return null;
    }

    @Override
    public void setMessageID(String string) {
    }

    @Override
    public RelatesTo getRelatesTo() {
        return null;
    }

    @Override
    public void setRelatesTo(RelatesTo[] reference) {
    }

    @Override
    public EndpointReference getReplyTo() {
        return null;
    }

    @Override
    public void setReplyTo(EndpointReference reference) {
    }

    @Override
    public EndpointReference getTo() {
        return to;
    }

    @Override
    public void setTo(EndpointReference reference) {
        to = reference;
    }

    @Override
    public void setWSAAction(String actionURI) {
    }

    @Override
    public String getWSAAction() {
        return null;
    }

    @Override
    public String getSoapAction() {
        return soapAction;
    }

    @Override
    public void setSoapAction(String string) {
        soapAction = string;
    }

    @Override
    public void setWSAMessageID(String messageID) {
        // TODO
    }

    @Override
    public String getWSAMessageID() {
        return null;  // TODO
    }

    @Override
    public boolean isDoingMTOM() {
        return false;
    }

    @Override
    public boolean isDoingSWA() {
        return false;
    }

    @Override
    public void setDoingMTOM(boolean b) {
    }

    @Override
    public void setDoingSWA(boolean b) {
    }

    @Override
    public boolean isDoingPOX() {
        return false;
    }

    @Override
    public void setDoingPOX(boolean b) {
    }

    @Override
    public boolean isDoingGET() {
        return false;
    }

    @Override
    public void setDoingGET(boolean b) {
    }

    @Override
    public boolean isSOAP11() {
        return envelope.getNamespace().getNamespaceURI().equals(
            SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    }

    @Override
    public void setResponse(boolean b) {
    }

    @Override
    public boolean isResponse() {
        return false;
    }

    @Override
    public void setFaultResponse(boolean b) {
    }

    @Override
    public boolean isFaultResponse() {
        return false;
    }

    @Override
    public int getTracingState() {
        return 0;  // TODO
    }

    @Override
    public void setTracingState(int tracingState) {
        // TODO
    }

    public MessageContext getSynapseContext() {
        return null;
    }

    public void setSynapseContext(MessageContext env) {
    }

    @Override
    public Stack<FaultHandler> getFaultStack() {
        return faultStack;
    }

    @Override
    public void pushFaultHandler(FaultHandler fault) {
        faultStack.push(fault);
    }

    @Override
    public Log getServiceLog() {
        return LogFactory.getLog(TestMessageContext.class);
    }

    @Override
    public Mediator getSequenceTemplate(String key) {
        Object o = localEntries.get(key);
        if (o != null && o instanceof Mediator) {
            return (Mediator) o;
        } else {
            Mediator m = getConfiguration().getSequence(key);
            if (m instanceof TemplateMediator) {
                TemplateMediator templateMediator = (TemplateMediator) m;
                synchronized (m) {
                    if (!templateMediator.isInitialized()) {
                        templateMediator.init(synEnv);
                    }
                }
            }
            localEntries.put(key, m);
            return m;
        }
    }
}
