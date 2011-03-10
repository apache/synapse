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

package org.apache.synapse.endpoints;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.HashMap;
import java.util.Map;

public class TemplateEndpoint extends AbstractEndpoint {
    private static final Log log = LogFactory.getLog(TemplateEndpoint.class);

    private String template = null;

    private Endpoint realEndpoint = null;

    private Map<String, String> parameters = new HashMap<String, String>();

    private String address = null;

    @Override
    public void send(MessageContext synCtx) {
        if (realEndpoint != null) {
            realEndpoint.send(synCtx);
        } else {
            informFailure(synCtx, SynapseConstants.ENDPOINT_IN_DIRECT_NOT_READY,
                    "Couldn't find the endpoint with the name " + getName() +
                            " & template : " + template);
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameterValue(String name) {
        return parameters.get(name);
    }

    public void addParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        super.init(synapseEnvironment);

        Template endpointTemplate = synapseEnvironment.getSynapseConfiguration().
                getEndpointTemplates().get(template);

        if (endpointTemplate == null) {
            handleException("Template " + template +
                    " cannot be found for the endpoint " + getName());
        }

        realEndpoint = endpointTemplate.create(this,
                synapseEnvironment.getSynapseConfiguration().getProperties());

        realEndpoint.init(synapseEnvironment);

        if (realEndpoint == null) {
            handleException("Couldn't retrieve the endpoint " + getName() +
                    " from the template: " + endpointTemplate.getName());
        }
    }
}
