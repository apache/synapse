/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.storage.beans;

import java.io.Serializable;

/**
 * This bean is used at the receiving side (of both server and client)
 * There is one object for each application message to be invoked.
 */

public class InvokerBean extends RMBean {

	private static final long serialVersionUID = -7839397509697276257L;

	/**
	 * Comment for <code>messageContextRefKey</code>
	 * 
	 * This is the messageContextRefKey that is obtained after saving a message context in a storage.
	 */
	private String messageContextRefKey;

	/**
	 * Comment for <code>sequenceID</code>
	 * The sequence ID of the sequence the message belong to.
	 */
	private String sequenceID;
	
	/**
	 * The runtime context that needs to be applied to the invoker thread before
	 * the message is run through the invoker.
	 */
	private Serializable context;
	
	/**
	 * Comment for <code>msgNo</code>
	 * The message number of the message.
	 */
	private long msgNo;

	/**
	 * Flags that are used to check if the primitive types on this bean
	 * have been set. If a primitive type has not been set then it will
	 * be ignored within the match method.
	 */
	private int flags = 0;
	private static final int MSG_NO_FLAG  = 0x00000001;	
	
	public InvokerBean() {

	}

	public InvokerBean(String key, long msgNo, String sequenceId) {
		this.setMessageContextRefKey(key);
		this.setMsgNo(msgNo);
		this.setSequenceID(sequenceId);
	}

	public InvokerBean(InvokerBean beanToCopy) {
		context = beanToCopy.getContext();
		messageContextRefKey = beanToCopy.getMessageContextRefKey();
		msgNo = beanToCopy.getMsgNo();
		sequenceID = beanToCopy.getSequenceID();
	}
	
	/**
	 * @return Returns the messageContextRefKey.
	 */
	public String getMessageContextRefKey() {
		return messageContextRefKey;
	}

	/**
	 * @param messageContextRefKey
	 *            The messageContextRefKey to set.
	 */
	public void setMessageContextRefKey(String messageContextRefKey) {
		this.messageContextRefKey = messageContextRefKey;
	}

	/**
	 * @return Returns the msgNo.
	 */
	public long getMsgNo() {
		return msgNo;
	}

	/**
	 * @param msgNo
	 *            The msgNo to set.
	 */
	public void setMsgNo(long msgNo) {
		this.msgNo = msgNo;
		this.flags |= MSG_NO_FLAG;
	}

	/**
	 * @return Returns the sequenceID.
	 */
	public String getSequenceID() {
		return sequenceID;
	}

	/**
	 * @param sequenceID
	 *            The sequenceID to set.
	 */
	public void setSequenceID(String sequenceId) {
		this.sequenceID = sequenceId;
	}
	
	public Serializable getContext() {
		return context;
	}

	public void setContext(Serializable context) {
		this.context = context;
	}
	
	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getName());
		result.append("\nSequence Id: "); result.append(sequenceID);
		result.append("\nMsg Number : "); result.append(msgNo);
		result.append("\nMessage Key: "); result.append(messageContextRefKey);
		return result.toString();
	}
	
	public boolean match(RMBean matchInfo) {
		InvokerBean bean = (InvokerBean) matchInfo;
		boolean select = true;

		if (bean.getMessageContextRefKey() != null && !bean.getMessageContextRefKey().equals(this.getMessageContextRefKey()))
			select = false;

		else if (bean.getSequenceID() != null && !bean.getSequenceID().equals(this.getSequenceID()))
			select = false;

		else if ((bean.flags & MSG_NO_FLAG) != 0 && bean.getMsgNo() != this.getMsgNo())
			select = false;
		
		return select;
	}

}
