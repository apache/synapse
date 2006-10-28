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
package org.apache.axis2.transport.niohttp;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.niohttp.impl.*;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportUtils;

import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.net.URL;
import java.net.MalformedURLException;

public class NHttpSender extends AbstractHandler implements TransportSender {

    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

        OMOutputFormat format = getOMOutputFormat(msgContext);

        // Trasnport URL maybe different from the WSA-To
        EndpointReference epr = null;
        String transportURL = (String)
            msgContext.getProperty(Constants.Configuration.TRANSPORT_URL);

        if (transportURL != null) {
            epr = new EndpointReference(transportURL);
        } else if
            ((msgContext.getTo() != null) &&
                !AddressingConstants.Submission.WSA_ANONYMOUS_URL.equals(
                    msgContext.getTo().getAddress()) &&
                !AddressingConstants.Final.WSA_ANONYMOUS_URL.equals(
                    msgContext.getTo().getAddress())) {
            epr = msgContext.getTo();
        }

        // select the OMElement of the message to be sent - checking if using REST
        OMElement dataOut = null;
        if (msgContext.isDoingREST()) {
            dataOut = msgContext.getEnvelope().getBody().getFirstElement();
        } else {
            dataOut = msgContext.getEnvelope();
        }

        if (epr != null) {
            // this is a new message being sent to the above EPR
            if (!epr.getAddress().equals(AddressingConstants.Final.WSA_NONE_URI)) {
                Reactor reactor = null;
                if (epr.getAddress().startsWith("http")) {
                    reactor = Reactor.getInstance(false);
                } else if (epr.getAddress().startsWith("https")) {
                    reactor = Reactor.getInstance(true);
                }

                if (reactor != null) {
                    HttpRequest req = null;
                    try {
                        req = new HttpRequest(new URL(epr.getAddress()));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        // TODO
                    }
                    populateHttpMessage(req, msgContext, format, dataOut);
                    reactor.send(req, new Axis2CallbackImpl(req, msgContext));

                } else {
                    // TODO
                }
            } else {
                // TODO handle
            }
        } else {
            // this is a response being sent to a request already received
            if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) != null) {
                if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) 
                    instanceof HttpRequest) {
                    sendAsyncResponse(msgContext, format, dataOut);
                } else {
                    // TODO handle
                }
            } else {
                throw new AxisFault("Both message 'TO' and Property MessageContext.TRANSPORT_OUT" +
                    " is Null, Do not know where to send");
            }
        }
        return InvocationResponse.CONTINUE;
    }

    private OMOutputFormat getOMOutputFormat(MessageContext msgContext) {
        OMOutputFormat format = new OMOutputFormat();
        String charSetEnc = (String) msgContext.getProperty(
            Constants.Configuration.CHARACTER_SET_ENCODING);

        if (charSetEnc != null) {
            format.setCharSetEncoding(charSetEnc);
        } else {
            OperationContext opctx = msgContext.getOperationContext();
            if (opctx != null) {
                charSetEnc = (String) opctx.getProperty(
                    Constants.Configuration.CHARACTER_SET_ENCODING);
            }
        }

        // if the char set enc is still not found use the default
        if (charSetEnc == null) {
            charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
        }

        msgContext.setDoingMTOM(HTTPTransportUtils.doWriteMTOM(msgContext));
        msgContext.setDoingSwA(HTTPTransportUtils.doWriteSwA(msgContext));
        msgContext.setDoingREST(HTTPTransportUtils.isDoingREST(msgContext));

        format.setSOAP11(msgContext.isSOAP11());
        format.setDoOptimize(msgContext.isDoingMTOM());
        format.setDoingSWA(msgContext.isDoingSwA());
        format.setCharSetEncoding(charSetEnc);
        return format;
    }

    private void sendAsyncResponse(MessageContext msgContext, OMOutputFormat format, OMElement dataOut) throws AxisFault {

        HttpRequest request = (HttpRequest) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
        HttpResponse response = request.createResponse();
        response.setStatus(ResponseStatus.OK);
        populateHttpMessage(response, msgContext, format, dataOut);
        response.commit();
    }

    private void populateHttpMessage(HttpMessage message, MessageContext msgContext, OMOutputFormat format, OMElement dataOut) throws AxisFault {

        String contentType;
        Object contentTypeObject = msgContext.getProperty(Constants.Configuration.CONTENT_TYPE);
        if (contentTypeObject != null) {
            contentType = (String) contentTypeObject;
        } else if (msgContext.isDoingREST()) {
            contentType = HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
        } else {
            contentType = format.getContentType();
            format.setSOAP11(msgContext.isSOAP11());
        }

        message.addHeader(org.apache.axis2.transport.niohttp.impl.Constants.CONTENT_TYPE,
            contentType + "; charset=" + format.getCharSetEncoding());

        OutputStream out = message.getOutputStream();
        format.setDoOptimize(msgContext.isDoingMTOM());
        try {
            dataOut.serializeAndConsume(out, format);
            out.close();
        } catch (Exception e) {
            throw new AxisFault(e);
        }        
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
