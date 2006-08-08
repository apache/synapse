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
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * The SynapseConfiguration holds the global configuration for a Synapse
 * instance. It:
 *  - contains the model of the definitions & rules to execute
 *	- contains named global properties and their values
 *  - contains named endpoint definitions
 */
public class SynapseConfiguration {

    private static final Log log = LogFactory.getLog(SynapseConfiguration.class);

    /** Holds named sequences of mediators for reuse */
    private Map namedSequences = new HashMap();

    /** Holds named endpoints (which results into absolute EPRs) for reuse */
    private Map namedEndpoints = new HashMap();

    /** Holds names Proxy services deployed through Synapse */
    private Map proxyServices = new HashMap();

    /** Holds global (system-wide) properties that apply to the synapse instance and every message */
    private Map globalProps = new HashMap();

    /** Hold referenced to the declared registries */
    private Map registryMap = new HashMap();

    /**
     * This is the "main" (or default) synapse mediator which mediates each and every message
     * It could/would hold a Mediator object or a DynamicProperty (if loaded from a registry)
     */
    private Object mainMediator = null;

    /**
     * Add a named mediator into this configuration
     * @param name the name for the sequence
     * @param m the mediator to be reffered to by the name
     */
    public void addNamedSequence(String name, Mediator m) {
        namedSequences.put(name, m);
    }

    /**
     * Allow a DynamicProperty to be added as a named sequence, this will become
     * a DynamicSequence
     * @param name the name of the sequence
     * @param dp a DynamicProperty reflecting the dynamic sequence
     */
    public void addNamedSequence(String name, DynamicProperty dp) {
        namedSequences.put(name, dp);
    }

    /**
     * Return the mediator named with the given name
     * @param name the name being looked up
     * @return the mediator referenced by the name
     */
    public Mediator getNamedSequence(String name) {
        Object o = namedSequences.get(name);
        if (o != null && o instanceof DynamicProperty) {
            DynamicProperty dp = (DynamicProperty) o;
            o = getRegistry(dp.getRegistryName()).getProperty(dp);
            if (o == null) {
                handleException("Invalid DynamicSequence for name : " + name + " from registry");
            }
        }
        return (Mediator) o;
    }

    /**
     * Return the "main" (or default) mediator of synapse. The main mediator mediates each and every
     * message flowing through the system. In an XML based configuration, this is specified within the
     * <rules> section of the configuration
     * @return the main mediator to be used
     */
    public Mediator getMainMediator() {
        Object o = mainMediator;
        if (o != null && o instanceof DynamicProperty) {
            DynamicProperty dp = (DynamicProperty) o;
            o = getRegistry(dp.getRegistryName()).getProperty(dp);
            if (o == null) {
                handleException("Invalid Synapse Mainmediator from registry");
            }
        }
        return (Mediator) o;
    }

    /**
     * Sets the main mediator for this instance
     * @param mainMediator the mediator to be used as the main mediator
     */
    public void setMainMediator(Mediator mainMediator) {
        this.mainMediator = mainMediator;
    }

    public void setMainMediator(DynamicProperty dp) {
        this.mainMediator = dp;
    }

    /**
     * Add a global (system-wide) property.
     * @param name the name of the property
     * @param value its value
     */
    public void addProperty(String name, Object value) {
        globalProps.put(name, value);
    }

    /**
     * Get the value of the named property
     * @param name key of the property being looked up
     * @return its value
     */
    public Object getProperty(String name) {
        Object o = globalProps.get(name);
        if (o != null && o instanceof DynamicProperty) {
            DynamicProperty dp = (DynamicProperty) o;
            o = getRegistry(dp.getRegistryName()).getProperty(dp);
            if (o == null) {
                handleException("Invalid DynamicProperty reference for key : " + name);
            }
        }
        return o;
    }

    /**
     * Return the raw DynamicProperty property with the given name if such an
     * object exists. This call does not load/re-load or check expiry of the
     * actual object from the registry for a DynamicProperty. If the given name
     * does not map to a DynamicProperty, this method returns null
     * @param name the name of a DynamicProperty
     * @return raw DynamicProperty object, or null
     */
    public DynamicProperty getDynamicProperty(String name) {
        Object o = globalProps.get(name);
        if (o != null && o instanceof DynamicProperty) {
            return (DynamicProperty) o;
        }
        return null;
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
     * Support DynamicEndpoints
     * @param name name of Dynamic Endpoint
     * @param dp the DynamicProperty referencing the endpoint
     */
    public void addNamedEndpoint(String name, DynamicProperty dp) {
        namedEndpoints.put(name, dp);
    }

    /**
     * Get the definition of a named endpoint
     * @param name the name being looked up
     * @return the endpoint definition which will resolve into an absolute address
     */
    public Endpoint getNamedEndpoint(String name) {
        Object o = namedEndpoints.get(name);
        if (o != null && o instanceof DynamicProperty) {
            DynamicProperty dp = (DynamicProperty) o;
            o = getRegistry(dp.getRegistryName()).getProperty(dp);
            if (o == null) {
                handleException("Invalid DynamicEndpoint for name : " + name + " from registry");
            }
        }
        return (Endpoint) o;
    }

    /**
     * Add a Proxy service to the configuration
     * @param name the name of the Proxy service
     * @param proxy the Proxy service instance
     */
    public void addProxyService(String name, ProxyService proxy) {
        proxyServices.put(name, proxy);
    }

    /**
     * Get the Proxy service with the given name
     * @param name the name being looked up
     * @return the Proxy service
     */
    public ProxyService getProxyService(String name) {
        return (ProxyService) proxyServices.get(name);
    }

    public Collection getProxyServices() {
        return proxyServices.values();
    }

    /**
     * Get the whole list of named sequences
     * as a Map
     */
    public Map getNamedSequences() {
        return namedSequences;
    }
    /**
     * Get the whole list of named endpoints
     * as a Map
     */
    public Map getNamedEndpoints() {
        return namedEndpoints;
    }
    /**
     * Get the whole list of global properties
     * as a Map
     */
    public Map getGlobalProps() {
        return globalProps;
    }

    /**
     * Add a registry into this configuration with the given name
     * @param name a name for the registry or null for default registry
     * @param reg the actual registry implementation
     */
    public void addRegistry(String name, Registry reg) {
        if (name == null) {
            name = "DEFAULT";
        }
        registryMap.put(name, reg);
    }

    /**
     * Get the named registry, or the default if name is null
     * @param name registry name or null - for default registry
     * @return actual registry for the given name or the default registry
     */
    public Registry getRegistry(String name) {
        if (name == null) {
            name = "DEFAULT";
        }
        Registry reg = (Registry) registryMap.get(name);
        if (reg == null) {
            handleException("Reference to non-existing registry named : " + name);
        }
        return reg;
    }

    /**
     * Get the map of registered registries
     * @return a map of registry name to registry instance
     */
    public Map getRegistries() {
        return registryMap;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
