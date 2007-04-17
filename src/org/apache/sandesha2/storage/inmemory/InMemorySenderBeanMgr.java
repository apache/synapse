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
 * 
 */

package org.apache.sandesha2.storage.inmemory;

import java.util.Iterator;
import java.util.List;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;

public class InMemorySenderBeanMgr extends InMemoryBeanMgr implements SenderBeanMgr {
	
	private static final Log log = LogFactory.getLog(InMemorySenderBeanMgr.class);

	public InMemorySenderBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.RETRANSMITTER);
	}

	public boolean delete(String MessageId) throws SandeshaStorageException {
		return super.delete(MessageId);
	}

	public SenderBean retrieve(String MessageId) throws SandeshaStorageException {
		return (SenderBean) super.retrieve(MessageId);
	}

	public boolean insert(SenderBean bean) throws SandeshaStorageException {
		if (bean.getMessageID() == null)
			throw new SandeshaStorageException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullMsgId));
		boolean result = super.insert(bean.getMessageID(), bean);
		mgr.getInMemoryTransaction().setSentMessages(true);
		return result;
	}

	public List find(String internalSequenceID) throws SandeshaStorageException {
		SenderBean temp = new SenderBean();
		temp.setInternalSequenceID(internalSequenceID);
		return super.find(temp);
	}
	
	public List find(SenderBean bean) throws SandeshaStorageException {
		return super.find(bean);
	}

	public SenderBean getNextMsgToSend(String sequenceId) throws SandeshaStorageException {
		if(log.isDebugEnabled()) log.debug("Entry: InMemorySenderBeanManager::getNextMessageToSend " + sequenceId);
		
		// Set up match criteria
		SenderBean matcher = new SenderBean();
		matcher.setSend(true);
		matcher.setSequenceID(sequenceId);
		matcher.setTimeToSend(System.currentTimeMillis());
		matcher.setTransportAvailable(true);
		
		List matches = super.find(matcher);
		if(log.isDebugEnabled()) log.debug("Found " + matches.size() + " messages");
		
		// Look for the message with the lowest send time, and send that one.
		SenderBean result = null;
		Iterator i = matches.iterator();
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
		
		if(log.isDebugEnabled()) log.debug("Exit: InMemorySenderBeanManager::getNextMessageToSend " + result);
		return result;
	}
	
	public boolean update(SenderBean bean) throws SandeshaStorageException {
		boolean result = super.update(bean.getMessageID(), bean);
		mgr.getInMemoryTransaction().setSentMessages(true);
		return result;
	}
	
	public SenderBean findUnique(SenderBean bean) throws SandeshaException {
		return (SenderBean) super.findUnique(bean);
	}

	public SenderBean retrieveFromMessageRefKey(String messageContextRefKey) {
		throw new UnsupportedOperationException("Deprecated method");
	}
	
	

}
