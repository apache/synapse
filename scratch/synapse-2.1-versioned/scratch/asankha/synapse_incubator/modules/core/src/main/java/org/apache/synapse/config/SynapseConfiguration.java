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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.registry.Registry;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.OMNode;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.net.URLConnection;
import java.io.IOException;

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
     * It could/would hold a Mediator object or a Property (if loaded from a registry)
     */
    private Object mainMediator = null;

    /** Hold reference to the Axis2 ConfigurationContext */
    private AxisConfiguration axisConfig = null;

    /** Save the path to the configuration file loaded, to save it later */
    private String pathToConfigFile = null;

    /**
     * The path to the currently loaded configuration file
     * @return file path to synapse.xml
     */
    public String getPathToConfigFile() {
        return pathToConfigFile;
    }

    /**
     * Set the path to the loaded synapse.xml
     * @param pathToConfigFile path to the synapse.xml loaded
     */
    public void setPathToConfigFile(String pathToConfigFile) {
        this.pathToConfigFile = pathToConfigFile;
    }

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
    public void addNamedSequence(String name, Property dp) {
        namedSequences.put(name, dp);
    }

    /**
     * Return the mediator named with the given name
     * @param name the name being looked up
     * @return the mediator referenced by the name
     */
    public Mediator getNamedSequence(String name) {
        Object o = namedSequences.get(name);
        if (o != null && o instanceof Property) {
            Property dp = (Property) o;
            o = getProperty(dp);
            if (o == null) {
                handleException("Invalid DynamicSequence for name : " + name + " from registry");
            }
        }
        // todo: do we need to check weather the o is a Mediator (DynamicProperty)
        return (Mediator) o;
    }

    /**
     * Deletes the mediator named with the given name
     * @param name of the mediator to be deleted
     */
    public void deleteNamedSequence(String name) {
        Object o = namedSequences.get(name);
        if(o == null) {
            handleException("Non existent sequence : " + name);
        } else {
            namedSequences.remove(name);
        }
    }

    /**
     * Return the "main" (or default) mediator of synapse. The main mediator mediates each and every
     * message flowing through the system. In an XML based configuration, this is specified within the
     * <rules> section of the configuration
     * @return the main mediator to be used
     */
    public Mediator getMainMediator() {
        Object o = mainMediator;
        if (o != null && o instanceof Property) {
            Property dp = (Property) o;
            o = getProperty(dp);
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

    public void setMainMediator(Property dp) {
        this.mainMediator = dp;
    }

    /**
     * Add a global (system-wide) property.
     * @param name the name of the property
     * @param value its value
     */
    public void addProperty(String name, Property value) {
        if(name != null && value != null) {
            if(globalProps.containsKey(name)) {
                log.warn("Overiding the global property with name : " + name);
            }
            if(value.getType() == Property.SRC_TYPE) {
                try {
                    URLConnection urlc = value.getSrc().openConnection();
                    XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(urlc.getInputStream());
                    StAXOMBuilder builder = new StAXOMBuilder(parser);
                    value.setValue(builder.getDocumentElement());
                } catch (IOException e) {
                    handleException("Can not read from the source : " + value.getSrc());
                } catch (XMLStreamException e) {
                    handleException("Can not load the source property : " + value.getName());
                }
            }

            if (value.getType() == Property.DYNAMIC_TYPE) {

                Registry registry = getRegistry(value.getRegistryName());

                if (registry == null) {
                    handleException("Registry not available.");
                }

                OMNode node = null;
                try {
                    node = registry.lookup(value.getKey());
                    if (node == null) {
                        handleException("Registry key should map to a XML resource.");
                    }
                } catch (Exception e) {
                    handleException("Registry key should map to a XML resource.");
                }
            }

            globalProps.put(name, value);
        } else {
            log.error("Name and the value of the property cannot be null");
        }
    }

    /**
     * Get the value of the named property
     * @param name key of the property being looked up
     * @return its value
     */
    public Object getProperty(String name) {
        Object o = globalProps.get(name);
        Object obj = null;
        if(o != null && o instanceof Property) {
            Property prop = (Property) o;
            if(prop.getType() == Property.DYNAMIC_TYPE) {
                obj = getRegistry(prop.getRegistryName()).getProperty(prop);
            } else {
                obj = prop.getValue();
            }
        }
        return obj;
    }

    /**
     * Get the Property object of the named property
     * @param name key of the property being looked up
     * @return its value
     */
    public Property getPropertyObject(String name) {
        Object o = globalProps.get(name);
        Property prop = null;
        if(o != null && o instanceof Property) {
            prop = (Property) o;
        } else {
            handleException("Property with name " + name + " doesnt exists in the registry");
        }
        return prop;
    }

    /**
     * Get the value of the named property
     * @param prop key of the property being looked up
     * @return its value
     */
    public Object getProperty(Property prop) {
        Object obj = null;
        if(prop != null) {
            if(prop.getType() == Property.DYNAMIC_TYPE) {
                obj = getRegistry(prop.getRegistryName()).getProperty(prop);
            } else {
                obj = prop.getValue();
            }
        }
        return obj;
    }

    /**
     * Deletes the mediator named with the given name
     * @param name of the property to be deleted
     */
    public void deleteProperty(String name) {
        Object o = globalProps.get(name);
        if(o == null) {
            handleException("Invalid property reference for key : " + name);
        } else {
            globalProps.remove(name);
        }
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
    public void addNamedEndpoint(String name, Property dp) {
        namedEndpoints.put(name, dp);
    }

    /**
     * Get the definition of a named endpoint
     * @param name the name being looked up
     * @return the endpoint definition which will resolve into an absolute address
     */
    public Endpoint getNamedEndpoint(String name) {
        Object o = namedEndpoints.get(name);
        if (o != null && o instanceof Property) {
            Property dp = (Property) o;
            o = getProperty(dp);
            if (o == null) {
                handleException("Invalid DynamicEndpoint for name : " + name + " from registry");
            }
        }
        return (Endpoint) o;
    }

    /**
     * Deletes the endpoint named with the given name
     * @param name of the endpoint to be deleted
     */
    public void deleteNamedEndpoint(String name) {
        Object o = namedEndpoints.get(name);
        if(o == null) {
            handleException("Invalid Endpoint for name : " + name + " from registry");
        } else {
            namedEndpoints.remove(name);
        }
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
        Object o = proxyServices.get(name);
        if (o != null && o instanceof Property) {
            Property dp = (Property) o;
            o = getProperty(dp);
            if (o == null) {
                handleException("Invalid DynamicEndpoint for name : " + name + " from registry");
            }
        }
        return (ProxyService) o;
    }

    /**
     * Deletes the Proxy Service named with the given name
     * @param name of the Proxy Service to be deleted
     */
    public void deleteProxyService(String name) {
        Object o = proxyServices.get(name);
        if(o == null) {
            handleException("Invalid proxyService for name : " + name + " from registry");
        } else {
            try {
                if(getAxisConfiguration().getServiceForActivation(name).isActive()) {
                    getAxisConfiguration().getService(name).setActive(false);
                }
                getAxisConfiguration().removeService(name);
                proxyServices.remove(name);
            } catch (AxisFault axisFault) {
                handleException(axisFault.getMessage());
            }
        }
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
     * Deletes the registry named with the given name
     * @param name of the registry to be deleted
     */
    public void deleteRegistry(String name) {
        Object o = registryMap.get(name);
        if(o == null) {
            handleException("Reference to non-existing registry named : " + name);
        } else {
            registryMap.remove(name);
        }
    }

    /**
     * Get the map of registered registries
     * @return a map of registry name to registry instance
     */
    public Map getRegistries() {
        return registryMap;
    }

    /**
     * Set the Axis2 AxisConfiguration to the SynapseConfiguration
     * @param axisConfig
     */
    public void setAxisConfiguration(AxisConfiguration axisConfig) {
        this.axisConfig = axisConfig;
    }

    /**
     * Get the Axis2 AxisConfiguration for the SynapseConfiguration
     * @return AxisConfiguration of the Axis2
     */
    public AxisConfiguration getAxisConfiguration() {
        return axisConfig;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
