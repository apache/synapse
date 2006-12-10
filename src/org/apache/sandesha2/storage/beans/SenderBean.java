/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.storage.beans;

/**
 * This bean is used at the sending side (of both server and client)
 * There is one eatry for each message to be sent.
 */

public class SenderBean extends RMBean {

	private static final long serialVersionUID = 5776347847725156786L;

	/**
	 * Comment for <code>messageID</code>
	 * The message id of the representing message.
	 * 
	 */
	private String messageID;

	/**
	 * Comment for <code>messageContextRefKey</code>
	 * Key retrieved by the storage mechanism after storing the message.
	 */
	private String messageContextRefKey;

	/**
	 * Comment for <code>send</code>
	 * The sender will not send the message unless this property is true.
	 */
	private boolean send;

	/**
	 * Comment for <code>internalSequenceID</code>
	 * Please see the comment of RMSBean.
	 */
	private String internalSequenceID;

	/**
	 * Comment for <code>sentCount</code>
	 * The number of times current message has been sent.
	 */
	private int sentCount = 0;

	/**
	 * Comment for <code>messageNumber</code>
	 * The message number of the current message.
	 */
	private long messageNumber = 0;

	/**
	 * Comment for <code>reSend</code>
	 * If this property if false. The message has to be sent only once. The entry has to be deleted after sending.
	 */
	private boolean reSend = true;

	/**
	 * Comment for <code>timeToSend</code>
	 * Message has to be sent only after this time.
	 */
	private long timeToSend = 0;
	
	/**
	 * Comment for <code>messageType</code>
	 * The type of the current message.
	 * Possible types are given in Sandesha2Constants.MessageTypes interface.
	 */
	private int messageType =0;
	
	/**
	 * The sequenceID of the sequence this message belong to.
	 * this may be null for some messages (e.g. create sequence);
	 */
	//TODO fill this property correctly
	private String sequenceID;
	
	/**
	 * TODO use the value in CreateSequenceBean.
	 */
	private String wsrmAnonURI;
	
	/**
	 * Destination URL of the message to be sent. This can be used to decide weather the message cannot be sent,
	 * before actyally reading the message from the storage.
	 */
	private String toAddress;
	
	public SenderBean() {

	}

	public SenderBean(String messageID, String key,
			boolean send,long timeToSend, String internalSequenceID, long messageNumber) {
		this.messageID = messageID;
		this.messageContextRefKey = key;
		//this.LastSentTime = lastSentTime;
		this.timeToSend = timeToSend;
		this.send = send;
		this.internalSequenceID = internalSequenceID;
		this.messageNumber = messageNumber;
	}

	public String getMessageContextRefKey() {
		return messageContextRefKey;
	}

	public void setMessageContextRefKey(String messageContextRefKey) {
		this.messageContextRefKey = messageContextRefKey;
	}

	public String getMessageID() {
		return messageID;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

	public boolean isSend() {
		return send;
	}

	public void setSend(boolean send) {
		this.send = send;
	}

	public String getInternalSequenceID() {
		return internalSequenceID;
	}

	public void setInternalSequenceID(String internalSequenceId) {
		this.internalSequenceID = internalSequenceId;
	}

	public int getSentCount() {
		return sentCount;
	}

	public void setSentCount(int sentCount) {
		this.sentCount = sentCount;
	}

	public long getMessageNumber() {
		return messageNumber;
	}

	public void setMessageNumber(long messageNumber) {
		this.messageNumber = messageNumber;
	}

	public boolean isReSend() {
		return reSend;
	}

	public void setReSend(boolean reSend) {
		this.reSend = reSend;
	}
	
	public long getTimeToSend() {
		return timeToSend;
	}
	
	public void setTimeToSend(long timeToSend) {
		this.timeToSend = timeToSend;
	}
	
	
	public int getMessageType() {
		return messageType;
	}
	
	public void setMessageType(int messagetype) {
		this.messageType = messagetype;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public void setSequenceID(String sequenceID) {
		this.sequenceID = sequenceID;
	}

	public String getWsrmAnonURI() {
		return wsrmAnonURI;
	}

	public void setWsrmAnonURI(String wsrmAnonURI) {
		this.wsrmAnonURI = wsrmAnonURI;
	}

	public String getToAddress() {
		return toAddress;
	}

	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		// There is a lot of data in this bean, so we don't trace it all.
		result.append(this.getClass().getName());
		result.append("\nSequence Id    : "); result.append(sequenceID);
		result.append("\nInternal Seq Id: "); result.append(internalSequenceID);
		result.append("\nMessage Number : "); result.append(messageNumber);
		result.append("\nMessage Type   : "); result.append(messageType);
		result.append("\nMessage Key    : "); result.append(messageContextRefKey);
		result.append("\nSend           : "); result.append(send);
		result.append("\nResend         : "); result.append(reSend);
		result.append("\nSent count     : "); result.append(sentCount);
		result.append("\nTime to send   : "); result.append(timeToSend);
		return result.toString();
	}
}
