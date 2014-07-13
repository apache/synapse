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
import org.apache.synapse.api.SOAPMessageContext;

public class Axis2SOAPMessageContext implements SOAPMessageContext {

	private MessageContext mc = null;

	private Map props = new HashMap();
	private boolean response = false;

	public Axis2SOAPMessageContext(MessageContext mc) {
		this.mc = mc;
		Boolean resp = (Boolean)mc.getProperty(Constants.ISRESPONSE_PROPERTY);
		if (resp!=null) response = resp.booleanValue();
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
		return getRelatesTo();

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
		return props.get(key);

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
		Boolean resp = (Boolean)mc.getProperty(Constants.ISRESPONSE_PROPERTY);
		if (resp!=null) response = resp.booleanValue();
	}

	public void setResponse(boolean b) {
		response = b;
		mc.setProperty(Constants.ISRESPONSE_PROPERTY, new Boolean(b));
	}

	public boolean isResponse() {
		return response;
	}

}
