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
import org.apache.sandesha2.storage.beans.RMBean;
import org.apache.sandesha2.storage.beans.SenderBean;

public class InMemorySenderBeanMgr extends InMemoryBeanMgr implements SenderBeanMgr {
	
	private static final Log log = LogFactory.getLog(InMemorySenderBeanMgr.class);

	public InMemorySenderBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.RETRANSMITTER);
	}

	public boolean delete(String MessageId) {
		return super.delete(MessageId);
	}

	public SenderBean retrieve(String MessageId) {
		return (SenderBean) super.retrieve(MessageId);
	}

	public boolean insert(SenderBean bean) throws SandeshaStorageException {
		if (bean.getMessageID() == null)
			throw new SandeshaStorageException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.nullMsgId));
		return super.insert(bean.getMessageID(), bean);
	}

	public List find(String internalSequenceID) {
		SenderBean temp = new SenderBean();
		temp.setInternalSequenceID(internalSequenceID);
		return super.find(temp);
	}
	
	protected boolean match(RMBean matchInfo, RMBean candidate) {
		if(log.isDebugEnabled()) log.debug("Entry: InMemorySenderBeanMgr::match");
		SenderBean bean = (SenderBean)matchInfo;
		SenderBean temp = (SenderBean) candidate;
		
		boolean add = true;

		if (bean.getMessageContextRefKey() != null && !bean.getMessageContextRefKey().equals(temp.getMessageContextRefKey())) {
			log.debug("MessageContextRefKey didn't match");
			add = false;
		}
		// Time is a bit special - we match all the beans that should be sent
		// before the moment in time that the match criteria give us.
		if (bean.getTimeToSend() > 0
				&& bean.getTimeToSend() < temp.getTimeToSend()) {
			log.debug("MessageContextRefKey didn't match");
			add = false;
		}
		
		if (bean.getMessageID() != null
				&& !bean.getMessageID().equals(temp.getMessageID())) {
			log.debug("MessageID didn't match");
			add = false;
		}
		
		if (bean.getInternalSequenceID() != null
				&& !bean.getInternalSequenceID().equals("")
				&& !bean.getInternalSequenceID().equals(
						temp.getInternalSequenceID())) {
			log.debug("InternalSequenceID didn't match");
			add = false;
		}
		
		if (bean.getMessageNumber() > 0
				&& bean.getMessageNumber() != temp.getMessageNumber()) {
			log.debug("MessageNumber didn't match");
			add = false;
		}
		
		if (bean.getMessageType() != Sandesha2Constants.MessageTypes.UNKNOWN
				&& bean.getMessageType() != temp.getMessageType()) {
			log.debug("MessageType didn't match");
			add = false;
		}

		if (bean.isSend() != temp.isSend()) {
			log.debug("isSend didn't match");
			add = false;
		}

		// Do not use the isReSend flag to match messages, as it can stop us from
		// detecting RM messages during 'getNextMsgToSend'
		//if (bean.isReSend() != temp.isReSend()) {
		//	log.debug("isReSend didn't match");
		//	add = false;
		//}

		if(log.isDebugEnabled()) log.debug("Exit: InMemorySenderBeanMgr::match, " + add);
		return add;
	}

	public List find(SenderBean bean) {
		return super.find(bean);
	}

	public SenderBean getNextMsgToSend() {
		// Set up match criteria
		SenderBean matcher = new SenderBean();
		matcher.setSend(true);
		matcher.setTimeToSend(System.currentTimeMillis());
		
		List matches = super.find(matcher);
		
		// We either return an application message or an RM message. If we find
		// an application message first then we carry on through the list to be
		// sure that we send the lowest app message avaliable. If we hit a RM
		// message first then we are done.
		SenderBean result = null;
		Iterator i = matches.iterator();
		while(i.hasNext()) {
			SenderBean bean = (SenderBean) i.next();
			if(bean.getMessageType() == Sandesha2Constants.MessageTypes.APPLICATION) {
				long number = bean.getMessageNumber();
				if(result == null || result.getMessageNumber() > number) {
					result = bean;
				}
			} else if(result == null) {
				result = bean;
				break;
			}
		}
		
		return result;
	}
	
	public boolean update(SenderBean bean) {
		return super.update(bean.getMessageID(), bean);
	}
	
	public SenderBean findUnique(SenderBean bean) throws SandeshaException {
		return (SenderBean) super.findUnique(bean);
	}

	public SenderBean retrieveFromMessageRefKey(String messageContextRefKey) {
		throw new UnsupportedOperationException("Deprecated method");
	}
	
	

}
