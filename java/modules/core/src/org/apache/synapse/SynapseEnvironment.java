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
package org.apache.synapse;

import org.apache.axis2.om.OMElement;
import org.apache.synapse.resources.ResourceHelper;

import java.util.Map;
import java.util.HashMap;


/**
 *
 * 
 * <p>Common stuff needed to embed Synapse into a given runtime (e.g. Axis2) 
 * <p>This interface is used by the processors, and also by EnvironmentAware mediators
 * 
 *
 */
public abstract class SynapseEnvironment {

    protected SynapseEnvironment parent;
    protected Map properties;

    protected SynapseEnvironment(SynapseEnvironment parent) {
        this.properties = new HashMap();
        this.parent = parent;
    }

    public SynapseEnvironment getParent() {
        return this.parent;
    }

    public void setParent(SynapseEnvironment parent) {
        this.parent = parent;
    }

    /**
     * Retrieves an object given a key.
     *
     * @param key - if not found, will return null
     * @return Returns the property.
     */
    public Object getProperty(String key) {
        Object obj = null;

        obj = properties.get(key);

        if ((obj == null) && (parent != null)) {
            obj = parent.getProperty(key);
        }

        return obj;
    }

    /**
     * Store a property for message context
     *
     * @param key
     * @param value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /*
      * This method injects a new message into the Synapse engine
      * It is used in a couple of ways. Firstly, this is how, for example,
      * Axis2 kicks messages into Synapse to start with.
      * <p>
      * Also mediators can use this to send messages that they want to be mediated by Synapse
      * <p>For example if you want to send a copy of a message somewhere, you can clone it and then
      * injectMessage()
      */
    abstract public void injectMessage(SynapseMessage smc);

    /*
      * Processors or Mediators that wish to load classes should use the ClassLoader given here
      */
    abstract public ClassLoader getClassLoader();


    /**
     * This method allows you send messages on. As opposed to injectMessage send message does not
     * process these through Synapse.
     * <p>
     * This will send request messages on, and send response messages back to the client
     */
    abstract public void send(SynapseMessage smc, SynapseEnvironment se);


    /**
     * This is used by the references to find a processor with a given name
     *
     */
    abstract public Processor lookupProcessor(String name);


    /**
     * This is how you add a processor to the list of processors. The name which it can be
     * retrieved by is the processor.getName()
     */
    abstract public void addProcessor(Processor p);


    /**
     * This returns the "Master Processor" which is the root processor for this instance of
     * Synapse. Usually this would be the processor derived from &ltsynapse>.
     */
    abstract public Processor getMasterProcessor();


    /**
     * This sets the root processor for the engine.
     */
    abstract public void setMasterProcessor(Processor p);

    /**
     * This method is responsible for updating resources via simple GET interface.
     */
   
    abstract public ResourceHelper getResourceHelper();

    abstract  public void addResourceProcessor(Processor p);
}
