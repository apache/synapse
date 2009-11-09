/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.sandesha2.Sandesha2Constants.SPEC_2005_02;
import org.apache.sandesha2.Sandesha2Constants.SPEC_2007_02;
import org.apache.sandesha2.Sandesha2Constants.WSRM_COMMON;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CloseSequence;
import org.apache.sandesha2.wsrm.CloseSequenceResponse;
import org.apache.sandesha2.wsrm.CreateSequence;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;
import org.apache.sandesha2.wsrm.MakeConnection;
import org.apache.sandesha2.wsrm.MessagePending;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;
import org.apache.sandesha2.wsrm.SequenceFault;
import org.apache.sandesha2.wsrm.TerminateSequence;
import org.apache.sandesha2.wsrm.TerminateSequenceResponse;
import org.apache.sandesha2.wsrm.UsesSequenceSTR;

/**
 * This class is used to hold a MessageContext within Sandesha. This is used to
 * easily manupilate the properties of a MessageContext.
 */

public class RMMsgContext {

	private MessageContext msgContext;

	private int messageType;

	private String rmNamespaceValue = null;
	
	private String rmSpecVersion = null;
	
	public RMMsgContext() {
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
	 * TODO Re-work this method as it's poorly named and confusing.
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
		
		boolean goingToAddHeader = (sequence != null) || (sequenceAcknowledgements.size()>0) || ackRequests.size()>0 || sequenceFault!=null || usesSequenceSTR!=null ||messagePending!=null;
		if(goingToAddHeader){
			// Clean up the SOAPHeader...
			SOAPHeader header = envelope.getHeader();
			if(header == null){
				header = ((SOAPFactory)envelope.getOMFactory()).createSOAPHeader(envelope);
			}else{
				Iterator existingHeaders = header.getChildElements();
				while(existingHeaders.hasNext()){
					OMElement oe = (OMElement)existingHeaders.next();
					if(rmNamespaceValue.equals(oe.getNamespace().getNamespaceURI())){
						oe.detach();
					}
				}
			}
			
			// Set up any header elements
			if(sequence != null){
				sequence.toHeader(header);
			}
			//there can be more than one sequence ack or ack request in a single message.
			for (Iterator<SequenceAcknowledgement> iter=sequenceAcknowledgements.iterator();iter.hasNext();) {
				SequenceAcknowledgement sequenceAck = (SequenceAcknowledgement) iter.next();
				sequenceAck.toHeader(header);
			}
			for (Iterator<AckRequested> iter=ackRequests.iterator();iter.hasNext();) {
				AckRequested ackReq = (AckRequested) iter.next();
				ackReq.toHeader(header);
			}
			if(sequenceFault != null){
				sequenceFault.toHeader(header);
			}
			if(usesSequenceSTR != null){
				usesSequenceSTR.toHeader(header);
			}
			if(messagePending != null){
				messagePending.toHeader(header);
			}
		}
		
		// Then set up the body element (if appropriate)
		if(createSequence != null){
			createSequence.toSOAPEnvelope(envelope);
		}
		if(createSequenceResponse != null){
			createSequenceResponse.toSOAPEnvelope(envelope);
		}
		if(terminateSequence != null){
			terminateSequence.toSOAPEnvelope(envelope);
		}
		if(terminateSequenceResponse != null){
			terminateSequenceResponse.toSOAPEnvelope(envelope);
		}
		if(closeSequence != null){
			closeSequence.toSOAPEnvelope(envelope);
		}
		if(closeSequenceResponse != null){
			closeSequenceResponse.toSOAPEnvelope(envelope);
		}
		if(makeConnection != null){
			makeConnection.toSOAPEnvelope(envelope);
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
	
	private Sequence sequence = null;
	
	//there can be more than one sequence ack or ack request in a single message.
	private ArrayList<SequenceAcknowledgement> sequenceAcknowledgements = new ArrayList<SequenceAcknowledgement>();
	private ArrayList<AckRequested> ackRequests = new ArrayList<AckRequested>();
	
	private CreateSequence createSequence = null;
	private CreateSequenceResponse createSequenceResponse = null;
	private TerminateSequence terminateSequence = null;
	private TerminateSequenceResponse terminateSequenceResponse = null;
	private CloseSequence closeSequence = null;
	private CloseSequenceResponse closeSequenceResponse = null;
	private UsesSequenceSTR usesSequenceSTR = null;
	private MessagePending messagePending = null;
	private MakeConnection makeConnection = null;
	private SequenceFault sequenceFault = null;
	
	public CreateSequence getCreateSequence() {
		return createSequence;
	}

	public CreateSequenceResponse getCreateSequenceResponse() {
		return createSequenceResponse;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public Iterator<SequenceAcknowledgement> getSequenceAcknowledgements() {
		return sequenceAcknowledgements.iterator();
	}

	public TerminateSequence getTerminateSequence() {
		return terminateSequence;
	}
	
	public TerminateSequenceResponse getTerminateSequenceResponse() {
		return terminateSequenceResponse;
	}

	public void setCreateSequence(CreateSequence createSequence) {
		this.createSequence = createSequence;
	}

	public void setCreateSequenceResponse(
			CreateSequenceResponse createSequenceResponse) {
		this.createSequenceResponse = createSequenceResponse;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	public void setSequenceAcknowledgements(
			ArrayList<SequenceAcknowledgement> sequenceAcknowledgements) {
		this.sequenceAcknowledgements = sequenceAcknowledgements;
	}
	
	public void addSequenceAcknowledgement (SequenceAcknowledgement sequenceAcknowledgement) {
		sequenceAcknowledgements.add(sequenceAcknowledgement);
	}

	public void setTerminateSequence(TerminateSequence terminateSequence) {
		this.terminateSequence = terminateSequence;
	}
	
	public void setTerminateSequenceResponse(TerminateSequenceResponse terminateSequenceResponse) {
		this.terminateSequenceResponse = terminateSequenceResponse;
	}

	public Iterator<AckRequested> getAckRequests() {
		return ackRequests.iterator();
	}

	public void setAckRequested(ArrayList<AckRequested> ackRequests) {
		this.ackRequests = ackRequests;
	}
	
	public void addAckRequested(AckRequested ackRequested) {
		ackRequests.add(ackRequested);
	}
	
	public void setMakeConnection(MakeConnection makeConnection) {
		this.makeConnection = makeConnection;
	}
	
	public void setMessagePending(MessagePending messagePending) {
		this.messagePending = messagePending;
	}
	
	public CloseSequence getCloseSequence() {
		return closeSequence;
	}

	public void setCloseSequence(CloseSequence closeSequence) {
		this.closeSequence = closeSequence;
	}

	public CloseSequenceResponse getCloseSequenceResponse() {
		return closeSequenceResponse;
	}

	public void setCloseSequenceResponse(CloseSequenceResponse closeSequenceResponse) {
		this.closeSequenceResponse = closeSequenceResponse;
	}
	
	public UsesSequenceSTR getUsesSequenceSTR() {
		return usesSequenceSTR;
	}
	
	public void setUsesSequenceSTR(UsesSequenceSTR header) {
		usesSequenceSTR = header;
	}

	public MakeConnection getMakeConnection() {
		return makeConnection;
	}

	public MessagePending getMessagePending() {
		return messagePending;
	}
	
	public SequenceFault getSequenceFault() {
		return sequenceFault;
	}

	public void setSequenceFault(SequenceFault sequenceFault2) {
		sequenceFault = sequenceFault2;
	}
	
	public void fromSOAPEnvelope(SOAPEnvelope envelope, String action) throws AxisFault {

		if (envelope == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullPassedElement));

		// Check for RM defined elements, using either spec version
		SOAPHeader header = envelope.getHeader();
		SOAPBody body = envelope.getBody();

		if(header != null){
			processHeaders(header);
		}
		if(body != null){
			processBody(body);
		}
	}
	
	private static HashSet<String> bodyLocalNames = new HashSet<String>();
	static{
		bodyLocalNames.add(Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE);
		bodyLocalNames.add(Sandesha2Constants.WSRM_COMMON.CREATE_SEQUENCE_RESPONSE);
		bodyLocalNames.add(Sandesha2Constants.WSRM_COMMON.CLOSE_SEQUENCE);
		bodyLocalNames.add(Sandesha2Constants.WSRM_COMMON.CLOSE_SEQUENCE_RESPONSE);
		bodyLocalNames.add(Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE);
		bodyLocalNames.add(Sandesha2Constants.WSRM_COMMON.TERMINATE_SEQUENCE_RESPONSE);
		bodyLocalNames.add(Sandesha2Constants.WSRM_COMMON.MAKE_CONNECTION);
	}
	
	private void processBody(SOAPBody body) throws AxisFault{
		if(bodyLocalNames.contains(body.getFirstElementLocalName())){
		// The body messages
		OMElement firstBodyElement = body.getFirstElement();
		if(firstBodyElement != null) {
			QName firstBodyQName = firstBodyElement.getQName();
			String namespace = firstBodyQName.getNamespaceURI();

			boolean isSPEC2007_02 = SPEC_2007_02.NS_URI.equals(namespace);
			boolean isSPEC2005_02 = false;
			if(!isSPEC2007_02){
				isSPEC2005_02 = SPEC_2005_02.NS_URI.equals(namespace);
			}

			if(isSPEC2005_02 || isSPEC2007_02){
				if(Sandesha2Constants.SPEC_2007_02.QNames.CreateSequence.equals(firstBodyQName)) {
					createSequence = new CreateSequence(namespace);
					createSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CreateSequence.equals(firstBodyQName)) {
					createSequence = new CreateSequence(namespace);
					createSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.CreateSequenceResponse.equals(firstBodyQName)) {
					createSequenceResponse = new CreateSequenceResponse(namespace);
					createSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CreateSequenceResponse.equals(firstBodyQName)) {
					createSequenceResponse = new CreateSequenceResponse(namespace);
					createSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.CloseSequence.equals(firstBodyQName)) {
					closeSequence = new CloseSequence(namespace);
					closeSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CloseSequence.equals(firstBodyQName)) {
					closeSequence = new CloseSequence(namespace);
					closeSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.CloseSequenceResponse.equals(firstBodyQName)) {
					closeSequenceResponse = new CloseSequenceResponse(namespace);
					closeSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.CloseSequenceResponse.equals(firstBodyQName)) {
					closeSequenceResponse = new CloseSequenceResponse(namespace);
					closeSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.TerminateSequence.equals(firstBodyQName)) {
					terminateSequence = new TerminateSequence(namespace);
					terminateSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.TerminateSequence.equals(firstBodyQName)) {
					terminateSequence = new TerminateSequence(namespace);
					terminateSequence.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2007_02.QNames.TerminateSequenceResponse.equals(firstBodyQName)) {
					terminateSequenceResponse = new TerminateSequenceResponse(namespace);
					terminateSequenceResponse.fromOMElement(body);
				} else if(Sandesha2Constants.SPEC_2005_02.QNames.TerminateSequenceResponse.equals(firstBodyQName)) {
					terminateSequenceResponse = new TerminateSequenceResponse(namespace);
					terminateSequenceResponse.fromOMElement(body);
				}
			}else if(Sandesha2Constants.SPEC_2007_02.QNames.MakeConnection.equals(firstBodyQName)) {
				makeConnection = new MakeConnection();
				makeConnection.fromOMElement(firstBodyElement);
			}
		}
		}
	}
	
	private void processHeaders(SOAPHeader header) throws AxisFault {
		Iterator headers = header.getChildElements();
		while(headers.hasNext()){
			SOAPHeaderBlock element = (SOAPHeaderBlock)headers.next();
			QName elementName = element.getQName();
			String namespace = elementName.getNamespaceURI();
			String localName = elementName.getLocalPart();

			boolean isSPEC2007_02 = SPEC_2007_02.NS_URI.equals(namespace);
			boolean isSPEC2005_02 = false;
			if(!isSPEC2007_02){
				isSPEC2005_02 = SPEC_2005_02.NS_URI.equals(namespace);
			}

			if(isSPEC2005_02 || isSPEC2007_02){
				boolean isProcessed = false;
				if(isSPEC2007_02){
					if(WSRM_COMMON.USES_SEQUENCE_STR.equals(localName)){
						usesSequenceSTR = new UsesSequenceSTR();
						usesSequenceSTR.fromHeaderBlock(element);
						isProcessed = true;
					}else if(WSRM_COMMON.MESSAGE_PENDING.equals(localName)){
						messagePending = new MessagePending();
						messagePending.fromHeaderBlock(element);
						isProcessed = true;
					}
				}

				if(!isProcessed){
					if(WSRM_COMMON.SEQUENCE.equals(localName)){
						sequence = new Sequence(namespace);
						sequence.fromHeaderBlock(element);
					}else if(WSRM_COMMON.SEQUENCE_ACK.equals(localName)){
						SequenceAcknowledgement sequenceAcknowledgement = new SequenceAcknowledgement(namespace, false);
						sequenceAcknowledgement.fromHeaderBlock(element);
						sequenceAcknowledgements.add(sequenceAcknowledgement);
					}else if(WSRM_COMMON.ACK_REQUESTED.equals(localName)){
						AckRequested ackRequest = new AckRequested(namespace);
						ackRequest.fromHeaderBlock(element);
						ackRequests.add(ackRequest);
					}else if(WSRM_COMMON.SEQUENCE_FAULT.equals(localName)){
						sequenceFault = new SequenceFault(namespace);
						sequenceFault.fromHeaderBlock(element);
					}	
				}
			}
		}
	}
}
