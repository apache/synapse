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

import org.apache.sandesha2.util.RangeString;

/**
 * This bean is used at the receiving side (of both server and client)
 * There is one entry for each sequence.
 */

public class RMDBean extends RMSequenceBean {
	
	private static final long serialVersionUID = -2976123838615087562L;

	/**
	 * This will be used as a referenced 
	 */
	private String referenceMessageKey;
	
	private String highestInMessageId;

	/**
	 * Once an inbound sequence is closed, or we receive a message with the
	 * 'LastMessage' marker, we record the message id of the highest message
	 * in the sequence.
	 */
	private String lastInMessageId;

	/** 
	 * For incoming sequences this gives the msg ranges of the messages that
	 * have been received (and also possibly an ack was sent, depending on the policy)
	 */
	private RangeString serverCompletedMessages = null;
	
	/**
	 * For IN_ORDER sequences, we can have finite ranges of messages that can be
	 * delivered out of order. These are maintained as a RangeString
	 */
	private RangeString outOfOrderRanges = null;
	
	/**
	 * To Address of the messages that will be received for this sequence.
	 */
	private String toAddress;
	
	/**
	 * Client side, we keep track of inbound and outbound sequence pairs. Each
	 * inbound sequence has the identifier of the associated outbound sequence.
	 */
	private String outboundInternalSequence;

	/**
	 * Comment for <code>nextMsgNoToProcess</code>
	 * The next message to be invoked of the representing sequence.
	 */
	private long nextMsgNoToProcess;
		
	private long highestInMessageNumber = 0;
		
	/**
	 * Flags that are used to check if the primitive types on this bean
	 * have been set. If a primitive type has not been set then it will
	 * be ignored within the match method.
	 */
	private int rmdFlags = 0;
	private static final int NEXT_MSG_NO_FLAG    = 0x00000001;
	private static final int HIGHEST_IN_MSG_FLAG = 0x00000010;

	public RMDBean() {
	}

	/**
	 * Constructor that copies all RMDBean values from the RMDBean supplied
	 * @param beanToCopy
	 */
	public RMDBean(RMDBean beanToCopy) {
		super(beanToCopy);
		highestInMessageId = beanToCopy.getHighestInMessageId();
		highestInMessageNumber = beanToCopy.getHighestInMessageNumber();
		lastInMessageId = beanToCopy.getLastInMessageId();
		nextMsgNoToProcess = beanToCopy.getNextMsgNoToProcess();
		outboundInternalSequence = beanToCopy.getOutboundInternalSequence();
		outOfOrderRanges = beanToCopy.getOutOfOrderRanges();
		referenceMessageKey = beanToCopy.getReferenceMessageKey();
		serverCompletedMessages = new RangeString(beanToCopy.getServerCompletedMessages().toString());
		toAddress = beanToCopy.getToAddress();
	}

	public RMDBean(String sequenceID, long nextNsgNo) {
		super(sequenceID);
		this.setNextMsgNoToProcess(nextNsgNo);
	}

	/**
	 * @return Returns the nextMsgNoToProcess.
	 */
	public long getNextMsgNoToProcess() {
		return nextMsgNoToProcess;
	}

	/**
	 * @param nextMsgNoToProcess
	 *            The nextMsgNoToProcess to set.
	 */
	public void setNextMsgNoToProcess(long nextMsgNoToProcess) {
		this.nextMsgNoToProcess = nextMsgNoToProcess;
		this.rmdFlags |= NEXT_MSG_NO_FLAG;
	}

	public String getReferenceMessageKey() {
		return referenceMessageKey;
	}

	public void setReferenceMessageKey(String referenceMessageKey) {
		this.referenceMessageKey = referenceMessageKey;
	}
	
	public String getHighestInMessageId() {
  	return highestInMessageId;
  }

	public void setHighestInMessageId(String highestInMessageId) {
  	this.highestInMessageId = highestInMessageId;
  	this.rmdFlags |= HIGHEST_IN_MSG_FLAG;
  }

	public long getHighestInMessageNumber() {
  	return highestInMessageNumber;
  }

	public void setHighestInMessageNumber(long highestInMessageNumber) {
  	this.highestInMessageNumber = highestInMessageNumber;
  }

	public RangeString getServerCompletedMessages() {
  	return serverCompletedMessages;
  }

	public void setServerCompletedMessages(RangeString serverCompletedMessages) {
  	this.serverCompletedMessages = serverCompletedMessages;
  }

	public String getLastInMessageId() {
  	return lastInMessageId;
  }

	public void setLastInMessageId(String lastInMessageId) {
  	this.lastInMessageId = lastInMessageId;
  }

	public RangeString getOutOfOrderRanges() {
  	return outOfOrderRanges;
  }

	public void setOutOfOrderRanges(RangeString outOfOrderRanges) {
		this.outOfOrderRanges = outOfOrderRanges;
	}
	
	public String getToAddress() {
		return toAddress;
	}

	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}

	public String getOutboundInternalSequence() {
		return outboundInternalSequence;
	}

	public void setOutboundInternalSequence(String outboundSequence) {
		this.outboundInternalSequence = outboundSequence;
	}

	public int getRmdFlags() {
		return rmdFlags;
	}

	public void setRmdFlags(int rmdFlags) {
		this.rmdFlags = rmdFlags;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getName());
		result.append(super.toString());
		result.append("\nNext Msg # : "); result.append(nextMsgNoToProcess);
		result.append("\nRef Msg Key: "); result.append(referenceMessageKey);
		result.append("\nHishestInMessageNumber: "); result.append(highestInMessageNumber);
		result.append("\nHishestInMessageKey: "); result.append(highestInMessageId);
		result.append("\nLastInMessageId: "); result.append(lastInMessageId);
		result.append("\nOutOfOrderRanges   : "); result.append(outOfOrderRanges);
		result.append("\nServerCompletedMsgs: "); result.append(serverCompletedMessages);
		result.append("\nOutbound int seq   : "); result.append(outboundInternalSequence);
		return result.toString();
	}

	public boolean match(RMBean matchInfo) {
		RMDBean bean = (RMDBean) matchInfo;

		boolean equal = true;
		
		if(!super.match(matchInfo))
			equal = false;

		else if(bean.getReferenceMessageKey() != null && !bean.getReferenceMessageKey().equals(this.getReferenceMessageKey()))
			equal = false;
		
		else if(bean.getHighestInMessageId() != null && !bean.getHighestInMessageId().equals(this.getHighestInMessageId()))
			equal = false;
		
		else if(bean.getLastInMessageId() != null && !bean.getLastInMessageId().equals(this.getLastInMessageId()))
			equal = false;
		
		else if(bean.getServerCompletedMessages() != null && !bean.getServerCompletedMessages().equals(this.getServerCompletedMessages()))
			equal = false;
		
		else if(bean.getOutOfOrderRanges() != null && !bean.getOutOfOrderRanges().equals(this.getOutOfOrderRanges()))
			equal = false;

		else if(bean.getToAddress() != null && !bean.getToAddress().equals(this.getToAddress()))
			equal = false;
		
		else if(bean.getOutboundInternalSequence() != null && !bean.getOutboundInternalSequence().equals(this.getOutboundInternalSequence()))
			equal = false;
		
		else if ((bean.rmdFlags & NEXT_MSG_NO_FLAG) != 0 && bean.getNextMsgNoToProcess() != this.getNextMsgNoToProcess())
			equal = false;
		
		else if ((bean.rmdFlags & HIGHEST_IN_MSG_FLAG) != 0 && bean.getHighestInMessageNumber() != this.getHighestInMessageNumber())
			equal = false;

		return equal;
	}
}
