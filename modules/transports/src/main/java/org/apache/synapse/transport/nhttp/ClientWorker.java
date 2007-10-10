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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.transport.nhttp;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.protocol.HTTP;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Performs processing of the HTTP response received for our outgoing request. An instance of this
 * class is created to process each unique response.
 */
public class ClientWorker implements Runnable {

    private static final Log log = LogFactory.getLog(ClientWorker.class);

    /** the Axis2 configuration context */
    private ConfigurationContext cfgCtx = null;
    /** the response message context that would be created */
    private MessageContext responseMsgCtx = null;
    /** the InputStream out of which the response body should be read */
    private InputStream in = null;
    /** the original request message context */
    private MessageContext outMsgCtx = null;
    /** the HttpResponse received */
    private HttpResponse response = null;

    /**
     * Create the thread that would process the response message received for the outgoing message
     * context sent
     * @param cfgCtx the Axis2 configuration context
     * @param in the InputStream to read the body of the response message received
     * @param outMsgCtx the original outgoing message context (i.e. corresponding request)
     */
    public ClientWorker(ConfigurationContext cfgCtx, InputStream in,
        HttpResponse response, MessageContext outMsgCtx) {

        this.cfgCtx = cfgCtx;
        this.in = in;
        this.outMsgCtx = outMsgCtx;
        this.response = response;

        try {
            responseMsgCtx = outMsgCtx.getOperationContext().
                getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
        } catch (AxisFault af) {
            log.error("Error getting IN message context from the operation context", af);
            return;
        }

        // this conditional block is to support Sandesha, as it uses an out-in mep, but without
        // creating the message context to write the response and adding it into the operation
        // context, as it may get a 202 accepted or 200. So if the operation is complete ignore
        // this message, else, create a new message context and handle this
        if (responseMsgCtx == null && outMsgCtx.getOperationContext().isComplete()) {
            if (log.isDebugEnabled()) {
                log.debug("Error getting IN message context from the operation context. " +
                        "Possibly an RM terminate sequence message");
            }
            return;

        } else {
            if (responseMsgCtx == null) {
                responseMsgCtx = new MessageContext();
                responseMsgCtx.setOperationContext(outMsgCtx.getOperationContext());
            }

            responseMsgCtx.setServerSide(true);
            responseMsgCtx.setDoingREST(outMsgCtx.isDoingREST());
            responseMsgCtx.setProperty(MessageContext.TRANSPORT_IN, outMsgCtx
                .getProperty(MessageContext.TRANSPORT_IN));
            responseMsgCtx.setTransportIn(outMsgCtx.getTransportIn());
            responseMsgCtx.setTransportOut(outMsgCtx.getTransportOut());

            // set any transport headers received
            Header[] headers = response.getAllHeaders();
            if (headers != null && headers.length > 0) {
                Map headerMap = new HashMap();
                for (int i=0; i<headers.length; i++) {
                    Header header = headers[i];
                    headerMap.put(header.getName(), header.getValue());
                }
                responseMsgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
            }

            responseMsgCtx.setAxisMessage(outMsgCtx.getOperationContext().getAxisOperation().
                getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
            responseMsgCtx.setOperationContext(outMsgCtx.getOperationContext());
            responseMsgCtx.setConfigurationContext(outMsgCtx.getConfigurationContext());
            responseMsgCtx.setTo(null);
        }
    }

    /**
     * Process the received response through Axis2
     */
    public void run() {
        // to support Sandesha.. if there isn't a response message context, we cannot read any
        // response and populate it with the soap envelope
        if (responseMsgCtx == null) {
            return;
        }

        SOAPEnvelope envelope = null;
        try {
            String contentType = response.getFirstHeader(HTTP.CONTENT_TYPE).getValue();
            String charSetEnc = BuilderUtil.getCharSetEncoding(contentType);
            if (charSetEnc == null) {
                charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
            }

            if (contentType.indexOf(HTTP.CHARSET_PARAM) > 0) {
                responseMsgCtx.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);
            } else {
                responseMsgCtx.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                    MessageContext.DEFAULT_CHAR_SET_ENCODING);
            }
            // workaround for Axis2 TransportUtils.createSOAPMessage() issue, where a response
            // of content type "text/xml" is thought to be REST if !MC.isServerSide(). This
            // question is still under debate and due to the timelines, I am commiting this
            // workaround as Axis2 1.2 is about to be released and Synapse 1.0
            responseMsgCtx.setServerSide(false);
            envelope = TransportUtils.createSOAPMessage(
                responseMsgCtx,
                in,
                contentType);
            responseMsgCtx.setServerSide(true);
            responseMsgCtx.setEnvelope(envelope);

        } catch (AxisFault af) {
            log.error("Fault creating response SOAP envelope", af);
            return;
        } catch (XMLStreamException e) {
            log.error("Error creating response SOAP envelope", e);
        } catch (IOException e) {
            log.error("Error closing input stream from which message was read", e);
        }

        AxisEngine engine = new AxisEngine(cfgCtx);
        try {
            engine.receive(responseMsgCtx);
        } catch (AxisFault af) {
            log.error("Fault processing response message through Axis2", af);
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignore) {}
    }

    // -------------- utility methods -------------
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }
}
