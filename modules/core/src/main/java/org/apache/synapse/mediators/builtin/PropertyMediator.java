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

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.Map;
import java.util.HashMap;

/**
 * The property mediator would save a named property as a local property
 * of the Synapse Message Context. Properties set this way could be
 * extracted through the XPath extension function "synapse:get-property(prop-name)"
 */
public class PropertyMediator extends AbstractMediator {

    private String name = null;
    private String value = null;
    private AXIOMXPath expression = null;
    private String scope = null;

    private static final Log log = LogFactory.getLog(PropertyMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    /**
     * Sets a property into the current (local) Synapse Context
     *
     * @param smc the message context
     * @return true always
     */
    public boolean mediate(MessageContext smc) {
        log.debug("Set-Property mediator :: mediate()");
        boolean shouldTrace = shouldTrace(smc.getTracingState());
        if (shouldTrace) {
            trace.trace("Start : Property mediator");
        }
        String value = (this.value != null ? this.value : Axis2MessageContext.getStringValue(
                getExpression(), smc));
        log.debug("Setting property : " + name +
                " (scope:" + (scope == null ? "default" : scope) + ") = " + value);
        if (shouldTrace) {
            trace.trace("Property Name : " + name +
                    " (scope:" + (scope == null ? "default" : scope) + ") set to " +
                    (this.value != null ? " value = " + this.value :
                            " result of expression " + expression + " = " + value));
        }
        if (scope == null) {
            smc.setProperty(name, value);

        } else if (Constants.SCOPE_CORRELATE.equals(scope)) {
            smc.setProperty(name, value);

        } else if (Constants.SCOPE_AXIS2.equals(scope)
                && smc instanceof Axis2MessageContext) {
            Axis2MessageContext axis2smc = (Axis2MessageContext) smc;
            org.apache.axis2.context.MessageContext axis2MessageCtx =
                    axis2smc.getAxis2MessageContext();
            axis2MessageCtx.getConfigurationContext().setProperty(name, value);

        } else if (Constants.SCOPE_TRANSPORT.equals(scope)
                && smc instanceof Axis2MessageContext) {
            Axis2MessageContext axis2smc = (Axis2MessageContext) smc;
            org.apache.axis2.context.MessageContext axis2MessageCtx =
                    axis2smc.getAxis2MessageContext();
            Object headers = axis2MessageCtx.getProperty(
                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

            if (headers != null && headers instanceof Map) {
                Map headersMap = (HashMap) headers;
                headersMap.put(name, value);
            }
            if (headers == null) {
                Map headersMap = new HashMap();
                headersMap.put(name, value);
                axis2MessageCtx.setProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                        headersMap);
            }

        } else {
            String msg = "Unsupported scope : " + scope + " for set-property mediator";
            handleException(msg);
        }

        if (shouldTrace) {
            trace.trace("End : Property mediator");
        }
        return true;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AXIOMXPath getExpression() {
        return expression;
    }

    public void setExpression(AXIOMXPath expression) {
        this.expression = expression;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
