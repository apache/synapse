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
import java.util.List;

import org.apache.axis2.context.AbstractContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.beanmanagers.NextMsgBeanMgr;
import org.apache.sandesha2.storage.beans.NextMsgBean;
import org.apache.sandesha2.storage.beans.RMBean;

public class InMemoryNextMsgBeanMgr extends InMemoryBeanMgr implements NextMsgBeanMgr {

	private static final Log log = LogFactory.getLog(InMemoryNextMsgBeanMgr.class);

	public InMemoryNextMsgBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.NEXT_MESSAGE);
	}

	public boolean delete(String sequenceId) {
		return super.delete(sequenceId);
	}

	public NextMsgBean retrieve(String sequenceId) {
		return (NextMsgBean) super.retrieve(sequenceId);
	}

	public boolean insert(NextMsgBean bean) {
		return super.insert(bean.getSequenceID(), bean);
	}

	public List find(NextMsgBean bean) {
		return super.find(bean);
	}
	
	protected boolean match(RMBean matchInfo, RMBean candidate) {
		NextMsgBean bean = (NextMsgBean) matchInfo;
		NextMsgBean temp = (NextMsgBean) candidate;

		boolean equal = true;

		if (bean.getNextMsgNoToProcess() > 0
				&& bean.getNextMsgNoToProcess() != temp
						.getNextMsgNoToProcess())
			equal = false;

		if (bean.getSequenceID() != null
				&& !bean.getSequenceID().equals(temp.getSequenceID()))
			equal = false;

		return equal;
	}

	public boolean update(NextMsgBean bean) {
		return super.update(bean.getSequenceID(), bean);
	}

	public Collection retrieveAll() {
		return super.find(null);
	}
	
	public NextMsgBean findUnique(NextMsgBean bean) throws SandeshaException {
		return (NextMsgBean) super.findUnique(bean);
	}
}
