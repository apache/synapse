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

import org.apache.synapse.api.Mediator;

import java.util.HashMap;
import java.util.Map;

/**
 * The SynapseConfiguration holds the global configuration for a Synapse
 * instance. It:
 *  - contains the model of the definitions & rules to execute
 *	- contains named global properties and their values
 *  - contains named endpoint definitions
 */
public class SynapseConfiguration {
    /** Holds named sequences of mediators for reuse */
    private Map namedSequences = new HashMap();

    /** Holds named endpoints (which results into absolute EPRs) for reuse */
    private Map namedEndpoints = new HashMap();

    /** Holds global (system-wide) properties that apply to the synapse instance and every message */
    private Map globalProps = new HashMap();

    /** This is the "main" (or default) synapse mediator which mediates each and every message */
    private Mediator mainMediator = null;

    /**
     * Add a named mediator into this configuration
     * @param name the name for the sequence
     * @param m the mediator to be reffered to by the name
     */
    public void addNamedMediator(String name, Mediator m) {
        namedSequences.put(name, m);
    }

    /**
     * Return the mediator named with the given name
     * @param name the name being looked up
     * @return the mediator referenced by the name
     */
    public Mediator getNamedMediator(String name) {
        return (Mediator) namedSequences.get(name);
    }

    /**
     * Return the "main" (or default) mediator of synapse. The main mediator mediates each and every
     * message flowing through the system. In an XML based configuration, this is specified within the
     * <rules> section of the configuration
     * @return the main mediator to be used
     */
    public Mediator getMainMediator() {
        return mainMediator;
    }

    /**
     * Sets the main mediator for this instance
     * @param mainMediator the mediator to be used as the main mediator
     */
    public void setMainMediator(Mediator mainMediator) {
        this.mainMediator = mainMediator;
    }

    /**
     * Add a global (system-wide) property. These properties must be string literals
     * @param name the name of the property
     * @param value its string value
     */
    public void addProperty(String name, String value) {
        globalProps.put(name, value);
    }

    /**
     * Get the value of the named property
     * @param name key of the property being looked up
     * @return its value
     */
    public String getProperty(String name) {
        return (String) globalProps.get(name);
    }

    /**
     * Define a named endpoint with the given name
     * @param name the name of the endpoint
     * @param endpoint the endpoint definition
     */
    public void addNamedEndpoint(String name, Endpoint endpoint) {
        namedEndpoints.put(name, endpoint);
    }

    /**
     * Get the definition of a named endpoint
     * @param name the name being looked up
     * @return the endpoint definition which will resolve into an absolute address
     */
    public Endpoint getNamedEndpoint(String name) {
        return (Endpoint) namedEndpoints.get(name);
    }
}
