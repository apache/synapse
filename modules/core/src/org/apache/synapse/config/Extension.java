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

/**
 * An Extension allows the Synapse configuration to be extended. The Spring
 * configuration support is implemented as such an extension, so that the
 * Synapse core will not be dependent on Spring classes. An extension
 * <b>must</b> specify the following methods to set and get its name.
 */
public interface Extension {

    public String getName();

    public void setName(String name);
    
}

