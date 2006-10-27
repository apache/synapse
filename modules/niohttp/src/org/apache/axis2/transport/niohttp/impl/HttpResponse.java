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
package org.apache.axis2.transport.niohttp.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;

/**
 * Represents an HttpResponse to a httpMessage received or sent.
 */
public class HttpResponse extends HttpMessage {

    private static final Log log = LogFactory.getLog(HttpResponse.class);

    /** The associated HttpRequest (if any) for this response */
    private HttpRequest request;
    /** The http version used */
    private String version = null;
    /** The response status - HTTP response */
    private ResponseStatus status = new ResponseStatus();

    /**
     * Create a HttpResponse for the given HttpRequest. Sets connection-close
     * if the httpMessage specified it.
     * @param request the HttpRequest for which this reponse will apply
     */
    public HttpResponse(HttpRequest request) {
        this.request = request;
        this.version = Constants.HTTP_11;
        if (request.isConnectionClose()) {
            addHeader(Constants.CONNECTION, Constants.CLOSE);
        }
    }

    /**
     * Create a new HttpResponse
     */
    public HttpResponse() {}

    public String getVersion() {
        return version;
    }

    public void setResponseCode(int code) {
        status.setCode(code);
    }

    public void setResponseMessage(String msg) {
        status.setMessage(msg);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Perform actual commit of this response out to the wire. Calls on the message
     * handler to process the message as required
     */
    public void commit() {
        if (outputStreamOpened) {
            // if someone didnt properly close the OutputStream after writing, flip buffer
            outputStreamOpened = false;
        }
        if (request != null) {
            request.getHandler().setResponse(this);
        }
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public void setStatus(ResponseStatus status, String msg) {
        this.status = status;
        status.setMessage(msg);
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String toStringLine() {
        return version + Constants.STRING_SP + status + Constants.CRLF;
    }

}
