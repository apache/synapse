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

import java.net.URL;

/**
 * An endpoint can be used to give a logical name to an endpoint address, and possibly reused.
 * If the address is not just a simple URL, then extensibility elements may be used to indicate
 * the address. (i.e. an endpoint always will "resolve" into an absolute endpoint address.
 *
 * In future registry lookups etc may be used to resolve a named endpoint into its absolute address
 */
public class Endpoint {

    private String name = null;
    private URL address = null;

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
}
