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

package org.apache.synapse.resources;

import org.apache.synapse.api.ResourceUpdateAware;
import org.apache.axiom.om.OMElement;

/**
 *
 */
public interface ResourceHelper {

    OMElement get(String uri); // get a resource
    OMElement get(String uri, ResourceUpdateAware mediator); // get a resource and be notified of changes
    void registerResourceHandler(ResourceHandler rh, String urlRoot);
         // register a handler for a given set of URLs (which share the same root)
    void notifyUpdate(String uriRoot);
         // a resource handler uses this to notify that all resources with that root have changed (Resource Push model)
}
