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
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 */

public class CreateSeqBean extends RMBean {
	
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
	 * Comment for <code>sequenceID</code>
	 * This is the actual Sequence ID of the sequence.
	 */
	private String sequenceID;

	public CreateSeqBean() {
	}

	public CreateSeqBean(String internalSeqID, String CreateSeqMsgID,
			String sequenceID) {
		this.internalSequenceID = internalSeqID;
		this.createSeqMsgID = CreateSeqMsgID;
		this.sequenceID = sequenceID;
	}

	public String getCreateSeqMsgID() {
		return createSeqMsgID;
	}

	public void setCreateSeqMsgID(String createSeqMsgID) {
		this.createSeqMsgID = createSeqMsgID;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public void setSequenceID(String sequenceID) {
		this.sequenceID = sequenceID;
	}

	public String getInternalSequenceID() {
		return internalSequenceID;
	}

	public void setInternalSequenceID(String internalSequenceID) {
		this.internalSequenceID = internalSequenceID;
	}

}