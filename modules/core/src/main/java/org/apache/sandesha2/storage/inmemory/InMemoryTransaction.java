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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beans.RMBean;
import org.apache.sandesha2.util.LoggingControl;

/**
 * This class does not really implement transactions, but it is a good
 * place to implement locking for the in memory storage manager.
 */

public class InMemoryTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(InMemoryTransaction.class);

	private InMemoryStorageManager manager;
	private String threadName;
	private ArrayList enlistedBeans = new ArrayList();
	private boolean sentMessages = false;
	private boolean active = true;
	private Thread thread;
	private boolean useSerialization;
	
	InMemoryTransaction(InMemoryStorageManager manager, Thread thread, boolean useSerialization) {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryTransaction::<init>");
		this.manager = manager;
		this.thread = thread;
		this.threadName = thread.getName();
		this.useSerialization = useSerialization;
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryTransaction::<init>, " + this);
	}
	
	public void commit() {
		releaseLocks();
		if(sentMessages && useSerialization) manager.getSender().wakeThread();
		active = false;
	}

	public void rollback() {
		releaseLocks();
		active = false;
	}
	
	public boolean isActive () {
		return active;
	}
	
	private class DummyTransaction extends ReentrantLock implements Transaction {

		private static final long serialVersionUID = -8095723965216941864L;

		public void commit() throws SandeshaStorageException {
			throw new SandeshaStorageException("Not supported");
		}

		public boolean isActive() {
			return false;
		}

		public void rollback() throws SandeshaStorageException {
			throw new SandeshaStorageException("Not supported");
		}

	}
	
	public void enlist(RMBean bean) {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryTransaction::enlist, " + bean);
		if (bean != null) {
			DummyTransaction tran = null;
			synchronized (bean) {
				tran = (DummyTransaction) bean.getTransaction();
				if (tran == null) {
					tran = new DummyTransaction();
					bean.setTransaction(tran);
				}
			}

			boolean locked = false;
			while (!locked) {
				locked = tran.tryLock();
				if (!locked) {

					try {
						locked = tran.tryLock(5, TimeUnit.SECONDS);
						if (!locked) {
							if (log.isDebugEnabled())
								log.debug("Waiting for bean lock 5 seconds");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				enlistedBeans.add(bean);

			}
		}
		
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryTransaction::enlist");
	}
	
	private void releaseLocks() {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryTransaction::releaseLocks, " + this);
		manager.removeTransaction(this);

		Iterator beans = enlistedBeans.iterator();
		while(beans.hasNext()) {
			RMBean bean = (RMBean) beans.next();
			DummyTransaction tran = (DummyTransaction) bean.getTransaction();
			tran.unlock();
		}
		enlistedBeans.clear();
		
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryTransaction::releaseLocks");
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("[InMemoryTransaction, thread:");
		result.append(thread);
		result.append(", name: ");
		result.append(threadName);
		result.append(", locks: ");
		result.append(enlistedBeans.size());
		result.append("]");
		return result.toString();
	}

	public void setSentMessages(boolean sentMessages) {
		this.sentMessages = sentMessages;
	}
	
	/**
	 * Get the thread which this transaction is associated with.
	 * @return
	 */
	public Thread getThread(){
		return thread;
	}
}





