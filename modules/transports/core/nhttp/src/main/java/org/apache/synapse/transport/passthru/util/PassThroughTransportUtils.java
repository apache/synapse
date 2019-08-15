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

package org.apache.synapse.transport.passthru.util;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.Constants;
import org.apache.axis2.transport.TransportUtils;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.protocol.HTTP;
import org.apache.http.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.ProtocolState;
import org.apache.synapse.transport.passthru.SourceContext;
import org.apache.synapse.transport.passthru.TargetContext;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.SourceConnections;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.utils.conn.SynapseNHttpClientConnection;

import java.net.InetAddress;
import java.util.Map;
import java.util.Iterator;

/**
 * Utility methods used by the transport.
 */
public class PassThroughTransportUtils {

    private static final Log log = LogFactory.getLog(PassThroughTransportUtils.class);

    private static final ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();

    /**
     * This method tries to determine the hostname of the given InetAddress without
     * triggering a reverse DNS lookup.  {@link java.net.InetAddress#getHostName()}
     * triggers a reverse DNS lookup which can be very costly in cases where reverse
     * DNS fails. Tries to parse a symbolic hostname from {@link java.net.InetAddress#toString()},
     * which is documented to return a String of the form "hostname / literal IP address"
     * with 'hostname' blank if not already computed & stored in <code>address</code>.
     * <p/>
     * If the hostname cannot be determined from InetAddress.toString(),
     * the value of {@link java.net.InetAddress#getHostAddress()} is returned.
     *
     * @param address The InetAddress whose hostname has to be determined
     * @return hostname, if it can be determined. host address, if not.
     */
    public static String getHostName(InetAddress address) {
        String result;
        String hostAddress = address.getHostAddress();
        String inetAddr = address.toString();
        int index1 = inetAddr.lastIndexOf('/');
        int index2 = inetAddr.indexOf(hostAddress);
        if (index2 == index1 + 1) {
            if (index1 == 0) {
                result = hostAddress;
            } else {
                result = inetAddr.substring(0, index1);
            }
        } else {
            result = hostAddress;
        }
        return result;
    }

    /**
     * Get the EPR for the message passed in
     * @param msgContext the message context
     * @return the destination EPR
     */
    public static EndpointReference getDestinationEPR(MessageContext msgContext) {
        // Transport URL can be different from the WSA-To
        String transportURL = (String) msgContext.getProperty(
            Constants.Configuration.TRANSPORT_URL);

        if (transportURL != null) {
            return new EndpointReference(transportURL);
        } else if (
            (msgContext.getTo() != null) && !msgContext.getTo().hasAnonymousAddress()) {
            return msgContext.getTo();
        }
        return null;
    }

    /**
     * Remove unwanted headers from the http response of outgoing request. These are headers which
     * should be dictated by the transport and not the user. We remove these as these may get
     * copied from the request messages
     * 
     * @param msgContext the Axis2 Message context from which these headers should be removed
     * @param targetConfiguration configuration for the passThrough handler
     */
    public static void removeUnwantedHeaders(MessageContext msgContext, TargetConfiguration targetConfiguration) {
        Map headers = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
		Map excessHeaders = (Map) msgContext.getProperty(NhttpConstants.EXCESS_TRANSPORT_HEADERS);

        if (headers == null || headers.isEmpty()) {
            return;
        }

        Iterator iter = headers.keySet().iterator();
        while (iter.hasNext()) {
            String headerName = (String) iter.next();
            if (HTTP.CONN_DIRECTIVE.equalsIgnoreCase(headerName) ||
                HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
                iter.remove();
            }

            if (HTTP.DATE_HEADER.equalsIgnoreCase(headerName)
                && !targetConfiguration.isPreserveHttpHeader(HTTP.DATE_HEADER)) {
                iter.remove();
            }

            if (HTTP.CONTENT_LEN.equalsIgnoreCase(headerName)
                && !targetConfiguration.isPreserveHttpHeader(HTTP.CONTENT_LEN)) {
                iter.remove();
            }

            if (HTTP.CONN_KEEP_ALIVE.equalsIgnoreCase(headerName)
                && !targetConfiguration.isPreserveHttpHeader(HTTP.CONN_KEEP_ALIVE)) {
                iter.remove();
            }

            if (HTTP.SERVER_HEADER.equalsIgnoreCase(headerName)
                && !targetConfiguration.isPreserveHttpHeader(HTTP.SERVER_HEADER)) {
                iter.remove();
            }

            if (HTTP.USER_AGENT.equalsIgnoreCase(headerName)
                && !targetConfiguration.isPreserveHttpHeader(HTTP.USER_AGENT)) {
                iter.remove();
            }
        }

    }

    /**
     * Determine the Http Status Code depending on the message type processed <br>
     * (normal response versus fault response) as well as Axis2 message context properties set
     * via Synapse configuration or MessageBuilders.
     *
     * @see PassThroughConstants#FAULTS_AS_HTTP_200
     * @see PassThroughConstants#HTTP_SC
     *
     * @param msgContext the Axis2 message context
     *
     * @return the HTTP status code to set in the HTTP response object
     */
    public static int determineHttpStatusCode(MessageContext msgContext) {

        int httpStatus = HttpStatus.SC_OK;

        // if this is a dummy message to handle http 202 case with non-blocking IO
        // set the status code to 202
        if (msgContext.isPropertyTrue(PassThroughConstants.SC_ACCEPTED)) {
            httpStatus = HttpStatus.SC_ACCEPTED;
        } else {
            // is this a fault message
            boolean handleFault = msgContext.getEnvelope() != null &&
                    (msgContext.getEnvelope().getBody().hasFault() || msgContext.isProcessingFault());

            // shall faults be transmitted with HTTP 200
            boolean faultsAsHttp200 =
                PassThroughConstants.TRUE.equals(
                    msgContext.getProperty(PassThroughConstants.FAULTS_AS_HTTP_200));

            // Set HTTP status code to 500 if this is a fault case and we shall not use HTTP 200
            if (handleFault && !faultsAsHttp200) {
                httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            }

            // Any status code previously set shall be overwritten with the value of the following
            // message context property if it is set.
            Object statusCode = msgContext.getProperty(PassThroughConstants.HTTP_SC);
            if (statusCode != null) {
                try {
                    httpStatus = Integer.parseInt(
                            msgContext.getProperty(PassThroughConstants.HTTP_SC).toString());
                } catch (NumberFormatException e) {
                    log.warn("Unable to set the HTTP status code from the property "
                            + PassThroughConstants.HTTP_SC + " with value: " + statusCode);
                }
            }
        }

        return httpStatus;
    }

    public static OMOutputFormat getOMOutputFormat(MessageContext msgContext) {
    	OMOutputFormat format;
    	if(msgContext.getProperty(PassThroughConstants.MESSAGE_OUTPUT_FORMAT) != null){
    		format = (OMOutputFormat) msgContext.getProperty(PassThroughConstants.MESSAGE_OUTPUT_FORMAT);
    	}else{
    		format = new OMOutputFormat();
    	}
     
        msgContext.setDoingMTOM(TransportUtils.doWriteMTOM(msgContext));
        msgContext.setDoingSwA(TransportUtils.doWriteSwA(msgContext));
        msgContext.setDoingREST(TransportUtils.isDoingREST(msgContext));
        format.setSOAP11(msgContext.isSOAP11());
        format.setDoOptimize(msgContext.isDoingMTOM());
        format.setDoingSWA(msgContext.isDoingSwA());

        format.setCharSetEncoding(TransportUtils.getCharSetEncoding(msgContext));
        Object mimeBoundaryProperty = msgContext.getProperty(Constants.Configuration.MIME_BOUNDARY);
        if (mimeBoundaryProperty != null) {
            format.setMimeBoundary((String) mimeBoundaryProperty);
        }

        return format;
    }

    public static boolean builderInvoked(MessageContext messageContext) {
        return Boolean.TRUE.equals(messageContext.getProperty(
                PassThroughConstants.MESSAGE_BUILDER_INVOKED));
    }

    public static void finishUsingSourceConnection(HttpResponse response,
                                                   NHttpServerConnection conn,
                                                   SourceConnections connections) {
        if (!connStrategy.keepAlive(response, conn.getContext()) ||
                SourceContext.get(conn).isShutDown()) {
            SourceContext.updateState(conn, ProtocolState.CLOSING);
            connections.closeConnection(conn);
        } else {
            // Reset connection state
            connections.releaseConnection(conn);
            // Ready to deal with a new request
            conn.requestInput();
        }
    }

    public static void finishUsingTargetConnection(HttpResponse response,
                                                   NHttpClientConnection conn,
                                                   TargetConnections connections) {
        if (!connStrategy.keepAlive(response, conn.getContext())) {
            // this is a connection we should not re-use
            TargetContext.updateState(conn, ProtocolState.CLOSING);
            connections.closeConnection(conn);
        } else {
            if (conn instanceof SynapseNHttpClientConnection) {
                ((SynapseNHttpClientConnection) conn).markForRelease();
            } else {
                connections.releaseConnection(conn);
            }
        }
    }

}
