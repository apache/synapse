/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.util.jaxp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.util.resolver.ResourceMap;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;

/**
 * External schema resource resolver for Validate Mediator
 * <p/>
 * This will used by Validate mediator to resolve external schema references defined in Validate mediator configuration
 * using <pre> &lt;resource location="location" key="key"/&gt; </pre> inside Validate mediator configuration.
 */
public class SchemaResourceResolver implements LSResourceResolver {

    private ResourceMap resourceMap;
    private SynapseConfiguration synCfg;
    private static final Log log = LogFactory.getLog(SchemaResourceResolver.class);

    public SchemaResourceResolver(SynapseConfiguration synCfg, ResourceMap resourceMap) {
        this.resourceMap = resourceMap;
        this.synCfg = synCfg;
    }

    /**
     * Lookup in {@link org.apache.synapse.util.resolver.ResourceMap} and returns
     * {@link org.apache.synapse.util.jaxp.SchemaResourceLSInput}
     */
    public LSInput resolveResource(String type, String namespaceURI,
                                   String publicId, String systemId,
                                   String baseURI) {

        if (log.isDebugEnabled()) {
            log.debug("Resolving Schema resource " + systemId);
        }

        if (resourceMap == null) {
            log.warn("Unable to resolve schema resource : \"" + systemId +
                    "\". External schema resources not " +
                    "defined in Validate mediator configuration");
            return null;
        }

        InputSource inputSource = resourceMap.resolve(synCfg, systemId);
        if (inputSource == null) {
            log.warn("Unable to resolve schema resource " + systemId);
            return null;
        }
        SchemaResourceLSInput schemaResourceLSInput = new SchemaResourceLSInput();
        schemaResourceLSInput.setByteStream(inputSource.getByteStream());
        schemaResourceLSInput.setSystemId(systemId);
        schemaResourceLSInput.setPublicId(publicId);
        schemaResourceLSInput.setBaseURI(baseURI);
        return schemaResourceLSInput;
    }
}

