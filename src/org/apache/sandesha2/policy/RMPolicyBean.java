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


/**
 * Used to hold RM Policy information.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 */

package org.apache.sandesha2.policy;


public class RMPolicyBean {
    private long inactiveTimeoutInterval;
    private long acknowledgementInterval;
    private long retransmissionInterval;
    private boolean exponentialBackoff;
    private int maximumRetransmissionCount;
    
    public RMPolicyBean () {
    	loadValuesFromPropertyFile ();
    }
    
    private void loadValuesFromPropertyFile () {
    	//TODO load policy values from the file.
    }
    
    public long getInactiveTimeoutInterval() {
        return inactiveTimeoutInterval;
    }
    
    public long getAcknowledgementInaterval() {
        return acknowledgementInterval;
    }
    
    public long getRetransmissionInterval() {
        return retransmissionInterval;
    }
    
    public boolean isExponentialBackoff() {
        return exponentialBackoff;
    }
    
    public void setExponentialBackoff(boolean exponentialBackoff) {
        this.exponentialBackoff = exponentialBackoff;        
    }
    
    public void setRetransmissionInterval(long retransmissionInterval) {
        this.retransmissionInterval = retransmissionInterval;
    }
    
    public void setInactiveTimeoutInterval(long inactiveTimeoutInterval) {
        this.inactiveTimeoutInterval = inactiveTimeoutInterval;
    }
    
    public void setAcknowledgementInterval(long acknowledgementInterval) {
        this.acknowledgementInterval = acknowledgementInterval;
    }

	public int getMaximumRetransmissionCount() {
		return maximumRetransmissionCount;
	}

	public void setMaximumRetransmissionCount(int maximumRetransmissionCount) {
		this.maximumRetransmissionCount = maximumRetransmissionCount;
	}
    
    
}
