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

import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.AxisFault;
import org.apache.axiom.soap.SOAPEnvelope;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class Axis2MessageContext implements MessageContext {

    private SynapseConfiguration cfg = null;
    private SynapseEnvironment   env = null;
    private Map properties = new HashMap();

    /** The Axis2 MessageContext reference */
    private org.apache.axis2.context.MessageContext axis2MessageContext = null;

    private boolean response = false;

    private boolean faultResponse = false;

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
        } else if (getConfiguration() != null) {
            return getConfiguration().getProperty(key);
        } else {
            return null;
        }
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Set getPropertyKeySet() {
        return properties.keySet();
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

    public void setMessageId(String messageID) {
        axis2MessageContext.setWSAMessageId(messageID);
    }

    public String getMessageId() {
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

    public void setDoingMTOM(boolean b) {
        axis2MessageContext.setDoingMTOM(b);
    }

    public boolean isDoingREST() {
        return axis2MessageContext.isDoingREST();
    }

    public void setDoingREST(boolean b) {
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

    public org.apache.axis2.context.MessageContext getAxis2MessageContext() {
        return axis2MessageContext;
    }

    public void setAxis2MessageContext(org.apache.axis2.context.MessageContext axisMsgCtx) {
        this.axis2MessageContext = axisMsgCtx;
        Boolean resp = (Boolean) axisMsgCtx.getProperty(Constants.ISRESPONSE_PROPERTY);
        if (resp != null)
            response = resp.booleanValue();
    }

}
