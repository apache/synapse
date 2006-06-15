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

import java.util.HashMap;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.CreateSeqBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.util.SandeshaUtil;

/**
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 */

public class InMemoryStorageManager extends StorageManager {

	private static InMemoryStorageManager instance = null;
    private final String MESSAGE_MAP_KEY = "Sandesha2MessageMap";
    private CreateSeqBeanMgr  createSeqBeanMgr = null;
    private NextMsgBeanMgr nextMsgBeanMgr = null;
    private SequencePropertyBeanMgr sequencePropertyBeanMgr = null;
    private SenderBeanMgr senderBeanMgr = null;
    private InvokerBeanMgr invokerBeanMgr = null;
    
	public InMemoryStorageManager(ConfigurationContext context) {
		super(context);
		
		this.createSeqBeanMgr = new InMemoryCreateSeqBeanMgr (context);
		this.nextMsgBeanMgr = new InMemoryNextMsgBeanMgr (context);
		this.senderBeanMgr = new InMemorySenderBeanMgr (context);
		this.invokerBeanMgr = new InMemoryInvokerBeanMgr (context);
		this.sequencePropertyBeanMgr = new InMemorySequencePropertyBeanMgr (context);
	}

	public Transaction getTransaction() {
		return new InMemoryTransaction();
	}

	public CreateSeqBeanMgr getCreateSeqBeanMgr() {
		return createSeqBeanMgr;
	}

	public NextMsgBeanMgr getNextMsgBeanMgr() {
		return nextMsgBeanMgr;
	}

	public SenderBeanMgr getRetransmitterBeanMgr() {
		return senderBeanMgr;
	}

	public SequencePropertyBeanMgr getSequencePropretyBeanMgr() {
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
			throw new SandeshaStorageException ("Storage Map not present");
		}
		
		Object oldEntry = storageMap.get(key);
		if (oldEntry==null)
			throw new SandeshaStorageException ("Entry is not present for updating");
		
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