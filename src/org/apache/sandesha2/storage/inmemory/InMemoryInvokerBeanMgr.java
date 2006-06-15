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
package org.apache.sandesha2.storage.inmemory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;

/**
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 */

public class InMemoryInvokerBeanMgr implements InvokerBeanMgr {
	
	private static final Log log = LogFactory.getLog(InMemoryInvokerBeanMgr.class);
	private Hashtable table = null;

	public InMemoryInvokerBeanMgr(AbstractContext context) {
		Object obj = context.getProperty(Sandesha2Constants.BeanMAPs.STORAGE_MAP);
		if (obj != null) {
			table = (Hashtable) obj;
		} else {
			table = new Hashtable();
			context.setProperty(Sandesha2Constants.BeanMAPs.STORAGE_MAP, table);
		}
	}

	public synchronized boolean insert(InvokerBean bean) {
		table.put(bean.getMessageContextRefKey(), bean);
		return true;
	}

	public synchronized boolean delete(String key) {
		return table.remove(key) != null;
	}

	public synchronized InvokerBean retrieve(String key) {
		return (InvokerBean) table.get(key);
	}

	public synchronized ResultSet find(String query) {
		throw new UnsupportedOperationException("selectRS() is not implemented");
	}

	public synchronized Collection find(InvokerBean bean) {
		ArrayList beans = new ArrayList();
		Iterator iterator = table.values().iterator();

		InvokerBean temp = new InvokerBean();
		while (iterator.hasNext()) {
			temp = (InvokerBean) iterator.next();
			boolean select = true;

			if (bean.getMessageContextRefKey() != null && !bean.getMessageContextRefKey().equals(temp.getMessageContextRefKey()))
				select = false;

			if (bean.getMsgNo() != 0 && bean.getMsgNo() != temp.getMsgNo())
				select = false;

			if (bean.getSequenceID() != null
					&& !bean.getSequenceID().equals(temp.getSequenceID()))
				select = false;
			
			if (bean.isInvoked()!=temp.isInvoked())
				select = false;

			if (select)
				beans.add(temp);
		}
		return beans;
	}

	public synchronized boolean update(InvokerBean bean) {
		if (table.get(bean.getMessageContextRefKey())==null)
			return false;

		return table.put(bean.getMessageContextRefKey(), bean) != null;
	}
	
	public synchronized InvokerBean findUnique (InvokerBean bean) throws SandeshaException {
		Collection coll = find(bean);
		if (coll.size()>1) {
			String message = "Non-Unique result";
			log.error(message);
			throw new SandeshaException (message);
		}
		
		Iterator iter = coll.iterator();
		if (iter.hasNext())
			return (InvokerBean) iter.next();
		else 
			return null;
	}

}