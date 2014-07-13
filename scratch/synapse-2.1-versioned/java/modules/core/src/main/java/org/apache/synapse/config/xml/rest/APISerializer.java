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

package org.apache.synapse.config.xml.rest;

import org.apache.axiom.om.*;
import org.apache.axis2.Constants;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.Handler;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.rest.Resource;

public class APISerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    public static OMElement serializeAPI(API api) {
        OMElement apiElt = fac.createOMElement("api", SynapseConstants.SYNAPSE_OMNAMESPACE);
        apiElt.addAttribute("name", api.getAPIName(), null);
        apiElt.addAttribute("context", api.getContext(), null);

        VersionStrategySerializer.serializeVersioningStrategy(api.getVersionStrategy(), apiElt) ;
        if (api.getHost() != null) {
            apiElt.addAttribute("hostname", api.getHost(), null);
        }
        if (api.getPort() != -1) {
            apiElt.addAttribute("port", String.valueOf(api.getPort()), null);
        }

        Resource[] resources = api.getResources();
        for (Resource r : resources) {
            OMElement resourceElt = ResourceSerializer.serializeResource(r);
            apiElt.addChild(resourceElt);
        }

        Handler[] handlers = api.getHandlers();
        if (handlers.length > 0) {
            OMElement handlersElt = fac.createOMElement("handlers", SynapseConstants.SYNAPSE_OMNAMESPACE);
            for (Handler handler : handlers) {
                OMElement handlerElt = fac.createOMElement("handler", SynapseConstants.SYNAPSE_OMNAMESPACE);
                handlerElt.addAttribute("class", handler.getClass().getName(), null);
                handlersElt.addChild(handlerElt);
            }
            apiElt.addChild(handlersElt);
        }

        if (api.getProtocol() == RESTConstants.PROTOCOL_HTTP_ONLY) {
            apiElt.addAttribute("transport", Constants.TRANSPORT_HTTP, null);
        } else if (api.getProtocol() == RESTConstants.PROTOCOL_HTTPS_ONLY) {
            apiElt.addAttribute("transport", Constants.TRANSPORT_HTTPS, null);
        }

        return apiElt;
    }
}
