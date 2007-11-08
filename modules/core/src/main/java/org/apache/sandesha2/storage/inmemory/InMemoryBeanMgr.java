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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beans.RMBean;

abstract class InMemoryBeanMgr {

	private static final Log log = LogFactory.getLog(InMemoryBeanMgr.class);
	private Hashtable table;
	protected InMemoryStorageManager mgr;

	protected InMemoryBeanMgr(InMemoryStorageManager mgr, AbstractContext context, String key) {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " <innit> " + mgr + ", " 
				+ context + ", " + key);
		this.mgr = mgr;
		Object obj = context.getProperty(key);
		if (obj != null) {
			table = (Hashtable) obj;
		} else {
			table = new Hashtable();
			context.setProperty(key, table);
		}
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " <init> " + this);
	}
	
	protected boolean insert(Object key, RMBean bean) throws SandeshaStorageException {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " insert " + key + ", " + bean);
		mgr.enlistBean(bean);
		synchronized (table) {
			table.put(key, bean);
		}
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " insert " + true);
		return true;
	}

	protected boolean delete(Object key) throws SandeshaStorageException {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " delete " + key);
		RMBean bean = null;
		synchronized (table) {
			bean = (RMBean) table.get(key);
		}
		if(bean != null) {
			mgr.enlistBean(bean);
			synchronized (table) {
				bean = (RMBean) table.remove(key);
			}
		}
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " delete " + bean);
		return bean != null;
	}

	protected RMBean retrieve(Object key) throws SandeshaStorageException {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " retrieve " + key);
		RMBean bean = null;
		synchronized (table) {
			bean = (RMBean) table.get(key);
		}
		if(bean != null) {
			mgr.enlistBean(bean);
			synchronized (table) {
				bean = (RMBean) table.get(key);
			}
		}
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " retrieve " + bean);
		return bean;
	}

	protected boolean update(Object key, RMBean bean) throws SandeshaStorageException {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " update " + key + ", " + bean);
		mgr.enlistBean(bean);
		RMBean oldBean = null;
		synchronized (table) {
			oldBean = (RMBean) table.get(key);
			table.put(key, bean);
		}
		if(oldBean == null) return false;
		
		mgr.enlistBean(oldBean);
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " update " + true);
		return true;
	}

	protected List find(RMBean matchInfo) throws SandeshaStorageException {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " find " + matchInfo);
		ArrayList beans = new ArrayList();
		synchronized (table) {
			if(matchInfo == null) {
				beans.addAll(table.values());
			} else {
				Iterator i = table.values().iterator();
				while(i.hasNext()) {
					RMBean candidate = (RMBean)i.next();
					if(candidate.match(matchInfo)) {
						beans.add(candidate);
					}
				}
			}
		}
		
		// Now we have a point-in-time view of the beans, lock them all
		Iterator i = beans.iterator();
		while(i.hasNext()) mgr.enlistBean((RMBean) i.next());
		
		// Finally remove any beans that are no longer in the table
		synchronized (table) {
			i = beans.iterator();
			while(i.hasNext()) {
				RMBean bean = (RMBean) i.next();
				if(!table.containsValue(bean)) {
					i.remove();
				}
			}
		}
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " find " + beans);
		return beans;
	}

	protected RMBean findUnique (RMBean matchInfo) throws SandeshaException {
		if(log.isDebugEnabled()) log.debug("Entry: InMemoryBeanMgr " + this.getClass() + " findUnique " + matchInfo);
		RMBean result = null;
		synchronized (table) {
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
						SandeshaException e = new SandeshaException(message);
						log.error(message, e);
						throw e;
					}
				}
			}
		}
		
		// Now we have a point-in-time view of the bean, lock it, and double
		// check that it is still in the table 
		if(result != null) {
			mgr.enlistBean(result);
			synchronized (table) {
				if(!table.containsValue(result)) result = null;
			}
		}
		
		if(log.isDebugEnabled()) log.debug("Exit: InMemoryBeanMgr " + this.getClass() + " findUnique " + result);
		return result;
	}

}
