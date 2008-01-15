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

import java.util.List;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;

public class InMemoryInvokerBeanMgr extends InMemoryBeanMgr implements InvokerBeanMgr {
	
	private static final Log log = LogFactory.getLog(InMemoryInvokerBeanMgr.class);

	public InMemoryInvokerBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.STORAGE_MAP);
	}

	public boolean insert(InvokerBean bean) throws SandeshaStorageException {
		//first check that an invoker bean does not already exist with the same msg and seq numbers
		InvokerBean finder = new InvokerBean();
		finder.setMsgNo(bean.getMsgNo());
		finder.setSequenceID(bean.getSequenceID());
		synchronized(this){
			if(super.findUnique(finder)!=null){
				if(log.isDebugEnabled()) log.debug("InMemoryInvokerBeanMgr insert failed due to existing invoker bean");
				return false;
			}
			else{
				return super.insert(bean.getMessageContextRefKey(), bean);
			}
		}

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
	
	public boolean update(InvokerBean bean) throws SandeshaStorageException {
		return super.update(bean.getMessageContextRefKey(), bean);
	}
	
	public InvokerBean findUnique(InvokerBean bean) throws SandeshaException {
		return (InvokerBean) super.findUnique(bean);
	}

}
