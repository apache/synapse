/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.msgprocessors;

import java.util.Iterator;

import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.CreateSeqBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.CreateSeqBean;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.MsgInitializer;
import org.apache.sandesha2.util.SOAPAbstractFactory;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.util.SpecSpecificConstants;
import org.apache.sandesha2.wsrm.Accept;
import org.apache.sandesha2.wsrm.AckRequested;
import org.apache.sandesha2.wsrm.CreateSequenceResponse;
import org.apache.sandesha2.wsrm.Identifier;
import org.apache.sandesha2.wsrm.Sequence;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;

/**
 * Responsible for processing an incoming Create Sequence Response message.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 */

public class CreateSeqResponseMsgProcessor implements MsgProcessor {
	
	private static final Log log = LogFactory.getLog(CreateSeqResponseMsgProcessor.class);
	
	public void processInMessage(RMMsgContext createSeqResponseRMMsgCtx)
			throws SandeshaException {
		
    if (log.isDebugEnabled())
      log.debug("Enter: CreateSeqResponseMsgProcessor::processInMessage");

    SOAPFactory factory = SOAPAbstractFactory.getSOAPFactory(SandeshaUtil
				.getSOAPVersion(createSeqResponseRMMsgCtx.getSOAPEnvelope()));

		MessageContext createSeqResponseMsg = createSeqResponseRMMsgCtx.getMessageContext();
		ConfigurationContext configCtx = createSeqResponseRMMsgCtx
			.getMessageContext().getConfigurationContext();		

		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx,configCtx.getAxisConfiguration());
		
		//Processing for ack if available
		
		SequenceAcknowledgement sequenceAck = (SequenceAcknowledgement) createSeqResponseRMMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);
		if (sequenceAck != null) {
			AcknowledgementProcessor ackProcessor = new AcknowledgementProcessor();
			ackProcessor.processInMessage(createSeqResponseRMMsgCtx);
		}

		//Processing the create sequence response.
		
		CreateSequenceResponse createSeqResponsePart = (CreateSequenceResponse) createSeqResponseRMMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.CREATE_SEQ_RESPONSE);
		if (createSeqResponsePart == null) {
			String message = "Create Sequence Response part is null";
			log.debug(message);
			throw new SandeshaException(message);
		}

		String newOutSequenceId = createSeqResponsePart.getIdentifier()
				.getIdentifier();
		if (newOutSequenceId == null) {
			String message = "New sequence Id is null";
			log.debug(message);
			throw new SandeshaException(message);
		}

		RelatesTo relatesTo = createSeqResponseRMMsgCtx.getMessageContext()
										.getRelatesTo();
		if (relatesTo==null) {
			String message = "Invalid create sequence message. RelatesTo part is not available";
			log.error("Invalid create sequence response message. RelatesTo part is not available");
			throw new SandeshaException ("Invalid create sequence message. RelatesTo part is not available");
		}
		String createSeqMsgId = relatesTo.getValue();


		SenderBeanMgr retransmitterMgr = storageManager
				.getRetransmitterBeanMgr();
		CreateSeqBeanMgr createSeqMgr = storageManager.getCreateSeqBeanMgr();

		CreateSeqBean createSeqBean = createSeqMgr.retrieve(createSeqMsgId);
		if (createSeqBean == null) {
			String message = "Create Sequence entry is not found";
			log.debug(message);
			throw new SandeshaException(message);
		}

		String internalSequenceId = createSeqBean.getInternalSequenceID();
		if (internalSequenceId == null || "".equals(internalSequenceId)) {
			String message = "TempSequenceId has is not set";
			log.debug(message);
			throw new SandeshaException(message);
		}
		
		createSeqBean.setSequenceID(newOutSequenceId);
		createSeqMgr.update(createSeqBean);

		SenderBean createSequenceSenderBean = retransmitterMgr.retrieve(createSeqMsgId);
		if (createSequenceSenderBean==null)
			throw new SandeshaException ("Create sequence entry is not found");
		
		//removing the Create Sequence Message from the storage
		String createSeqStorageKey = createSequenceSenderBean.getMessageContextRefKey();
		storageManager.removeMessageContext(createSeqStorageKey);
		
		//deleting the create sequence entry.
		retransmitterMgr.delete(createSeqMsgId);

		//storing new out sequence id
		SequencePropertyBeanMgr sequencePropMgr = storageManager
				.getSequencePropretyBeanMgr();
		SequencePropertyBean outSequenceBean = new SequencePropertyBean(
				internalSequenceId, Sandesha2Constants.SequenceProperties.OUT_SEQUENCE_ID,
				newOutSequenceId);
		SequencePropertyBean internalSequenceBean = new SequencePropertyBean(
				newOutSequenceId,
				Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID, internalSequenceId);
		
		
		sequencePropMgr.insert(outSequenceBean);
		sequencePropMgr.insert(internalSequenceBean);

		//processing for accept (offer has been sent)
		Accept accept = createSeqResponsePart.getAccept();
		if (accept != null) {
			//Find offered sequence from internal sequence id.
			SequencePropertyBean offeredSequenceBean = sequencePropMgr
					.retrieve(internalSequenceId,
							Sandesha2Constants.SequenceProperties.OFFERED_SEQUENCE);

			//TODO this should be detected in the Fault manager.
			if (offeredSequenceBean == null) {
				String message = "No offered sequence entry. But an accept was received"; 
				log.debug(message);
				throw new SandeshaException(message);
			}

			String offeredSequenceId = (String) offeredSequenceBean.getValue();

			EndpointReference acksToEPR = accept.getAcksTo().getAddress()
					.getEpr();
			SequencePropertyBean acksToBean = new SequencePropertyBean();
			acksToBean.setName(Sandesha2Constants.SequenceProperties.ACKS_TO_EPR);
			acksToBean.setSequenceID(offeredSequenceId);
			acksToBean.setValue(acksToEPR.getAddress());

			sequencePropMgr.insert(acksToBean);

			NextMsgBean nextMsgBean = new NextMsgBean();
			nextMsgBean.setSequenceID(offeredSequenceId);
			nextMsgBean.setNextMsgNoToProcess(1);

			NextMsgBeanMgr nextMsgMgr = storageManager.getNextMsgBeanMgr();
			nextMsgMgr.insert(nextMsgBean);
			
			String rmSpecVersion = createSeqResponseRMMsgCtx.getRMSpecVersion();
			
			SequencePropertyBean specVersionBean = new SequencePropertyBean (
					offeredSequenceId,Sandesha2Constants.SequenceProperties.RM_SPEC_VERSION,rmSpecVersion);
			sequencePropMgr.insert(specVersionBean);
			
			SequencePropertyBean receivedMsgBean = new SequencePropertyBean(
					offeredSequenceId, Sandesha2Constants.SequenceProperties.SERVER_COMPLETED_MESSAGES, "");
			sequencePropMgr.insert(receivedMsgBean);
			
			SequencePropertyBean msgsBean = new SequencePropertyBean();
			msgsBean.setSequenceID(offeredSequenceId);
			msgsBean.setName(Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES);
			msgsBean.setValue("");
			sequencePropMgr.insert(msgsBean);
			
			
			//setting the addressing version.
			String addressingNamespace = createSeqResponseRMMsgCtx.getAddressingNamespaceValue();
			SequencePropertyBean addressingVersionBean = new SequencePropertyBean (
					offeredSequenceId,Sandesha2Constants.SequenceProperties.ADDRESSING_NAMESPACE_VALUE,addressingNamespace);
			sequencePropMgr.insert(addressingVersionBean);
			
		}

		SenderBean target = new SenderBean();
		target.setInternalSequenceID(internalSequenceId);
		target.setSend(false);
		target.setReSend(true);

		Iterator iterator = retransmitterMgr.find(target).iterator();
		while (iterator.hasNext()) {
			SenderBean tempBean = (SenderBean) iterator.next();

			//updating the application message
			String key = tempBean.getMessageContextRefKey();
			MessageContext applicationMsg = storageManager.retrieveMessageContext(key,configCtx); 

			//TODO make following exception message more understandable to the user (probably some others exceptions messages as well)
			if (applicationMsg==null)
				throw new SandeshaException ("Unavailable application message");
			
			String rmVersion = SandeshaUtil.getRMVersion(internalSequenceId,storageManager);
			if (rmVersion==null)
				throw new SandeshaException ("Cant find the rmVersion of the given message");
			
			String assumedRMNamespace = SpecSpecificConstants.getRMNamespaceValue(rmVersion);
			
			RMMsgContext applicaionRMMsg = MsgInitializer
					.initializeMessage(applicationMsg);

			Sequence sequencePart = (Sequence) applicaionRMMsg
					.getMessagePart(Sandesha2Constants.MessageParts.SEQUENCE);
			if (sequencePart == null) {
				String message = "Sequence part is null";
				log.debug(message);
				throw new SandeshaException(message);
			}
			
			Identifier identifier = new Identifier(factory,assumedRMNamespace);
			identifier.setIndentifer(newOutSequenceId);

			sequencePart.setIdentifier(identifier);

			AckRequested ackRequestedPart = (AckRequested) applicaionRMMsg
					.getMessagePart(Sandesha2Constants.MessageParts.ACK_REQUEST);
			if (ackRequestedPart != null) {
				Identifier id1 = new Identifier(factory,assumedRMNamespace);
				id1.setIndentifer(newOutSequenceId);
				ackRequestedPart.setIdentifier(id1);
			}

			try {
				applicaionRMMsg.addSOAPEnvelope();
			} catch (AxisFault e) {
				throw new SandeshaException(e.getMessage());
			}
			
			//asking to send the application msssage
			tempBean.setSend(true);
			retransmitterMgr.update(tempBean);
			
			//updating the message. this will correct the SOAP envelope string.
			storageManager.updateMessageContext(key,applicationMsg);
		}
		
		SequenceManager.updateLastActivatedTime(internalSequenceId,storageManager);
		
		createSeqResponseRMMsgCtx.getMessageContext().getOperationContext()
				.setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN,
						"false");

		createSeqResponseRMMsgCtx.pause();
    
    if (log.isDebugEnabled())
      log.debug("Exit: CreateSeqResponseMsgProcessor::processInMessage");
	}
	
	public void processOutMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
    if (log.isDebugEnabled())
    {
      log.debug("Enter: CreateSeqResponseMsgProcessor::processOutMessage");
      log.debug("Exit: CreateSeqResponseMsgProcessor::processOutMessage");
    }

	}
}
