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
import org.jaxen.UnresolvableException;
import org.jaxen.VariableContext;

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
    private final SOAPEnvelope envelope;
    
    public SynapseVariableContext(SOAPEnvelope envelope) {
        this.envelope = envelope;
    }
    
    public Object getVariableValue(String namespaceURI, String prefix, String localName) throws UnresolvableException {
        if (namespaceURI == null) {
            if (localName.equals("body")) {
                return envelope.getBody();
            } else if (localName.equals("header")) {
                return envelope.getHeader();
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
