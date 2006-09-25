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
package org.apache.synapse.config;

import org.apache.axis2.description.Parameter;
import org.apache.neethi.Policy;

import java.net.URL;

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
    private URL address = null;
    /** The name of the actual endpoint to which this instance refers to */
    private String ref = null;
    /** Should messages be sent in an WS-RM Sequence ? */
    private boolean reliableMessagingOn = false;
    /** Should messages be sent using WS-A? */
    private boolean addressingOn = false;
    /** Should messages be sent using WS-Security? */
    private boolean securityOn = false;
    /** Any WS-RM Policy overrides to be used when communicating with this endpoint */
    private Policy wsRMPolicy = null;
    /** The Apache Rampart OutflowSecurity configuration to be used */
    private Parameter outflowSecurity = null;
    /** The Apache Rampart InflowSecurity configuration to be used */
    private Parameter inflowSecurity = null;

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
    public URL getAddress() {
        return address;
    }

    /**
     * Set an absolute URL as the address for this named endpoint
     * @param address the absolute address to be used
     */
    public void setAddress(URL address) {
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
     * Return the OutflowSecurity configuration to be used (See Rampart)
     * @return the OutflowSecurity to be used, or null if WS-Sec is not on
     */
    public Parameter getOutflowSecurity() {
        return outflowSecurity;
    }

    /**
     * Set the OutflowSecurity configuration to be used (See Apache Rampart)
     * @param outflowSecurity the Rampart OutflowSecurity configuration to be used if any
     */
    public void setOutflowSecurity(Parameter outflowSecurity) {
        this.outflowSecurity = outflowSecurity;
    }

    /**
     * Return the InflowSecurity configuration to be used (See Rampart)
     * @return the InflowSecurity to be used, or null if WS-Sec is not on
     */
    public Parameter getInflowSecurity() {
        return inflowSecurity;
    }

    /**
     * Set the InflowSecurity configuration to be used (See Apache Rampart)
     * @param inflowSecurity the Rampart InflowSecurity configuration to be used if any
     */
    public void setInflowSecurity(Parameter inflowSecurity) {
        this.inflowSecurity = inflowSecurity;
    }

    /**
     * Get the WS-RM Policy overrides
     * @return the WS-RM Policy to be used when communicating with this endpoint
     */
    public Policy getWsRMPolicy() {
        return wsRMPolicy;
    }

    /**
     * Set the WS-RM Policy to be used when communicating with this endpoint
     * @param wsRMPolicy the Policy override
     */
    public void setWsRMPolicy(Policy wsRMPolicy) {
        this.wsRMPolicy = wsRMPolicy;
    }
}
