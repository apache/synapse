/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config;



/**
 * An endpoint can be used to give a logical name to an endpoint address, and possibly reused.
 * If the address is not just a simple URL, then extensibility elements may be used to indicate
 * the address. (i.e. an endpoint always will "resolve" into an absolute endpoint address.
 *
 * In future registry lookups etc may be used to resolve a named endpoint into its absolute address
 */
public class Endpoint {

    /** The name of this endpoint instance */
    private String name = null;
    /** The simple address this endpoint resolves to - if explicitly specified */
    private String address = null;
    /** The name of the actual endpoint to which this instance refers to */
    private String ref = null;
    /** Should messages be sent in an WS-RM Sequence ? */
    private boolean reliableMessagingOn = false;
    /** Should messages be sent using WS-A? */
    private boolean addressingOn = false;
    /** Should messages be sent using WS-Security? */
    private boolean securityOn = false;
    /** The "key" for any WS-RM Policy overrides to be used */
    private String wsRMPolicyKey = null;
    /** The "key" for any Rampart Security Policy to be used */
    private String wsSecPolicyKey = null;
    /** use a separate listener - implies addressing is on **/
	private boolean useSeparateListener = false;
	/** force REST on **/
	private boolean forcePOX = false;
	/** force SOAP on **/
	private boolean forceSOAP = false;
    /** use MTOM **/
    private boolean useMTOM = false;
    /** use SWA **/
    private boolean useSwa = false;
	
	

    /**
     * Return the name of the endpoint
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this named endpoint
     * @param name the name to be set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * This should return the absolute EPR address referenced by the named endpoint. This may be possibly computed.
     * @return an absolute address to be used to reference the named endpoint
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set an absolute URL as the address for this named endpoint
     * @param address the absolute address to be used
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Get the name of the Endpoint to which this instance refers to
     * @return the name of the referenced endpoint
     */
    public String getRef() {
        return ref;
    }

    /**
     * Set the name of an Endpoint as the referenced endpoint of this instance
     * @param ref the name of the Endpoint referenced
     */
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Is RM turned on on this endpoint?
     * @return true if on
     */
    public boolean isReliableMessagingOn() {
        return reliableMessagingOn;
    }

    /**
     * Request that RM be turned on/off on this endpoint
     * @param reliableMessagingOn
     */
    public void setReliableMessagingOn(boolean reliableMessagingOn) {
        this.reliableMessagingOn = reliableMessagingOn;
    }

    /**
     * Is WS-A turned on on this endpoint?
     * @return true if on
     */
    public boolean isAddressingOn() {
        return addressingOn;
    }

    /**
     * Request that WS-A be turned on/off on this endpoint
     * @param addressingOn
     */
    public void setAddressingOn(boolean addressingOn) {
        this.addressingOn = addressingOn;
    }

    /**
     * Is WS-Security turned on on this endpoint?
     * @return true if on
     */
    public boolean isSecurityOn() {
        return securityOn;
    }

    /**
     * Request that WS-Sec be turned on/off on this endpoint
     * @param securityOn
     */
    public void setSecurityOn(boolean securityOn) {
        this.securityOn = securityOn;
    }

    /**
     * Return the Rampart Security configuration policys' 'key' to be used (See Rampart)
     * @return the ORampart Security configuration policys' 'key' to be used (See Rampart)
     */
    public String getWsSecPolicyKey() {
        return wsSecPolicyKey;
    }

    /**
     * Set the Rampart Security configuration policys' 'key' to be used (See Rampart)
     * @param wsSecPolicyKey the Rampart Security configuration policys' 'key' to be used (See Rampart)
     */
    public void setWsSecPolicyKey(String wsSecPolicyKey) {
        this.wsSecPolicyKey = wsSecPolicyKey;
    }

    /**
     * Get the WS-RM configuration policys' 'key' to be used (See Sandesha2)
     * @return the WS-RM configuration policys' 'key' to be used (See Sandesha2)
     */
    public String getWsRMPolicyKey() {
        return wsRMPolicyKey;
    }

    /**
     * Set the WS-RM configuration policys' 'key' to be used (See Sandesha2)
     * @param wsRMPolicyKey the WS-RM configuration policys' 'key' to be used (See Sandesha2)
     */
    public void setWsRMPolicyKey(String wsRMPolicyKey) {
        this.wsRMPolicyKey = wsRMPolicyKey;
    }

	public void setUseSeparateListener(boolean b) {
		this.useSeparateListener = b;	
	}
	public boolean isUseSeparateListener() {
		return useSeparateListener;
	}

	public void setForcePOX(boolean forcePOX) {
		this.forcePOX = forcePOX;
	}

	public boolean isForcePOX() {
		return forcePOX;
	}

	public void setForceSOAP(boolean forceSOAP) {
		this.forceSOAP = forceSOAP;
	}

	public boolean isForceSOAP() {
		return forceSOAP;
	}

    public boolean isUseMTOM() {
        return useMTOM;
    }

    public void setUseMTOM(boolean useMTOM) {
        this.useMTOM = useMTOM;
    }

    public boolean isUseSwa() {
        return useSwa;
    }

    public void setUseSwa(boolean useSwa) {
        this.useSwa = useSwa;
    }
}
