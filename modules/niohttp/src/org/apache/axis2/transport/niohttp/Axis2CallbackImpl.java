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

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.niohttp.impl.HttpRequest;
import org.apache.axis2.transport.niohttp.impl.HttpResponse;
import org.apache.axis2.transport.niohttp.impl.Callback;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axiom.soap.SOAPEnvelope;

public class Axis2CallbackImpl implements Callback {

    /** The HttpRequest for which we received this HttpResponse */
    private HttpRequest request;
    /** The HttpResponse to be handled */
    private HttpResponse response;
    /** The original Axis2 MessageContext (request) */
    private MessageContext outMsgCtx;

    public Axis2CallbackImpl(HttpRequest request, MessageContext msgCtx) {
        this.request = request;
        this.outMsgCtx = msgCtx;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public MessageContext getOutMsgCtx() {
        return outMsgCtx;
    }

    public void setOutMsgCtx(MessageContext outMsgCtx) {
        this.outMsgCtx = outMsgCtx;
    }

    public void run() {

        outMsgCtx.getOperationContext().getAxisOperation();

        MessageContext responseMsgCtx = new MessageContext();
        //responseMsgCtx.setOptions(outMsgCtx.getOptions()); // this is called a hack.. why?
        // Options object reused above so soapAction needs to be removed so
        // that soapAction+wsa:Action on response don't conflict
        responseMsgCtx.setSoapAction("");
        responseMsgCtx.setServerSide(true);
        responseMsgCtx.setDoingREST(outMsgCtx.isDoingREST());
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_IN, outMsgCtx
            .getProperty(MessageContext.TRANSPORT_IN));
        responseMsgCtx.setTransportIn(outMsgCtx.getTransportIn());
        responseMsgCtx.setTransportOut(outMsgCtx.getTransportOut());

        responseMsgCtx.setOperationContext(outMsgCtx.getOperationContext());
        responseMsgCtx.setConfigurationContext(outMsgCtx.getConfigurationContext());
        responseMsgCtx.getOptions().setRelationships(
            new RelatesTo[] { new RelatesTo(outMsgCtx.getMessageID()) });
        responseMsgCtx.setTo(null);

        try {
            SOAPEnvelope resenvelope = TransportUtils.createSOAPMessage(
                        responseMsgCtx,
                        response.getInputStream(),
                        outMsgCtx.getEnvelope().getNamespace().getNamespaceURI());
            responseMsgCtx.setEnvelope(resenvelope);
            AxisEngine engine = new AxisEngine(outMsgCtx.getConfigurationContext());
            engine.receive(responseMsgCtx);
        } catch (AxisFault af) {
            af.printStackTrace();
        }
    }
}
