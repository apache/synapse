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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.passthru.util.TargetRequestFactory;

import java.io.OutputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class acts as a gateway for differed delivery of the messages. When a message is to be
 * delivered it is submitted to this class. If a connection is available to the target this
 * class will try to deliver the message immediately over that connection. If a connection is
 * not available it will queue the message and request a connection from the pool. When a new
 * connection is available a queued message will be sent through it. 
 */
public class DeliveryAgent {

    private static final Log log = LogFactory.getLog(DeliveryAgent.class);

    /**
     * This Map holds the messages that need to be delivered. But at the moment maximum
     * number of connections to the host:pair is being used. So these messages has to wait
     * until a new connection is available.
     */
    private Map<String, Queue<MessageContext>> waitingMessages =
            new ConcurrentHashMap<String, Queue<MessageContext>>();

    /** The connection management */
    private TargetConnections targetConnections;

    /** Configuration of the sender */
    private TargetConfiguration targetConfiguration;

    private TargetErrorHandler targetErrorHandler;

    /** Lock for synchronizing access */
    private Lock lock = new ReentrantLock();

    /**
     * Create a delivery agent with the target configuration and connection management.
     *
     * @param targetConfiguration configuration of the sender
     * @param targetConnections connection management
     */
    public DeliveryAgent(TargetConfiguration targetConfiguration,
                         TargetConnections targetConnections) {
        this.targetConfiguration = targetConfiguration;
        this.targetConnections = targetConnections;
        this.targetErrorHandler = new TargetErrorHandler(targetConfiguration);
    }


    /**
     * This method queues the message for delivery. If a connection is already existing for
     * the destination epr, the message will be delivered immediately. Otherwise message has
     * to wait until a connection is established. In this case this method will inform the
     * system about the need for a connection.
     *
     * @param msgContext the message context to be sent
     * @param host host name of epr
     * @param port port of the of epr
     * @throws AxisFault if an error occurs
     */
    public void submit(MessageContext msgContext, String host, int port)
            throws AxisFault {

            String key = host + ":" + port;

            // first we queue the message
            Queue<MessageContext> queue = null;
            lock.lock();
            try {
                queue = waitingMessages.get(key);
                if (queue == null) {
                    queue = new ConcurrentLinkedQueue<MessageContext>();
                    waitingMessages.put(key, queue);
                }
                if (queue.size() == Integer.MAX_VALUE) {
                    MessageContext msgCtx = queue.poll();
                    targetErrorHandler.handleError(msgCtx,
                            ErrorCodes.CONNECTION_TIMEOUT,
                            "Error connecting to the back end",
                            null,
                            ProtocolState.REQUEST_READY);
                }

                queue.add(msgContext);
            } finally {
                lock.unlock();
            }

            NHttpClientConnection conn = targetConnections.getConnection(host, port);
            if (conn != null) {
            	conn.resetInput();
            	conn.resetOutput();
                MessageContext messageContext = queue.poll();

                if (messageContext != null) {
                    tryNextMessage(messageContext, conn);
                }
            }
    }

    public void errorConnecting(String host, int port, int errorCode, String message) {
        String key = host + ":" + port;

        Queue<MessageContext> queue = waitingMessages.get(key);
        if (queue != null) {
            MessageContext msgCtx = queue.poll();

            if (msgCtx != null) {
                String errorMessage = "Error while connecting to the endpoint";
                if (message != null) {
                    errorMessage += " (" + message + ")";
                }
                targetErrorHandler.handleError(msgCtx, errorCode, errorMessage,
                        null, ProtocolState.REQUEST_READY);
                synchronized (msgCtx) {
                    msgCtx.setProperty(PassThroughConstants.WAIT_BUILDER_IN_STREAM_COMPLETE,
                                       Boolean.TRUE);
                    msgCtx.notifyAll();
                }
            }
        } else {
            throw new IllegalStateException("Queue cannot be null for: " + key);
        }
    }

    /**
     * Notification for a connection availability. When this occurs a message in the
     * queue for delivery will be tried.
     *
     * @param host name of the remote host
     * @param port remote port number
     * @param conn connection made available to process the request
     */
    public void connected(String host, int port, NHttpClientConnection conn) {
        Queue<MessageContext> queue = null;
        lock.lock();
        try {
            queue = waitingMessages.get(host + ":" + port);
        } finally {
            lock.unlock();
        }

        while (queue.size() > 0) {
            if (conn == null) {
                // Try to get an existing connection from pool. Here we should not ask to create
                // new connections as it may ended up with extra connections. New connections are
                // created upon request submission.
                conn = targetConnections.getExistingConnection(host, port);
            }
            if (conn != null) {
                MessageContext messageContext = queue.poll();

                if (messageContext != null) {
                    tryNextMessage(messageContext, conn);
                }
                conn = null;
            } else {
                break;
            }
        }
    }

    private void tryNextMessage(MessageContext messageContext, NHttpClientConnection conn) {
        if (conn != null) {
            try {
                TargetContext.get(conn).setRequestMsgCtx(messageContext);

                submitRequest(conn, messageContext);
            } catch (AxisFault e) {
                log.error("IO error while sending the request out", e);
            }
        }
    }

    private void submitRequest(NHttpClientConnection conn, MessageContext msgContext) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Submitting new request to the connection: " + conn);
        }

        TargetRequest request = TargetRequestFactory.create(msgContext, targetConfiguration);
        TargetContext.setRequest(conn, request);

        Pipe pipe = (Pipe) msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        if (pipe != null) {
            pipe.attachConsumer(conn);
            request.connect(pipe);
            if (Boolean.TRUE.equals(msgContext.getProperty(
                    PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
                synchronized (msgContext) {
                    OutputStream out = pipe.getOutputStream();
                    msgContext.setProperty(PassThroughConstants.BUILDER_OUTPUT_STREAM, out);
                    msgContext.setProperty(PassThroughConstants.WAIT_BUILDER_IN_STREAM_COMPLETE, Boolean.TRUE);
                    msgContext.notifyAll();
                }
                return;
            }
        }

        conn.requestOutput();
    }    

}
