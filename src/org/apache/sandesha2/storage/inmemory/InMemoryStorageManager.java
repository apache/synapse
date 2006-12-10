/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.apache.sandesha2.storage.inmemory;

import java.util.Collection;
import java.util.HashMap;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMBean;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.Invoker;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.workers.Sender;

public class InMemoryStorageManager extends StorageManager {

	private static Log log = LogFactory.getLog(InMemoryStorageManager.class);

	private static InMemoryStorageManager instance = null;
    private final String MESSAGE_MAP_KEY = "Sandesha2MessageMap";
    private RMSBeanMgr  rMSBeanMgr = null;
    private RMDBeanMgr rMDBeanMgr = null;
    private SequencePropertyBeanMgr sequencePropertyBeanMgr = null;
    private SenderBeanMgr senderBeanMgr = null;
    private InvokerBeanMgr invokerBeanMgr = null;
    private Sender sender = null;
    private Invoker invoker = null;
    private HashMap transactions = new HashMap();
    
	public InMemoryStorageManager(ConfigurationContext context) {
		super(context);
		
		this.rMSBeanMgr = new InMemoryRMSBeanMgr (this, context);
		this.rMDBeanMgr = new InMemoryRMDBeanMgr (this, context);
		this.senderBeanMgr = new InMemorySenderBeanMgr (this, context);
		this.invokerBeanMgr = new InMemoryInvokerBeanMgr (this, context);
		this.sequencePropertyBeanMgr = new InMemorySequencePropertyBeanMgr (this, context);
		this.sender = new Sender();
		this.invoker = new Invoker();
	}

	public Transaction getTransaction() {
		// Calling getTransaction is the only way to set up a new transaction. If you
		// do some work that requires a tran without there being a transaction in scope
		// then the enlist method will throw an exception.
		Transaction result = null;
		synchronized (transactions) {
			Thread key = Thread.currentThread();
			String name = key.getName();
			int    id = System.identityHashCode(key);
			result = (Transaction) transactions.get(key);
			if(result == null) {
				result = new InMemoryTransaction(this, name, id);
				transactions.put(key, result);
			} else {
				// We just returned an existing transaction. That might be ok, but it
				// might be an indication of a real problem.
				if(log.isDebugEnabled()) log.debug("Possible re-used transaction: " + result);
			}
		}
		return result;
	}
	
	void removeTransaction(Transaction t) {
		synchronized (transactions) {
			Collection entries = transactions.values();
			entries.remove(t);
		}
	}
	
	/** 
	 * Gets the Invoker for this Storage manager
	 */
	public SandeshaThread getInvoker() {
	  return invoker;
  }

	/** 
	 * Gets the Sender for this Storage manager
	 */
	public SandeshaThread getSender() {
	  return sender;
  }

	void enlistBean(RMBean bean) throws SandeshaStorageException {
		InMemoryTransaction t = null;
		synchronized (transactions) {
			Thread key = Thread.currentThread();
			t = (InMemoryTransaction) transactions.get(key);
			if(t == null) {
				// We attempted to do some work without a transaction in scope
				String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.noTransaction);
				SandeshaStorageException e = new SandeshaStorageException(message);
				if(log.isDebugEnabled()) log.debug(message, e);
				throw e;
			}
		}
		t.enlist(bean);
	}
	
	public RMSBeanMgr getCreateSeqBeanMgr() {
		return rMSBeanMgr;
	}

	public RMDBeanMgr getNextMsgBeanMgr() {
		return rMDBeanMgr;
	}

	public SenderBeanMgr getRetransmitterBeanMgr() {
		return senderBeanMgr;
	}

	public SequencePropertyBeanMgr getSequencePropertyBeanMgr() {
		return sequencePropertyBeanMgr;
	}

	public InvokerBeanMgr getStorageMapBeanMgr() {
		return invokerBeanMgr;
	}

	public void init(ConfigurationContext context) {
		setContext(context);
	}

	public static InMemoryStorageManager getInstance(
			ConfigurationContext context) {
		if (instance == null)
			instance = new InMemoryStorageManager(context);

		return instance;
	}
	
	public MessageContext retrieveMessageContext(String key,ConfigurationContext context) {
		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		if (storageMap==null)
			return null;
		
		return (MessageContext) storageMap.get(key);
	}

	public void storeMessageContext(String key,MessageContext msgContext) {
		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		
		if (storageMap==null) {
			storageMap = new HashMap ();
			getContext().setProperty(MESSAGE_MAP_KEY,storageMap);
		}
		
		if (key==null)
		    key = SandeshaUtil.getUUID();
		
		storageMap.put(key,msgContext);
	}

	public void updateMessageContext(String key,MessageContext msgContext) throws SandeshaStorageException { 
		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		
		if (storageMap==null) {
			throw new SandeshaStorageException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.storageMapNotPresent));
		}
		
		Object oldEntry = storageMap.get(key);
		if (oldEntry==null)
			throw new SandeshaStorageException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.entryNotPresentForUpdating));
		
		storeMessageContext(key,msgContext);
	}
	
	public void removeMessageContext(String key) throws SandeshaStorageException { 
		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		
		if (storageMap==null) {
			return;
		}
		
		Object entry = storageMap.get(key);
		if (entry!=null)
			storageMap.remove(key);
	}
	
	public void  initStorage (AxisModule moduleDesc) {
		
	}

	public SOAPEnvelope retrieveSOAPEnvelope(String key) throws SandeshaStorageException {
		// TODO no real value
		return null;
	}

	public void storeSOAPEnvelope(SOAPEnvelope envelope, String key) throws SandeshaStorageException {
		// TODO no real value
	}

	
	
}


