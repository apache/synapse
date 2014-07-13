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
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.utils.EndpointDefinition;

/**
 * This class represents the endpoints referred by keys. It does not store the actual referred
 * endpoint as a private variable as it could expire. Therefore, it only stores the key and gets the
 * actual endpoint from the synapse configuration.
 * <p/>
 * As this is also an instance of endpoint, this can be used any place, where a normal endpoint is
 * used.
 */
public class IndirectEndpoint extends FaultHandler implements Endpoint {

    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);
    private static final Log log = LogFactory.getLog(IndirectEndpoint.class);

    private String name = null;
    private String key = null;
    private Endpoint parentEndpoint = null;

    public void send(MessageContext synMessageContext) {

        // get the actual endpoint and send
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        assert endpoint != null;
        if (endpoint.isActive(synMessageContext)) {
             // register this as the immediate fault handler for this message.
            synMessageContext.pushFaultHandler(this);
            endpoint.send(synMessageContext);
        } else {
            // if this is a child of some other endpoint, inform parent about the failure.
            // if not, inform to the next fault handler.
            if (parentEndpoint != null) {
                auditWarn("Endpoint : " + endpoint.getName() + " is currently inactive" +
                        " - invoking parent endpoint", synMessageContext);
                parentEndpoint.onChildEndpointFail(this, synMessageContext);

            } else {
                auditWarn("Endpoint : " + endpoint.getName() + " is currently inactive" +
                        " - invoking fault handler / assuming failure", synMessageContext);

                Object o = synMessageContext.getFaultStack().pop();
                if (o != null) {
                    ((FaultHandler) o).handleFault(synMessageContext);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * IndirectEndpoints are active if its referref endpoint is active and vise versa. Therefore,
     * this returns if its referred endpoint is active or not.
     *
     * @param synMessageContext MessageContext of the current message.
     * @return true if the referred endpoint is active. false otherwise.
     */
    public boolean isActive(MessageContext synMessageContext) {
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        assert endpoint != null;
        return endpoint.isActive(synMessageContext);
    }

    /**
     * Activating or deactivating an IndirectEndpoint is the activating or deactivating its
     * referref endpoint. Therefore, this sets the active state of its referred endpoint.
     *
     * @param active            true if active. false otherwise.
     * @param synMessageContext MessageContext of the current message.
     */
    public void setActive(boolean active, MessageContext synMessageContext) {
        Endpoint endpoint = synMessageContext.getEndpoint(key);
        if (endpoint == null) {
            handleException("Reference to non-existent endpoint for key : " + key);
        }

        assert endpoint != null;
        endpoint.setActive(active, synMessageContext);
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {

        // if this is a child of some other endpoint, inform parent about the failure.
        // if not, inform to the next fault handler.
        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synMessageContext);
        } else {
            Object o = synMessageContext.getFaultStack().pop();
            if (o != null) {
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    protected void auditWarn(String msg, MessageContext msgContext) {
        log.warn(msg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().warn(msg);
        }
        if (shouldTrace(msgContext)) {
            trace.warn(msg);
        }
    }

    public boolean shouldTrace(MessageContext synCtx) {
        Endpoint endpoint = synCtx.getEndpoint(key);
        EndpointDefinition endptDefn = null;
        if (endpoint instanceof AddressEndpoint) {
            AddressEndpoint addEndpt = (AddressEndpoint) endpoint;
            endptDefn = addEndpt.getEndpoint();
        } else if (endpoint instanceof WSDLEndpoint) {
            WSDLEndpoint wsdlEndpt = (WSDLEndpoint) endpoint;
            endptDefn = wsdlEndpt.getEndpoint();
        }

        return endptDefn != null && ((endptDefn.getTraceState() == SynapseConstants.TRACING_ON) ||
                (endptDefn.getTraceState() == SynapseConstants.TRACING_UNSET &&
                        synCtx.getTracingState() == SynapseConstants.TRACING_ON));
    }

    public void onFault(MessageContext synMessageContext) {
        // At this point,child endpoint is in inactive state
        // if this is a child of some other endpoint, inform parent about the failure.
        // if not, inform to the next fault handler.
        if (parentEndpoint != null) {
            parentEndpoint.onChildEndpointFail(this, synMessageContext);
        } else {
            Object o = synMessageContext.getFaultStack().pop();
            if (o != null) {
                ((FaultHandler) o).handleFault(synMessageContext);
            }
        }
    }
}
