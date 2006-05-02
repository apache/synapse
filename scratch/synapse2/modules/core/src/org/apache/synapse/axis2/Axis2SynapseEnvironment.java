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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.config.SynapseConfiguration;

/**
 * <p> This is the Axis2 implementation of the SynapseEnvironment
 */
public class Axis2SynapseEnvironment implements SynapseEnvironment {

    private ClassLoader cl = null;
    private SynapseConfiguration config = null;
    private Log log = LogFactory.getLog(getClass());

    public Axis2SynapseEnvironment(OMElement synapseConfiguration,
                                   ClassLoader cl) {
        super();
        this.cl = cl;
//        if (synapseConfiguration!=null)
//            mainmediator = MediatorFactoryFinder.getMediator(this, synapseConfiguration);
        // TODO set main mediator here
    }

    public void injectMessage(SynapseMessage smc) {
        smc.setSynapseEnvironment(this);
        getConfiguration().getMainMediator().mediate(smc);
    }

    public void send(SynapseMessage sm) {
        if (sm.isResponse())
            Axis2Sender.sendBack(sm);
        else
            Axis2Sender.sendOn(sm);
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    public void setClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    public SynapseConfiguration getConfiguration() {
        return config;
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        this.config = cfg;
    }

}
