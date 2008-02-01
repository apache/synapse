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
	
	/**
	 * @deprecated use toEndpointReference instead
	 */
	private String toEPR;
	private EndpointReference toEndpointReference;

	/**
	 * @deprecated use replyToEndpointReference instead
	 */
	private String replyToEPR;
	private EndpointReference replyToEndpointReference;
	
	/**
	 * @deprecated use acksToEndpointRef instead
	 */
	private String acksToEPR;
	private EndpointReference acksToEndpointRef;
	
	private String RMVersion;
	
	/**
	 * Comment for <code>securityTokenData</code>
	 * This is the security token data needed to reconstruct the token that secures this sequence.
	 */
	private String securityTokenData;

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
	
	private String serviceName = null;

	/**
	 * Flags that are used to check if the primitive types on this bean
	 * have been set. If a primitive type has not been set then it will
	 * be ignored within the match method.
	 */
	private int flags = 0;
	private static final int LAST_ACTIVATED_TIME_FLAG    = 0x00000001;
	private static final int CLOSED_FLAG                 = 0x00000010;
	private static final int TERMINATED_FLAG             = 0x00000100;
	private static final int POLLING_MODE_FLAG           = 0x00001000;

	public RMSequenceBean() {

	}
	
	/**
	 * Constructor that copies all RMSBean values from the RMSBean supplied
	 * @param beanToCopy
	 */
	public RMSequenceBean(RMSequenceBean beanToCopy) {
		acksToEPR = beanToCopy.getAcksToEPR();
		acksToEndpointRef = beanToCopy.getAcksToEndpointReference();
		closed = beanToCopy.isClosed();
		lastActivatedTime = beanToCopy.getLastActivatedTime();
		pollingMode = beanToCopy.isPollingMode();
		replyToEPR = beanToCopy.getReplyToEPR();
		replyToEndpointReference = beanToCopy.getReplyToEndpointReference();
		RMVersion = beanToCopy.getRMVersion();
		securityTokenData = beanToCopy.getSecurityTokenData();		
		sequenceID = beanToCopy.getSequenceID();
		terminated = beanToCopy.isTerminated();
		toEPR = beanToCopy.getToEPR(); 	
		toEndpointReference = beanToCopy.getToEndpointReference();
		serviceName = beanToCopy.getServiceName();
	}

	public RMSequenceBean(String sequenceID) {
		this.setSequenceID(sequenceID);
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
	
	/**
	 * @deprecated
	 */
	public String getAcksToEPR() {
		return acksToEPR;
	}
	
	public EndpointReference getAcksToEndpointReference(){
		if(acksToEndpointRef==null && acksToEPR!=null){
			//this is for release to release compatability with serializaed data
			acksToEndpointRef = new EndpointReference(acksToEPR);;
		}
		return acksToEndpointRef;
	}

	/**
	 * @deprecated
	 */
	public void setAcksToEPR(String acksToEPR) {
		this.acksToEPR = acksToEPR;
	}
	
	public void setAcksToEndpointReference(EndpointReference acksToEndpointRef){
		this.acksToEndpointRef = acksToEndpointRef;
		if(acksToEndpointRef != null){
			acksToEPR = acksToEndpointRef.getAddress();
		}
	}

	/**
	 * @deprecated
	 */
	public String getReplyToEPR() {
  	return replyToEPR;
  }
	
	public EndpointReference getReplyToEndpointReference(){
		if(replyToEndpointReference==null && replyToEPR!=null){
			//this is for release to release compatability with serializaed data
			replyToEndpointReference = new EndpointReference(replyToEPR);;
		}
		return replyToEndpointReference;
	}	

	/**
	 * @deprecated
	 */
	public void setReplyToEPR(String replyToEPR) {
  	this.replyToEPR = replyToEPR;
  }
	
	public void setReplyToEndpointReference(EndpointReference replyToEndpointRef){
		this.replyToEndpointReference = replyToEndpointRef;
		replyToEPR = replyToEndpointRef.getAddress();
	}	

	/**
	 * @deprecated
	 */
	public String getToEPR() {
  	return toEPR;
  }
	
	public EndpointReference getToEndpointReference(){
		if(toEndpointReference==null && toEPR!=null){
			//this is for release to release compatability with serializaed data
			toEndpointReference = new EndpointReference(toEPR);;
		}
		return toEndpointReference;
	}		

	/**
	 * @deprecated
	 */
	public void setToEPR(String toEPR) {
  	this.toEPR = toEPR;
  }
	
	public void setToEndpointReference(EndpointReference toEndpointRef){
		this.toEndpointReference = toEndpointRef;
		toEPR = toEndpointReference.getAddress();
	}
	
	public boolean isPollingMode() {
		return pollingMode;
	}

	public void setPollingMode(boolean pollingMode) {
		this.pollingMode = pollingMode;
		this.flags |= POLLING_MODE_FLAG;
	}

	public boolean isClosed() {
  	return closed;
  }

	public void setClosed(boolean closed) {
  	this.closed = closed;
  	this.flags |= CLOSED_FLAG;
  }

	public boolean isTerminated() {
  	return terminated;
  }

	public void setTerminated(boolean terminated) {
  	this.terminated = terminated;
  	this.flags |= TERMINATED_FLAG;
  }

	public long getLastActivatedTime() {
  	return lastActivatedTime;
  }

	public void setLastActivatedTime(long lastActivatedTime) {
		this.lastActivatedTime = lastActivatedTime;
		this.flags |= LAST_ACTIVATED_TIME_FLAG;
	}

	public String getRMVersion() {
		return RMVersion;
	}

	public void setRMVersion(String version) {
		RMVersion = version;
	}

	public String getSecurityTokenData() {
		return securityTokenData;
	}

	public void setSecurityTokenData(String securityTokenData) {
		this.securityTokenData = securityTokenData;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
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
		result.append("\nRMVersion        : "); result.append(RMVersion);	
		result.append("\nServiceName        : "); result.append(serviceName);	
		result.append("\nHas SecurityToken: "); result.append(securityTokenData != null && securityTokenData.length() > 0);
		return result.toString();
	}
	
	public boolean match(RMBean matchInfo) {
		RMSequenceBean bean = (RMSequenceBean) matchInfo;
		boolean match = true;
		
		if(bean.getSequenceID() != null && !bean.getSequenceID().equals(this.getSequenceID()))
			match = false;
		
		else if((bean.getToEndpointReference() != null && this.getToEndpointReference()!=null && !bean.getToEndpointReference().getAddress().equals(this.getToEndpointReference().getAddress())) ||
				(bean.getToEPR() != null && !bean.getToEPR().equals(this.getToEPR())))
			match = false;
		
		else if((bean.getReplyToEndpointReference() != null && this.getReplyToEndpointReference()!=null && !bean.getReplyToEndpointReference().getAddress().equals(this.getReplyToEndpointReference().getAddress())) ||
				(bean.getReplyToEPR() != null && !bean.getReplyToEPR().equals(this.getReplyToEPR())))
			match = false;
		
		else if((bean.getAcksToEndpointReference() != null && this.getAcksToEndpointReference()!=null && !bean.getAcksToEndpointReference().getAddress().equals(this.getAcksToEndpointReference().getAddress())) ||
				(bean.getAcksToEPR() != null && !bean.getAcksToEPR().equals(this.getAcksToEPR())))
			match = false;
		
		else if(bean.getRMVersion() != null && !bean.getRMVersion().equals(this.getRMVersion()))
			match = false;
		
		else if(bean.getSecurityTokenData() != null && !bean.getSecurityTokenData().equals(this.getSecurityTokenData()))
			match = false;

// Avoid matching on the last active time
//		else if((bean.flags & LAST_ACTIVATED_TIME_FLAG) != 0 && bean.getLastActivatedTime() != this.getLastActivatedTime())
//			match = false;
		
		else if((bean.flags & CLOSED_FLAG) != 0 && bean.isClosed() != this.isClosed())
			match = false;

		else if((bean.flags & TERMINATED_FLAG) != 0 && bean.isTerminated() != this.isTerminated())
			match = false;
		
		else if((bean.flags & POLLING_MODE_FLAG) != 0 && bean.isPollingMode() != this.isPollingMode())
			match = false;
		
		return match;
	}
}
