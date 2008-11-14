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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.axis2.context.AbstractContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;

public class InMemoryRMSBeanMgr extends InMemoryBeanMgr<RMSBean> implements RMSBeanMgr {

	private Lock lock = new ReentrantLock();
	
	private ConcurrentHashMap<String, String> seqID2csm = new ConcurrentHashMap<String, String>();
	private ConcurrentHashMap<String, String> intSeqID2csm = new ConcurrentHashMap<String, String>();
	private ConcurrentHashMap<String, String> inUseSeqIDs = new ConcurrentHashMap<String, String>();

	public InMemoryRMSBeanMgr(InMemoryStorageManager mgr, AbstractContext context) {
		super(mgr, context, Sandesha2Constants.BeanMAPs.CREATE_SEQUECE);
	}
	
	private boolean isSeqIDUsable(String seqID, String createSeqMsgID, boolean isInsert){
		boolean isUsable = true;
		if(seqID != null) {
			Object o = inUseSeqIDs.putIfAbsent(seqID, createSeqMsgID);
			
			if(isInsert && o!= null){
				isUsable = false;
			}
			
			if(o != null && !o.equals(createSeqMsgID)){
				isUsable = false;
			}
		}
		return isUsable;
	}

	public boolean insert(RMSBean bean) throws SandeshaStorageException {
		boolean res = false;
		lock.lock();
		if(isSeqIDUsable(bean.getSequenceID(), bean.getCreateSeqMsgID(), true)){
			if(intSeqID2csm.get(bean.getInternalSequenceID())==null){
				res = super.insert(bean.getCreateSeqMsgID(), bean);
				if(res){
					if(bean.getInternalSequenceID()!=null){
						intSeqID2csm.put(bean.getInternalSequenceID(), bean.getCreateSeqMsgID());
					}
					if(bean.getSequenceID()!=null){
						seqID2csm.put(bean.getSequenceID(), bean.getCreateSeqMsgID());
					}
				}
			}			
		} 

		lock.unlock();

		return res;
	}

	public boolean delete(String msgId) throws SandeshaStorageException {
		RMSBean removed = (RMSBean) super.delete(msgId);
		if(removed!=null){
			seqID2csm.remove(removed.getSequenceID());
			intSeqID2csm.remove(removed.getInternalSequenceID());			
			inUseSeqIDs.remove(removed.getSequenceID());
		}
		return removed!=null;

	}

	public RMSBean retrieve(String msgId) throws SandeshaStorageException {
		return (RMSBean) super.retrieve(msgId);
	}

	public boolean update(RMSBean bean) throws SandeshaStorageException {
		boolean result = false;
		
		if(isSeqIDUsable(bean.getSequenceID(), bean.getCreateSeqMsgID(), false)){
			result = super.update(bean.getCreateSeqMsgID(), bean);
			if(bean.getInternalSequenceID()!=null){
				intSeqID2csm.put(bean.getInternalSequenceID(), bean.getCreateSeqMsgID());
			}
			if(bean.getSequenceID()!=null){
				seqID2csm.put(bean.getSequenceID(), bean.getCreateSeqMsgID());
			}
		} 

		return result;		
	}

	public List<RMSBean> find(RMSBean bean) throws SandeshaStorageException {
		return super.find(bean);
	}
	
	public RMSBean findUnique (RMSBean bean) throws SandeshaStorageException {
		return (RMSBean) super.findUnique(bean);
	}
	
	public RMSBean retrieveBySequenceID(String seqId) throws SandeshaStorageException {
			String csid = (String) seqID2csm.get(seqId);
			RMSBean bean = null;
			if(csid!=null){
				bean = retrieve(csid);
			}
			if(bean == null){
				RMSBean finder = new RMSBean();
				finder.setSequenceID(seqId);
				bean = findUnique(finder);
			}
			return bean;
		}
	
	public RMSBean retrieveByInternalSequenceID(String internalSeqId) throws SandeshaStorageException {
			String csid = (String) intSeqID2csm.get(internalSeqId);
			RMSBean bean = null;
			if(csid!=null){
				bean = retrieve(csid);
			}
			if(bean == null){
				RMSBean finder = new RMSBean();
				finder.setInternalSequenceID(internalSeqId);
				bean = findUnique(finder);
			}
			return bean;
		}

}
