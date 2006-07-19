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

package org.apache.sandesha2.util;

import java.util.ArrayList;

import org.apache.sandesha2.policy.RMPolicyBean;

/**
 * Used to hold peoperties loaded from sandesha2.properties file or
 * Sandesha2Constants.
 */

public class SandeshaPropertyBean {

	RMPolicyBean policyBean = new RMPolicyBean();

	// String storageManagerClass = null;
	boolean inOrder = true;

	ArrayList msgTypesToDrop = null;

	private String inMemoryStorageManagerClass = null;

	private String permanentStorageManagerClass = null;

	public long getInactiveTimeoutInterval() {
		return policyBean.getInactiveTimeoutInterval();
	}

	public long getAcknowledgementInaterval() {
		return policyBean.getAcknowledgementInaterval();
	}

	public long getRetransmissionInterval() {
		return policyBean.getRetransmissionInterval();
	}

	public boolean isExponentialBackoff() {
		return policyBean.isExponentialBackoff();
	}

	public void setExponentialBackoff(boolean exponentialBackoff) {
		policyBean.setExponentialBackoff(exponentialBackoff);
	}

	public void setRetransmissionInterval(long retransmissionInterval) {
		policyBean.setRetransmissionInterval(retransmissionInterval);
	}

	public void setInactiveTimeoutInterval(long value, String measure) {
		long timeOut = -1;

		if (measure == null) {
			policyBean.setInactiveTimeoutInterval(timeOut);
			return;
		} else if ("seconds".equals(measure)) {
			timeOut = value * 1000;
		} else if ("minutes".equals(measure)) {
			timeOut = value * 60 * 1000;
		} else if ("hours".equals(measure)) {
			timeOut = value * 60 * 60 * 1000;
		} else if ("days".equals(measure)) {
			timeOut = value * 24 * 60 * 60 * 1000;
		}

		policyBean.setInactiveTimeoutInterval(timeOut);

	}

	public void setInactiveTimeoutInterval(long inactivityTimeoutInterval) {
		policyBean.setInactiveTimeoutInterval(inactivityTimeoutInterval);
	}

	public void setAcknowledgementInterval(long acknowledgementInterval) {
		policyBean.setAcknowledgementInterval(acknowledgementInterval);
	}

	// public String getStorageManagerClass() {
	// return storageManagerClass;
	// }
	//	
	// public void setStorageManagerClass(String storageManagerClass) {
	// this.storageManagerClass = storageManagerClass;
	// }

	public String getInMemoryStorageManagerClass() {
		return inMemoryStorageManagerClass;
	}

	public void setInMemoryStorageManagerClass(String inMemoryStorageManagerClass) {
		this.inMemoryStorageManagerClass = inMemoryStorageManagerClass;
	}

	public String getPermanentStorageManagerClass() {
		return permanentStorageManagerClass;
	}

	public void setPermanentStorageManagerClass(String permanentStorageManagerClass) {
		this.permanentStorageManagerClass = permanentStorageManagerClass;
	}

	public RMPolicyBean getPolicyBean() {
		return policyBean;
	}

	public boolean isInOrder() {
		return inOrder;
	}

	public void setInOrder(boolean inOrder) {
		this.inOrder = inOrder;
	}

	public ArrayList getMsgTypesToDrop() {
		return msgTypesToDrop;
	}

	public void setMsgTypesToDrop(ArrayList msgTypesToDrop) {
		this.msgTypesToDrop = msgTypesToDrop;
	}

	public void addMsgTypeToDrop(Integer typeNo) {

		if (typeNo != null) {
			if (msgTypesToDrop == null)
				msgTypesToDrop = new ArrayList();

			msgTypesToDrop.add(typeNo);
		}
	}

	public int getMaximumRetransmissionCount() {
		return policyBean.getMaximumRetransmissionCount();
	}

	public void setMaximumRetransmissionCount(int maximumRetransmissionCount) {
		policyBean.setMaximumRetransmissionCount(maximumRetransmissionCount);
	}

}
