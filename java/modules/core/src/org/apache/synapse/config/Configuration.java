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

import java.util.Map;
import java.util.HashMap;

public class Configuration {

    /** The name of this configuration */
    private String name;
    /** The type of this named Configuration */
    private String type;

    /**
     * Custom (extensible properties of this configuration
     * e.g. type=Spring, file=url etc
     */
    private Map properties = new HashMap();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public String getProperty(String name) {
        return (String) properties.get(name);
    }
}

