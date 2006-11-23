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

package org.apache.sandesha2.storage.inmemory;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beans.RMBean;

/**
 * This class does not really implement transactions, but it is a good
 * place to implement locking for the in memory storage manager.
 */

public class InMemoryTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(InMemoryTransaction.class);

	private InMemoryStorageManager manager;
	private Long key;
	private String threadName;
	private ArrayList enlistedBeans = new ArrayList();
	
	InMemoryTransaction(InMemoryStorageManager manager, Long key, String threadName) {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryTransaction::<init>");
		this.manager = manager;
		this.key = key;
		this.threadName = threadName;
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryTransaction::<init>, " + this);
	}
	
	public void commit() {
		releaseLocks();
	}

	public void rollback() {
		releaseLocks();
	}
	
	public boolean isActive () {
		return !enlistedBeans.isEmpty();
	}

	public void enlist(RMBean bean) {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryTransaction::enlist, " + bean);
		if(bean != null) {
			synchronized (bean) {
				Transaction other = bean.getTransaction();
				while(other != null && other != this) {

					if(!enlistedBeans.isEmpty()) {
						Exception e = new Exception("Possible deadlock");
						if(log.isDebugEnabled()) {
							log.debug("Possible deadlock", e);
							log.debug(this + ", " + bean);
						}
					}

					try {
						if(log.isDebugEnabled()) log.debug("This " + this + " waiting for " + other);
						bean.wait();
					} catch(InterruptedException e) {
						// Do nothing
					}
					other = bean.getTransaction();
				}
				if(other == null) {
					if(log.isDebugEnabled()) log.debug(this + " locking bean");
					bean.setTransaction(this);
					enlistedBeans.add(bean);
				}
			}
		}
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryTransaction::enlist");
	}
	
	private void releaseLocks() {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryTransaction::releaseLocks, " + this);
		manager.removeTransaction(this);

		Iterator beans = enlistedBeans.iterator();
		while(beans.hasNext()) {
			RMBean bean = (RMBean) beans.next();
			synchronized (bean) {
				bean.setTransaction(null);
				bean.notify();
			}
		}
		enlistedBeans.clear();
		
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryTransaction::releaseLocks");
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("[InMemoryTransaction #");
		result.append(key);
		result.append(", name: ");
		result.append(threadName);
		result.append(", locks: ");
		result.append(enlistedBeans.size());
		result.append("]");
		return result.toString();
	}
}
