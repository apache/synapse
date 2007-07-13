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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;

import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.RMBean;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.Invoker;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.workers.Sender;

public class InMemoryStorageManager extends StorageManager {

	private static Log log = LogFactory.getLog(InMemoryStorageManager.class);

	private static InMemoryStorageManager instance = null;
    private RMSBeanMgr  rMSBeanMgr = null;
    private RMDBeanMgr rMDBeanMgr = null;
    private SenderBeanMgr senderBeanMgr = null;
    private InvokerBeanMgr invokerBeanMgr = null;
    private Sender sender = null;
    private Invoker invoker = null;
    private PollingManager pollingManager = null;
    private HashMap transactions = new HashMap();
    private boolean useSerialization = false;
    private HashMap storageMap = new HashMap();
    
	public InMemoryStorageManager(ConfigurationContext context)
	throws SandeshaException
	{
		super(context);
		
		SandeshaPolicyBean policy = SandeshaUtil.getPropertyBean(context.getAxisConfiguration());
		useSerialization = policy.isUseMessageSerialization();
		
		// Note that while inOrder is a global property we can decide if we need the
		// invoker thread at this point. If we change this to be a sequence-level
		// property then we'll need to revisit this.
		boolean inOrder = policy.isInOrder();
		boolean polling = policy.isEnableMakeConnection();
		
		this.rMSBeanMgr = new InMemoryRMSBeanMgr (this, context);
		this.rMDBeanMgr = new InMemoryRMDBeanMgr (this, context);
		this.senderBeanMgr = new InMemorySenderBeanMgr (this, context);
		this.invokerBeanMgr = new InMemoryInvokerBeanMgr (this, context);
		this.sender = new Sender();
		if(inOrder) this.invoker = new Invoker();
		if(polling) this.pollingManager = new PollingManager();
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

	InMemoryTransaction getInMemoryTransaction() {
		InMemoryTransaction result = null;
		synchronized (transactions) {
			Thread key = Thread.currentThread();
			result = (InMemoryTransaction) transactions.get(key);
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
	
	/** 
	 * Gets the PollingManager for this Storage manager
	 */
	public PollingManager getPollingManager() {
		return pollingManager;
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

	public InvokerBeanMgr getInvokerBeanMgr() {
		return invokerBeanMgr;
	}

	public static InMemoryStorageManager getInstance(
			ConfigurationContext context)
	throws SandeshaException
	{
		if (instance == null)
			instance = new InMemoryStorageManager(context);

		return instance;
	}
	
	public MessageContext retrieveMessageContext(String key,ConfigurationContext context) throws SandeshaStorageException {
		if(log.isDebugEnabled()) log.debug("Enter: InMemoryStorageManager::retrieveMessageContext, key: " + key);
		
		MessageContext messageContext = null;
		try {
			if(useSerialization) {
				SerializedStorageEntry entry = (SerializedStorageEntry) storageMap.get(key);
				
				if(entry != null) {
					if(entry.message != null) {
						// We have the real message, so use that, but make sure that future users create
						// their own copy.
						messageContext = entry.message;
						entry.message = null;
					} else {
						ByteArrayInputStream stream = new ByteArrayInputStream(entry.data);
						ObjectInputStream is = new ObjectInputStream(stream);
						messageContext = (MessageContext) is.readObject();
						messageContext.activate(entry.context);
	
						OperationContext opCtx = messageContext.getOperationContext();
						if(opCtx != null) {
							MessageContext inMsgCtx = opCtx.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
							if(inMsgCtx != null) {
								inMsgCtx.setProperty(RequestResponseTransport.TRANSPORT_CONTROL, entry.inTransportControl);
								inMsgCtx.setProperty(MessageContext.TRANSPORT_OUT,               entry.inTransportOut);
								inMsgCtx.setProperty(Constants.OUT_TRANSPORT_INFO,               entry.inTransportOutInfo);
							}
						}
						
						messageContext.setProperty(RequestResponseTransport.TRANSPORT_CONTROL, entry.transportControl);
						messageContext.setProperty(MessageContext.TRANSPORT_OUT,               entry.transportOut);
						messageContext.setProperty(Constants.OUT_TRANSPORT_INFO,               entry.transportOutInfo);
					}
				}

			} else {
				StorageEntry entry = (StorageEntry) storageMap.get(key);
				
				if(entry != null) {
					messageContext = entry.msgContext;
					SOAPEnvelope clonedEnvelope = SandeshaUtil.cloneEnvelope(entry.envelope);
					messageContext.setEnvelope(clonedEnvelope);
				}
			}
		} catch (Exception e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.failedToLoadMessage, e.toString());
			if(log.isDebugEnabled()) log.debug(message);
			throw new SandeshaStorageException(message, e);
		}

		if(log.isDebugEnabled()) log.debug("Exit: InMemoryStorageManager::retrieveMessageContext, " + messageContext);
		return messageContext; 
	}

	public void storeMessageContext(String key,MessageContext msgContext)
	throws SandeshaStorageException
	{
		if(log.isDebugEnabled()) log.debug("Enter: InMemoryStorageManager::storeMessageContext, key: " + key);
		
		if (key==null)
		    key = SandeshaUtil.getUUID();
		
		try {
			if(useSerialization) {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				// Remove the MustUnderstand parts for serialized message

				SandeshaUtil.removeMustUnderstand(msgContext.getEnvelope());
				ObjectOutputStream s = new ObjectOutputStream(stream);
				s.writeObject(msgContext);
				s.close();
				
				SerializedStorageEntry entry = new SerializedStorageEntry();
				// Store a reference to the real message, as well as serializing it
				entry.message = msgContext;
				entry.data = stream.toByteArray();
				entry.context = msgContext.getConfigurationContext();
				
				OperationContext opCtx = msgContext.getOperationContext();
				if(opCtx != null) {
					MessageContext inMsgCtx = opCtx.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
					if(inMsgCtx != null) {
						entry.inTransportControl = inMsgCtx.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
						entry.inTransportOut     = inMsgCtx.getProperty(MessageContext.TRANSPORT_OUT);
						entry.inTransportOutInfo = inMsgCtx.getProperty(Constants.OUT_TRANSPORT_INFO);
					}
				}
				entry.transportControl = msgContext.getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
				entry.transportOut     = msgContext.getProperty(MessageContext.TRANSPORT_OUT);
				entry.transportOutInfo = msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
				
				storageMap.put(key, entry);

			} else {
				//We are storing the original envelope here.
				//Storing a cloned version will caus HeaderBlocks to loose their setProcessed information.
				StorageEntry entry = new StorageEntry();
				entry.msgContext = msgContext;
				entry.envelope = msgContext.getEnvelope();
				
				//building the full enveloper before storing.
				SOAPEnvelope envelope = msgContext.getEnvelope();
				envelope.buildWithAttachments();
				
				entry.envelope = envelope;
				
				storageMap.put(key,entry);
			}
		} catch(Exception e) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.failedToStoreMessage, e.toString());
			if(log.isDebugEnabled()) log.debug(message);
			throw new SandeshaStorageException(message, e);
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryStorageManager::storeMessageContext, key: " + key);
	}

	public void updateMessageContext(String key,MessageContext msgContext) throws SandeshaStorageException { 
		if(log.isDebugEnabled()) log.debug("Enter: InMemoryStorageManager::updateMessageContext, key: " + key);

		Object oldEntry = storageMap.remove(key);
		if (oldEntry==null)
			throw new SandeshaStorageException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.entryNotPresentForUpdating));
		
		storeMessageContext(key,msgContext);

		if(log.isDebugEnabled()) log.debug("Exit: InMemoryStorageManager::updateMessageContext, key: " + key);
	}
	
	public void removeMessageContext(String key) { 
		if(log.isDebugEnabled()) log.debug("Enter: InMemoryStorageManager::removeMessageContext, key: " + key);

		storageMap.remove(key);
		
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryStorageManager::removeMessageContext, key: " + key);
	}
	
	public void  initStorage (AxisModule moduleDesc) {
		
	}

	private class SerializedStorageEntry {
		MessageContext       message;
		byte[]               data;
		ConfigurationContext context;
		Object               transportControl;
		Object               transportOut;
		Object               transportOutInfo;
		Object               inTransportControl;
		Object               inTransportOut;
		Object               inTransportOutInfo;
	}
	private class StorageEntry {
		MessageContext msgContext;
		SOAPEnvelope   envelope;
	}
}







