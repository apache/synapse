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
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.CreateSeqBean;
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

		toAddress = rmMsgCtx.getTo().getAddress();
		sequenceKey = (String) options.getProperty(SandeshaClientConstants.SEQUENCE_KEY);
		internalSequenceID = SandeshaUtil.getInternalSequenceID(toAddress, sequenceKey);

		// Does the sequence exist ?
		sequenceExists = false;
		outSequenceID = null;
		
		// Get the Create sequence bean with the matching internal sequenceid 
		CreateSeqBean createSeqFindBean = new CreateSeqBean();
		createSeqFindBean.setInternalSequenceID(internalSequenceID);

		CreateSeqBean createSeqBean = storageManager.getCreateSeqBeanMgr().findUnique(createSeqFindBean);
		
		if (createSeqBean == null)
		{
			if (log.isDebugEnabled())
				log.debug("Exit: WSRMParentProcessor::setupOutMessage Sequence doesn't exist");
			
			throw new SandeshaException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.couldNotSendTerminateSeqNotFound, internalSequenceID));			
		}
		
		if (createSeqBean.getSequenceID() != null)
		{
			sequenceExists = true;		
			outSequenceID = createSeqBean.getSequenceID();
		}
		else
			outSequenceID = Sandesha2Constants.TEMP_SEQUENCE_ID;			

		String rmVersion = SandeshaUtil.getRMVersion(getInternalSequenceID(), getStorageManager());
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
		
		String transportTo = SandeshaUtil.getSequenceProperty(internalSequenceID,
				Sandesha2Constants.SequenceProperties.TRANSPORT_TO, storageManager);
		if (transportTo != null) {
			rmMsgCtx.setProperty(Constants.Configuration.TRANSPORT_URL, transportTo);
		}		
		
		//setting msg context properties
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_ID, outSequenceID);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.INTERNAL_SEQUENCE_ID, internalSequenceID);
		rmMsgCtx.setProperty(Sandesha2Constants.MessageContextProperties.SEQUENCE_PROPERTY_KEY , sequenceKey);

		rmMsgCtx.addSOAPEnvelope();

		// Ensure the outbound message us secured using the correct token
		String tokenData = SandeshaUtil.getSequenceProperty(internalSequenceID,
				Sandesha2Constants.SequenceProperties.SECURITY_TOKEN,
				storageManager);
		if(tokenData != null) {
			SecurityManager secMgr = SandeshaUtil.getSecurityManager(configurationContext);
			SecurityToken token = secMgr.recoverSecurityToken(tokenData);
			secMgr.applySecurityToken(token, msgContext);
		}
		
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
		
		msgContext.setProperty(Sandesha2Constants.QUALIFIED_FOR_SENDING, Sandesha2Constants.VALUE_FALSE);

		senderBean.setReSend(false);

		SenderBeanMgr retramsmitterMgr = storageManager.getRetransmitterBeanMgr();
		
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

}
