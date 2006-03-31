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


import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.resources.ResourceHelperFactory;
import org.apache.synapse.resources.ResourceHelper;
import org.apache.synapse.resources.xml.ResourceMediator;

import org.apache.synapse.xml.MediatorFactoryFinder;
import org.apache.axiom.om.OMElement;

/**
 *
 *
 *
 * <p> This is the Axis2 implementation of the SynapseEnvironment
 *
 */
public class Axis2SynapseEnvironment extends SynapseEnvironment {
	
	
    private Mediator mainmediator = null;

    private ClassLoader cl = null;

    private Map mediators = new HashMap();

    private Log log = LogFactory.getLog(getClass());
    private ResourceHelperFactory fac = ResourceHelperFactory.newInstance();
    //resourceProcessors keeps track of all <resources/>
    //private HashMap resourceMediators = new HashMap();

    public Axis2SynapseEnvironment(OMElement synapseConfiguration,
                                   ClassLoader cl) {
        super(null);
        this.cl = cl;
        if (synapseConfiguration!=null)
            mainmediator = MediatorFactoryFinder.getMediator(this, synapseConfiguration);
    }

    public void injectMessage(SynapseMessage smc) {
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
    public Mediator lookupResourceMediator(String uriRoot) {
        return (Mediator) fac.getResourceMediator(uriRoot);
    }

    public void addResourceMediator(String uri, Mediator m) {
        log.debug("adding "+uri+" with "+m.getClass());
    	
    	/*if (resourceMediators.containsKey(uri)) {
            throw new SynapseException(
                    "Uri Root is already exists. Not acceptable");
        }*/
        
        fac.addResourceMediator(uri, (ResourceMediator)m);
    }

    public ResourceHelper getResourceHelper() {
        //ResourceHelperFactory fac = ResourceHelperFactory.newInstance();
        //fac.setResourceProcessorsMap(this.resourceMediators);
        //log.debug("size in env is "+this.resourceMediators.size());
        return fac.createResourceHelper();
    }

	
}
