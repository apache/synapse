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

import java.util.Collection;
import java.util.List;

import org.apache.axis2.context.AbstractContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;

public class InMemoryRMDBeanMgr extends InMemoryBeanMgr<RMDBean> implements RMDBeanMgr {

	public InMemoryRMDBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.NEXT_MESSAGE);
	}

	public boolean delete(String sequenceId) throws SandeshaStorageException {
		return (super.delete(sequenceId)!=null);

	}

	public RMDBean retrieve(String sequenceId) throws SandeshaStorageException {
		return (RMDBean) super.retrieve(sequenceId);
	}

	public boolean insert(RMDBean bean) throws SandeshaStorageException {
		return super.insert(bean.getSequenceID(), bean);
	}

	public List<RMDBean> find(RMDBean bean) throws SandeshaStorageException {
		return super.find(bean);
	}
	
	public boolean update(RMDBean bean) throws SandeshaStorageException {
		return super.update(bean.getSequenceID(), bean);
	}

	public Collection<RMDBean> retrieveAll() throws SandeshaStorageException {
		return super.find(null);
	}
	
	public RMDBean findUnique(RMDBean bean) throws SandeshaStorageException {
		return (RMDBean) super.findUnique(bean);
	}
}
