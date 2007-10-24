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
package org.apache.synapse.transport.nhttp;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.synapse.transport.nhttp.util.PipeImpl;
import org.apache.synapse.transport.nhttp.util.WorkerPool;
import org.apache.synapse.transport.nhttp.util.WorkerPoolFactory;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.*;
import org.apache.http.util.EncodingUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * The server connection handler. An instance of this class is used by each IOReactor, to
 * process every connection. Hence this class should not store any data related to a single
 * connection - as this is being shared.
 */
public class ServerHandler implements NHttpServiceHandler {

    private static final Log log = LogFactory.getLog(ServerHandler.class);

    /** the HTTP protocol parameters to adhere to */
    private final HttpParams params;
    /** the factory to create HTTP responses */
    private final HttpResponseFactory responseFactory;
    /** the HTTP response processor */
    private final HttpProcessor httpProcessor;
    /** the strategy to re-use connections */
    private final ConnectionReuseStrategy connStrategy;

    /** the Axis2 configuration context */
    ConfigurationContext cfgCtx = null;
    /** the nhttp configuration */
    private NHttpConfiguration cfg = null;
    /** is this https? */
    private boolean isHttps = false;

    /** the thread pool to process requests */
    private WorkerPool workerPool = null;

    private static final String REQUEST_SINK_CHANNEL = "request-sink-channel";
    private static final String RESPONSE_SOURCE_CHANNEL = "response-source-channel";
    private static final String REQUEST_BUFFER = "request-buffer";
    private static final String RESPONSE_BUFFER = "response-buffer";

    public ServerHandler(final ConfigurationContext cfgCtx, final HttpParams params,
        final boolean isHttps) {
        super();
        this.cfgCtx = cfgCtx;
        this.params = params;
        this.isHttps = isHttps;
        this.responseFactory = new DefaultHttpResponseFactory();
        this.httpProcessor = getHttpProcessor();
        this.connStrategy = new DefaultConnectionReuseStrategy();

        this.cfg = NHttpConfiguration.getInstance();
        this.workerPool = WorkerPoolFactory.getWorkerPool(
            cfg.getServerCoreThreads(),
            cfg.getServerMaxThreads(),
            cfg.getServerKeepalive(),
            cfg.getServerQueueLen(),
            "Server Worker thread group", "HttpServerWorker");
    }

    /**
     * Process a new incoming request
     * @param conn the connection
     */
    public void requestReceived(final NHttpServerConnection conn) {

        HttpContext context = conn.getContext();
        HttpRequest request = conn.getHttpRequest();
        context.setAttribute(ExecutionContext.HTTP_REQUEST, request);

        // allocate temporary buffers to process this request
        context.setAttribute(REQUEST_BUFFER, ByteBuffer.allocate(cfg.getBufferZise()));
        context.setAttribute(RESPONSE_BUFFER, ByteBuffer.allocate(cfg.getBufferZise()));

        try {
            PipeImpl requestPipe  = new PipeImpl(); // the pipe used to process the request
            PipeImpl responsePipe = new PipeImpl(); // the pipe used to process the response
            context.setAttribute(REQUEST_SINK_CHANNEL, requestPipe.sink());
            context.setAttribute(RESPONSE_SOURCE_CHANNEL, responsePipe.source());

            // create the default response to this request
            ProtocolVersion httpVersion = request.getRequestLine().getProtocolVersion();
            HttpResponse response = responseFactory.newHttpResponse(
                httpVersion, HttpStatus.SC_OK, context);
            response.setParams(this.params);

            // create a basic HttpEntity using the source channel of the response pipe
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(Channels.newInputStream(responsePipe.source()));
            if (httpVersion.greaterEquals(HttpVersion.HTTP_1_1)) {
                entity.setChunked(true);
            }
            response.setEntity(entity);

            // hand off processing of the request to a thread off the pool
            workerPool.execute(
                new ServerWorker(cfgCtx, conn, isHttps, this,
                    request, Channels.newInputStream(requestPipe.source()),
                    response, Channels.newOutputStream(responsePipe.sink())));

        } catch (IOException e) {
            handleException("Error processing request received for : " +
                request.getRequestLine().getUri(), e, conn);
        } catch (Exception e) {
            handleException("Error processing request received for : " +
                request.getRequestLine().getUri(), e, conn);
        }
    }

    /**
     * Process ready input by writing it into the Pipe
     * @param conn the connection being processed
     * @param decoder the content decoder in use
     */
    public void inputReady(final NHttpServerConnection conn, final ContentDecoder decoder) {

        HttpContext context = conn.getContext();
        WritableByteChannel sink = (WritableByteChannel) context.getAttribute(REQUEST_SINK_CHANNEL);
        ByteBuffer inbuf = (ByteBuffer) context.getAttribute(REQUEST_BUFFER);

        try {
            while (decoder.read(inbuf) > 0) {
                inbuf.flip();
                sink.write(inbuf);
                inbuf.compact();
            }

            if (decoder.isCompleted()) {
                sink.close();
            }

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        }
    }

    public void responseReady(NHttpServerConnection conn) {
        // New API method - should not require
    }

    /**
     * Process ready output by writing into the channel
     * @param conn the connection being processed
     * @param encoder the content encoder in use
     */
    public void outputReady(final NHttpServerConnection conn, final ContentEncoder encoder) {

        HttpContext context = conn.getContext();
        HttpResponse response = conn.getHttpResponse();
        ReadableByteChannel source = (ReadableByteChannel) context.getAttribute(RESPONSE_SOURCE_CHANNEL);
        ByteBuffer outbuf = (ByteBuffer) context.getAttribute(RESPONSE_BUFFER);

        try {
            int bytesRead = source.read(outbuf);
            if (bytesRead == -1 && outbuf.position() == 0) {
                encoder.complete();
            } else {
                outbuf.flip();
                encoder.write(outbuf);
                outbuf.compact();
            }

            if (encoder.isCompleted()) {
                source.close();
                if (!connStrategy.keepAlive(response, context)) {
                    conn.close();
                }
            }

        } catch (IOException e) {
            handleException("I/O Error : " + e.getMessage(), e, conn);
        }
    }

    /**
     * Commit the response to the connection. Processes the response through the configured
     * HttpProcessor and submits it to be sent out
     * @param conn the connection being processed
     * @param response the response to commit over the connection
     */
    public void commitResponse(final NHttpServerConnection conn, final HttpResponse response) {
        try {
            httpProcessor.process(response, conn.getContext());
            conn.submitResponse(response);
        } catch (HttpException e) {
            handleException("Unexpected HTTP protocol error : " + e.getMessage(), e, conn);
        } catch (IOException e) {
            handleException("IO error submiting response : " + e.getMessage(), e, conn);
        }
    }


    /**
     * Handle connection timeouts by shutting down the connections
     * @param conn the connection being processed
     */
    public void timeout(final NHttpServerConnection conn) {
        HttpRequest req = (HttpRequest) conn.getContext().getAttribute(
                ExecutionContext.HTTP_REQUEST);
        if (req != null) {
            if (log.isDebugEnabled()) {
                log.debug("Connection Timeout for request to : " + req.getRequestLine().getUri() +
                        " Probably the keepalive connection was closed");
            }
        } else {
            log.warn("Connection Timeout");
        }
        shutdownConnection(conn);
    }

    public void connected(final NHttpServerConnection conn) {
        log.trace("New incoming connection");
    }

    public void closed(final NHttpServerConnection conn) {
        log.trace("Connection closed");
    }

    /**
     * Handle HTTP Protocol violations with an error response
     * @param conn the connection being processed
     * @param e the exception encountered
     */
    public void exception(final NHttpServerConnection conn, final HttpException e) {
        HttpContext context = conn.getContext();
        HttpRequest request = conn.getHttpRequest();
        ProtocolVersion ver = request.getRequestLine().getProtocolVersion();
        HttpResponse response = responseFactory.newHttpResponse(
            ver, HttpStatus.SC_BAD_REQUEST, context);
        byte[] msg = EncodingUtils.getAsciiBytes("Malformed HTTP request: " + e.getMessage());
        ByteArrayEntity entity = new ByteArrayEntity(msg);
        entity.setContentType("text/plain; charset=US-ASCII");
        response.setEntity(entity);
        commitResponse(conn, response);
    }

    /**
     * Handle IO errors while reading or writing to underlying channels
     * @param conn the connection being processed
     * @param e the exception encountered
     */
    public void exception(NHttpServerConnection conn, IOException e) {
        if (e instanceof ConnectionClosedException ||
                e.getMessage().contains("Connection reset by peer") ||
                e.getMessage().contains("forcibly closed")) {
            if (log.isDebugEnabled()) {
                log.debug("I/O error (Probably the keepalive connection " +
                        "was closed):" + e.getMessage());
            }
        } else {
            log.error("I/O error: " + e.getMessage());
        }
        shutdownConnection(conn);
    }

    // ----------- utility methods -----------

    private void handleException(String msg, Exception e, NHttpServerConnection conn) {
        log.error(msg, e);
        if (conn != null) {
            shutdownConnection(conn);
        }
    }

    /**
     * Shutdown the connection ignoring any IO errors during the process
     * @param conn the connection to be shutdown
     */
    private void shutdownConnection(final HttpConnection conn) {
        try {
            conn.shutdown();
        } catch (IOException ignore) {}
    }

    /**
     * Return the HttpProcessor for responses
     * @return the HttpProcessor that processes HttpResponses of this server
     */
    private HttpProcessor getHttpProcessor() {
        BasicHttpProcessor httpProcessor = new BasicHttpProcessor();
        httpProcessor.addInterceptor(new ResponseDate());
        httpProcessor.addInterceptor(new ResponseServer());
        httpProcessor.addInterceptor(new ResponseContent());
        httpProcessor.addInterceptor(new ResponseConnControl());
        return httpProcessor;
    }
}
