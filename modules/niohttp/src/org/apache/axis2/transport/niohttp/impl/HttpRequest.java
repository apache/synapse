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

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Represents an HttpRequest message.
 */
public class HttpRequest extends HttpMessage {

    private static final Log log = LogFactory.getLog(HttpRequest.class);

    private IncomingHandler handler;

    private String host;
    private int port;
    private String path;
    private String method;
    private String protocol;
    private Map requestParams;
    private boolean secure = false;

    /** A pointer to a runnable to be executed once a response is received for this httpMessage */
    private Runnable onResponse = null;

    public HttpRequest(URL url) {
        this.host = url.getHost();
        this.port = url.getPort();
        setPath(url.getPath()); // populate requestParams as well...
        this.method = Constants.POST;
        this.protocol = Constants.HTTP_11;
        setConnectionClose(); // use connection-close for outgoing requests by default
    }

    /**
     * Get the Runnable code block that should be executed upon receiving a response to
     * this HttpRequest
     * @return a Runnable code block to execute on receipt of a response to this httpMessage
     */
    public Runnable getOnResponse() {
        return onResponse;
    }

    /**
     * Set the response handler Runnable code block
     * @param onResponse the Runnable to be executed on receipt of a response to the httpMessage
     */
    public void setOnResponse(Runnable onResponse) {
        this.onResponse = onResponse;
    }

    /**
     * Set the incoming message handler
     * @param handler the incoming message handler
     */
    public void setHandler(IncomingHandler handler) {
        this.handler = handler;
    }

    public IncomingHandler getHandler() {
        return handler;
    }

    //TODO
    public HttpRequest() {
    }

    // TODO
    public HttpResponse createResponse() {
        return new HttpResponse(this);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }


    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setPath(String path) {
        this.path = path;
        requestParams = new HashMap();
        int qPos = path.indexOf("?");
        if (qPos != -1) {
            String params = path.substring(qPos+1);
            StringTokenizer st = new StringTokenizer(params, "&");
            while (st.hasMoreTokens()) {
                String kvPair = st.nextToken();
                int eqPos = kvPair.indexOf("=");
                if (eqPos != -1) {
                    requestParams.put(kvPair.substring(0, eqPos), kvPair.substring(eqPos));
                } else {
                    requestParams.put(kvPair, null);
                }
            }
        }
    }

    public Iterator getParameterNames() {
        return requestParams.keySet().iterator();
    }

    public String getParameter(String name) {
        return (String) requestParams.get(name); 
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String toStringLine() {
        return method + Constants.STRING_SP + path +
            Constants.STRING_SP + protocol + Constants.CRLF;
    }

    /**
     * Return a ByteBuffer representation of this message in HTTP wire-format
     * @return the ByteBuffer representation of this message
     */
    public ByteBuffer getBuffer() {
        return ByteBuffer.wrap(toString().getBytes());
    }

    /**
     * Does this message specify a connection-close?
     * @return true if connection should be closed
     */
    public boolean isConnectionClose() {
        String connection = (String) headers.get(Constants.CONNECTION);
        if (connection == null && Constants.HTTP_10.equals(protocol)) {
            return true;
        } else {
            return Constants.CLOSE.equals(headers.get(Constants.CONNECTION));
        }
    }

    /**
     * Causes the request to contain an empty body (i.e. for a GET etc)
     */
    public void setEmptyBody() {
        buffer.position(bodyStart);
        buffer.flip();
    }

    //------------------------------ TESTING CODE ------------------------
    /**
     * Convenience method for testing etc
     * @param body
     */
    public void setBody(String body) {
        buffer.position(bodyStart);
        buffer.put(body.getBytes());
        buffer.flip();
    }
}
