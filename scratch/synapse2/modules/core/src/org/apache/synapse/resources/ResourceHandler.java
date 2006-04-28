package org.apache.synapse.resources;

import org.apache.axiom.om.OMElement;

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

public interface ResourceHandler {
    OMElement get(String uri);

    void setProperty(String name, String value);
    String getProperty(String name);
    String[] getPropertyNames();

    boolean isUpdated(String uriRoot); // used to poll if resource has changed (Pull model)

}
