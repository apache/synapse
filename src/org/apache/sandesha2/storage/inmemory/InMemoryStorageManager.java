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

import javax.xml.stream.XMLStreamReader;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
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
    private final String ENVELOPE_MAP_KEY = "Sandesha2EnvelopeMap";
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
				// We don't want to return an existing transaction, as someone else should
				// decide if we commit it or not. If we get here then we probably have a
				// bug.
				if(log.isDebugEnabled()) log.debug("Possible re-used transaction: " + result);
				result = null;
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
	
	public RMSBeanMgr getRMSBeanMgr() {
		return rMSBeanMgr;
	}

	public RMDBeanMgr getRMDBeanMgr() {
		return rMDBeanMgr;
	}

	public SenderBeanMgr getSenderBeanMgr() {
		return senderBeanMgr;
	}

	public SequencePropertyBeanMgr getSequencePropertyBeanMgr() {
		return sequencePropertyBeanMgr;
	}

	public InvokerBeanMgr getInvokerBeanMgr() {
		return invokerBeanMgr;
	}

	public static InMemoryStorageManager getInstance(
			ConfigurationContext context) {
		if (instance == null)
			instance = new InMemoryStorageManager(context);

		return instance;
	}
	
	public MessageContext retrieveMessageContext(String key,ConfigurationContext context) throws SandeshaStorageException {
		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		if (storageMap==null)
			return null;
		
		MessageContext messageContext = (MessageContext) storageMap.get(key);
		
		HashMap envMap = (HashMap) getContext().getProperty(ENVELOPE_MAP_KEY);
		if(envMap==null) {
			return null;
		}
		
		//Get hold of the original SOAP envelope
		SOAPEnvelope envelope = (SOAPEnvelope)envMap.get(key);
		
		//Now clone the env and set it in the message context
		if (envelope!=null) {
			
			XMLStreamReader streamReader = envelope.cloneOMElement().getXMLStreamReader();
			SOAPEnvelope clonedEnvelope = new StAXSOAPModelBuilder(streamReader, null).getSOAPEnvelope();
			try {
				messageContext.setEnvelope(clonedEnvelope);
			} catch (AxisFault e) {
				throw new SandeshaStorageException (e);
			}
		}
		
		return messageContext; 
	}

	public void storeMessageContext(String key,MessageContext msgContext) {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryStorageManager::storeMessageContext, key: " + key);
		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		
		if (storageMap==null) {
			storageMap = new HashMap ();
			getContext().setProperty(MESSAGE_MAP_KEY,storageMap);
		}
		
		if (key==null)
		    key = SandeshaUtil.getUUID();
		
		storageMap.put(key,msgContext);
		
		//Now get hold of the SOAP envelope and store it in the env map
		HashMap envMap = (HashMap) getContext().getProperty(ENVELOPE_MAP_KEY);
		
		if(envMap==null) {
			envMap = new HashMap ();
			getContext().setProperty(ENVELOPE_MAP_KEY, envMap);
		}
		
		SOAPEnvelope envelope = msgContext.getEnvelope();
		//storing a cloned version of the envelope in the Map.
		if (envelope!=null) {			
			XMLStreamReader streamReader = envelope.cloneOMElement().getXMLStreamReader();
			SOAPEnvelope clonedEnvelope = new StAXSOAPModelBuilder(streamReader, null).getSOAPEnvelope();
			envMap.put(key, clonedEnvelope);
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryStorageManager::storeMessageContext, key: " + key);
	}

	public void updateMessageContext(String key,MessageContext msgContext) throws SandeshaStorageException { 
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryStorageManager::updateMessageContext, key: " + key);

		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		
		if (storageMap==null) {
			throw new SandeshaStorageException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.storageMapNotPresent));
		}
		
		Object oldEntry = storageMap.get(key);
		if (oldEntry==null)
			throw new SandeshaStorageException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.entryNotPresentForUpdating));
		
		HashMap envMap = (HashMap) getContext().getProperty(ENVELOPE_MAP_KEY);

		storageMap.remove(key);
		if (envMap!=null)
			envMap.remove(key);
		
		storeMessageContext(key,msgContext);

		if(log.isDebugEnabled()) log.debug("Exit: InMemoryStorageManager::updateMessageContext, key: " + key);
	}
	
	public void removeMessageContext(String key) throws SandeshaStorageException { 
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryStorageManager::removeMessageContext, key: " + key);

		HashMap storageMap = (HashMap) getContext().getProperty(MESSAGE_MAP_KEY);
		HashMap envelopeMap = (HashMap) getContext().getProperty(ENVELOPE_MAP_KEY);
		

		if (storageMap!=null)
			storageMap.remove(key);
		
		if (envelopeMap!=null)
			envelopeMap.remove(key);
		
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryStorageManager::removeMessageContext, key: " + key);
	}
	
	public void  initStorage (AxisModule moduleDesc) {
		
	}

}


