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

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

/**
 * This is a class for representing a request to be sent to a target.
 */
public class TargetRequest {

    /** Configuration of the sender */
    private TargetConfiguration targetConfiguration;

    private Pipe pipe = null;

	/** Headers map */
	private Map<String, TreeSet<String>> headers = new HashMap<String, TreeSet<String>>();

    /** URL */
    private URL url;

    /** HTTP Method */
    private String method;

    /** HTTP request created for sending the message */
    private HttpRequest request = null;

    /** Whether chunk encoding should be used */
    private boolean chunk = true;

    /** HTTP version that should be used */
    private ProtocolVersion version = null;

    /** Weather full url is used for the request */
    private boolean fullUrl = false;

    /** Port to be used for the request */
    private int port = 80;

    /** Weather this request has a body */
    private boolean hasEntityBody = true;

    /** Keep alive request */
    private boolean keepAlive = true;
    
    /**
     * Create a target request.
     *
     * @param targetConfiguration the configuration of the sender
     * @param url the url to be used
     * @param method the HTTP method
     * @param hasEntityBody weather request has an entity body
     */
    public TargetRequest(TargetConfiguration targetConfiguration, URL url,
                         String method, boolean hasEntityBody) {
        this(targetConfiguration, method, url, hasEntityBody);
    }

    public TargetRequest(TargetConfiguration targetConfiguration, String method,
                         URL url, boolean hasEntityBody) {
        this.method = method;
        this.url = url;
        this.targetConfiguration = targetConfiguration;
        this.hasEntityBody = hasEntityBody;
    }

    public void connect(Pipe pipe) {
        this.pipe = pipe;
    }

    public void start(NHttpClientConnection conn) throws IOException, HttpException {
        if (pipe != null) {
            TargetContext.get(conn).setWriter(pipe);
        }

        String path = fullUrl ? url.toString() :
                ("".equals(url.getPath()) ? "/" : url.getPath()) +
                    (url.getQuery() != null ? "?" + url.getQuery() : "");

        long contentLength = -1;
        String contentLengthHeader = null;
        
		if (headers.get(HTTP.CONTENT_LEN) != null && headers.get(HTTP.CONTENT_LEN).size() > 0) {
			contentLengthHeader = headers.get(HTTP.CONTENT_LEN).first();
		}

		if (contentLengthHeader != null) {
			contentLength = Integer.parseInt(contentLengthHeader);
			headers.remove(HTTP.CONTENT_LEN);
		}
  
        
        MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
        Long lengthValue = (Long) requestMsgCtx.getProperty(
                PassThroughConstants.PASS_THROUGH_MESSAGE_LENGTH);
        if (lengthValue != null){
        	contentLength = lengthValue;
        }

        //fix for  POST_TO_URI
        if (requestMsgCtx.isPropertyTrue(NhttpConstants.POST_TO_URI)){
        	path = url.toString();
        }

        Object o = requestMsgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (o != null && o instanceof Map) {
            Map _headers = (Map) o;
            String trpContentType = (String) _headers.get(HTTP.CONTENT_TYPE);
            if (trpContentType != null && !trpContentType.equals("")) {
                if (trpContentType.contains(PassThroughConstants.CONTENT_TYPE_MULTIPART_RELATED) &&
                    !requestMsgCtx.isPropertyTrue(PassThroughConstants.MESSAGE_BUILDER_INVOKED)) {
                    // If the message is multipart/related but it hasn't been built
                    // we can copy the content-type header of the request
                	addHeader(HTTP.CONTENT_TYPE, trpContentType);
                }
            }
        }
                                                            
        if (hasEntityBody) {
            request = new BasicHttpEntityEnclosingRequest(method, path,
                    version != null ? version : HttpVersion.HTTP_1_1);

            BasicHttpEntity entity = new BasicHttpEntity();

            if (requestMsgCtx.isPropertyTrue(NhttpConstants.FORCE_HTTP_CONTENT_LENGTH)) {
                entity.setChunked(false);
                if (requestMsgCtx.isPropertyTrue(PassThroughConstants.COPY_CONTENT_LENGTH_FROM_INCOMING)
                        && contentLength > 0) {
                    entity.setContentLength(contentLength);
                }
            } else {
                if (contentLength != -1) {
                    entity.setChunked(false);
                    entity.setContentLength(contentLength);
                } else {
                    entity.setChunked(chunk);
                }
            }
            ((BasicHttpEntityEnclosingRequest) request).setEntity(entity);
           
        } else {
            request = new BasicHttpRequest(method, path,
                    version != null ? version : HttpVersion.HTTP_1_1);
        }

        
		Set<Map.Entry<String, TreeSet<String>>> entries = headers.entrySet();
		for (Map.Entry<String, TreeSet<String>> entry : entries) {
			if (entry.getKey() != null) {
				Iterator<String> i = entry.getValue().iterator();
				while (i.hasNext()) {
					request.addHeader(entry.getKey(), i.next());
				}
			}
		}
        
        //setup wsa action..
        if (request != null){
        	
    		String soapAction = requestMsgCtx.getSoapAction();
            if (soapAction == null) {
                soapAction = requestMsgCtx.getWSAAction();
            }
            if (soapAction == null) {
            	requestMsgCtx.getAxisOperation().getInputAction();
            }

            if (requestMsgCtx.isSOAP11() && soapAction != null && soapAction.length() > 0) {
                Header existingHeader = request.getFirstHeader(HTTPConstants.HEADER_SOAP_ACTION);
                if (existingHeader != null) {
                	request.removeHeader(existingHeader);
                }
                MessageFormatter messageFormatter =
                    MessageFormatterDecoratorFactory.createMessageFormatterDecorator(requestMsgCtx);
                request.setHeader(HTTPConstants.HEADER_SOAP_ACTION,
                        messageFormatter.formatSOAPAction(requestMsgCtx, null, soapAction));
            }
    	}

        this.processChunking(conn, requestMsgCtx);

        if (!keepAlive) {
            request.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        }

        // Pre-process HTTP request
        HttpContext context = conn.getContext();
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, new HttpHost(url.getHost(), port));
        context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);

        // start the request
        targetConfiguration.getHttpProcessor().process(request, context);
        conn.submitRequest(request);

        if (hasEntityBody) {
            TargetContext.updateState(conn, ProtocolState.REQUEST_HEAD);
        } else {
            TargetContext.updateState(conn, ProtocolState.REQUEST_DONE);
        }
    }

    
	public void addHeader(String name, String value) {
		if (headers.get(name) == null) {
			TreeSet<String> values = new TreeSet<String>();
			values.add(value);
			if (HTTP.CONTENT_TYPE.equalsIgnoreCase(name)) {
				headers.put(HTTP.CONTENT_TYPE, values);
			} else {
				headers.put(name, values);
			}
		} else {
			if (HTTP.CONTENT_TYPE.equalsIgnoreCase(name)) {
				headers.remove(HTTP.CONTENT_TYPE);
				TreeSet<String> values = new TreeSet<String>();
				values.add(value);
				headers.put(HTTP.CONTENT_TYPE, values);
			} else {
				TreeSet<String> values = headers.get(name);
				values.add(value);
			}
		}
	}
    
	/**
	 * Handles the chunking messages in PassThough context, create a temporary buffer and
     * calculate the message size before writing to the external buffer, which is required the
     * context of handling DISABLED chunking messages.
	 * 
	 * @param conn Client HTTP connection
	 * @param requestMsgCtx Current request message context

	 * @throws IOException
	 */
	private void processChunking(NHttpClientConnection conn,
                                 MessageContext requestMsgCtx) throws IOException {

		String disableChunking = (String) requestMsgCtx.getProperty(
                PassThroughConstants.DISABLE_CHUNKING);
		String forceHttp10 = (String) requestMsgCtx.getProperty(PassThroughConstants.FORCE_HTTP_1_0);
	    if ("true".equals(disableChunking) || "true".equals(forceHttp10)) {
	    	if (requestMsgCtx.getEnvelope().getBody().getFirstElement() == null) {
				BasicHttpEntity entity = (BasicHttpEntity) ((BasicHttpEntityEnclosingRequest) request).getEntity();    
				try {
					RelayUtils.buildMessage(requestMsgCtx);
					this.hasEntityBody = true;
					Pipe pipe = (Pipe) requestMsgCtx.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
					if (pipe != null) {
						pipe.attachConsumer(conn);
						this.connect(pipe);
						if (Boolean.TRUE.equals(requestMsgCtx.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							MessageFormatter formatter =  MessageProcessorSelector.getMessageFormatter(requestMsgCtx);
							OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(requestMsgCtx);
							formatter.writeTo(requestMsgCtx, format, out, false);
							OutputStream _out = pipe.getOutputStream();
							IOUtils.write(out.toByteArray(), _out);
						
							entity.setContentLength(out.toByteArray().length);
							entity.setChunked(false);
						}
					}
					// pipe.setSerializationComplete(true);
				} catch (XMLStreamException e) {
					 e.printStackTrace();
				
				}
			}

		}  
    }

    /**
     * Consume the data from the pipe and write it to the wire.
     *
     * @param conn the connection to the target
     * @param encoder encoder for writing the message through
     * @throws java.io.IOException if an error occurs
     * @return number of bytes written
     */
    public int write(NHttpClientConnection conn, ContentEncoder encoder) throws IOException {
        int bytes = 0;
        if (pipe != null) {
            bytes = pipe.consume(encoder);
        }

        if (encoder.isCompleted()) {
            targetConfiguration.getMetrics().
                    notifySentMessageSize(conn.getMetrics().getSentBytesCount());

            TargetContext.updateState(conn, ProtocolState.REQUEST_DONE);
        }
        
        return bytes;

    }

    public boolean hasEntityBody() {
        return hasEntityBody;
    }


    public String getMethod() {
        return method;
    }

    public void setChunk(boolean chunk) {
        this.chunk = chunk;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setFullUrl(boolean fullUrl) {
        this.fullUrl = fullUrl;
    }

    public void setVersion(ProtocolVersion version) {
        this.version = version;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

	public HttpRequest getRequest() {
		return request;
	}

}
