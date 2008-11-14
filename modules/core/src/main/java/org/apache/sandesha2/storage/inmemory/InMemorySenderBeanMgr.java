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

package org.apache.sandesha2.storage.inmemory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;
import org.apache.sandesha2.util.LoggingControl;

public class InMemorySenderBeanMgr extends InMemoryBeanMgr<SenderBean> implements SenderBeanMgr {
	
	private static final Log log = LogFactory.getLog(InMemorySenderBeanMgr.class);

	ConcurrentHashMap<String, String> sequenceIdandMessNum2MessageId = new ConcurrentHashMap<String, String>();

	public InMemorySenderBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.RETRANSMITTER);
	}

	public boolean delete(String MessageId) throws SandeshaStorageException {
		SenderBean bean =(SenderBean) super.delete(MessageId);
		if(bean.getSequenceID()!=null && bean.getMessageNumber()>0){
			sequenceIdandMessNum2MessageId.remove(bean.getSequenceID()+":"+bean.getMessageNumber());
		}
		return bean!=null;

	}

	public SenderBean retrieve(String MessageId) throws SandeshaStorageException {
		return (SenderBean) super.retrieve(MessageId);
	}

	public SenderBean retrieve(String sequnceId, long messageNumber) throws SandeshaStorageException {
		String MessageId = (String) sequenceIdandMessNum2MessageId.get(sequnceId+":"+messageNumber);
		if(MessageId == null){
			return null;
		}
		return (SenderBean) super.retrieve(MessageId);
	}


	public boolean insert(SenderBean bean) throws SandeshaStorageException {
		if (bean.getMessageID() == null)
			throw new SandeshaStorageException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullMsgId));
		boolean result = super.insert(bean.getMessageID(), bean);
		if(bean.getSequenceID()!=null && bean.getMessageNumber()>0){
			sequenceIdandMessNum2MessageId.put(bean.getSequenceID()+":"+bean.getMessageNumber(), bean.getMessageID());
		}		
		mgr.getInMemoryTransaction().setSentMessages(true);
		return result;
	}

	public List<SenderBean> find(String internalSequenceID) throws SandeshaStorageException {
		SenderBean temp = new SenderBean();
		temp.setInternalSequenceID(internalSequenceID);
		return super.find(temp);
	}
	
	public List<SenderBean> find(SenderBean bean) throws SandeshaStorageException {
		return super.find(bean);
	}

	public SenderBean getNextMsgToSend(String sequenceId) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemorySenderBeanManager::getNextMessageToSend " + sequenceId);
		
		// Set up match criteria
		SenderBean matcher = new SenderBean();
		matcher.setSend(true);
		matcher.setSequenceID(sequenceId);
		matcher.setTimeToSend(System.currentTimeMillis());
		matcher.setTransportAvailable(true);
		
		List<SenderBean> matches = super.findNoLock(matcher);
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Found " + matches.size() + " messages");
		
		// Look for the message with the lowest send time, and send that one.
		SenderBean result = null;
		Iterator<SenderBean> i = matches.iterator();
		while(i.hasNext()) {
			SenderBean bean = (SenderBean) i.next();
			if (bean.getTimeToSend()<0)
				continue; //Beans with negative timeToSend values are not considered as candidates for sending.
			
			if (bean.getSentCount() > 0 && !bean.isReSend())
				continue; //Avoid re-sending messages that we should not resend
			
			// Check that the Send time has not been updated under another thread
			if (!bean.match(matcher))
				continue;
			
			if(result == null) {
				result = bean;
			} else if(result.getTimeToSend() > bean.getTimeToSend()) {
				result = bean;
			}
		}
		// Because the beans weren't locked before, need to do a retrieve to get a locked copy.
		// And then check that it's still valid
		if(result!=null){
			result = retrieve(result.getMessageID());
			if(!result.match(matcher)){
				result = null;
			}
		}
		
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemorySenderBeanManager::getNextMessageToSend " + result);
		return result;
	}
	
	public boolean update(SenderBean bean) throws SandeshaStorageException {
		boolean result = super.update(bean.getMessageID(), bean);
		if(bean.getSequenceID()!=null && bean.getMessageNumber()>0){
			sequenceIdandMessNum2MessageId.put(bean.getSequenceID()+":"+bean.getMessageNumber(), bean.getMessageID());
		}
		mgr.getInMemoryTransaction().setSentMessages(true);
		return result;
	}
	
	public SenderBean findUnique(SenderBean bean) throws SandeshaStorageException {
		return super.findUnique(bean);
	}

	public SenderBean retrieveFromMessageRefKey(String messageContextRefKey) {
		throw new UnsupportedOperationException("Deprecated method");
	}
	
	

}
