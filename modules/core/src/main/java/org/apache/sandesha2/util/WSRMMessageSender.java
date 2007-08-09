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
package org.apache.sandesha2.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.storage.beans.SenderBean;

public class WSRMMessageSender  {

	private static final Log log = LogFactory.getLog(WSRMMessageSender.class);
	
	private MessageContext msgContext;
	private StorageManager storageManager;
	private ConfigurationContext configurationContext;
	private String toAddress;
	private String sequenceKey;
	private String internalSequenceID;
	private boolean sequenceExists;
	private String outSequenceID;
	private String rmVersion;
	private RMSBean rmsBean;
	
	/**
	 * Extracts information from the rmMsgCtx specific for processing out messages
	 * 
	 * @param rmMsgCtx
	 * @throws AxisFault
	 */
	protected void setupOutMessage(RMMsgContext rmMsgCtx) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: WSRMParentProcessor::setupOutMessage");

		msgContext = rmMsgCtx.getMessageContext();
		configurationContext = msgContext.getConfigurationContext();
		Options options = msgContext.getOptions();

		storageManager = SandeshaUtil.getSandeshaStorageManager(configurationContext,
				configurationContext.getAxisConfiguration());

		internalSequenceID = 
			(String)rmMsgCtx.getProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID);
		
		toAddress = rmMsgCtx.getTo().getAddress();
		sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		
		if(internalSequenceID==null)
		{
			internalSequenceID = SandeshaUtil.getInternalSequenceID(toAddress, sequenceKey);			
		}

		// Does the sequence exist ?
		sequenceExists = false;
		outSequenceID = null;
		
		// Get the RMSBean with the matching internal sequenceid 
		rmsBean = SandeshaUtil.getRMSBeanFromInternalSequenceId(storageManager, internalSequenceID);
		
		if (rmsBean == null)
		{
			if (log.isDebugEnabled())
				log.debug("Exit: WSRMParentProcessor::setupOutMessage Sequence doesn't exist");
			
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotSendTerminateSeqNotFound, internalSequenceID));			
		}
		
		if (rmsBean.getSequenceID() != null)
		{
			sequenceExists = true;		
			outSequenceID = rmsBean.getSequenceID();
		}
		else
			outSequenceID = Sandesha2Constants.TEMP_SEQUENCE_ID;			

		rmVersion = rmsBean.getRMVersion();
		if (rmVersion == null)
			throw new SandeshaException(SandeshaMessageHelper.getMessage(SandeshaMessageKeys.cannotDecideRMVersion));

		if (log.isDebugEnabled())
			log.debug("Exit: WSRMParentProcessor::setupOutMessage");
  }
	
	
	protected void sendOutgoingMessage(RMMsgContext rmMsgCtx, int msgType, long delay) throws AxisFault {
		if (log.isDebugEnabled())
			log.debug("Enter: WSRMParentProcessor::sendOutgoingMessage " + msgType + ", " + delay);
		
		rmMsgCtx.setFlow(MessageContext.OUT_FLOW);
		getMsgContext().setProperty(Sandesha2Constants.APPLICATION_PROCESSING_DONE, "true");

		rmMsgCtx.setTo(new EndpointReference(toAddress));
		
		String transportTo = rmsBean.getTransportTo();
		if (transportTo != null) {
			rmMsgCtx.setProperty(Constants.Configuration.TRANSPORT_URL, transportTo);
		}		
		
		//setting msg context properties
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID, outSequenceID);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID, internalSequenceID);

		rmMsgCtx.addSOAPEnvelope();

		// Ensure the outbound message us secured using the correct token
		RMMsgCreator.secureOutboundMessage(getRMSBean(), msgContext);
		
		String key = SandeshaUtil.getUUID();

		SenderBean senderBean = new SenderBean();
		senderBean.setMessageType(msgType);
		senderBean.setMessageContextRefKey(key);
		senderBean.setTimeToSend(System.currentTimeMillis() + delay);
		senderBean.setMessageID(msgContext.getMessageID());
		
		// Set the internal sequence id and outgoing sequence id for the terminate message
		senderBean.setInternalSequenceID(internalSequenceID);
		if (sequenceExists)
		{
			senderBean.setSend(true);
			senderBean.setSequenceID(outSequenceID);
		}
		else
			senderBean.setSend(false);			
		
		EndpointReference to = msgContext.getTo();
		if (to!=null)
			senderBean.setToAddress(to.getAddress());

		// If this message is targetted at an anonymous address then we must not have a transport
		// ready for it, as the current message is not a reply.
		if(to == null || to.hasAnonymousAddress())
			senderBean.setTransportAvailable(false);
		
		msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		senderBean.setReSend(false);

		SenderBeanMgr retramsmitterMgr = storageManager.getSenderBeanMgr();
		
		SandeshaUtil.executeAndStore(rmMsgCtx, key);
	
		retramsmitterMgr.insert(senderBean);
		
		if (log.isDebugEnabled())
			log.debug("Exit: WSRMParentProcessor::sendOutgoingMessage");

	}
	

	public final StorageManager getStorageManager() {
  	return storageManager;
  }

	public final String getInternalSequenceID() {
  	return internalSequenceID;
  }

	public final MessageContext getMsgContext() {
  	return msgContext;
  }

	public final String getOutSequenceID() {
  	return outSequenceID;
  }

	public final boolean isSequenceExists() {
  	return sequenceExists;
  }

	public final String getSequenceKey() {
  	return sequenceKey;
  }

	public final String getToAddress() {
  	return toAddress;
  }

	public final ConfigurationContext getConfigurationContext() {
  	return configurationContext;
  }

	public final String getRMVersion() {
  	return rmVersion;
  }
	
	public final RMSBean getRMSBean() {
		return rmsBean;
	}

}
