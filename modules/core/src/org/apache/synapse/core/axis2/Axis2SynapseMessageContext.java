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
package org.apache.synapse.core.axis2;

import org.apache.synapse.SynapseContext;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.config.SynapseConfiguration;

import java.util.Map;
import java.util.HashMap;

public class Axis2SynapseMessageContext implements SynapseContext {

    private SynapseConfiguration cfg = null;
    private SynapseEnvironment   env = null;
    private SynapseMessage       msg = null;
    private Map properties = new HashMap();

    public SynapseConfiguration getConfiguration() {
        return cfg;
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        this.cfg = cfg;
    }

    public SynapseEnvironment getSynapseEnvironment() {
        return env;
    }

    public void setSynapseEnvironment(SynapseEnvironment env) {
        this.env = env;
    }

    public void setSynapseMessage(SynapseMessage msg) {
        this.msg = msg;
    }

    public SynapseMessage getSynapseMessage() {
        return msg;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
}
