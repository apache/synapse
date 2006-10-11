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

public class Axis2CallbackImpl implements Callback {

    private HttpRequest request;
    private HttpResponse response;
    private MessageContext msgCtx;

    public Axis2CallbackImpl(HttpRequest request, MessageContext msgCtx) {
        this.request = request;
        this.msgCtx = msgCtx;
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

    public MessageContext getMsgCtx() {
        return msgCtx;
    }

    public void setMsgCtx(MessageContext msgCtx) {
        this.msgCtx = msgCtx;
    }

    public void run() {
        System.out.println("Reponse received for HttpRequest : " + request +
            "\nAxis message  :" + msgCtx.getEnvelope());
    }
}
