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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Entry;

import java.util.List;

/**
 * This class represents a real endpoint referred by a key. An Indirect endpoint does not really
 * have a life, but merely acts as a virtual endpoint for the actual endpoint refferred.
 */
public class IndirectEndpoint extends AbstractEndpoint {

    private String key = null;
    private Endpoint realEndpoint = null;

    /**
     * Send by calling to the real endpoint
     * @param synCtx the message to send
     */
    public void send(MessageContext synCtx) {

        reLoadAndInitEndpoint(((Axis2MessageContext) synCtx).getAxis2MessageContext().getConfigurationContext());

        if (realEndpoint != null) {
            realEndpoint.send(synCtx);
        } else {
            informFailure(synCtx, SynapseConstants.ENDPOINT_IN_DIRECT_NOT_READY,
                    "Couldn't find the endpoint with the key : " + key);
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    /**
     * Ready to send, if the real endpoint is ready
     */
    public boolean readyToSend() {
        return realEndpoint.readyToSend();
    }

    @Override
    public void setName(String endpointName) {
        // do nothing, also prevent this endpoint from binding to JMX
    }

    @Override
    public EndpointContext getContext() {
        return realEndpoint.getContext();
    }

    @Override
    public List<Endpoint> getChildren() {
        return realEndpoint.getChildren();
    }

    @Override
    /**
     * Since an Indirect never sends messages for real, it has no moetrics.. but those of its
     * actual endpoint
     */
    public EndpointView getMetricsMBean() {
        return realEndpoint.getMetricsMBean();
    }

    @Override
    /**
     * Figure out the real endpoint we proxy for, and make sure its initialized
     */
    public synchronized void init(ConfigurationContext cc) {
       reLoadAndInitEndpoint(cc);
    }

    @Override
    public String toString() {
        return "[Indirect Endpoint [ " + key + "]]";
    }

    /**
     * Reload as needed , either from registry , local entries or predefined endpoints 
     * @param cc ConfigurationContext
     */
    private synchronized void reLoadAndInitEndpoint(ConfigurationContext cc) {

        Parameter parameter = cc.getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_CONFIG);
        if (parameter != null && parameter.getValue() instanceof SynapseConfiguration) {
            SynapseConfiguration synCfg = (SynapseConfiguration) parameter.getValue();

            boolean reLoad = (realEndpoint == null);
            if (!reLoad) {

                Entry entry = synCfg.getEntryDefinition(key);
                if (entry != null && entry.isDynamic()) {

                    if (!entry.isCached() || entry.isExpired()) {
                        reLoad = true;
                    }
                }
            }

            if (reLoad) {

                if (log.isDebugEnabled()) {
                    log.debug("Loading real endpoint with key : " + key);
                }

                realEndpoint = synCfg.getEndpoint(key);
                if (realEndpoint != null && !realEndpoint.isInitialized()) {
                    realEndpoint.init(cc);
                }
            }
        }
    }
}
