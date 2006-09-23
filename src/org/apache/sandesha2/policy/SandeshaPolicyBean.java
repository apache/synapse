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

import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.PolicyComponent;
import org.apache.sandesha2.Sandesha2Constants;

/**
 * Used to hold peoperties loaded from sandesha2.properties file or
 * Sandesha2Constants.
 */

public class SandeshaPolicyBean implements Assertion {

	// String storageManagerClass = null;
	boolean inOrder = true;

	ArrayList msgTypesToDrop = null;

	private String inMemoryStorageManagerClass = null;

	private String permanentStorageManagerClass = null;

	private String securityManagerClass = null;

	private long inactiveTimeoutValue;

	private String inactivityTimeoutMeasure;

	private long inactivityTimeoutInterval = -1;

	private long acknowledgementInterval;

	private long retransmissionInterval;

	private boolean exponentialBackoff;

	private int maximumRetransmissionCount;

	public void setInactiveTimeoutInterval(long value, String measure) {
		long timeOut = -1;

		if (measure == null) {
			this.inactivityTimeoutInterval = value;
		} else if ("seconds".equals(measure)) {
			timeOut = value * 1000;
		} else if ("minutes".equals(measure)) {
			timeOut = value * 60 * 1000;
		} else if ("hours".equals(measure)) {
			timeOut = value * 60 * 60 * 1000;
		} else if ("days".equals(measure)) {
			timeOut = value * 24 * 60 * 60 * 1000;
		}

		this.inactivityTimeoutInterval = timeOut;

	}

	public void setAcknowledgementInterval(long acknowledgementInterval) {
		this.acknowledgementInterval = acknowledgementInterval;
	}

	public String getInMemoryStorageManagerClass() {
		return inMemoryStorageManagerClass;
	}

	public void setInMemoryStorageManagerClass(
			String inMemoryStorageManagerClass) {
		this.inMemoryStorageManagerClass = inMemoryStorageManagerClass;
	}

	public String getPermanentStorageManagerClass() {
		return permanentStorageManagerClass;
	}

	public void setPermanentStorageManagerClass(
			String permanentStorageManagerClass) {
		this.permanentStorageManagerClass = permanentStorageManagerClass;
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
		return maximumRetransmissionCount;
	}

	public void setMaximumRetransmissionCount(int maximumRetransmissionCount) {
		this.maximumRetransmissionCount = maximumRetransmissionCount;
	}

	public String getSecurityManagerClass() {
		return securityManagerClass;
	}

	public void setSecurityManagerClass(String className) {
		this.securityManagerClass = className;
	}

	public QName getName() {
		return Sandesha2Constants.Assertions.Q_ELEM__RMBEAN;
	}

	public boolean isOptional() {
		return false;
	}

	public PolicyComponent normalize() {
		throw new UnsupportedOperationException("TODO");
	}

	public void serialize(XMLStreamWriter writer) throws XMLStreamException {
		throw new UnsupportedOperationException("TODO");
	}

	public boolean equal(PolicyComponent policyComponent) {
		throw new UnsupportedOperationException("TODO");
	}

	public short getType() {
		return Constants.TYPE_ASSERTION;
	}

	public boolean isExponentialBackoff() {
		return exponentialBackoff;
	}

	public void setExponentialBackoff(boolean exponentialBackoff) {
		this.exponentialBackoff = exponentialBackoff;
	}

	public long getRetransmissionInterval() {
		return retransmissionInterval;
	}

	public void setRetransmissionInterval(long retransmissionInterval) {
		this.retransmissionInterval = retransmissionInterval;
	}

	public long getAcknowledgementInterval() {
		return acknowledgementInterval;
	}

	public long getInactivityTimeoutInterval() {
		if (inactivityTimeoutInterval<0)
			setInactiveTimeoutInterval(inactiveTimeoutValue, inactivityTimeoutMeasure);
		
		return inactivityTimeoutInterval;
	}

	public void setInactiveTimeoutValue(long inactiveTimeoutValue) {
		this.inactiveTimeoutValue = inactiveTimeoutValue;
	}

	public void setInactivityTimeoutMeasure(String inactivityTimeoutMeasure) {
		this.inactivityTimeoutMeasure = inactivityTimeoutMeasure;
	}
	
	

}
