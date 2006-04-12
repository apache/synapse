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

package org.apache.synapse.axis2;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.apache.synapse.Metrics;
import org.apache.synapse.MetricsFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.registry.Registry;
//import org.apache.synapse.resources.ResourceHelperFactory;
//import org.apache.synapse.resources.ResourceHelper;
//import org.apache.synapse.resources.xml.ResourceMediator;

import org.apache.synapse.xml.MediatorFactoryFinder;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.addressing.EndpointReference;

/**
 *
 *
 *
 * <p> This is the Axis2 implementation of the SynapseEnvironment
 *
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {
	
    private Mediator mainmediator = null;

    private ClassLoader cl = null;
    private Map properties = new HashMap();
    private Map mediators = new HashMap();

    private Log log = LogFactory.getLog(getClass());

    public Axis2SynapseEnvironment(OMElement synapseConfiguration,
                                   ClassLoader cl) {
        super();
        this.cl = cl;
        if (synapseConfiguration!=null)
            mainmediator = MediatorFactoryFinder.getMediator(this, synapseConfiguration);
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

    
    public void injectMessage(SynapseMessage smc) {
    	smc.setSynapseEnvironment(this);
    	mainmediator.mediate(smc);
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    public void setClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    public void send(SynapseMessage sm) {
        if (sm.isResponse())
            Axis2Sender.sendBack(sm);
        else
            Axis2Sender.sendOn(sm);
    }


    public Mediator lookupMediator(String name) {
        return (Mediator) mediators.get(name);
    }

    public void addMediator(String name, Mediator m) {
        log.debug("adding mediator with name " + name);
        if (mediators.containsKey(name))
            log.warn("name " + name + "already present");
        mediators.put(name, m);
    }

    public Mediator getMasterMediator() {
        return mainmediator;
    }

    public void setMasterMediator(Mediator m) {
        mainmediator = m;
    }

    // lookup methods for resources handling
 //   public Mediator lookupResourceMediator(String uriRoot) {
   //     return (Mediator) fac.getResourceMediator(uriRoot);
   // }

  //  public void addResourceMediator(String uri, Mediator m) {
   //     log.debug("adding "+uri+" with "+m.getClass());
    	
    	/*if (resourceMediators.containsKey(uri)) {
            throw new SynapseException(
                    "Uri Root is already exists. Not acceptable");
        }*/
        
   //     fac.addResourceMediator(uri, (ResourceMediator)m);
   // }

   // public ResourceHelper getResourceHelper() {
        //ResourceHelperFactory fac = ResourceHelperFactory.newInstance();
        //fac.setResourceProcessorsMap(this.resourceMediators);
        //log.debug("size in env is "+this.resourceMediators.size());
    //    return fac.createResourceHelper();
   // }

	public Registry getRegistry() {
		// TODO Auto-generated method stub
		return null;
	}

	public Registry getRegistry(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMetricsFactory(String URIPrefix, MetricsFactory mf) {
		// TODO Auto-generated method stub
		
	}

	public Metrics getMetrics(String URI) {
		// TODO Auto-generated method stub
		return null;
	}

	public Metrics getMetrics(EndpointReference epr) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addRegistry(String name, Registry reg) {
		// TODO Auto-generated method stub
		
	}

	public void addRegistry(Registry reg) {
		// TODO Auto-generated method stub
		
	}

	public void addMetricsFactory(String URIPrefix, MetricsFactory mf) {
		// TODO Auto-generated method stub
		
	}

	
}
