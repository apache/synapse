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

import java.util.List;

import org.apache.axis2.context.AbstractContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.storage.beans.RMBean;

public class InMemoryInvokerBeanMgr extends InMemoryBeanMgr implements InvokerBeanMgr {

	public InMemoryInvokerBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.STORAGE_MAP);
	}

	public boolean insert(InvokerBean bean) throws SandeshaStorageException {
		boolean result = super.insert(bean.getMessageContextRefKey(), bean);
		mgr.getInMemoryTransaction().setReceivedMessages(true);
		return result;
	}

	public boolean delete(String key) throws SandeshaStorageException {
		return super.delete(key);
	}

	public InvokerBean retrieve(String key) throws SandeshaStorageException {
		return (InvokerBean) super.retrieve(key);
	}

	public List find(InvokerBean bean) throws SandeshaStorageException {
		return super.find(bean);
	}
	
	protected boolean match(RMBean matchInfo, RMBean candidate) {
		InvokerBean bean = (InvokerBean) matchInfo;
		InvokerBean temp = (InvokerBean) candidate;

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
		
		return select;
	}

	public boolean update(InvokerBean bean) throws SandeshaStorageException {
		boolean result = super.update(bean.getMessageContextRefKey(), bean);
		mgr.getInMemoryTransaction().setReceivedMessages(true);
		return result;
	}
	
	public InvokerBean findUnique(InvokerBean bean) throws SandeshaException {
		return (InvokerBean) super.findUnique(bean);
	}

}
