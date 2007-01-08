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
 * There is on object of this for each sequence.
 */

public class RMSBean extends RMSequenceBean {
	
	private static final long serialVersionUID = 7051201094510208784L;

	/**
	 * Comment for <code>internalSequenceID</code>
	 * This property is a unique identifier that can be used to identify the messages of a certain sequence.
	 * This is specially used by the sending side, since sequence id is not available in the begining.
	 * For the client side, indernal sequence id is a concantination of wsa:To and SEQUENCE_KEY (SEQUENCE_KEY can be set as a property).
	 * For the server side, this is the sequenceId of the incoming sequence.
	 */
	private String internalSequenceID;

	/**
	 * Comment for <code>createSeqMsgID</code>
	 * This is the message ID of the create sequence message.
	 */
	private String createSeqMsgID;
	
	/**
	 * Comment for <code>securityTokenData</code>
	 * This is the security token data needed to reconstruct the token that secures this sequence.
	 */
	private String securityTokenData;
	

	/**
	 * Set to true if this RMS used a WS-RM anon replyTo, so that we can poll for messages
	 * related to that URI.
	 */
	private boolean pollingMode;

	
	/**
	 * The key that is used to store the CreateSequence message in the Message Storage.
	 * This is stored here, so that the CreateSequence message can be used as a reference when Sandesha
	 * want the generate new messages. (e.g. MakeConnection)
	 */
	private String createSequenceMsgStoreKey;
	
	/**
	 * This is stored here, so that the message pointed by this can be used as a reference when Sandesha
	 * want the generate new messages. (e.g. MakeConnection). Create sequence message could not be used 
	 * here since it may be subjected to things like encryption.
	 */
	private String referenceMessageStoreKey;
	
	/** 
	 * This is the last error that was encountered when sending a message
	 */
	private Exception lastSendError = null;
	
	/**
	 * This is the timestamp of when the last error occured when sending
	 */
	private long lastSendErrorTimestamp = -1;
		
	/**
	 * The last Out message number
	 */
	private long lastOutMessage = 0;
	
	/**
	 * The Highest out message number
	 * Keeps track of the highest transmitted message
	 */
	private long highestOutMessageNumber = 0;
	
	/**
	 * The highest out message relates to message id	 
	 * Keeps track of the highest transmitted message
	 */
  private String highestOutRelatesTo = null;
	
	public RMSBean() {
	}


	public String getCreateSeqMsgID() {
		return createSeqMsgID;
	}

	public void setCreateSeqMsgID(String createSeqMsgID) {
		this.createSeqMsgID = createSeqMsgID;
	}

	public String getInternalSequenceID() {
		return internalSequenceID;
	}

	public void setInternalSequenceID(String internalSequenceID) {
		this.internalSequenceID = internalSequenceID;
	}

	public String getSecurityTokenData() {
		return securityTokenData;
	}

	public void setSecurityTokenData(String securityTokenData) {
		this.securityTokenData = securityTokenData;
	}

	public String getCreateSequenceMsgStoreKey() {
		return createSequenceMsgStoreKey;
	}

	public void setCreateSequenceMsgStoreKey(String createSequenceMsgStoreKey) {
		this.createSequenceMsgStoreKey = createSequenceMsgStoreKey;
	}


	public String getReferenceMessageStoreKey() {
		return referenceMessageStoreKey;
	}

	public void setReferenceMessageStoreKey(String referenceMessageStoreKey) {
		this.referenceMessageStoreKey = referenceMessageStoreKey;
	}
	
	public boolean isPollingMode() {
		return pollingMode;
	}

	public void setPollingMode(boolean pollingMode) {
		this.pollingMode = pollingMode;
	}

	public Exception getLastSendError() {
  	return lastSendError;
  }

	public void setLastSendError(Exception lastSendError) {
  	this.lastSendError = lastSendError;
  }


	public long getLastSendErrorTimestamp() {
  	return lastSendErrorTimestamp;
  }


	public void setLastSendErrorTimestamp(long lastSendErrorTimestamp) {
  	this.lastSendErrorTimestamp = lastSendErrorTimestamp;
  }

	
	public long getLastOutMessage() {
  	return lastOutMessage;
  }

	public void setLastOutMessage(long lastOutMessage) {
  	this.lastOutMessage = lastOutMessage;
  }

	public long getHighestOutMessageNumber() {
  	return highestOutMessageNumber;
  }


	public void setHighestOutMessageNumber(long highestOutMessageNumber) {
  	this.highestOutMessageNumber = highestOutMessageNumber;
  }


	public String getHighestOutRelatesTo() {
  	return highestOutRelatesTo;
  }


	public void setHighestOutRelatesTo(String highestOutRelatesTo) {
  	this.highestOutRelatesTo = highestOutRelatesTo;
  }

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getName());
		result.append(super.toString());
		result.append("\nInternal Seq Id  : "); result.append(internalSequenceID);
		result.append("\nCreateSeq Msg Id : "); result.append(createSeqMsgID);
		result.append("\nHas SecurityToken: "); result.append(securityTokenData != null && securityTokenData.length() > 0);
		result.append("\nCreateSeq Msg Key: "); result.append(createSequenceMsgStoreKey);
		result.append("\nReference Msg Key: "); result.append(referenceMessageStoreKey);
		result.append("\nPolling          : "); result.append(pollingMode);
		result.append("\nLastOutMessageNumber: "); result.append(lastOutMessage);
		result.append("\nHighestOutMessage: "); result.append(highestOutMessageNumber);
		result.append("\nHighestOutRelatesTo: ");result.append(highestOutRelatesTo);
		return result.toString();
	}

}
