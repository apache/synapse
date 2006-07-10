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
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beans.NextMsgBean;

public class InMemoryNextMsgBeanMgr implements NextMsgBeanMgr {

	private static final Log log = LogFactory.getLog(InMemoryNextMsgBeanMgr.class);
	private Hashtable table = null;

	public InMemoryNextMsgBeanMgr(AbstractContext context) {
		Object obj = context.getProperty(Sandesha2Constants.BeanMAPs.NEXT_MESSAGE);

		if (obj != null) {
			table = (Hashtable) obj;
		} else {
			table = new Hashtable();
			context.setProperty(Sandesha2Constants.BeanMAPs.NEXT_MESSAGE, table);
		}
	}

	public synchronized boolean delete(String sequenceId) {
		return table.remove(sequenceId) != null;
	}

	public synchronized NextMsgBean retrieve(String sequenceId) {
		return (NextMsgBean) table.get(sequenceId);
	}

	public synchronized boolean insert(NextMsgBean bean) {
		table.put(bean.getSequenceID(), bean);
		return true;
	}

	public synchronized ResultSet find(String query) {
		throw new UnsupportedOperationException("selectRS() is not supported");
	}

	public synchronized Collection find(NextMsgBean bean) {
		ArrayList beans = new ArrayList();
		Iterator iterator = table.values().iterator();

		if (bean == null)
			return beans;

		NextMsgBean temp;
		while (iterator.hasNext()) {
			temp = (NextMsgBean) iterator.next();

			boolean equal = true;

			if (bean.getNextMsgNoToProcess() > 0
					&& bean.getNextMsgNoToProcess() != temp
							.getNextMsgNoToProcess())
				equal = false;

			if (bean.getSequenceID() != null
					&& !bean.getSequenceID().equals(temp.getSequenceID()))
				equal = false;

			if (equal)
				beans.add(temp);

		}
		return beans;
	}

	public synchronized boolean update(NextMsgBean bean) {
		if (table.get(bean.getSequenceID())==null)
			return false;

		return table.put(bean.getSequenceID(), bean) != null;
	}

	public synchronized Collection retrieveAll() {
		return table.values();
	}
	
	public synchronized NextMsgBean findUnique(NextMsgBean bean) throws SandeshaException {
		Collection coll = find(bean);
		if (coll.size()>1) {
			String message = "Non-Unique result";
			log.error(message);
			throw new SandeshaException (message);
		}
		
		Iterator iter = coll.iterator();
		if (iter.hasNext())
			return (NextMsgBean) iter.next();
		else 
			return null;
	}
}