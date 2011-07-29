/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.nhttp.endpoints;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of endpoints for a listener.
 */
public class EndpointsConfiguration {

    public static final String URL_PATTERN = "urlPattern";
    public static final String MESSAGE_BUILDERS = "messageBuilders";
    public static final String MESSAGE_BUILDER = "messageBuilder";
    public static final String CONTENT_TYPE = "contentType";
    public static final String CLASS = "class";
    public static final String PARAMETER = "parameter";
    public static final String NAME = "name";
    public static final String ENDPOINT = "endpoint";
    private List<Endpoint> endpoints = new ArrayList<Endpoint>();

    /**
     * Return the endpoint matching the given URL.
     * @param url url of the request
     * @return the endpoint matching the given url
     */
    public Endpoint getEndpoint(String url) {
        for (Endpoint epr : endpoints) {
            if (epr.isMatching(url)) {
                return epr;
            }
        }

        return null;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void addEndpoint(Endpoint endpoint) {
        endpoints.add(endpoint);
    }
}
