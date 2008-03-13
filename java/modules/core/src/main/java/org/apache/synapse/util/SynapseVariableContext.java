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
package org.apache.synapse.util;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.jaxen.UnresolvableException;
import org.jaxen.VariableContext;

import java.util.Map;

/**
 * Jaxen variable context for the XPath variables implicitly exposed by Synapse.
 * It exposes the following variables:
 * <dl>
 *   <dt><tt>body</tt></dt>
 *   <dd>The SOAP 1.1 or 1.2 body element.</dd>
 *   <dt><tt>header</tt></dt>
 *   <dd>The SOAP 1.1 or 1.2 header element.</dd>
 * </dl>
 */
public class SynapseVariableContext implements VariableContext {

    private final MessageContext synCtx;
    private final SOAPEnvelope env;

    public SynapseVariableContext(MessageContext synCtx) {
        this.synCtx = synCtx;
        this.env = synCtx.getEnvelope();
    }

    public SynapseVariableContext(SOAPEnvelope env) {
        this.synCtx = null;
        this.env = env;
    }
    
    public Object getVariableValue(String namespaceURI, String prefix, String localName)
        throws UnresolvableException {

        if (namespaceURI == null) {
            
            if (env != null) {
                
                if (localName.equals("body")) {
                    return env.getBody();
                } else if (localName.equals("header")) {
                    return env.getHeader();
                }
            
            }

            if (prefix != null && !"".equals(prefix) && synCtx != null) {

                if ("ctx".equals(prefix)) {

                    return synCtx.getProperty(localName);

                } else if ("axis2".equals(prefix)) {

                    return ((Axis2MessageContext)
                        synCtx).getAxis2MessageContext().getProperty(localName);

                } else if ("trp".equals(prefix)) {

                    org.apache.axis2.context.MessageContext axis2MessageContext =
                        ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                    Object headers = axis2MessageContext.getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        return headersMap.get(localName);
                    }
                }
            }
        }

        StringBuilder message = new StringBuilder("No such variable \"");
        if (namespaceURI != null) {
            message.append('{').append(namespaceURI).append('}');
        }
        
        message.append(localName).append('"');
        throw new UnresolvableException(message.toString());
    }
}
