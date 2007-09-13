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
		this.mgr = mgr;
		Object obj = context.getProperty(key);
		if (obj != null) {
			table = (Hashtable) obj;
		} else {
			table = new Hashtable();
			context.setProperty(key, table);
		}
	}
	
	protected boolean insert(Object key, RMBean bean) throws SandeshaStorageException {
		mgr.enlistBean(bean);
		synchronized (table) {
			table.put(key, bean);
		}
		return true;
	}

	protected boolean delete(Object key) throws SandeshaStorageException {
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
		return bean != null;
	}

	protected RMBean retrieve(Object key) throws SandeshaStorageException {
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
		return bean;
	}

	protected boolean update(Object key, RMBean bean) throws SandeshaStorageException {
		mgr.enlistBean(bean);
		RMBean oldBean = null;
		synchronized (table) {
			oldBean = (RMBean) table.get(key);
			table.put(key, bean);
		}
		if(oldBean == null) return false;
		
		mgr.enlistBean(oldBean);
		return true;
	}

	protected List find(RMBean matchInfo) throws SandeshaStorageException {
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

		return beans;
	}

	protected RMBean findUnique (RMBean matchInfo) throws SandeshaException {
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
		
		return result;
	}

}
