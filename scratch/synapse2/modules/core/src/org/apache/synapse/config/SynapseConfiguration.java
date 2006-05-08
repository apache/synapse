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

public class SynapseConfiguration {

    private Map namedSequences = new HashMap();
    private Map namedEndpoints = new HashMap();
    private Map globalProps = new HashMap();

    private Mediator mainMediator = null;

    public void addNamedMediator(String name, Mediator m) {
        namedSequences.put(name, m);
    }

    public Mediator getNamedMediator(String name) {
        return (Mediator) namedSequences.get(name);
    }

    public Mediator getMainMediator() {
        return mainMediator;
    }

    public void setMainMediator(Mediator mainMediator) {
        this.mainMediator = mainMediator;
    }

    public void addProperty(String name, String value) {
        globalProps.put(name, value);
    }

    public String getProperty(String name) {
        return (String) globalProps.get(name);
    }
}
