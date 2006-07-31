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

package org.apache.sandesha2.policy;

public class PolicyEngineData {
	
	private long acknowledgementInterval = -1;
	private boolean exponentialBackoff = false;
	private long inactivityTimeout = -1;
	private String inactivityTimeoutMeassure = null;
	private boolean invokeInOrder = true;
	private String messageTypesToDrop = null;
	private long retransmissionInterval =  -1;
	private String permanentStorageManager = null;
	private String inMemoryStorageManager = null;	
//	private String storageManager = null;
	private int maximumRetransmissionCount; 
	private String securityManager = null;
	
	private boolean acknowledgementIntervalSet = false;
	private boolean exponentialBackoffSet = false;
	private boolean inactivityTimeoutSet = false;
	private boolean inactivityTimeoutMeassureSet = false;
	private boolean invokeInOrderSet = false;
	private boolean messageTypesToDropSet = false;
	private boolean retransmissionIntervalSet = false;
	private boolean permanentStorageManagerSet = false;
	private boolean inMemoryStorageManagerSet = false;	
//	private boolean storageManagerSet = false;
	private boolean maximumRetransmissionCountSet = false;
	private boolean securityManagerSet = false;

	public boolean isExponentialBackoff() {
		return exponentialBackoff;
	}

	public void setExponentialBackoff(boolean exponentialBackoff) {
		this.exponentialBackoff = exponentialBackoff;
		setExponentialBackoffSet(true);
	}

	public long getInactivityTimeout() {
		return inactivityTimeout;
	}

	public void setInactivityTimeout(long inactivityTimeout) {
		this.inactivityTimeout = inactivityTimeout;
		setInactivityTimeoutSet(true);
	}

	public String getInactivityTimeoutMeassure() {
		return inactivityTimeoutMeassure;
	}

	public void setInactivityTimeoutMeassure(String inactivityTimeoutMeassure) {
		this.inactivityTimeoutMeassure = inactivityTimeoutMeassure;
		setInactivityTimeoutMeassureSet(true);
	}

	public boolean isInvokeInOrder() {
		return invokeInOrder;
	}

	public void setInvokeInOrder(boolean invokeInOrder) {
		this.invokeInOrder = invokeInOrder;
		setInvokeInOrderSet (true);
	}

	public String getMessageTypesToDrop() {
		return messageTypesToDrop;
	}

	public void setMessageTypesToDrop(String messageTypesToDrop) {
		this.messageTypesToDrop = messageTypesToDrop;
		setMessageTypesToDropSet(true);
	}

	public long getRetransmissionInterval() {
		return retransmissionInterval;
	}

	public void setRetransmissionInterval(long retransmissionInterval) {
		this.retransmissionInterval = retransmissionInterval;
		setRetransmissionIntervalSet(true);
	}

//	public String getPermanentStorageManager() {
//		return permanentStorageMgr;
//	}
//
//	public void setPermanentStorageManager(String storageManager) {
//		this.permanentStorageMgr = storageManager;
//	}

	public void initializeWithDefaults() {
		
	}

	public PolicyEngineData copy() {
		PolicyEngineData ped = new PolicyEngineData();
		
		if (isAcknowledgementIntervalSet())
			ped.setAcknowledgementInterval(this.getAcknowledgementInterval());
		
		if (isExponentialBackoffSet())
			ped.setExponentialBackoff(this.isExponentialBackoff());
		
		if (isInactivityTimeoutSet())
			ped.setInactivityTimeout(this.getInactivityTimeout());
		
		if (isInactivityTimeoutMeassureSet())
			ped.setInactivityTimeoutMeassure(this.getInactivityTimeoutMeassure());
		
		if (isInvokeInOrderSet())
		    ped.setInvokeInOrder(this.isInvokeInOrder());
		
		if (isMessageTypesToDropSet())
			ped.setMessageTypesToDrop(this.getMessageTypesToDrop());
		
		if (isRetransmissionIntervalSet())
			ped.setRetransmissionInterval(this.getRetransmissionInterval());
		
		//ped.setPermanentStorageManager(this.getPermanentStorageManager());
		
//		if (isStorageManagerSet())
//			ped.setStorageManager(this.getStorageManager());
		
		if (isInMemoryStorageManagerSet())
			ped.setInMemoryStorageManager(this.getInMemoryStorageManager());
		
		if (isPermanentStorageManagerSet())
			ped.setPermanentStorageManager(this.getPermanentStorageManager());
		
		if (isMaximumRetransmissionCountSet())
			ped.setMaximumRetransmissionCount(this.getMaximumRetransmissionCount());
		
		return ped;
	}

	public void setAcknowledgementInterval(long acknowledgementInterval) {
		this.acknowledgementInterval = acknowledgementInterval;
		setAcknowledgementIntervalSet(true);
	}
	
	public long getAcknowledgementInterval() {
		return acknowledgementInterval;
	}
	
//	public void setStorageManager(String storageManager) {
//		this.storageManager = storageManager;
//		setStorageManagerSet(true);
//	}
//	
//	public String getStorageManager() {
//		return storageManager;
//	}

	public int getMaximumRetransmissionCount() {
		return maximumRetransmissionCount;
	}

	public void setMaximumRetransmissionCount(int maximumRetransmissionCount) {
		this.maximumRetransmissionCount = maximumRetransmissionCount;
		setMaximumRetransmissionCountSet(true);
	}

	public boolean isAcknowledgementIntervalSet() {
		return acknowledgementIntervalSet;
	}

	public boolean isExponentialBackoffSet() {
		return exponentialBackoffSet;
	}

	public boolean isInactivityTimeoutMeassureSet() {
		return inactivityTimeoutMeassureSet;
	}

	public boolean isInactivityTimeoutSet() {
		return inactivityTimeoutSet;
	}

	public boolean isInMemoryStorageManagerSet() {
		return inMemoryStorageManagerSet;
	}

	public boolean isInvokeInOrderSet() {
		return invokeInOrderSet;
	}

	public boolean isMaximumRetransmissionCountSet() {
		return maximumRetransmissionCountSet;
	}

	public boolean isMessageTypesToDropSet() {
		return messageTypesToDropSet;
	}
	
	public boolean isPermanentStorageManagerSet() {
		return permanentStorageManagerSet;
	}

	public String getPermanentStorageManager() {
		return permanentStorageManager;
	}
	
	public String getInMemoryStorageManager() {
		return inMemoryStorageManager;
	}

	public boolean isRetransmissionIntervalSet() {
		return retransmissionIntervalSet;
	}

	public String getSecurityManager() {
		return securityManager;
	}
	
	public void setSecurityManager(String className) {
		securityManager = className;
		securityManagerSet = true;
	}
	
	public boolean isSecuritymanagerSet() {
		return securityManagerSet;
	}
//	public boolean isStorageManagerSet() {
//		return storageManagerSet;
//	}

	private void setAcknowledgementIntervalSet(boolean acknowledgementIntervalSet) {
		this.acknowledgementIntervalSet = acknowledgementIntervalSet;
	}

	private void setExponentialBackoffSet(boolean exponentialBackoffSet) {
		this.exponentialBackoffSet = exponentialBackoffSet;
	}

	private void setInactivityTimeoutMeassureSet(boolean inactivityTimeoutMeassureSet) {
		this.inactivityTimeoutMeassureSet = inactivityTimeoutMeassureSet;
	}

	private void setInactivityTimeoutSet(boolean inactivityTimeoutSet) {
		this.inactivityTimeoutSet = inactivityTimeoutSet;
	}

	public void setInMemoryStorageManager(String inMemoryStorageManager) {
		this.inMemoryStorageManager = inMemoryStorageManager;
		setInmemoryStorageManagerSet(true);
	}

	private void setInmemoryStorageManagerSet(boolean inMemoryStorageManagerSet) {
		this.inMemoryStorageManagerSet = inMemoryStorageManagerSet;
	}

	private void setInvokeInOrderSet(boolean invokeInOrderSet) {
		this.invokeInOrderSet = invokeInOrderSet;
	}

	public void setMaximumRetransmissionCountSet(boolean maximumRetransmissionCountSet) {
		this.maximumRetransmissionCountSet = maximumRetransmissionCountSet;
	}

	private void setMessageTypesToDropSet(boolean messageTypesToDropSet) {
		this.messageTypesToDropSet = messageTypesToDropSet;
	}

	public void setPermanentStorageManager(String permanentStorageManager) {
		this.permanentStorageManager = permanentStorageManager;
		setPermanentStorageManagerSet(true);
	}

	private void setPermanentStorageManagerSet(boolean permanentStorageManagerSet) {
		this.permanentStorageManagerSet = permanentStorageManagerSet;
	}

	private void setRetransmissionIntervalSet(boolean retransmissionIntervalSet) {
		this.retransmissionIntervalSet = retransmissionIntervalSet;
	}

//	private void setStorageManagerSet(boolean storageManagerSet) {
//		this.storageManagerSet = storageManagerSet;
//	}
}
