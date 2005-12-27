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

package org.apache.synapse.axis2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseMessage;

/**
 *
 * 
 * <p>
 * A wrapper on Axis2's Message Context that implements the SynapseMessage interface
 *
 */
public class Axis2SynapseMessage implements SynapseMessage {

	private MessageContext mc = null;

	private Map props = new HashMap();

	private boolean response = false;

    private boolean faultResponse = false;

    public Axis2SynapseMessage(MessageContext mc) {
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

	public void setRelatesTo(RelatesTo reference) {
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

	public void setWSAMessageId(String messageID) {
		mc.setWSAMessageId(messageID);

	}

	public String getWSAMessageId() {
		return mc.getWSAMessageId();
	}

	public Object getProperty(String key) {
        Object obj = props.get(key);
        if ( obj == null) {
            obj = mc.getProperty(key);
        }        
        return obj;

	}

	public void setProperty(String key, Object value) {
		props.put(key, value);
	}

	public Iterator getPropertyNames() {
		return props.keySet().iterator();
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

	public MessageContext getMessageContext() {
		return mc;
	}

	public void setMessageContext(MessageContext mc) {
		this.mc = mc;
		Boolean resp = (Boolean) mc.getProperty(Constants.ISRESPONSE_PROPERTY);
		if (resp != null)
			response = resp.booleanValue();
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

}
