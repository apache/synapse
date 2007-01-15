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
	
	private String toEPR;

	private String replyToEPR;
	
	private String acksToEPR;
	
	private long lastActivatedTime;
 
  /**
   * Indicates that a sequence is closed
   */
  private boolean closed = false;

  /**
   * Indicates that a sequence is terminated
   */
  private boolean terminated = false;  

	/**
	 * This tells weather this sequence is in the polling mode or not.
	 * PollingManager will use this property decide the sequences that need
	 * polling and will do MakeConnections on them.
	 */
	private boolean pollingMode=false;
	
	private String rMVersion;

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
	
	public String getAcksToEPR() {
  	return acksToEPR;
  }

	public void setAcksToEPR(String acksToEPR) {
  	this.acksToEPR = acksToEPR;
  }

	public String getReplyToEPR() {
  	return replyToEPR;
  }

	public void setReplyToEPR(String replyToEPR) {
  	this.replyToEPR = replyToEPR;
  }

	public String getToEPR() {
  	return toEPR;
  }

	public void setToEPR(String toEPR) {
  	this.toEPR = toEPR;
  }

	public boolean isPollingMode() {
		return pollingMode;
	}

	public void setPollingMode(boolean pollingMode) {
		this.pollingMode = pollingMode;
	}

	public boolean isClosed() {
  	return closed;
  }

	public void setClosed(boolean closed) {
  	this.closed = closed;
  }

	public boolean isTerminated() {
  	return terminated;
  }

	public void setTerminated(boolean terminated) {
  	this.terminated = terminated;
  }

	public long getLastActivatedTime() {
  	return lastActivatedTime;
  }

	public void setLastActivatedTime(long lastActivatedTime) {
  	this.lastActivatedTime = lastActivatedTime;
  }

	public String getRMVersion() {
  	return rMVersion;
  }

	public void setRMVersion(String rmVersion) {
  	this.rMVersion = rmVersion;
  }

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("\nSequence Id  : "); result.append(sequenceID);
		result.append("\ntoEPR        : "); result.append(toEPR);
		result.append("\nreplyToEPR   : "); result.append(replyToEPR);
		result.append("\nacksToEPR    : "); result.append(acksToEPR);
		result.append("\nPolling    : "); result.append(pollingMode);
		result.append("\nClosed       : "); result.append(closed);		
		result.append("\nTerminated       : "); result.append(terminated);		
		result.append("\nLastActivatedTime: "); result.append(lastActivatedTime);	
		result.append("\nRMVersion        : "); result.append(rMVersion);	
		return result.toString();
	}
}
