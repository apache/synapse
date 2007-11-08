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
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;

public class InMemoryRMSBeanMgr extends InMemoryBeanMgr implements RMSBeanMgr {

	public InMemoryRMSBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.CREATE_SEQUECE);
	}

	public boolean insert(RMSBean bean) throws SandeshaStorageException {
		return super.insert(bean.getCreateSeqMsgID(), bean);
	}

	public boolean delete(String msgId) throws SandeshaStorageException {
		return super.delete(msgId);
	}

	public RMSBean retrieve(String msgId) throws SandeshaStorageException {
		return (RMSBean) super.retrieve(msgId);
	}

	public boolean update(RMSBean bean) throws SandeshaStorageException {
		return super.update(bean.getCreateSeqMsgID(), bean);
	}

	public List find(RMSBean bean) throws SandeshaStorageException {
		return super.find(bean);
	}
	
	public RMSBean findUnique (RMSBean bean) throws SandeshaException {
		return (RMSBean) super.findUnique(bean);
	}

}
