/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.axis2.transport.nhttp;

import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMElement;
import org.safehaus.asyncweb.http.HttpRequest;
import org.safehaus.asyncweb.http.HttpResponse;
import org.safehaus.asyncweb.http.ResponseStatus;

import java.io.OutputStream;

/**
 * If the message is being sent to an EPR, this implementation currently sends it through the
 * commons HTTP sender
 */
public class AsyncHTTPSender extends AbstractHandler implements TransportSender {

    public void invoke(MessageContext msgContext) throws AxisFault {

        OMOutputFormat format = new OMOutputFormat();
        String charSetEnc = (String) msgContext.getProperty(
            Constants.Configuration.CHARACTER_SET_ENCODING);

        if (charSetEnc != null) {
            format.setCharSetEncoding(charSetEnc);
        } else {
            OperationContext opctx = msgContext.getOperationContext();
            if (opctx != null) {
                charSetEnc = (String) opctx.getProperty(Constants.Configuration.CHARACTER_SET_ENCODING);
            }
        }

        /**
         * If the char set enc is still not found use the default
         */
        if (charSetEnc == null) {
            charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
        }

        msgContext.setDoingMTOM(HTTPTransportUtils.doWriteMTOM(msgContext));
        msgContext.setDoingREST(HTTPTransportUtils.isDoingREST(msgContext));
        format.setSOAP11(msgContext.isSOAP11());
        format.setDoOptimize(msgContext.isDoingMTOM());
        format.setCharSetEncoding(charSetEnc);

        // Trasnport URL can be different from the WSA-To. So processing
        // that now.
        EndpointReference epr = null;
        String transportURL = (String) msgContext.getProperty(
            Constants.Configuration.TRANSPORT_URL);

        if (transportURL != null) {
            epr = new EndpointReference(transportURL);
        } else if (
            (msgContext.getTo() != null) &&
                !AddressingConstants.Submission.WSA_ANONYMOUS_URL.equals(
                    msgContext.getTo().getAddress()) &&
                !AddressingConstants.Final.WSA_ANONYMOUS_URL.equals(
                    msgContext.getTo().getAddress())) {
            epr = msgContext.getTo();
        }

        // Check for the REST behaviour, if you desire rest beahaviour
        // put a <parameter name="doREST" value="true"/> at the
        // server.xml/client.xml file
        // ######################################################
        // Change this place to change the wsa:toepr
        // epr = something
        // ######################################################
        OMElement dataOut = null;

        /**
         * Figuringout the REST properties/parameters
         */
        if (msgContext.isDoingREST()) {
            dataOut = msgContext.getEnvelope().getBody().getFirstElement();
        } else {
            dataOut = msgContext.getEnvelope();
        }

        if (epr != null) {
            if (!epr.getAddress().equals(AddressingConstants.Final.WSA_NONE_URI)) {
                new CommonsHTTPTransportSender().writeMessageWithCommons(
                    msgContext, epr, dataOut, format);
            }
        } else {
            if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) != null) {
                sendAsyncResponse(msgContext, format, dataOut);
            }
            else {
                throw new AxisFault("Both the TO and Property MessageContext.TRANSPORT_OUT is Null, No where to send");
            }
        }

        if (msgContext.getOperationContext() != null) {
            msgContext.getOperationContext()
                    .setProperty(Constants.RESPONSE_WRITTEN,
                            Constants.VALUE_TRUE);
        }
    }

    private void sendAsyncResponse(MessageContext msgContext, OMOutputFormat format, OMElement dataOut) throws AxisFault {

        HttpRequest request = (HttpRequest) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
        HttpResponse response = request.createHttpResponse();

        response.setStatus(ResponseStatus.OK);
        OutputStream out = response.getOutputStream();

        format.setDoOptimize(msgContext.isDoingMTOM());
        try {
            dataOut.serializeAndConsume(out, format);
        } catch (Exception e) {
            throw new AxisFault(e);
        }

        request.commitResponse(response);
    }

    public void cleanup(MessageContext msgContext) throws AxisFault {
        // do nothing
    }

    public void init(ConfigurationContext confContext, TransportOutDescription transportOut) throws AxisFault {
        // do nothing
    }

    public void stop() {
        // do nothing
    }
}
