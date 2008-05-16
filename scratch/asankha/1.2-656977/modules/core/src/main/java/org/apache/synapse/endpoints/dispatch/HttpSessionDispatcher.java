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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;

import java.util.Map;

/**
 * Dispatches sessions based on HTTP cookies. Session is initiated by the server in the first response
 * when it sends "Set-Cookie" HTTP header with the session ID. For all successive messages client
 * should send "Cookie" HTTP header with session ID send by the server.
 */
public class HttpSessionDispatcher implements Dispatcher {

    private static final Log log = LogFactory.getLog(HttpSessionDispatcher.class);

    private final static String TRANSPORT_HEADERS = "TRANSPORT_HEADERS";
    /*HTTP Headers  */
    private final static String COOKIE = "Cookie";
    private final static String SET_COOKIE = "Set-Cookie";

    /**
     * Check if "Cookie" HTTP header is available. If so, check if that cookie is in the session map.
     * If cookie is available, there is a session for this cookie. return the (server) endpoint for
     * that session.
     *
     * @param synCtx MessageContext possibly containing a "Cookie" HTTP header.
     * @return Endpoint Server endpoint for the given HTTP session.
     */
    public Endpoint getEndpoint(MessageContext synCtx, DispatcherContext dispatcherContext) {

        Endpoint endpoint = null;

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        Object o = axis2MessageContext.getProperty(TRANSPORT_HEADERS);
        if (o != null && o instanceof Map) {
            Map headerMap = (Map) o;
            Object cookie = headerMap.get(COOKIE);

            if (cookie != null && cookie instanceof String) {
                Object ep = dispatcherContext.getEndpoint((String) cookie);
                if (ep != null && ep instanceof Endpoint) {
                    endpoint = (Endpoint) ep;
                }
            }
        }

        return endpoint;
    }

    /**
     * Searches for "Set-Cookie" HTTP header in the message context. If found and that given
     * session ID is not already in the session map update the session map by mapping the cookie
     * to the endpoint.
     *
     * @param synCtx   MessageContext possibly containing the "Set-Cookie" HTTP header.
     * @param endpoint Endpoint to be mapped to the session.
     */
    public void updateSession(MessageContext synCtx, DispatcherContext dispatcherContext, Endpoint endpoint) {

        if (endpoint == null || dispatcherContext == null) {
            return;
        }

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        Object o = axis2MessageContext.getProperty(TRANSPORT_HEADERS);
        if (o != null && o instanceof Map) {
            Map headerMap = (Map) o;
            Object cookie = headerMap.get(SET_COOKIE);

            if (cookie != null && cookie instanceof String) {
                dispatcherContext.setEndpoint((String) cookie, endpoint);
            }
        }
    }

    public void unbind(MessageContext synCtx, DispatcherContext dispatcherContext) {

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        Object o = axis2MessageContext.getProperty(TRANSPORT_HEADERS);
        if (o != null && o instanceof Map) {
            Map headerMap = (Map) o;
            Object cookie = headerMap.get(COOKIE);

            if (cookie != null && cookie instanceof String) {
                dispatcherContext.removeSession((String) cookie);
            }
        }
    }

    /**
     * HTTP sessions are initiated by the server.
     *
     * @return true
     */
    public boolean isServerInitiatedSession() {
        return true;
    }
}
