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
	 * Comment for <code>msgNo</code>
	 * The message number of the message.
	 */
	private long msgNo;

	/**
	 * Comment for <code>sequenceID</code>
	 * The sequence ID of the sequence the message belong to.
	 */
	private String sequenceID;
	
	/**
	 * Comment for <code>invoked</code>
	 * Weather the message has been invoked by the invoker.
	 */
	private boolean invoked = false;
	

	public InvokerBean() {

	}

	public InvokerBean(String key, long msgNo, String sequenceId) {
		this.messageContextRefKey = key;
		this.msgNo = msgNo;
		this.sequenceID = sequenceId;
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
	
	public boolean isInvoked() {
		return invoked;
	}
	
	public void setInvoked(boolean invoked) {
		this.invoked = invoked;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getName());
		result.append("\nSequence Id: "); result.append(sequenceID);
		result.append("\nMsg Number : "); result.append(msgNo);
		result.append("\nInvoked    : "); result.append(invoked);
		result.append("\nMessage Key: "); result.append(messageContextRefKey);
		return result.toString();
	}
}
