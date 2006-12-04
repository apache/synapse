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

import java.util.Collection;
import java.util.List;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.SequencePropertyBeanMgr;
import org.apache.sandesha2.storage.beans.RMBean;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;

public class InMemorySequencePropertyBeanMgr extends InMemoryBeanMgr implements SequencePropertyBeanMgr {
	
	private static final Log log = LogFactory.getLog(InMemorySequencePropertyBeanMgr.class);

	public InMemorySequencePropertyBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.SEQUENCE_PROPERTY);
	}

	public boolean delete(String sequenceId, String name) throws SandeshaStorageException {
		return super.delete(getId(sequenceId, name));
	}

	public SequencePropertyBean retrieve(String sequenceId, String name) throws SandeshaStorageException {
		return (SequencePropertyBean) super.retrieve(getId(sequenceId, name));
	}

	public boolean insert(SequencePropertyBean bean) throws SandeshaStorageException {
		return super.insert(getId(bean), bean);
	}

	public List find(SequencePropertyBean bean) throws SandeshaStorageException {
		return super.find(bean);
	}
	
	protected boolean match(RMBean matchInfo, RMBean candidate) {
		SequencePropertyBean bean = (SequencePropertyBean) matchInfo;
		SequencePropertyBean temp = (SequencePropertyBean) candidate;

		boolean equal = true;

		if (bean.getSequencePropertyKey() != null
				&& !bean.getSequencePropertyKey().equals(temp.getSequencePropertyKey()))
			equal = false;

		if (bean.getName() != null
				&& !bean.getName().equals(temp.getName()))
			equal = false;

		if (bean.getValue() != null
				&& !bean.getValue().equals(temp.getValue()))
			equal = false;

		return equal;
	}

	public boolean update(SequencePropertyBean bean) throws SandeshaStorageException {	
		return super.update(getId(bean), bean);
	}
	
	public boolean updateOrInsert(SequencePropertyBean bean) {	
		throw new UnsupportedOperationException("Deprecated method");
	}

	private String getId(SequencePropertyBean bean) {
		return bean.getSequencePropertyKey() + ":" + bean.getName();
	}
	private String getId(String sequenceId, String name) {
		return sequenceId + ":" + name;
	}
	
	public SequencePropertyBean findUnique(SequencePropertyBean bean) throws SandeshaException {
		return (SequencePropertyBean) super.findUnique(bean);
	}

	public Collection retrieveAll() throws SandeshaStorageException {
		return super.find(null);
	}
	
}
