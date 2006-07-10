/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sandesha2.client;

import java.util.ArrayList;
import java.util.HashMap;



/**
 * This gives a report explaining the current state of the Sandesha2 
 * system.
 */
public class SandeshaReport {
	
	private ArrayList incomingSequenceList = null;
	private ArrayList outgoingSequenceList = null;
	private HashMap sequenceStatusMap = null;
	private HashMap noOfCompletedMessagesMap = null;
	private HashMap outgoingInternalSequenceIDMap = null;
	
	public SandeshaReport () {
		incomingSequenceList = new ArrayList ();
		outgoingSequenceList = new ArrayList ();
		sequenceStatusMap = new HashMap ();
		noOfCompletedMessagesMap = new HashMap ();
		outgoingInternalSequenceIDMap = new HashMap ();
	}

	public long getCompletedMessagesCount(String sequenceID) {
		Long lng = (Long) noOfCompletedMessagesMap.get(sequenceID);
		if (lng==null)
			return -1;
		
		return lng.longValue();
	}

	public ArrayList getIncomingSequenceList() {
		return incomingSequenceList;
	}

	public ArrayList getOutgoingSequenceList() {
		return outgoingSequenceList;
	}

	public byte getSequenceStatusMap(String sequenceID) {
		Byte status = (Byte) sequenceStatusMap.get(sequenceID);
		if (status==null)
			return SequenceReport.SEQUENCE_STATUS_UNKNOWN;
		
		return status.byteValue();
	}

	public void addToIncomingSequenceList (String incomingSequenceID) {
		incomingSequenceList.add(incomingSequenceID);
	}
	
	public void addToOutgoingSequenceList (String outSequenceID) {
		outgoingSequenceList.add(outSequenceID);
	}
	
	public void addToNoOfCompletedMessagesMap (String id, long noOfMsgs) {
		noOfCompletedMessagesMap.put(id, new Long (noOfMsgs));
	}
	
	public void addToSequenceStatusMap (String id, byte status) {
		sequenceStatusMap.put(id, new Byte (status));
	}
	
	public String getInternalSequenceIdOfOutSequence (String outSequenceID) {
		return (String) outgoingInternalSequenceIDMap.get(outSequenceID);
	}
	
	public void addToOutgoingInternalSequenceMap (String outSequenceID, String internalSequenceID) {
		outgoingInternalSequenceIDMap.put(outSequenceID,internalSequenceID);
	}
	
}
