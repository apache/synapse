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
import org.apache.axis2.transport.base.MetricsCollector;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.ContentOutputBuffer;
import org.apache.http.nio.util.ContentInputBuffer;
import org.apache.http.nio.util.SharedInputBuffer;
import org.apache.http.nio.util.SharedOutputBuffer;
import org.apache.http.nio.entity.ContentInputStream;
import org.apache.http.nio.entity.ContentOutputStream;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.*;
import org.apache.http.util.EncodingUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    /** the buffer allocator */
    private final ByteBufferAllocator allocator;

    /** the Axis2 configuration context */
    ConfigurationContext cfgCtx = null;
    /** the nhttp configuration */
    private NHttpConfiguration cfg = null;
    /** is this https? */
    private boolean isHttps = false;

    /** the thread pool to process requests */
    private WorkerPool workerPool = null;
    /** the metrics collector */
    private MetricsCollector metrics = null;

    public static final String REQUEST_SINK_BUFFER = "synapse.request-sink-buffer";
    public static final String RESPONSE_SOURCE_BUFFER = "synapse.response-source-buffer";

    public ServerHandler(final ConfigurationContext cfgCtx, final HttpParams params,
        final boolean isHttps, final MetricsCollector metrics) {
        super();
        this.cfgCtx = cfgCtx;
        this.params = params;
        this.isHttps = isHttps;
        this.metrics = metrics;
        this.responseFactory = new DefaultHttpResponseFactory();
        this.httpProcessor = getHttpProcessor();
        this.connStrategy = new DefaultConnectionReuseStrategy();
        this.allocator = new HeapByteBufferAllocator();

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

        // Mark request as not yet fully read, to detect timeouts from harmless keepalive deaths
        conn.getContext().setAttribute(NhttpConstants.REQUEST_READ, Boolean.FALSE);

        try {
            InputStream is;
            // Only create an input buffer and ContentInputStream if the request has content
            if (request instanceof HttpEntityEnclosingRequest) {
                ContentInputBuffer inputBuffer = new SharedInputBuffer(cfg.getBufferSize(), conn, allocator);
                context.setAttribute(REQUEST_SINK_BUFFER, inputBuffer);
                is = new ContentInputStream(inputBuffer);
            } else {
                is = null;
            }
            
            ContentOutputBuffer outputBuffer = new SharedOutputBuffer(cfg.getBufferSize(), conn, allocator);
            context.setAttribute(RESPONSE_SOURCE_BUFFER, outputBuffer);
            OutputStream os = new ContentOutputStream(outputBuffer);

            // create the default response to this request
            ProtocolVersion httpVersion = request.getRequestLine().getProtocolVersion();
            HttpResponse response = responseFactory.newHttpResponse(
                httpVersion, HttpStatus.SC_OK, context);
            response.setParams(this.params);

            // create a basic HttpEntity using the source channel of the response pipe
            BasicHttpEntity entity = new BasicHttpEntity();
            if (httpVersion.greaterEquals(HttpVersion.HTTP_1_1)) {
                entity.setChunked(true);
            }
            response.setEntity(entity);

            // hand off processing of the request to a thread off the pool
            workerPool.execute(
                new ServerWorker(cfgCtx, conn, isHttps, metrics, this,
                    request, is, response, os));

        } catch (Exception e) {
            if (metrics != null) {
                metrics.incrementFaultsReceiving();
            }
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
        ContentInputBuffer inBuf = (ContentInputBuffer) context.getAttribute(REQUEST_SINK_BUFFER);

        try {
            int bytesRead = inBuf.consumeContent(decoder);
            if (metrics != null && bytesRead > 0) {
                metrics.incrementBytesReceived(bytesRead);
            }

            if (decoder.isCompleted()) {
                if (metrics != null) {
                    metrics.incrementMessagesReceived();
                }
                // remove the request we have fully read, to detect harmless keepalive timeouts from
                // real timeouts while reading requests
                context.setAttribute(NhttpConstants.REQUEST_READ, Boolean.TRUE);
            }

        } catch (IOException e) {
            if (metrics != null) {
                metrics.incrementFaultsReceiving();
            }
            handleException("I/O Error at inputReady : " + e.getMessage(), e, conn);
        }
    }

    /**
     * Process ready output by writing into the channel
     * @param conn the connection being processed
     * @param encoder the content encoder in use
     */
    public void outputReady(final NHttpServerConnection conn, final ContentEncoder encoder) {

        HttpContext context = conn.getContext();
        HttpResponse response = conn.getHttpResponse();
        ContentOutputBuffer outBuf = (ContentOutputBuffer) context.getAttribute(RESPONSE_SOURCE_BUFFER);

        try {
            int bytesWritten = outBuf.produceContent(encoder);
            if (metrics != null && bytesWritten > 0) {
                metrics.incrementBytesSent(bytesWritten);
            }

            if (encoder.isCompleted()) {
                Boolean reqRead = (Boolean) conn.getContext().getAttribute(NhttpConstants.REQUEST_READ);
                if (reqRead != null && !reqRead) {
                    try {
                        // this is a connection we should not re-use
                        conn.close();
                    } catch (Exception ignore) {}
                } else if (!connStrategy.keepAlive(response, context)) {
                    conn.close();
                } else {
                    conn.requestInput();
                }
            }

        } catch (IOException e) {
            if (metrics != null) {
                metrics.incrementFaultsSending();
            }
            handleException("I/O Error at outputReady : " + e.getMessage(), e, conn);
        }
    }

    /**
     * Commit the response to the connection. Processes the response through the configured
     * HttpProcessor and submits it to be sent out. This method hides any exceptions and is targetted
     * for non critical (i.e. browser requests etc) requests, which are not core messages
     * @param conn the connection being processed
     * @param response the response to commit over the connection
     */
    public void commitResponseHideExceptions(final NHttpServerConnection conn, final HttpResponse response) {
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
     * Commit the response to the connection. Processes the response through the configured
     * HttpProcessor and submits it to be sent out. Re-Throws exceptions, after closing connections
     * @param conn the connection being processed
     * @param response the response to commit over the connection
     * @throws IOException
     * @throws HttpException
     */
    public void commitResponse(final NHttpServerConnection conn,
        final HttpResponse response) throws IOException, HttpException {
        try {
            httpProcessor.process(response, conn.getContext());
            conn.submitResponse(response);
        } catch (HttpException e) {
            shutdownConnection(conn);
            throw e;
        } catch (IOException e) {
            shutdownConnection(conn);
            throw e;
        }
    }

    /**
     * Handle connection timeouts by shutting down the connections
     * @param conn the connection being processed
     */
    public void timeout(final NHttpServerConnection conn) {
        HttpContext context = conn.getContext();
        Boolean read = (Boolean) context.getAttribute(NhttpConstants.REQUEST_READ);

        if (read != null && read) {
            if (log.isDebugEnabled()) {
                log.debug("Keepalive connection was closed");
            }
        } else {
            log.error("Connection Timeout - before message body was fully read : " + conn);
            if (metrics != null) {
                metrics.incrementTimeoutsReceiving();
            }
        }
        shutdownConnection(conn);
    }

    public void connected(final NHttpServerConnection conn) {
        if (log.isTraceEnabled()) {
            log.trace("New incoming connection");
        }
    }

    public void responseReady(NHttpServerConnection conn) {

        metrics.notifyReceivedMessageSize(conn.getMetrics().getReceivedBytesCount());
        metrics.notifySentMessageSize(conn.getMetrics().getSentBytesCount());
        conn.getMetrics().reset();

        if (log.isTraceEnabled()) {
            log.trace("Ready to send response");
        }
    }

    public void closed(final NHttpServerConnection conn) {

        HttpContext context = conn.getContext();
        shutdownConnection(conn);
        context.removeAttribute(REQUEST_SINK_BUFFER);
        context.removeAttribute(RESPONSE_SOURCE_BUFFER);

        if (log.isTraceEnabled()) {
            log.trace("Connection closed");
        }
    }

    /**
     * Handle HTTP Protocol violations with an error response
     * @param conn the connection being processed
     * @param e the exception encountered
     */
    public void exception(final NHttpServerConnection conn, final HttpException e) {
        if (metrics != null) {
            metrics.incrementFaultsReceiving();
        }

        HttpContext context = conn.getContext();
        HttpRequest request = conn.getHttpRequest();
        ProtocolVersion ver = HttpVersion.HTTP_1_0;
        if (request != null && request.getRequestLine() != null) {
            ver = request.getRequestLine().getProtocolVersion();
        }
        HttpResponse response = responseFactory.newHttpResponse(
            ver, HttpStatus.SC_BAD_REQUEST, context);

        byte[] msg = EncodingUtils.getAsciiBytes("Malformed HTTP request: " + e.getMessage());
        ByteArrayEntity entity = new ByteArrayEntity(msg);
        entity.setContentType("text/plain; charset=US-ASCII");
        response.setEntity(entity);
        try {
            commitResponseHideExceptions(conn, response);
        } catch (Exception ignore) {}        
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
            String msg = e.getMessage().toLowerCase();
            if (msg.indexOf("broken") != -1) {
                log.warn("I/O error (Probably the connection " +
                        "was closed by the remote party):" + e.getMessage());
            } else {
                log.error("I/O error: " + e.getMessage(), e);
            }
            if (metrics != null) {
                metrics.incrementFaultsReceiving();
            }
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
    private void shutdownConnection(final NHttpServerConnection conn) {
        SharedOutputBuffer outputBuffer = (SharedOutputBuffer)
            conn.getContext().getAttribute(RESPONSE_SOURCE_BUFFER);
        if (outputBuffer != null) {
            outputBuffer.close();
        }
        SharedInputBuffer inputBuffer = (SharedInputBuffer)
            conn.getContext().getAttribute(REQUEST_SINK_BUFFER);
        if (inputBuffer != null) {
            inputBuffer.close();
        }
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

    public int getActiveCount() {
        return workerPool.getActiveCount();
    }

    public int getQueueSize() {
        return workerPool.getQueueSize();
    }

    public MetricsCollector getMetrics() {
        return metrics;
    }

    public void stop() {
        try {
            workerPool.shutdown(1000);
        } catch (InterruptedException ignore) {}
    }
}
