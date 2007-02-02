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

import org.apache.sandesha2.util.RangeString;

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
	 * The highest out message relates to message id	 
	 * Keeps track of the highest transmitted message
	 */
	private String highestOutRelatesTo = null;
  
	/** 
	 * For out going sequences this gives the message ranges that were sent and that were successfully
	 * acked by the other end point.
	 */
	private RangeString clientCompletedMessages = null;
  
	private String transportTo;

	private String offeredEndPoint = null;

	private String offeredSequence = null;
	
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
	 * The next sequence number to apply to the message
	 */
	private long nextMessageNumber = -1;
  
	/**
	 * Indicates that a terminate sequence message was added.
	 */
	private boolean terminateAdded = false;
  
	/**
	 * Indicates that a sequence has timed out.
	 */
	private boolean timedOut = false;
  
	/**
	 * Indicates the client has sent a close sequence
	 */
	private boolean sequenceClosedClient = false;

	/**
	 * The number of messages that were acked
	 */
	private long numberOfMessagesAcked = 0;

	/**
	 * Flags that are used to check if the primitive types on this bean
	 * have been set. If a primitive type has not been set then it will
	 * be ignored within the match method.
	 */
	private int rmsFlags = 0;
	private static final int LAST_SEND_ERROR_TIME_FLAG = 0x00000001;
	private static final int LAST_OUT_MSG_FLAG         = 0x00000010;
	private static final int HIGHEST_OUT_MSG_FLAG      = 0x00000100;
	private static final int NEXT_MSG_NUM_FLAG         = 0x00001000;
	private static final int TERMINATE_ADDED_FLAG      = 0x00010000;
	private static final int TIMED_OUT_FLAG            = 0x00100000;
	private static final int SEQ_CLOSED_CLIENT_FLAG    = 0x01000000;
	private static final int ACKED_MESSAGES_FLAG       = 0x10000000;

  /**
   * In WSRM Anon URI scenario, we may not want to terminate a perticular sequence until the CreateSequence has been received
   * for the response side, other wise PollingManager will pause the polling process in termination and we will never be able
   * to get the CS.
   */
  private boolean terminationPauserForCS = false;
  
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
  	this.rmsFlags |= LAST_SEND_ERROR_TIME_FLAG;
  }
	
	public long getLastOutMessage() {
  	return lastOutMessage;
  }

	public void setLastOutMessage(long lastOutMessage) {
  	this.lastOutMessage = lastOutMessage;
  	this.rmsFlags |= LAST_OUT_MSG_FLAG;
  }

	public long getHighestOutMessageNumber() {
  	return highestOutMessageNumber;
  }

	public void setHighestOutMessageNumber(long highestOutMessageNumber) {
  	this.highestOutMessageNumber = highestOutMessageNumber;
  	rmsFlags |= HIGHEST_OUT_MSG_FLAG;
  }

	public String getHighestOutRelatesTo() {
  	return highestOutRelatesTo;
  }

	public void setHighestOutRelatesTo(String highestOutRelatesTo) {
  	this.highestOutRelatesTo = highestOutRelatesTo;
  }

	public long getNextMessageNumber() {
  	return nextMessageNumber;
  }

	public void setNextMessageNumber(long nextMessageNumber) {
  	this.nextMessageNumber = nextMessageNumber;
  	rmsFlags |= NEXT_MSG_NUM_FLAG;
  }

	public RangeString getClientCompletedMessages() {
  	return clientCompletedMessages;
  }

	public void setClientCompletedMessages(RangeString clientCompletedMessages) {
  	this.clientCompletedMessages = clientCompletedMessages;
  }

	public boolean isTerminateAdded() {
  	return terminateAdded;
  }

	public void setTerminateAdded(boolean terminateAdded) {
  	this.terminateAdded = terminateAdded;
  	this.rmsFlags |= TERMINATE_ADDED_FLAG;
  }

	public boolean isTimedOut() {
  	return timedOut;
  }

	public void setTimedOut(boolean timedOut) {
  	this.timedOut = timedOut;
  	this.rmsFlags |= TIMED_OUT_FLAG;
  }

	public boolean isSequenceClosedClient() {
  	return sequenceClosedClient;
  }

	public void setSequenceClosedClient(boolean sequenceClosedClient) {
  	this.sequenceClosedClient = sequenceClosedClient;
  	this.rmsFlags |= SEQ_CLOSED_CLIENT_FLAG;
  }
	
	public long getNumberOfMessagesAcked() {
  	return numberOfMessagesAcked;
  }

	public void setNumberOfMessagesAcked(long numberOfMessagesAcked) {
  	this.numberOfMessagesAcked = numberOfMessagesAcked;
  	this.rmsFlags |= ACKED_MESSAGES_FLAG;
  }
	
	public String getTransportTo() {
  	return transportTo;
  }

	public void setTransportTo(String transportTo) {
  	this.transportTo = transportTo;
  }

	public String getOfferedEndPoint() {
  	return offeredEndPoint;
  }

	public void setOfferedEndPoint(String offeredEndPoint) {
  	this.offeredEndPoint = offeredEndPoint;
  }
	
	public String getOfferedSequence() {
  	return offeredSequence;
  }

	public void setOfferedSequence(String offeredSequence) {
  	this.offeredSequence = offeredSequence;
  }
	
	public boolean isTerminationPauserForCS() {
		return terminationPauserForCS;
	}

	public void setTerminationPauserForCS(boolean terminationPauserForCS) {
		this.terminationPauserForCS = terminationPauserForCS;
	}


	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getClass().getName());
		result.append(super.toString());
		result.append("\nInternal Seq Id  : "); result.append(internalSequenceID);
		result.append("\nCreateSeq Msg Id : "); result.append(createSeqMsgID);
		result.append("\nCreateSeq Msg Key: "); result.append(createSequenceMsgStoreKey);
		result.append("\nReference Msg Key: "); result.append(referenceMessageStoreKey);
		result.append("\nLastOutMessageNumber: "); result.append(lastOutMessage);
		result.append("\nHighestOutMessage: "); result.append(highestOutMessageNumber);
		result.append("\nHighestOutRelatesTo: ");result.append(highestOutRelatesTo);
		result.append("\nNextMessageNumber: "); result.append(nextMessageNumber);
		result.append("\nTerminateAdded   : "); result.append(terminateAdded);
		result.append("\nTimedOut         : "); result.append(timedOut);
		result.append("\nClosedClient     : "); result.append(sequenceClosedClient);
		result.append("\nNumAckedMsgs     : "); result.append(numberOfMessagesAcked);
		result.append("\nTransportTo      : "); result.append(transportTo);
		result.append("\nOfferedEndPoint  : "); result.append(offeredEndPoint);
		result.append("\nOfferedSequence  : "); result.append(offeredSequence);
		if (lastSendErrorTimestamp > 0) {
			result.append("\nLastError        : "); result.append(lastSendError);
			result.append("\nLastErrorTime    : "); result.append(lastSendErrorTimestamp);
		}
		result.append("\nClientCompletedMsgs: "); result.append(clientCompletedMessages);
		return result.toString();
	}
	
	public boolean match(RMBean matchInfo) {
		RMSBean bean = (RMSBean) matchInfo;
		boolean match = true;
		
		if(!super.match(matchInfo))
			match = false;
		
		else if(bean.getInternalSequenceID() != null && !bean.getInternalSequenceID().equals(this.getInternalSequenceID()))
			match = false;
		
		else if(bean.getCreateSeqMsgID() != null && !bean.getCreateSeqMsgID().equals(this.getCreateSeqMsgID()))
			match = false;
		
		else if(bean.getCreateSequenceMsgStoreKey() != null && !bean.getCreateSequenceMsgStoreKey().equals(this.getCreateSequenceMsgStoreKey()))
			match = false;
		
		else if(bean.getReferenceMessageStoreKey() != null && !bean.getReferenceMessageStoreKey().equals(this.getReferenceMessageStoreKey()))
			match = false;

// Avoid matching on the error information
//		else if(bean.getLastSendError() != null && !bean.getLastSendError().equals(this.getLastSendError()))
//			match = false;
		
		else if(bean.getHighestOutRelatesTo() != null && !bean.getHighestOutRelatesTo().equals(this.getHighestOutRelatesTo()))
			match = false;
		
		else if(bean.getClientCompletedMessages() != null && !bean.getClientCompletedMessages().equals(this.getClientCompletedMessages()))
			match = false;

		else if(bean.getTransportTo() != null && !bean.getTransportTo().equals(this.getTransportTo()))
			match = false;

		else if(bean.getOfferedEndPoint() != null && !bean.getOfferedEndPoint().equals(this.getOfferedEndPoint()))
			match = false;

		else if(bean.getOfferedSequence() != null && !bean.getOfferedSequence().equals(this.getOfferedSequence()))
			match = false;

// Avoid matching on the error information
//		else if((bean.rmsFlags & LAST_SEND_ERROR_TIME_FLAG) != 0 && bean.getLastSendErrorTimestamp() != this.getLastSendErrorTimestamp())
//			match = false;
		
		else if((bean.rmsFlags & LAST_OUT_MSG_FLAG) != 0 && bean.getLastOutMessage() != this.getLastOutMessage())
			match = false;
		
		else if((bean.rmsFlags & HIGHEST_OUT_MSG_FLAG) != 0 && bean.getHighestOutMessageNumber() != this.getHighestOutMessageNumber())
			match = false;
		
		else if((bean.rmsFlags & NEXT_MSG_NUM_FLAG) != 0 && bean.getNextMessageNumber() != this.getNextMessageNumber())
			match = false;
		
		else if((bean.rmsFlags & TERMINATE_ADDED_FLAG) != 0 && bean.isTerminateAdded() != this.isTerminateAdded())
			match = false;
		
		else if((bean.rmsFlags & TIMED_OUT_FLAG) != 0 && bean.isTimedOut() != this.isTimedOut())
			match = false;
		
		else if((bean.rmsFlags & SEQ_CLOSED_CLIENT_FLAG) != 0 && bean.isSequenceClosedClient() != this.isSequenceClosedClient())
			match = false;
		
		else if((bean.rmsFlags & ACKED_MESSAGES_FLAG) != 0 && bean.getNumberOfMessagesAcked() != this.getNumberOfMessagesAcked())
			match = false;
		
		return match;
	}
}
