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

package org.apache.sandesha2.storage;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.workers.SandeshaThread;

/**
 * Storage managers should extend this.
 * 
 */

public abstract class StorageManager {

	private ConfigurationContext context;

	public StorageManager(ConfigurationContext context) {
		this.context = context;
	}

	public ConfigurationContext getContext() {
		return context;
	}

	public void setContext(ConfigurationContext context) {
		if (context != null)
			this.context = context;
	}
	
	public void shutdown(){
		//shutdown the running threads
		getSender().stopRunning();
		
		SandeshaThread thread = getInvoker();
		if(thread != null) thread.stopRunning();
		
		thread = getPollingManager();
		if(thread != null) thread.stopRunning();
		
		//removing the reference to the configuration context
		context=null;
	}
	
	public abstract void initStorage (AxisModule moduleDesc) throws SandeshaStorageException;

	public abstract Transaction getTransaction();
	
	public abstract SandeshaThread getSender();
	
	/**
	 * Get the invoker that hands inbound messages over to the application. This
	 * may be null, in which case the inbound messages will be dispatched directly
	 * to the application without switching them over to the invoker.
	 * @return null if messages should be delivered directly to the application,
	 * otherwise return a SandeshaThread.
	 */
	public abstract SandeshaThread getInvoker();
	
	/**
	 * Get the thread that generates polling requests to send to remote endpoints.
	 * This may be null, in which case the storage manager does not support polling.
	 * @return null if polling is diabled, otherwise return a PollingManager.
	 */
	public abstract PollingManager getPollingManager();

	public abstract RMSBeanMgr getRMSBeanMgr();

	public abstract RMDBeanMgr getRMDBeanMgr();

	public abstract SenderBeanMgr getSenderBeanMgr();

	public abstract InvokerBeanMgr getInvokerBeanMgr();
	
	public abstract void storeMessageContext (String storageKey,MessageContext msgContext) throws SandeshaStorageException;
	
	public abstract void updateMessageContext (String storageKey,MessageContext msgContext) throws SandeshaStorageException;

	public abstract MessageContext retrieveMessageContext (String storageKey, ConfigurationContext configContext) throws SandeshaStorageException;

	public abstract void removeMessageContext (String storageKey) throws SandeshaStorageException;

}
