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

package org.apache.synapse.endpoints.dispatch;

import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.MessageContext;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * This dispatcher is implemented to demonstrate a sample client session. It will detect sessions
 * based on the <syn:ClientID xmlns:syn="http://ws.apache.org/namespaces/synapse"> soap header of the
 * request message. Therefore, above header has to be included in the request soap messages by the
 * client who wants to initiate and maintain a session.
 */
public class SimpleClientSessionDispatcher implements Dispatcher {

    /**
     * Map to store session -> endpoint mappings. Synchronized map is used as this is accessed by
     * multiple threds (e.g. multiple clients different sessions).
     */
    private Map sessionMap = Collections.synchronizedMap(new HashMap());

    public Endpoint getEndpoint(MessageContext synCtx) {

        SOAPHeader header = synCtx.getEnvelope().getHeader();

        if(header != null) {
            OMElement csID = header.getFirstChildWithName(
                    new QName("http://ws.apache.org/namespaces/synapse", "ClientID", "syn"));
            if(csID != null && csID.getText() != null) {
                Object o = sessionMap.get(csID.getText());
                if (o != null) {
                    return (Endpoint) o;
                }
            }
        }

        return null;
    }

    public void updateSession(MessageContext synCtx, Endpoint endpoint) {

        SOAPHeader header = synCtx.getEnvelope().getHeader();

        if(header != null) {
            OMElement csID = header.getFirstChildWithName(
                    new QName("http://ws.apache.org/namespaces/synapse", "ClientID", "syn"));
            if(csID != null && csID.getText() != null) {
                // synchronized to avoid possible replacement of sessions
                synchronized(sessionMap) {
                    if (!sessionMap.containsKey(csID.getText())) {
                        sessionMap.put(csID.getText(), endpoint);
                    }
                }
            }
        }
    }

    public void unbind(MessageContext synCtx) {

        SOAPHeader header = synCtx.getEnvelope().getHeader();

        if(header != null) {
            OMElement csID = header.getFirstChildWithName(
                    new QName("http://ws.apache.org/namespaces/synapse", "ClientID", "syn"));
            if(csID != null && csID.getText() != null) {
                sessionMap.remove(csID.getText());
            }
        }
    }

    public boolean isServerInitiatedSession() {
        return false;
    }
}
