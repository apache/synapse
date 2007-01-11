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

import java.util.List;

/**
 * This bean is used at the receiving side (of both server and client)
 * There is one entry for each sequence.
 */

public class RMDBean extends RMSequenceBean {
	
	private static final long serialVersionUID = -2976123838615087562L;

	/**
	 * Comment for <code>nextMsgNoToProcess</code>
	 * The next message to be invoked of the representing sequence.
	 */
	private long nextMsgNoToProcess;
		
	/**
	 * This will be used as a referenced 
	 */
	private String referenceMessageKey;
	
	private long highestInMessageNumber = 0;
	
	private String highestInMessageId;
	
	/** For incoming sequences this gives the msg no's of the messages that were
	 * received (may be an ack was sent - depending on the policy)
	 */
	private List serverCompletedMessages = null;

	public RMDBean() {

	}

	public RMDBean(String sequenceID, long nextNsgNo) {
		super(sequenceID);
		this.nextMsgNoToProcess = nextNsgNo;
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
  }

	public long getHighestInMessageNumber() {
  	return highestInMessageNumber;
  }

	public void setHighestInMessageNumber(long highestInMessageNumber) {
  	this.highestInMessageNumber = highestInMessageNumber;
  }
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getName());
		result.append(super.toString());
		result.append("\nNext Msg # : "); result.append(nextMsgNoToProcess);
		result.append("\nRef Msg Key: "); result.append(referenceMessageKey);
		result.append("\nHishestInMessageNumber: "); result.append(highestInMessageNumber);
		result.append("\nHishestInMessageKey: "); result.append(highestInMessageId);
		return result.toString();
	}

	public List getServerCompletedMessages() {
  	return serverCompletedMessages;
  }

	public void setServerCompletedMessages(List serverCompletedMessages) {
  	this.serverCompletedMessages = serverCompletedMessages;
  }

}
