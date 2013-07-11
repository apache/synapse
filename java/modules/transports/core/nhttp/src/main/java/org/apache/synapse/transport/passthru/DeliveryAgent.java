/*
 *  Copyright 2013 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.passthru.util.TargetRequestFactory;

import java.io.OutputStream;
import java.util.Queue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.URL;
import java.net.MalformedURLException;
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

    /** The maximum number of messages that can wait for a connection */
    private int maxWaitingMessages = Integer.MAX_VALUE;

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
     * @param epr the endpoint to which the message should be sent
     * @throws AxisFault if an error occurs
     */
    public void submit(MessageContext msgContext, EndpointReference epr)
            throws AxisFault {
        try {
            URL url = new URL(epr.getAddress());
            String host = url.getHost();
            int port = url.getPort();
            if (port == -1) {
                // use default
                if ("http".equals(url.getProtocol())) {
                    port = 80;
                } else if ("https".equals(url.getProtocol())) {
                    port = 443;
                }
            }

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
                if (queue.size() == maxWaitingMessages) {
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
                MessageContext messageContext = queue.poll();

                if (messageContext != null) {
                    tryNextMessage(messageContext, conn);
                }
            }

        } catch (MalformedURLException e) {
            handleException("Malformed URL in the target EPR", e);
        }
    }

    public void errorConnecting(String host, int port, int errorCode, String message) {
        String key = host + ":" + port;

        Queue<MessageContext> queue = waitingMessages.get(key);
        if (queue != null) {
            MessageContext msgCtx = queue.poll();

            if (msgCtx != null) {
                targetErrorHandler.handleError(msgCtx,
                        errorCode,
                        "Error connecting to the back end",
                        null,
                        ProtocolState.REQUEST_READY);
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
     */
    public void connected(String host, int port) {
        Queue<MessageContext> queue = null;
        lock.lock();
        try {
            queue = waitingMessages.get(host + ":" + port);
        } finally {
            lock.unlock();
        }

        while (queue.size() > 0) {
            NHttpClientConnection conn = targetConnections.getConnection(host, port);
            if (conn != null) {
                MessageContext messageContext = queue.poll();

                if (messageContext != null) {
                    tryNextMessage(messageContext, conn);
                }
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
            if (Boolean.TRUE.equals(msgContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
                synchronized (msgContext) {
                    OutputStream out = pipe.getOutputStream();
                    msgContext.setProperty("GET_ME_OUT", out);
                    msgContext.setProperty("READY2ROCK", Boolean.TRUE);
                    msgContext.notifyAll();
                }
                return;
            }
        }

        conn.requestOutput();
    }    

    /**
     * Throws an AxisFault if an error occurs at this level
     * @param s a message describing the error
     * @param e original exception leads to the error condition
     * @throws AxisFault wrapping the original exception
     */
    private void handleException(String s, Exception e) throws AxisFault {
        log.error(s, e);
        throw new AxisFault(s, e);
    }
}
