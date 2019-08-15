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
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SourceResponse {

    private Pipe pipe = null;

	/** Transport headers */
	private Map<String, TreeSet<String>> headers = new HashMap<String, TreeSet<String>>();

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

    private boolean versionChangeRequired =false;

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

        if (versionChangeRequired) {
            response.setStatusLine(version, status);
        } else if (statusLine != null) {
            response.setStatusLine(version, status, statusLine);
        } else {
            response.setStatusCode(status);
        }

        BasicHttpEntity entity = new BasicHttpEntity();

        int contentLength = -1;
    	String contentLengthHeader = null; 
        if(headers.get(HTTP.CONTENT_LEN) != null && headers.get(HTTP.CONTENT_LEN).size() > 0) {
        	contentLengthHeader = headers.get(HTTP.CONTENT_LEN).first();
        } 

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
		Set<Map.Entry<String, TreeSet<String>>> entries = headers.entrySet();

		for (Map.Entry<String, TreeSet<String>> entry : entries) {
			if (entry.getKey() != null) {
				Iterator<String> i = entry.getValue().iterator();
				while (i.hasNext()) {
					response.addHeader(entry.getKey(), i.next());
				}
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
    	if(headers.get(name) == null) {
    		TreeSet<String> values = new TreeSet<String>(); 
    		values.add(value);
    		headers.put(name, values);
    	} else {
    		TreeSet<String> values = headers.get(name);
    		values.add(value);
    	}
    }

    /**
     * Process the disable chunking option in response path.
     */
    public void processChunkingOptions(MessageContext responseMsgContext) throws IOException {
        boolean forceHttp10 = responseMsgContext.isPropertyTrue(
                PassThroughConstants.FORCE_HTTP_1_0, false);
        boolean disableChunking = responseMsgContext.isPropertyTrue(
                PassThroughConstants.DISABLE_CHUNKING, false);
        if (!forceHttp10 && !disableChunking) {
            return;
        }
        if (!responseMsgContext.isPropertyTrue(PassThroughConstants.MESSAGE_BUILDER_INVOKED, false)) {
            try {
                RelayUtils.buildMessage(responseMsgContext, false);
                responseMsgContext.getEnvelope().buildWithAttachments();
            } catch (Exception e) {
                throw new AxisFault(e.getMessage(), e);
            }
        }
        if (forceHttp10) {
            version = HttpVersion.HTTP_1_0;
            versionChangeRequired = true;
        }
        Boolean noEntityBody =
                (Boolean) responseMsgContext.getProperty(PassThroughConstants.NO_ENTITY_BODY);
        if (Boolean.TRUE.equals(noEntityBody)) {
            headers.remove(HTTP.CONTENT_TYPE);
            return;
        }
        TreeSet<String> contentLength = new TreeSet<String>();
        contentLength.add(Long.toString(getStreamLength(responseMsgContext)));
        headers.put(HTTP.CONTENT_LEN, contentLength);
    }

    /**
     * Write the stream to a temporary storage and calculate the content length
     */
    private long getStreamLength(MessageContext msgContext) throws IOException {
        CountingOutputStream counter = new CountingOutputStream(
                NullOutputStream.NULL_OUTPUT_STREAM);
        try {
            MessageProcessorSelector.getMessageFormatter(msgContext).writeTo(msgContext,
                     PassThroughTransportUtils.getOMOutputFormat(msgContext), counter, true);
        } finally {
            counter.close();
        }
        return counter.getCount();
    }

    public void setStatus(int status) {
        this.status = status;
    } 

}
