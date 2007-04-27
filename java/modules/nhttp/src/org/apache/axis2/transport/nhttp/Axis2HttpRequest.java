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
package org.apache.axis2.transport.nhttp;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.nhttp.util.PipeImpl;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.TransportUtils;
import org.apache.http.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.HttpPost;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Pipe;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Iterator;

/**
 * Represents an outgoing Axis2 HTTP/s request. It holds the EPR of the destination, the
 * Axis2 MessageContext to be sent, an HttpHost object which captures information about the
 * destination, and a Pipe used to write the message stream to the destination
 */
public class Axis2HttpRequest {

    private static final Log log = LogFactory.getLog(Axis2HttpRequest.class);

    /** the EPR of the destination */
    private EndpointReference epr = null;
    /** the HttpHost that contains the HTTP connection information */
    private HttpHost httpHost = null;
    /** the message context being sent */
    private MessageContext msgContext = null;
    /** the Pipe which facilitates the serialization output to be written to the channel */
    private PipeImpl pipe = null;
    /** The Axis2 MessageFormatter that will ensure proper serialization as per Axis2 semantics */
    MessageFormatter messageFormatter = null;
    /** The OM Output format holder */
    OMOutputFormat format = null;

    public Axis2HttpRequest(EndpointReference epr, HttpHost httpHost, MessageContext msgContext) {
        this.epr = epr;
        this.httpHost = httpHost;
        this.msgContext = msgContext;
        this.format = Util.getOMOutputFormat(msgContext);
        try {
            messageFormatter = TransportUtils.getMessageFormatter(msgContext);
        } catch (AxisFault axisFault) {
            log.error("Cannot find a suitable MessageFormatter : " + axisFault.getMessage());
        }
        try {
            this.pipe = new PipeImpl();
        } catch (IOException e) {
            log.error("Error creating pipe to write message body");
        }
    }

    public EndpointReference getEpr() {
        return epr;
    }

    public HttpHost getHttpHost() {
        return httpHost;
    }

    public MessageContext getMsgContext() {
        return msgContext;
    }

    /**
     * Create and return a new HttpPost request to the destination EPR
     * @return the HttpRequest to be sent out
     */
    public HttpRequest getRequest() throws IOException {
        HttpPost httpRequest = new HttpPost(epr.getAddress());
        httpRequest.setEntity(new BasicHttpEntity());

        // set any transport headers
        Object o = msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (o != null && o instanceof Map) {
            Map headers = (Map) o;
            Iterator iter = headers.keySet().iterator();
            while (iter.hasNext()) {
                Object header = iter.next();
                Object value = headers.get(header);
                if (header instanceof String && value != null && value instanceof String) {
                    httpRequest.setHeader((String) header, (String) value);
                }
            }
        }

        // if the message is SOAP 11 (for which a SOAPAction is *required*), and
        // the msg context has a SOAPAction or a WSA-Action (give pref to SOAPAction)
        // use that over any transport header that may be available
        String soapAction = msgContext.getSoapAction();
        if (soapAction == null) {
            soapAction = msgContext.getWSAAction();
        }
        if (soapAction == null) {
            msgContext.getAxisOperation().getInputAction();
        }

        if (msgContext.isSOAP11() && soapAction != null &&
            soapAction.length() > 0) {
            Header existingHeader =
                httpRequest.getFirstHeader(HTTPConstants.HEADER_SOAP_ACTION);
            if (existingHeader != null) {
                httpRequest.removeHeader(existingHeader);
            }
            httpRequest.setHeader(HTTPConstants.HEADER_SOAP_ACTION,
                soapAction);
        }


        httpRequest.setHeader(
            HTTP.CONTENT_TYPE,
            messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));

        return httpRequest;
    }

    /**
     * Return the source channel of the pipe that bridges the serialized output to the socket
     * @return source channel to read serialized message contents
     */
    public ReadableByteChannel getSourceChannel() {
        log.debug("get source channel of the pipe on which the outgoing response is written");
        return pipe.source();
    }

    /**
     * Start streaming the message into the Pipe, so that the contents could be read off the source
     * channel returned by getSourceChannel()
     * @throws AxisFault on error
     */
    public void streamMessageContents() throws AxisFault {

        log.debug("start streaming outgoing http request");
        OutputStream out = Channels.newOutputStream(pipe.sink());

        messageFormatter.writeTo(msgContext, format, out, true);
            try {
            out.flush();
            out.close();
        } catch (IOException e) {
            handleException("Error closing outgoing message stream", e);
        }
    }

    // -------------- utility methods -------------
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }
}
