/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
 */

package org.apache.sandesha2.client;

import java.util.ArrayList;

/**
 *This report will contain details of a specific sequence.
 */

public class SequenceReport {

	public static final byte SEQUENCE_STATUS_UNKNOWN = 0;
	public static final byte SEQUENCE_STATUS_INITIAL = 1;
	public static final byte SEQUENCE_STATUS_ESTABLISHED = 2;
	public static final byte SEQUENCE_STATUS_TERMINATED = 3;
	public static final byte SEQUENCE_STATUS_TIMED_OUT = 4;
	private static final byte MAX_SEQUENCE_STATUS = 4;
	
	public static final byte SEQUENCE_DIRECTION_UNKNOWN=0;
	public static final byte SEQUENCE_DIRECTION_IN=1;
	public static final byte SEQUENCE_DIRECTION_OUT=2;
	private static final byte MAX_SEQUENCE_DIRECTION = 2;
	
	private byte sequenceStatus = SEQUENCE_STATUS_UNKNOWN;
	private byte sequenceDirection = SEQUENCE_DIRECTION_UNKNOWN;
	private String sequenceID = null;
	private String internalSequenceID = null;   //only for outgoing sequences
	private ArrayList completedMessages = null; //no of messages acked (both for incoming and outgoing)
	private boolean secureSequence = false;
	
	public SequenceReport () {
		completedMessages = new ArrayList ();
	}
	
	public void setSequenceStatus (byte sequenceStatus) {
		if (sequenceStatus>=SEQUENCE_STATUS_UNKNOWN && sequenceStatus<=MAX_SEQUENCE_STATUS) {
			this.sequenceStatus = sequenceStatus;
		}
	}
	
	public void setSequenceDirection (byte sequenceDirection) {
		if (sequenceDirection>=SEQUENCE_DIRECTION_UNKNOWN && sequenceDirection<=MAX_SEQUENCE_DIRECTION) {
			this.sequenceDirection = sequenceDirection;
		}
	}
	
	public byte getSequenceStatus () {
		return sequenceStatus;
	}
	
	public byte getSequenceDirection () {
		return sequenceDirection;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public void setSequenceID(String sequenceID) {
		this.sequenceID = sequenceID;
	}
	
	public ArrayList getCompletedMessages () {
		return completedMessages;
	}

	public void addCompletedMessage (Long messageNo) {
		completedMessages.add(messageNo);
	}
	
	public void setCompletedMessages (ArrayList completedMessages) {
		this.completedMessages = completedMessages;
	}

	public String getInternalSequenceID() {
		return internalSequenceID;
	}

	public void setInternalSequenceID(String internalSequenceID) {
		this.internalSequenceID = internalSequenceID;
	}

	public boolean isSecureSequence() {
		return secureSequence;
	}

	public void setSecureSequence(boolean secureSequence) {
		this.secureSequence = secureSequence;
	}
	
	
}
