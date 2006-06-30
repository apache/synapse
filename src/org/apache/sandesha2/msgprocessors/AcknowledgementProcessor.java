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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;
import org.apache.sandesha2.util.AcknowledgementManager;
import org.apache.sandesha2.util.FaultManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.util.SequenceManager;
import org.apache.sandesha2.util.TerminateManager;
import org.apache.sandesha2.wsrm.AcknowledgementRange;
import org.apache.sandesha2.wsrm.Nack;
import org.apache.sandesha2.wsrm.SequenceAcknowledgement;

/**
 * Responsible for processing an incoming acknowledgement message.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 */

public class AcknowledgementProcessor implements MsgProcessor {

	private static final Log log = LogFactory.getLog(AcknowledgementProcessor.class);
	
	public void processInMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
    if (log.isDebugEnabled())
      log.debug("Enter: AcknowledgementProcessor::processInMessage");
    
		SequenceAcknowledgement sequenceAck = (SequenceAcknowledgement) rmMsgCtx
				.getMessagePart(Sandesha2Constants.MessageParts.SEQ_ACKNOWLEDGEMENT);
		if (sequenceAck == null) {
			String message = "Sequence acknowledgement part is null";
			log.debug(message);
			throw new SandeshaException(message);
		}
		
		MessageContext msgCtx = rmMsgCtx.getMessageContext();
		ConfigurationContext configCtx = msgCtx.getConfigurationContext();
		
		StorageManager storageManager = SandeshaUtil.getSandeshaStorageManager(configCtx,configCtx.getAxisConfiguration());
		
		//setting mustUnderstand to false.
		sequenceAck.setMustUnderstand(false);
		rmMsgCtx.addSOAPEnvelope();


		SenderBeanMgr retransmitterMgr = storageManager
				.getRetransmitterBeanMgr();
		SequencePropertyBeanMgr seqPropMgr = storageManager
				.getSequencePropertyBeanMgr();

		Iterator ackRangeIterator = sequenceAck.getAcknowledgementRanges()
				.iterator();

		Iterator nackIterator = sequenceAck.getNackList().iterator();
		String outSequenceId = sequenceAck.getIdentifier().getIdentifier();
		if (outSequenceId == null || "".equals(outSequenceId)) {
			String message = "OutSequenceId is null";
			log.debug(message);
			throw new SandeshaException(message);
		}

		FaultManager faultManager = new FaultManager();
		RMMsgContext faultMessageContext = faultManager.checkForUnknownSequence(rmMsgCtx,outSequenceId,storageManager);
		if (faultMessageContext != null) {
			
			ConfigurationContext configurationContext = msgCtx.getConfigurationContext();
			AxisEngine engine = new AxisEngine(configurationContext);
			
			try {
				engine.sendFault(faultMessageContext.getMessageContext());
			} catch (AxisFault e) {
				throw new SandeshaException ("Could not send the fault message",e);
			}
			
			msgCtx.pause();
			return;
		}
		
		faultMessageContext = faultManager.checkForInvalidAcknowledgement(rmMsgCtx,storageManager);
		if (faultMessageContext != null) {
			
			ConfigurationContext configurationContext = msgCtx.getConfigurationContext();
			AxisEngine engine = new AxisEngine(configurationContext);
			
			try {
				engine.sendFault(faultMessageContext.getMessageContext());
			} catch (AxisFault e) {
				throw new SandeshaException ("Could not send the fault message",e);
			}
			
			msgCtx.pause();
			return;
		}
		
        String internalSequenceID = SandeshaUtil.getSequenceProperty(outSequenceId,Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID,storageManager);
		
        //updating the last activated time of the sequence.
		SequenceManager.updateLastActivatedTime(internalSequenceID,storageManager);
		
		SequencePropertyBean internalSequenceBean = seqPropMgr.retrieve(
				outSequenceId, Sandesha2Constants.SequenceProperties.INTERNAL_SEQUENCE_ID);

		if (internalSequenceBean == null || internalSequenceBean.getValue() == null) {
			String message = "TempSequenceId is not set correctly";
			log.debug(message);
			
			throw new SandeshaException(message);
		}

		String internalSequenceId = (String) internalSequenceBean.getValue();

		//Following happens in the SandeshaGlobal handler
		rmMsgCtx.getMessageContext()
				.setProperty(Sandesha2Constants.ACK_PROCSSED, "true");

		//Removing relatesTo - Some v1_0 endpoints tend to set relatesTo value for ack messages.
		//Because of this dispatching may go wrong. So we set relatesTo value to null for ackMessages. 
		//(this happens in the SandeshaGlobal handler). Do this only if this is a standalone ACK.
//		if (rmMsgCtx.getMessageType() == Sandesha2Constants.MessageTypes.ACK)
//			rmMsgCtx.setRelatesTo(null);

		SenderBean input = new SenderBean();
		input.setSend(true);
		input.setReSend(true);
		Collection retransmitterEntriesOfSequence = retransmitterMgr
				.find(input);

		ArrayList ackedMessagesList = new ArrayList ();
		while (ackRangeIterator.hasNext()) {
			AcknowledgementRange ackRange = (AcknowledgementRange) ackRangeIterator
					.next();
			long lower = ackRange.getLowerValue();
			long upper = ackRange.getUpperValue();

			for (long messageNo = lower; messageNo <= upper; messageNo++) {
				SenderBean retransmitterBean = getRetransmitterEntry(
						retransmitterEntriesOfSequence, messageNo);
				if (retransmitterBean != null) {
					retransmitterMgr.delete(retransmitterBean.getMessageID());
					
					//removing the application message from the storage.
					String storageKey = retransmitterBean.getMessageContextRefKey();
					storageManager.removeMessageContext(storageKey);
				}
				
				ackedMessagesList.add(new Long (messageNo));
			}
		}

		while (nackIterator.hasNext()) {
			Nack nack = (Nack) nackIterator.next();
			long msgNo = nack.getNackNumber();

			//TODO - Process Nack
		}
		
		//setting acked message date.
		//TODO add details specific to each message.
		long noOfMsgsAcked = getNoOfMessagesAcked(sequenceAck.getAcknowledgementRanges().iterator());
		SequencePropertyBean noOfMsgsAckedBean = seqPropMgr.retrieve(outSequenceId,Sandesha2Constants.SequenceProperties.NO_OF_OUTGOING_MSGS_ACKED);
		boolean added = false;
		
		if (noOfMsgsAckedBean==null) {
			added = true;
			noOfMsgsAckedBean = new SequencePropertyBean ();
			noOfMsgsAckedBean.setSequenceID(outSequenceId);
			noOfMsgsAckedBean.setName(Sandesha2Constants.SequenceProperties.NO_OF_OUTGOING_MSGS_ACKED);
		}
		
		noOfMsgsAckedBean.setValue(Long.toString(noOfMsgsAcked));
		
		if (added) 
			seqPropMgr.insert(noOfMsgsAckedBean);
		else
			seqPropMgr.update(noOfMsgsAckedBean);
		
		
		//setting the completed_messages list. This gives all the messages of the sequence that were acked.
		SequencePropertyBean allCompletedMsgsBean = seqPropMgr.retrieve(internalSequenceId,Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES);
		if (allCompletedMsgsBean==null) {
			allCompletedMsgsBean = new SequencePropertyBean ();
			allCompletedMsgsBean.setSequenceID(internalSequenceId);
			allCompletedMsgsBean.setName(Sandesha2Constants.SequenceProperties.CLIENT_COMPLETED_MESSAGES);
			
			seqPropMgr.insert(allCompletedMsgsBean);
		}
				
		String str = ackedMessagesList.toString();
		allCompletedMsgsBean.setValue(str);
		
		seqPropMgr.update(allCompletedMsgsBean);		
		
		String lastOutMsgNoStr = SandeshaUtil.getSequenceProperty(internalSequenceId,Sandesha2Constants.SequenceProperties.LAST_OUT_MESSAGE_NO,storageManager);
		if (lastOutMsgNoStr!=null ) {
			long highestOutMsgNo = 0;
			if (lastOutMsgNoStr!=null) {
				highestOutMsgNo = Long.parseLong(lastOutMsgNoStr);
			}
			
			if (highestOutMsgNo>0) {
				boolean complete = AcknowledgementManager.verifySequenceCompletion (
				sequenceAck.getAcknowledgementRanges().iterator(),highestOutMsgNo);
			
				if (complete) 
					TerminateManager.addTerminateSequenceMessage(rmMsgCtx, outSequenceId,internalSequenceId,storageManager);
			}
		}
		
		//stopping the progress of the message further.
		rmMsgCtx.pause();	
    
    if (log.isDebugEnabled())
      log.debug("Exit: AcknowledgementProcessor::processInMessage");
	}
	

	private SenderBean getRetransmitterEntry(Collection collection,
			long msgNo) {
		Iterator it = collection.iterator();
		while (it.hasNext()) {
			SenderBean bean = (SenderBean) it.next();
			if (bean.getMessageNumber() == msgNo)
				return bean;
		}

		return null;
	}


	
	private static long getNoOfMessagesAcked (Iterator ackRangeIterator) {
		long noOfMsgs = 0;
		while (ackRangeIterator.hasNext()) {
			AcknowledgementRange acknowledgementRange = (AcknowledgementRange) ackRangeIterator.next();
			long lower = acknowledgementRange.getLowerValue();
			long upper = acknowledgementRange.getUpperValue();
			
			for (long i=lower;i<=upper;i++) {
				noOfMsgs++;
			}
		}
		
		return noOfMsgs;
	}
	
	public void processOutMessage(RMMsgContext rmMsgCtx) throws SandeshaException {
    if (log.isDebugEnabled())
    {
      log.debug("Enter: AcknowledgementProcessor::processOutMessage");
      log.debug("Exit: AcknowledgementProcessor::processOutMessage");
    }

	}
}
