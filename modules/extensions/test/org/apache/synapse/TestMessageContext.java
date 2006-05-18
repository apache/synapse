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
package org.apache.synapse;

import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.AxisFault;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class TestMessageContext implements MessageContext {

    private Map properties = new HashMap();

    private SynapseConfiguration synCfg = null;

    SOAPEnvelope envelope = null;

    private EndpointReference to = null;

    public SynapseConfiguration getConfiguration() {
        return synCfg;
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        this.synCfg = cfg;
    }

    public SynapseEnvironment getEnvironment() {
        return null;
    }

    public void setEnvironment(SynapseEnvironment se) {
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

    //---------
    public SOAPEnvelope getEnvelope() {
        if (envelope == null)
            return OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        else
            return envelope;
    }

    public void setEnvelope(SOAPEnvelope envelope) throws AxisFault {
        this.envelope = envelope;
    }

    public EndpointReference getFaultTo() {
        return null;
    }

    public void setFaultTo(EndpointReference reference) {
    }

    public EndpointReference getFrom() {
        return null;
    }

    public void setFrom(EndpointReference reference) {
    }

    public String getMessageID() {
        return null;
    }

    public void setMessageID(String string) {
    }

    public RelatesTo getRelatesTo() {
        return null;
    }

    public void setRelatesTo(RelatesTo[] reference) {
    }

    public EndpointReference getReplyTo() {
        return null;
    }

    public void setReplyTo(EndpointReference reference) {
    }

    public EndpointReference getTo() {
        return to;
    }

    public void setTo(EndpointReference reference) {
        to = reference;
    }

    public void setWSAAction(String actionURI) {
    }

    public String getWSAAction() {
        return null;
    }

    public String getSoapAction() {
        return null;
    }

    public void setSoapAction(String string) {
    }

    public void setMessageId(String messageID) {
    }

    public String getMessageId() {
        return null;
    }

    public boolean isDoingMTOM() {
        return false;
    }

    public void setDoingMTOM(boolean b) {
    }

    public boolean isDoingREST() {
        return false;
    }

    public void setDoingREST(boolean b) {
    }

    public boolean isSOAP11() {
        return envelope.getNamespace().getName().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    }

    public void setResponse(boolean b) {
    }

    public boolean isResponse() {
        return false;
    }

    public void setFaultResponse(boolean b) {
    }

    public boolean isFaultResponse() {
        return false;
    }

    public MessageContext getSynapseContext() {
        return null;
    }

    public void setSynapseContext(MessageContext env) {
    }
}
