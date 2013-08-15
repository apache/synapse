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

package org.apache.synapse.transport.passthru;

import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourceResponse {

    private Pipe pipe = null;

    /** Transport headers */
    private Map<String, String> headers = new HashMap<String, String>();

    /** Status of the response */
    private int status = HttpStatus.SC_OK;

    /** Status line */
    private String statusLine = null;

    /** Actual response submitted */
    private HttpResponse response = null;

    /** Configuration of the receiver */
    private SourceConfiguration sourceConfiguration;

    /** Version of the response */
    private ProtocolVersion version = HttpVersion.HTTP_1_1;

    private SourceRequest request = null;

    public SourceResponse(SourceConfiguration config, int status, SourceRequest request) {
        this(config, status, null, request);
    }

    public SourceResponse(SourceConfiguration config, int status, String statusLine,
                          SourceRequest request) {
        this.status = status;
        this.statusLine = statusLine;
        this.sourceConfiguration = config;
        this.request = request;
    }

    public void connect(Pipe pipe) {
        this.pipe = pipe;

        if (request != null && pipe != null) {
            SourceContext.get(request.getConnection()).setWriter(pipe);
        }
    }

    /**
     * Starts the response by writing the headers
     * @param conn connection
     * @throws java.io.IOException if an error occurs
     * @throws org.apache.http.HttpException if an error occurs
     */
    public void start(NHttpServerConnection conn) throws IOException, HttpException {
        // create the response
        response = sourceConfiguration.getResponseFactory().newHttpResponse(
                request.getVersion(), HttpStatus.SC_OK,
                request.getConnection().getContext());

        if (statusLine != null) {
            response.setStatusLine(version, status, statusLine);
        } else {
            response.setStatusCode(status);
        }

        BasicHttpEntity entity = new BasicHttpEntity();

        int contentLength = -1;
        String contentLengthHeader = headers.get(HTTP.CONTENT_LEN);
        if (contentLengthHeader != null) {
            contentLength = Integer.parseInt(contentLengthHeader);

            headers.remove(HTTP.CONTENT_LEN);
        }

        if (contentLength != -1) {
            entity.setChunked(false);
            entity.setContentLength(contentLength);
        } else {
            entity.setChunked(true);
        }

        response.setEntity(entity);

        // set any transport headers
        Set<Map.Entry<String, String>> entries = headers.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey() != null) {
                response.addHeader(entry.getKey(), entry.getValue());
            }
        }

        SourceContext.updateState(conn, ProtocolState.RESPONSE_HEAD);

        // Pre-process HTTP response
        HttpContext context = conn.getContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        context.setAttribute(HttpCoreContext.HTTP_RESPONSE, response);
        context.setAttribute(HttpCoreContext.HTTP_REQUEST,
                SourceContext.getRequest(conn).getRequest());
        
        sourceConfiguration.getHttpProcessor().process(response, context);
        conn.submitResponse(response);        
    }

    /**
     * Consume the content through the Pipe and write them to the wire
     * @param conn connection
     * @param encoder encoder
     * @throws java.io.IOException if an error occurs
     * @return number of bytes written
     */
    public int write(NHttpServerConnection conn, ContentEncoder encoder) throws IOException {        
        int bytes = 0;
        if (pipe != null) {
            bytes = pipe.consume(encoder);
        } else {
            encoder.complete();
        }
        // Update connection state
        if (encoder.isCompleted()) {
            SourceContext.updateState(conn, ProtocolState.RESPONSE_DONE);
            sourceConfiguration.getMetrics().notifySentMessageSize(
                    conn.getMetrics().getSentBytesCount());
            PassThroughTransportUtils.finishUsingSourceConnection(response, conn,
                    sourceConfiguration.getSourceConnections());
        }
        return bytes;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public void setStatus(int status) {
        this.status = status;
    }        
}
