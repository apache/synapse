/*
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

package org.apache.synapse.core.axis2;

import javax.wsdl.xml.WSDLLocator;

import org.apache.synapse.config.SynapseConfiguration;
import org.xml.sax.InputSource;

/**
 * Class that adapts a ResourceMap object to WSDLLocator.
 */
public class ResourceMapWSDLLocator implements WSDLLocator {
    private final InputSource baseInputSource;
    private final String baseURI;
    private final ResourceMap resourceMap;
    private final SynapseConfiguration synCfg;
    
    private String latestImportURI;
    
    public ResourceMapWSDLLocator(InputSource baseInputSource,
                                  String baseURI,
                                  ResourceMap resourceMap,
                                  SynapseConfiguration synCfg) {
        this.baseInputSource = baseInputSource;
        this.baseURI = baseURI;
        this.resourceMap = resourceMap;
        this.synCfg = synCfg;
    }

    public InputSource getBaseInputSource() {
        return baseInputSource;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public InputSource getImportInputSource(String parentLocation, String relativeLocation) {
        InputSource result = resourceMap.resolve(synCfg, relativeLocation);
        latestImportURI = relativeLocation;
        return result;
    }

    public String getLatestImportURI() {
        return latestImportURI;
    }

    public void close() {
    }
}
