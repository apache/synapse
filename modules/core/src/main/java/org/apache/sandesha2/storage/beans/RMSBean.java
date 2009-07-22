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

package org.apache.sandesha2.storage.beans;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.util.Range;
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
	private EndpointReference offeredEndPointEPR = null;

	private String offeredSequence = null;
	
	private String anonymousUUID = null;
	
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
	 * The number of reply messages that we expect
	 */
	private long expectedReplies = 0;
	
	/**
	 * When sending a RM Protocol message from SandeshaClient if there isn't
	 * a SOAP version specified in the Options, this version will be used.
	 * .NET interop requires all messages to be sent with the same SOAP version.
	 */
	private int soapVersion;

	/**
	 * In WSRM Anon URI scenario, we may not want to terminate a perticular sequence until the CreateSequence has been received
	 * for the response side, other wise PollingManager will pause the polling process in termination and we will never be able
	 * to get the CS.
	 */
	private boolean terminationPauserForCS = false;
  	/**
	 * If this is set, the current sequence is not expected to auto terminate when all the acks are received.
	 * I.e. the user explicitly have to call for termination (using SandeshaClient).  
	 */
	private boolean avoidAutoTermination = false;

	/**
	 * Flags that are used to check if the primitive types on this bean
	 * have been set. If a primitive type has not been set then it will
	 * be ignored within the match method.
	 */
	private int rmsFlags = 0;
	
	/**
	 * Indicates the reallocation state.  The states can be either:
	 * notReallocated - The bean hasn't been reallocated
	 * reallocated - The bean is to be reallocated
	 * ReallocatedBeanComplete - The bean was created for reallocation but is no longer needed as itself has been reallocated
	 * BeanUsedForReallocation - The bean was created for reallocation
	 * ReallocationFailed - The reallocation of this bean failed
	 */
	private int reallocated = Sandesha2Constants.WSRM_COMMON.NOT_REALLOCATED;
	
	/**
	 * Contains the internalSeqID of the seq that has sent the reallocated msgs
	 */
	private String internalSeqIDOfSeqUsedForReallocation = null;
	
	public static final int LAST_SEND_ERROR_TIME_FLAG = 0x00000001;
	public static final int LAST_OUT_MSG_FLAG         = 0x00000010;
	public static final int HIGHEST_OUT_MSG_FLAG      = 0x00000100;
	public static final int NEXT_MSG_NUM_FLAG         = 0x00001000;
	public static final int TERMINATE_ADDED_FLAG      = 0x00010000;
	public static final int TIMED_OUT_FLAG            = 0x00100000;
	public static final int SEQ_CLOSED_CLIENT_FLAG    = 0x01000000;
	public static final int ACKED_MESSAGES_FLAG       = 0x10000000;
	public static final int TERM_PAUSER_FOR_CS        = 0x00000002;
	public static final int EXPECTED_REPLIES          = 0x00000020;
	public static final int SOAP_VERSION_FLAG         = 0x00000200;
	
	public RMSBean() {
	}

	/**
	 * Constructor that copies all RMSBean values from the RMSBean supplied
	 * @param beanToCopy
	 */	
	public RMSBean(RMSBean beanToCopy) {
		super(beanToCopy);
		 anonymousUUID = beanToCopy.getAnonymousUUID();
		 if (beanToCopy.getClientCompletedMessages() != null)
			 clientCompletedMessages = new RangeString(beanToCopy.getClientCompletedMessages());
		 createSeqMsgID = beanToCopy.getCreateSeqMsgID();
		 createSequenceMsgStoreKey = beanToCopy.getCreateSequenceMsgStoreKey();
		 expectedReplies = beanToCopy.getExpectedReplies();
		 highestOutMessageNumber = beanToCopy.getHighestOutMessageNumber();
		 highestOutRelatesTo = beanToCopy.getHighestOutRelatesTo();
		 internalSequenceID = beanToCopy.getInternalSequenceID();
		 lastOutMessage = beanToCopy.getLastOutMessage();
		 lastSendError = beanToCopy.getLastSendError();
		 lastSendErrorTimestamp = beanToCopy.getLastSendErrorTimestamp();
		 nextMessageNumber = beanToCopy.getNextMessageNumber();
		 offeredEndPoint = beanToCopy.getOfferedEndPoint();
		 offeredEndPointEPR = beanToCopy.getOfferedEndPointEPR();
		 offeredSequence = beanToCopy.getOfferedSequence();
		 referenceMessageStoreKey = beanToCopy.getReferenceMessageStoreKey();
		 sequenceClosedClient = beanToCopy.isSequenceClosedClient();
		 soapVersion = beanToCopy.getSoapVersion();
		 terminateAdded = beanToCopy.isTerminateAdded();
		 terminationPauserForCS = beanToCopy.isTerminationPauserForCS();
		 timedOut = beanToCopy.isTimedOut();
		 transportTo = beanToCopy.getTransportTo();
		 avoidAutoTermination = beanToCopy.isAvoidAutoTermination();	
		 reallocated = beanToCopy.isReallocated();
		 internalSeqIDOfSeqUsedForReallocation = beanToCopy.getInternalSeqIDOfSeqUsedForReallocation();
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
		long numberOfMessagesAcked = 0;
		if (clientCompletedMessages != null) {
			// Process this value based on the ClientCompletedMessages
			Range ranges[] = clientCompletedMessages.getRanges();
	
			for (int rangeIndex=0; rangeIndex < ranges.length; rangeIndex++) {
				Range range = ranges[rangeIndex];
				numberOfMessagesAcked = range.upperValue - range.lowerValue + 1;
			}
		}
  	return numberOfMessagesAcked;
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
  	this.rmsFlags |= TERM_PAUSER_FOR_CS;
	}


	public long getExpectedReplies() {
		return expectedReplies;
	}

	public void setExpectedReplies(long expectedReplies) {
		this.expectedReplies = expectedReplies;
		this.rmsFlags |= EXPECTED_REPLIES;
	}

	public String getAnonymousUUID() {
		return anonymousUUID;
	}

	public void setAnonymousUUID(String anonymousUUID) {
		this.anonymousUUID = anonymousUUID;
	}

	public boolean isAvoidAutoTermination() {
		return avoidAutoTermination;
	}

	public void setAvoidAutoTermination(boolean avoidAutoTermination) {
		this.avoidAutoTermination = avoidAutoTermination;
	}

	public int getSoapVersion() {
		return soapVersion;
	}

	public void setSoapVersion(int soapVersion) {
		this.soapVersion = soapVersion;
	}

	public int getRmsFlags() {
		return rmsFlags;
	}

	public void setRmsFlags(int rmsFlags) {
		this.rmsFlags = rmsFlags;
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
		result.append("\nTerminatePauser  : "); result.append(terminationPauserForCS);
		result.append("\nTimedOut         : "); result.append(timedOut);
		result.append("\nClosedClient     : "); result.append(sequenceClosedClient);
		result.append("\nExpectedReplies  : "); result.append(expectedReplies);
		result.append("\nTransportTo      : "); result.append(transportTo);
		if(offeredEndPointEPR != null){
			result.append("\nOfferedEndPoint  : "); result.append(offeredEndPointEPR.getAddress());
		} else {
			result.append("\nOfferedEndPoint  : null");
		}
		result.append("\nOfferedSequence  : "); result.append(offeredSequence);
		if (lastSendErrorTimestamp > 0) {
			result.append("\nLastError        : "); result.append(lastSendError);
			result.append("\nLastErrorTime    : "); result.append(lastSendErrorTimestamp);
		}
		result.append("\nClientCompletedMsgs: "); result.append(clientCompletedMessages);
		result.append("\nAnonymous UUID     : "); result.append(anonymousUUID);
		result.append("\nSOAPVersion  : "); result.append(soapVersion);
		result.append("\nReallocated  : "); result.append(reallocated);
		result.append("\nInternalSeqIDOfSeqUsedForReallocation  : "); result.append(internalSeqIDOfSeqUsedForReallocation);
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

		else if(bean.getOfferedEndPointEPR() != null && !bean.getOfferedEndPointEPR().getAddress().equals(this.getOfferedEndPointEPR().getAddress()))
			match = false;
		
		else if(bean.getOfferedEndPoint() != null && !bean.getOfferedEndPoint().equals(this.getOfferedEndPoint()))
			match = false;

		else if(bean.getOfferedSequence() != null && !bean.getOfferedSequence().equals(this.getOfferedSequence()))
			match = false;

		else if(bean.getAnonymousUUID() != null && !bean.getAnonymousUUID().equals(this.getAnonymousUUID()))
			match = false;
		
		else if((bean.getInternalSeqIDOfSeqUsedForReallocation() != null && !bean.getInternalSeqIDOfSeqUsedForReallocation().equals(this.getInternalSeqIDOfSeqUsedForReallocation())))
			match = false;
		
		else if(bean.isReallocated() != this.isReallocated())
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

		else if ((bean.rmsFlags & SOAP_VERSION_FLAG) != 0 && bean.getSoapVersion() != this.getSoapVersion())
			match = false;
	
		else if((bean.rmsFlags & TIMED_OUT_FLAG) != 0 && bean.isTimedOut() != this.isTimedOut())
			match = false;
		
		else if((bean.rmsFlags & SEQ_CLOSED_CLIENT_FLAG) != 0 && bean.isSequenceClosedClient() != this.isSequenceClosedClient())
			match = false;
		
		else if((bean.rmsFlags & ACKED_MESSAGES_FLAG) != 0 && bean.getNumberOfMessagesAcked() != this.getNumberOfMessagesAcked())
			match = false;
		
		else if((bean.rmsFlags & TERM_PAUSER_FOR_CS) != 0 && bean.isTerminationPauserForCS() != this.isTerminationPauserForCS())
			match = false;

		else if((bean.rmsFlags & EXPECTED_REPLIES) != 0 && bean.getExpectedReplies() != this.getExpectedReplies())
			match = false;
		


		return match;
	}

	public int isReallocated() {
		return reallocated;
	}

	public void setReallocated(int reallocated) {
		this.reallocated = reallocated;
	}

	public String getInternalSeqIDOfSeqUsedForReallocation() {
		return internalSeqIDOfSeqUsedForReallocation;
	}

	public void setInternalSeqIDOfSeqUsedForReallocation(String internalSeqIDOfSeqUsedForReallocation) {
		this.internalSeqIDOfSeqUsedForReallocation = internalSeqIDOfSeqUsedForReallocation;
	}

	public EndpointReference getOfferedEndPointEPR() {
		return offeredEndPointEPR;
	}

	public void setOfferedEndPointEPR(EndpointReference offeredEndPointEPR) {
		this.offeredEndPointEPR = offeredEndPointEPR;
		this.offeredEndPoint = offeredEndPointEPR.getAddress();
	}

}
