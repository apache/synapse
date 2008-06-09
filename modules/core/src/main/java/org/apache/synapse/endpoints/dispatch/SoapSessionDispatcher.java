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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.Endpoint;

import javax.xml.namespace.QName;

public class SoapSessionDispatcher implements Dispatcher {

    private static final Log log = LogFactory.getLog(SoapSessionDispatcher.class);

    private static final QName QNAME_SERVICE_GROUP_ID
            = new QName("http://ws.apache.org/namespaces/axis2", "ServiceGroupId", "axis2");

    /**
     * Gives the endpoint based on the service group context ID of the request message.
     *
     * @param synCtx Request MessageContext, possibly containing a service group context ID.
     * @return Endpoint associated with the soap session, if current message is a soap session
     *         message and if current message is not the first message of the session. Returns null,
     *         if an Endpoint could not be found for the session.
     */
    public Endpoint getEndpoint(MessageContext synCtx, DispatcherContext dispatcherContext) {

        Endpoint endpoint = null;
        SOAPHeader header = synCtx.getEnvelope().getHeader();
        
        if (header != null) {
            OMElement sgcElm = header.getFirstChildWithName(QNAME_SERVICE_GROUP_ID);

            if (sgcElm != null) {
                String sgcID = sgcElm.getText();

                if (sgcID != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Using the ServiceGroupId SOAP header value "
                                + sgcID + " to retrieve endpoint on the session");
                    }
                    endpoint = dispatcherContext.getEndpoint(sgcID);
                }
            } else if (log.isDebugEnabled()) {
                log.debug("Couldn't find the ServiceQroupId SOAP " +
                        "header to retrieve the endpoint on the session");
            }
        }

        return endpoint;
    }

    /**
     * As this is a server initiated session, this method will only be called for response messages.
     * It extracts the service group context ID (if available) from the message and updates the
     * session (service group context ID) -> endpoint map.
     *
     * @param synCtx   MessageContext of the response message.
     * @param endpoint Endpoint to associate with the session.
     */
    public void updateSession(MessageContext synCtx, DispatcherContext dispatcherContext,
        Endpoint endpoint) {

        if (endpoint == null || dispatcherContext == null) {
            return;
        }
        // get the service group context id
        // check if service group context id is a key of any entry
        // if not, add an entry <service group context id, endpoint>


        SOAPHeader header = synCtx.getEnvelope().getHeader();

        if (header != null) {
            OMElement replyTo = header.getFirstChildWithName(
                    AddressingConstants.Final.QNAME_WSA_REPLY_TO);

            if (replyTo != null) {
                OMElement referenceParameters = replyTo.getFirstChildWithName(new QName(
                        "http://www.w3.org/2005/08/addressing", "ReferenceParameters", "wsa"));

                if (referenceParameters != null) {
                    OMElement sgcElm
                            = referenceParameters.getFirstChildWithName(QNAME_SERVICE_GROUP_ID);

                    if (sgcElm != null) {
                        // synchronized to avoid possible replacement of sessions
                        String sgcID = sgcElm.getText();

                        if (sgcID != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Using the ServiceGroupId value "
                                        + sgcID + " to update the endpoint session");
                            }
                            dispatcherContext.setEndpoint(sgcID, endpoint);
                        }
                    } else if (log.isDebugEnabled()) {
                        log.debug("Couldn't find the WSA ServiceQroupId on the " +
                                "ReferenceParameters of the Reply-To header to update the session");
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("Couldn't find the WSA ReferenceParameters in the Reply-To " +
                            "header to retrieve the ServiceQroupId");
                }
            } else if (log.isDebugEnabled()) {
                log.debug("Couldn't find the WSA Reply-To header to retrieve the ServiceQroupId");
            }
        }
    }

    public void unbind(MessageContext synCtx, DispatcherContext dispatcherContext) {

        SOAPHeader header = synCtx.getEnvelope().getHeader();

        if (header != null) {
            OMElement sgcIDElm = header.getFirstChildWithName(QNAME_SERVICE_GROUP_ID);

            if (sgcIDElm != null) {
                String sgcID = sgcIDElm.getText();

                if (sgcID != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Using the ServiceGroupId value "
                                + sgcID + " to unbind session");
                    }
                    dispatcherContext.removeSession(sgcID);
                }
            } else if (log.isDebugEnabled()) {
                log.debug("Couldn't find the ServiceQroupId SOAP header to unbind the session");
            }
        }
    }

    /**
     * Soap session is initiated by the server. So this method always returns true.
     *
     * @return true
     */
    public boolean isServerInitiatedSession() {
        return true;
    }
}
