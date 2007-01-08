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
 * There is one entry for each sequence.
 */

public class RMSequenceBean extends RMBean {
	
	private static final long serialVersionUID = 1335488724679432728L;
	/**
	 * Comment for <code>sequenceID</code>
	 * The sequenceID of the representing sequence.
	 */
	private String sequenceID;

	public RMSequenceBean() {

	}

	public RMSequenceBean(String sequenceID) {
		this.sequenceID = sequenceID;
	}

	/**
	 * @return Returns the sequenceId.
	 */
	public String getSequenceID() {
		return sequenceID;
	}

	/**
	 * @param sequenceId
	 *            The sequenceId to set.
	 */
	public void setSequenceID(String sequenceID) {
		this.sequenceID = sequenceID;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("\nSequence Id: "); result.append(sequenceID);
		return result.toString();
	}

}
