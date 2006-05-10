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
package org.apache.synapse.core.axis2;

import org.apache.synapse.SynapseMessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.AxisFault;
import org.apache.axiom.soap.SOAPEnvelope;

import java.util.Map;
import java.util.HashMap;

public class Axis2SynapseMessageContext implements SynapseMessageContext {

    private SynapseConfiguration cfg = null;
    private SynapseEnvironment   env = null;
    private Map properties = new HashMap();

    /** The Axis2 MessageContext reference */
    private MessageContext mc = null;

    private boolean response = false;

    private boolean faultResponse = false;

    public SynapseConfiguration getConfiguration() {
        return cfg;
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        this.cfg = cfg;
    }

    public SynapseEnvironment getSynapseEnvironment() {
        return env;
    }

    public void setSynapseEnvironment(SynapseEnvironment env) {
        this.env = env;
    }

    public Object getProperty(String key) {
        Object ret = properties.get(key);
        if (ret != null) {
            return ret;
        } else if (getConfiguration() != null) {
            return getConfiguration().getProperty(key);
        } else {
            return null;
        }
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    //--------------------
    public Axis2SynapseMessageContext(MessageContext mc) {
        setMessageContext(mc);
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

    public SOAPEnvelope getEnvelope() {
        return mc.getEnvelope();
    }

    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        mc.setEnvelope(envelope);
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
        mc.setRelationships(reference);
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

    public void setMessageId(String messageID) {
        mc.setWSAMessageId(messageID);
    }

    public String getMessageId() {
        return mc.getMessageID();
    }

    public String getSoapAction() {
        return mc.getSoapAction();
    }

    public void setSoapAction(String string) {
        mc.setSoapAction(string);
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
        response = b;
        mc.setProperty(Constants.ISRESPONSE_PROPERTY, Boolean.valueOf(b));
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

    public MessageContext getMessageContext() {
        return mc;
    }

    public void setMessageContext(MessageContext mc) {
        this.mc = mc;
        Boolean resp = (Boolean) mc.getProperty(Constants.ISRESPONSE_PROPERTY);
        if (resp != null)
            response = resp.booleanValue();
    }

}
