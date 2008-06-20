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
import java.util.List;
import java.util.Map.Entry;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beans.RMBean;
import org.apache.sandesha2.util.LoggingControl;

import java.util.concurrent.ConcurrentHashMap;;

abstract class InMemoryBeanMgr {

	private static final Log log = LogFactory.getLog(InMemoryBeanMgr.class);
	protected ConcurrentHashMap table;
	protected InMemoryStorageManager mgr;

	protected InMemoryBeanMgr(InMemoryStorageManager mgr, AbstractContext context, String key) {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " <innit> " + mgr + ", " 
				+ context + ", " + key);
		this.mgr = mgr;
		Object obj = context.getProperty(key);
		if (obj != null) {
			table = (ConcurrentHashMap) obj;
		} else {
			table = new ConcurrentHashMap();
			context.setProperty(key, table);
		}
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " <init> " + this);
	}
	
	protected boolean insert(Object key, RMBean bean) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " insert " + key + ", " + bean);
		mgr.enlistBean(bean);
		
		Object oldValue = table.putIfAbsent(key, bean);
		boolean wasInserted = (oldValue == null);

		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " insert " + wasInserted);
		return wasInserted;
	}

	protected boolean delete(Object key) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " delete " + key);
		RMBean bean = (RMBean) table.remove(key);
		if(bean != null) {
			mgr.enlistBean(bean);
		}
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " delete " + bean);
		return bean != null;
	}

	protected RMBean retrieve(Object key) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " retrieve " + key);
		RMBean bean = (RMBean) table.get(key);
		if(bean != null) {
			mgr.enlistBean(bean);
			bean = (RMBean) table.get(key);
		}
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " retrieve " + bean);
		return bean;
	}

	protected boolean update(Object key, RMBean bean) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " update " + key + ", " + bean);
		mgr.enlistBean(bean);
		RMBean oldBean = (RMBean) table.put(key, bean);
		if(oldBean == null) return false;
		mgr.enlistBean(oldBean);
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " update " + true);
		return true;
	}

	protected List find(RMBean matchInfo) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " find " + matchInfo);
		ArrayList beans = new ArrayList();

		if(matchInfo == null) {
			beans.addAll(table.values());
		} else {
			Iterator i = table.entrySet().iterator();
			while(i.hasNext()) {
				Entry e = (Entry)i.next();
				RMBean candidate = (RMBean)e.getValue();
				if(candidate.match(matchInfo)) {
					mgr.enlistBean(candidate);
					// Only return beans which are still in the table
					// once we have a lock on them
					if(candidate.equals(table.get(e.getKey()))){
						beans.add(candidate);
					}
				}
			}
		}
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " find " + beans);
		return beans;
	}
	
	protected List findNoLock(RMBean matchInfo) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " find " + matchInfo);
		ArrayList beans = new ArrayList();

		if(matchInfo == null) {
			beans.addAll(table.values());
		} else {
			Iterator i = table.entrySet().iterator();
			while(i.hasNext()) {
				Entry e = (Entry)i.next();
				RMBean candidate = (RMBean)e.getValue();
				if(candidate.match(matchInfo)) {
					beans.add(candidate);
				}
			}
		}
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " find " + beans);
		return beans;
	}

	protected RMBean findUnique (RMBean matchInfo) throws SandeshaStorageException {
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " findUnique " + matchInfo);
		RMBean result = findUniqueNoLock(matchInfo);		
		// Now we have a point-in-time view of the bean, lock it, and double
		// check that it is still in the table 
		if(result != null) {
			mgr.enlistBean(result);
			if(!table.containsValue(result)) result = null;
		}
		
		if(LoggingControl.isAnyTracingEnabled() && log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " findUnique " + result);
		return result;
	}
  
	protected RMBean findUniqueNoLock (RMBean matchInfo) throws SandeshaStorageException {
		RMBean result = null;
		Iterator i = table.values().iterator();
		while(i.hasNext()) {
			RMBean candidate = (RMBean)i.next();
			if(candidate.match(matchInfo)) {
				if(result == null) {
					result = candidate;
				} else {
					String message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.nonUniqueResult,
							result.toString(),
							candidate.toString());
					SandeshaStorageException e = new SandeshaStorageException(message);
					log.error(message, e);
					throw e;
				}
			}
		}

		return result;
	}
}
