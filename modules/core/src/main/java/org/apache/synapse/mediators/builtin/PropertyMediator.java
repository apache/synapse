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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The property mediator would save(or remove) a named property as a local property of
 * the Synapse Message Context or as a property of the Axis2 Message Context or
 * as a Transport Header.
 * Properties set this way could be extracted through the XPath extension function
 * "synapse:get-property(scope,prop-name)"
 */

public class PropertyMediator extends AbstractMediator {

    /** The Name of the property  */
    private String name = null;
    /** The Value to be set*/
    private String value = null;
    /** The XPath expr. to get value  */
    private AXIOMXPath expression = null;
    /** The scope for which decide properties where to go*/
    private String scope = null;
    /** The Action - set or remove */
    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;
    /** Set the property (ACTION_SET) or remove it (ACTION_REMOVE). Defaults to ACTION_SET */
    private int action = ACTION_SET;

    /**
     * Sets a property into the current (local) Synapse Context or into the Axis Message Context
     * or into Transports Header
     * And Removes above properties from the corresspounding locations
     *
     * @param synCtx the message context
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Start : Property mediator");

            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Message : " + synCtx.getEnvelope());
            }
        }

        if (action == ACTION_SET) {

            String resultValue = (value != null ? value :
                Axis2MessageContext.getStringValue(expression, synCtx));

            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Setting property : " + name + " at scope : " +
                    (scope == null ? "default" : scope) + " to : " + resultValue + " (i.e. " +
                    (value != null ? "constant : " + value :
                          "result of expression : " + expression) + ")");
            }

            if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
                //Setting property into the  Synapse Context
                synCtx.setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_AXIS2.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                //Setting property into the  Axis2 Message Context
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_CLIENT.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                //Setting property into the  Axis2 Message Context client options
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.getOptions().setProperty(name, resultValue);

            } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                //Setting Transport Headers
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                Object headers = axis2MessageCtx.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                if (headers != null && headers instanceof Map) {
                    Map headersMap = (HashMap) headers;
                    headersMap.put(name, resultValue);
                }
                if (headers == null) {
                    Map headersMap = new HashMap();
                    headersMap.put(name, resultValue);
                    axis2MessageCtx.setProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                            headersMap);
                }
            }

        } else {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Removing property : " + name +
                    " (scope:" + (scope == null ? "default" : scope) + ")");
            }

            if (scope == null || XMLConfigConstants.SCOPE_DEFAULT.equals(scope)) {
                //Removing property from the  Synapse Context
                Set pros = synCtx.getPropertyKeySet();
                if (pros != null) {
                    pros.remove(name);
                }

            } else if ((XMLConfigConstants.SCOPE_AXIS2.equals(scope) ||
                XMLConfigConstants.SCOPE_CLIENT.equals(scope))
                && synCtx instanceof Axis2MessageContext) {
                
                //Removing property from the Axis2 Message Context
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                axis2MessageCtx.removeProperty(name);

            } else if (XMLConfigConstants.SCOPE_TRANSPORT.equals(scope)
                    && synCtx instanceof Axis2MessageContext) {
                // Removing transport headers
                Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2smc.getAxis2MessageContext();
                Object headers = axis2MessageCtx.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                if (headers != null && headers instanceof Map) {
                    Map headersMap = (HashMap) headers;
                    headersMap.remove(name);
                } else {
                    if (traceOrDebugOn) {
                        traceOrDebug(traceOn, "No transport headers found for the message");
                    }
                }
            }
        }
        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "End : Property mediator");
        }
        return true;
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

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }
}
