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

import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.Map;
import java.util.HashMap;

public class TestSynapseMessageContext implements SynapseMessageContext {

    private Map properties = new HashMap();

    private SynapseMessage synMsg = null;

    private SynapseConfiguration synCfg = null;

    public SynapseConfiguration getConfiguration() {
        return synCfg;
    }

    public void setConfiguration(SynapseConfiguration cfg) {
        this.synCfg = cfg;
    }

    public SynapseEnvironment getSynapseEnvironment() {
        return null;
    }

    public void setSynapseEnvironment(SynapseEnvironment se) {
    }

    public void setSynapseMessage(SynapseMessage sm) {
        synMsg = sm;
    }

    public SynapseMessage getSynapseMessage() {
        if (synMsg == null)
            return new TestSynapseMessage();
        else
            return synMsg;
    }

    public Object getProperty(String key) {
        Object ret = properties.get(key);
        if (ret != null) {
            return ret;
        } else if (getConfiguration() != null) {
            return getConfiguration().getProperty(key);
        } else {
            return null;
        }
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }}
