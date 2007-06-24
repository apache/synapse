/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.wsrm.IOMRMPart;

/**
 * This class is used to hold a MessageContext within Sandesha. This is used to
 * easily manupilate the properties of a MessageContext.
 */

public class RMMsgContext {

	private MessageContext msgContext;

	private HashMap rmMessageParts;

	private int messageType;

	private String rmNamespaceValue = null;
	
	private String rmSpecVersion = null;
	
	public RMMsgContext() {
		rmMessageParts = new HashMap();
		messageType = Sandesha2Constants.MessageTypes.UNKNOWN;
		rmNamespaceValue = Sandesha2Constants.DEFAULT_RM_NAMESPACE;
	}

	public void setMessageContext(MessageContext msgCtx) {
		this.msgContext = msgCtx;
	}

	public RMMsgContext(MessageContext ctx) {
		this();
		this.msgContext = ctx;
	}

	/**
	 * To add a new SOAP envelope to the message. The generated envelope will belong 
	 * to the SOAP version of the MessageContext. We will be going through each MessagePart and adding it to the
	 * envelope. In other words all the MessageParts that are available in the RMMsg will be added to the SOAP 
	 * envelope after this.
	 * 
	 * @throws SandeshaException
	 */
	public void addSOAPEnvelope() throws AxisFault {
		int SOAPVersion = Sandesha2Constants.SOAPVersion.v1_1;

		if (!msgContext.isSOAP11())
			SOAPVersion = Sandesha2Constants.SOAPVersion.v1_2;

		if (msgContext.getEnvelope() == null) {
			try {
				msgContext.setEnvelope(SOAPAbstractFactory.getSOAPFactory(
						SOAPVersion).getDefaultEnvelope());
			} catch (AxisFault e) {
				throw new SandeshaException(e.getMessage());
			}
		}

		SOAPEnvelope envelope = msgContext.getEnvelope();
		Iterator keys = rmMessageParts.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = (Integer) keys.next();
			int partId = key.intValue();
			
			if (isMultiPart(partId)) {
				for (Iterator it=getMessageParts(partId);it.hasNext();) {
					IOMRMPart rmPart = (IOMRMPart) it.next();
					rmPart.toSOAPEnvelope(envelope);
				}
			} else {
				IOMRMPart rmPart = (IOMRMPart) rmMessageParts.get(key);
				rmPart.toSOAPEnvelope(envelope);
			}
		}
	}

	public int getMessageType() {
		return messageType;
	}
	
	
	/**
	 * The message type can be used to easily identify what this message is.
	 * Possible message types are given in the Constnts.MessageTypes interface.
	 * 
	 * @param msgType
	 */
	public void setMessageType(int msgType) {
		if (msgType >= 0 && msgType <= Sandesha2Constants.MessageTypes.MAX_MESSAGE_TYPE)
			this.messageType = msgType;
	}

	
	/**
	 * Sets an IRMPart object to the MessageContext. Possible parts are give in the 
	 * 
	 * 
	 * @param partId
	 * @param part
	 */
	public void setMessagePart(int partId, IOMRMPart part) {
		if (partId >= 0 && partId <= Sandesha2Constants.MessageParts.MAX_MSG_PART_ID) {
			if (isMultiPart(partId)) {
				ArrayList partList = (ArrayList) rmMessageParts.get(new Integer (partId));
				if (partList==null) {
					partList = new ArrayList ();
					rmMessageParts.put(new Integer (partId),partList);
				}
				partList.add(part);
			} else {
				rmMessageParts.put(new Integer(partId), part); 
			}
		}
	}
	

	public IOMRMPart getMessagePart(int partId) throws SandeshaException {
		if (isMultiPart(partId)) {
			String message = "It is possible for a multiple MessageParts of this type to exit. Please call the 'getMessageParts' method";
			throw new SandeshaException (message);
		}
		
		return (IOMRMPart) rmMessageParts.get(new Integer(partId));
	}
	
	public Iterator getMessageParts (int partId) {
		Object obj = rmMessageParts.get(new Integer (partId));
		if (obj==null)
			return new ArrayList().iterator();
		
		if (obj instanceof ArrayList) {
			return ((ArrayList) obj).iterator();
		} else {
			ArrayList arr = new ArrayList ();
			arr.add(obj);
			return arr.iterator();
		}
	}
	
	public void removeMessageParts (int messageType) {
		rmMessageParts.remove (new Integer (messageType));
	}
	
	//checks weather there can be multiple elements of these parts,
	//if so getMessageParts method has to be called to get a ArrayList of parts..
	public boolean isMultiPart (int messagePartId) {
		if (messagePartId==Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT||
			messagePartId==Sandesha2Constants.MessageParts.ACK_REQUEST)
			return true;
		
		return false;
	}

	public EndpointReference getFrom() {
		return msgContext.getFrom();
	}

	public EndpointReference getTo() {
		return msgContext.getTo();
	}

	public EndpointReference getReplyTo() {
		return msgContext.getReplyTo();
	}

	public RelatesTo getRelatesTo() {
		return msgContext.getRelatesTo();
	}

	public String getMessageId() {
		return msgContext.getMessageID();
	}

	public void setFaultTo(EndpointReference epr) {
		msgContext.setFaultTo(epr);
	}

	public EndpointReference getFaultTo() {
		return msgContext.getFaultTo();
	}

	public SOAPEnvelope getSOAPEnvelope() {
		return msgContext.getEnvelope();
	}

	public void setSOAPEnvelop(SOAPEnvelope envelope) throws SandeshaException {

		try {
			msgContext.setEnvelope(envelope);
		} catch (AxisFault e) {
			throw new SandeshaException(e.getMessage());
		}
	}

	public void setFrom(EndpointReference epr) {
		msgContext.setFrom(epr);
	}

	public void setTo(EndpointReference epr) {
		msgContext.setTo(epr);
	}

	public void setReplyTo(EndpointReference epr) {
		msgContext.setReplyTo(epr);
	}

	public void setMessageId(String messageId) {
		msgContext.setMessageID(messageId);
	}

	public void setAction(String action) {
		msgContext.setWSAAction(action);
	}

	public void addRelatesTo(RelatesTo relatesTo) {
		msgContext.addRelatesTo(relatesTo);
	}

	public void setWSAAction(String URI) {
		msgContext.setWSAAction(URI);
	}

	public String getWSAAction() {
		return msgContext.getWSAAction();
	}

	public MessageContext getMessageContext() {
		return msgContext;
	}

	public Object getProperty(String key) {
		if (msgContext == null)
			return null;

		return msgContext.getProperty(key);
	}

	public boolean setProperty(String key, Object val) {
		if (msgContext == null)
			return false;

		msgContext.setProperty(key, val);
		return true;
	}

	public ConfigurationContext getConfigurationContext() {
		if (msgContext == null)
			return null;

		return msgContext.getConfigurationContext();
	}

	
	public void setSOAPAction(String SOAPAction) {
		msgContext.setSoapAction(SOAPAction);
	}
	
	public void pause () {
		if (msgContext!=null)
			msgContext.pause();
	}
	
	public void setPaused (boolean pause) {
		if (msgContext!=null)
			msgContext.setPaused(pause);
	}

	public String getRMNamespaceValue() {
		return rmNamespaceValue;
	}

	public void setRMNamespaceValue(String rmNamespaceValue) {
		this.rmNamespaceValue = rmNamespaceValue;
		
		if (Sandesha2Constants.SPEC_2005_02.NS_URI.equals(rmNamespaceValue)) { 
			rmSpecVersion = Sandesha2Constants.SPEC_VERSIONS.v1_0;
		} else if (Sandesha2Constants.SPEC_2007_02.NS_URI.equals(rmNamespaceValue)) {
			rmSpecVersion = Sandesha2Constants.SPEC_VERSIONS.v1_1;
		}
	}
	
	public String getRMSpecVersion () {
		if (rmSpecVersion==null) {
			//this may hv been set in the Options object.
			if (msgContext!=null && msgContext.getOptions()!=null)
			rmSpecVersion = (String) msgContext.getOptions().getProperty(SandeshaClientConstants.RM_SPEC_VERSION);
		}
		
		return rmSpecVersion;
	}
	
	public void setFlow (int flow) {
		msgContext.setFLOW(flow);
	}
	
	public int getFlow () {
		return msgContext.getFLOW();
	}

	/**
	 * This will return the sequenceId if it could be derived from the SOAP envelope, in the
	 * message initiation.
	 * 
	 * @return
	 */
	public String getGeneratedSequenceId () {
		return (String) msgContext.getProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID);
	}
}
