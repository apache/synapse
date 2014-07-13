package org.apache.axis2.test;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.EnvironmentAware;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
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
*
*/

public class EnvironmentAwareMediator implements Mediator, EnvironmentAware {
    private SynapseEnvironment se;
    private ClassLoader cl;
    public boolean mediate(SynapseMessage smc) {
        getSynapseEnvironment();
        getClassLoader();
        return true;
    }

    public void setSynapseEnvironment(SynapseEnvironment se) {
        this.se =se ;
        if (se == null) {
            throw new SynapseException("EnvironmentAware - SynapseEnvironment injection is Faling");
        }
    }

    public void setClassLoader(ClassLoader cl) {
        this.cl = cl;
        if (cl == null ) {
            throw new SynapseException("EnvironmentAware - ClassLoader injection is Faling");
        }
    }

    public SynapseEnvironment getSynapseEnvironment() {
        return se;
    }

    public ClassLoader getClassLoader() {
        return cl;
    }
}
