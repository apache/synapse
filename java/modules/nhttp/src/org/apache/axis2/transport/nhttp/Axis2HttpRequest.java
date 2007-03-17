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
    private Pipe pipe = null;

    public Axis2HttpRequest(EndpointReference epr, HttpHost httpHost, MessageContext msgContext) {
        this.epr = epr;
        this.httpHost = httpHost;
        this.msgContext = msgContext;
        try {
            this.pipe = Pipe.open();
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
    public HttpRequest getRequest() {
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

        httpRequest.setHeader(
            HTTP.CONTENT_TYPE, Util.getContentType(msgContext) +
            "; charset=" + Util.getOMOutputFormat(msgContext).getCharSetEncoding());

        return httpRequest;
    }

    /**
     * Return the source channel of the pipe that bridges the serialized output to the socket
     * @return source channel to read serialized message contents
     */
    public Pipe.SourceChannel getSourceChannel() {
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
        OMOutputFormat format = Util.getOMOutputFormat(msgContext);

        try {
            (msgContext.isDoingREST() ?
                msgContext.getEnvelope().getBody().getFirstElement() : msgContext.getEnvelope())
                .serializeAndConsume(out, format);
        } catch (XMLStreamException e) {
            handleException("Error serializing response message", e);
        }

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
