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

package org.apache.synapse.transport.fix;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.base.AbstractTransportListener;
import org.apache.synapse.transport.base.AbstractTransportSender;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.threads.WorkerPool;
import quickfix.*;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * FIXIncomingMessageHandler is responsible for handling all incoming FIX messages. This is where the
 * Quickfix/J engine meets Synapse core. Admin level FIX messages are handled by Quickfix/J itself.
 * All the application level messages are handed over to the Synapse core.
 */
public class FIXIncomingMessageHandler implements Application {

    private ConfigurationContext cfgCtx;
    /** A thread pool used to process incoming FIX messages */
    private WorkerPool workerPool;
    /** AxisService to which this FIX application is bound to */
    private AxisService service;
    private Log log;
    /** A boolean value indicating the type of the FIX application */
    private boolean acceptor;
    /** A Map of counters with one counter per session */
    private Map<SessionID, Integer> countersMap;
    private Queue<MessageContext> outgoingMessages;
    private boolean allNewApproach;
    private Semaphore semaphore;

    public FIXIncomingMessageHandler(ConfigurationContext cfgCtx, WorkerPool workerPool,
                             AxisService service, boolean acceptor) {
        this.cfgCtx = cfgCtx;
        this.workerPool = workerPool;
        this.service = service;
        this.log = LogFactory.getLog(this.getClass());
        this.acceptor = acceptor;
        countersMap = new HashMap<SessionID, Integer>();
        outgoingMessages = new LinkedBlockingQueue<MessageContext>();
        semaphore = new Semaphore(0);
        getResponseHandlingApproach();
    }

    private void getResponseHandlingApproach() {
        Parameter param = service.getParameter(FIXConstants.FIX_RESPONSE_HANDLER_APPROACH);
        if (param != null) {
            if ("false".equals(param.getValue().toString())) {
                allNewApproach = false;
                return;
            }
        }
        allNewApproach = true;
    }

    public void setOutgoingMessageContext(MessageContext msgCtx) {
        if (!allNewApproach) {
            outgoingMessages.offer(msgCtx);
        }
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new AxisFIXException(msg, e);
    }

    /**
     * This method is called when quickfix creates a new session. A session
     * comes into and remains in existence for the life of the application.
     * Sessions exist whether or not a counter party is connected to it. As soon
     * as a session is created, the application can begin sending messages to it. If no one
     * is logged on, the messages will be sent at the time a connection is
     * established with the counterparty.
     *
     * @param sessionID QuickFIX session ID
     */
    public void onCreate(SessionID sessionID) {
        log.info("New FIX session created: " + sessionID.toString());
    }

    /**
     * This callback notifies when a valid logon has been established with a
     * counter party. This is called when a connection has been established and
     * the FIX logon process has completed with both parties exchanging valid
     * logon messages.
     *
     * @param sessionID QuickFIX session ID
     */
    public void onLogon(SessionID sessionID) {
        countersMap.put(sessionID, 0);
        log.info("FIX session logged on: " + sessionID.toString());
        semaphore.release();
    }

    /**
     * This callback notifies when a FIX session is no longer online. This
     * could happen during a normal logout exchange or because of a forced
     * termination or a loss of network connection.
     *
     * @param sessionID QuickFIX session ID
     */
    public void onLogout(SessionID sessionID) {
        countersMap.put(sessionID, 0);
        FIXTransportSender trpSender = (FIXTransportSender) cfgCtx.getAxisConfiguration().
                getTransportOut(FIXConstants.TRANSPORT_NAME).getSender();
        trpSender.logOutIncomingSession(sessionID);
        log.info("FIX session logged out: " + sessionID.toString());
    }

    /**
     * This callback provides Synapse with a peek at the administrative messages
     * that are being sent from your FIX engine to the counter party. This is
     * normally not useful for an application however it is provided for any
     * logging one may wish to do.
     *
     * @param message QuickFIX message
     * @param sessionID QuickFIX session ID
     */
    public void toAdmin(Message message, SessionID sessionID) {
        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            try {
                sb.append("Sending admin level FIX message to ").append(message.getHeader().getField(new TargetCompID()).getValue());
                sb.append("\nMessage Type: ").append(message.getHeader().getField(new MsgType()).getValue());
                sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
                sb.append("\nSender ID: ").append(message.getHeader().getField(new SenderCompID()).getValue());
            } catch (FieldNotFound e) {
                sb.append("Sending admin level FIX message...");
                log.warn("One or more required fields are not found in the response message", e);
            }
            log.debug(sb.toString());
            if (log.isTraceEnabled()) {
                log.trace("Message: " + message.toString());
            }
        }
    }

    /**
     * This callback notifies when an administrative message is sent from a
     * counterparty to the FIX engine.
     *
     * @param message QuickFIX message
     * @param sessionID QuickFIX session ID
     * @throws FieldNotFound
     * @throws IncorrectDataFormat
     * @throws IncorrectTagValue
     * @throws RejectLogon causes a logon reject
     */
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {

        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Received admin level FIX message from ").append(message.getHeader().getField(new SenderCompID()).getValue());
            sb.append("\nMessage Type: ").append(message.getHeader().getField(new MsgType()).getValue());
            sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
            sb.append("\nReceiver ID: ").append(message.getHeader().getField(new TargetCompID()).getValue());
            log.debug(sb.toString());
            if (log.isTraceEnabled()) {
                log.trace("Message: " + message.toString());
            }
        }
    }

    /**
     * This is a callback for application messages that are being sent to a
     * counterparty.
     *
     * @param message QuickFIX message
     * @param sessionID QuickFIX session ID
     * @throws DoNotSend This exception aborts message transmission
     */
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
          if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            try {
                sb.append("Sending application level FIX message to ").append(message.getHeader().getField(new TargetCompID()).getValue());
                sb.append("\nMessage Type: ").append(message.getHeader().getField(new MsgType()).getValue());
                sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
                sb.append("\nSender ID: ").append(message.getHeader().getField(new SenderCompID()).getValue());
            } catch (FieldNotFound e) {
                sb.append("Sending application level FIX message...");
                log.warn("One or more required fields are not found in the response message", e);
            }
            log.debug(sb.toString());
            if (log.isTraceEnabled()) {
                log.trace("Message: " + message.toString());
            }
        }
    }

    /**
     * This callback receives messages for the application. This is one of the
     * core entry points for the FIX application. Every application level
     * request will come through here. A new thread will be spawned from the
     * thread pool for each incoming message.
     *
     * @param message QuickFIX message
     * @param sessionID QuickFIX session ID
     * @throws FieldNotFound
     * @throws IncorrectDataFormat
     * @throws IncorrectTagValue
     * @throws UnsupportedMessageType
     */
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType {
        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Received FIX message from ").append(message.getHeader().getField(new SenderCompID()).getValue());
            sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
            sb.append("\nReceiver ID: ").append(message.getHeader().getField(new TargetCompID()).getValue());
            log.debug(sb.toString());
            if (log.isTraceEnabled()) {
                log.trace("Message: " + message.toString());
            }
        }

        int counter = countersMap.get(sessionID);
        counter++;
        countersMap.put(sessionID, counter);

        workerPool.execute(new FIXWorkerThread(message, sessionID, counter));
    }

    /**
     * This Runnable class can be used when it is required to process each incoming message
     * using separate threads.
     */
    class FIXWorkerThread implements Runnable {

        private Message message;
        private SessionID sessionID;
        private int counter;

        public FIXWorkerThread(Message message, SessionID sessionID, int counter) {
            this.message = message;
            this.sessionID = sessionID;
            this.counter = counter;
        }

        private void handleIncomingRequest() {
            //Create message context for the incmong message
            AbstractTransportListener trpListener = (AbstractTransportListener) cfgCtx.getAxisConfiguration().
                    getTransportIn(FIXConstants.TRANSPORT_NAME).getReceiver();

            MessageContext msgCtx = trpListener.createMessageContext();
            msgCtx.setProperty(Constants.OUT_TRANSPORT_INFO, new FIXOutTransportInfo(sessionID));

            if (service != null) {
                // Set the service for which the message is intended to
                msgCtx.setAxisService(service);
                // find the operation for the message, or default to one
                Parameter operationParam = service.getParameter(BaseConstants.OPERATION_PARAM);
                QName operationQName = (
                    operationParam != null ?
                        BaseUtils.getQNameFromString(operationParam.getValue()) :
                        BaseConstants.DEFAULT_OPERATION);

                AxisOperation operation = service.getOperation(operationQName);
                if (operation != null) {
                    msgCtx.setAxisOperation(operation);
                    msgCtx.setSoapAction("urn:" + operation.getName().getLocalPart());
                }
            }

            String fixApplication = FIXConstants.FIX_INITIATOR;
            if (acceptor) {
                fixApplication = FIXConstants.FIX_ACCEPTOR;
            }
            else {
                msgCtx.setProperty("synapse.isresponse", true);
            }

            try {
                //Put the FIX message in a SOAPEnvelope
                FIXUtils.getInstance().setSOAPEnvelope(message, counter, sessionID.toString(), msgCtx);
                trpListener.handleIncomingMessage(
                        msgCtx,
                        FIXUtils.getTransportHeaders(service.getName(), fixApplication),
                        null,
                        FIXConstants.FIX_DEFAULT_CONTENT_TYPE
                );
            } catch (AxisFault e) {
                handleException("Error while processing FIX message", e);
            }
        }

        private void handleIncomingResponse(MessageContext outMsgCtx) {
            AbstractTransportSender trpSender = (AbstractTransportSender) cfgCtx.getAxisConfiguration().
                        getTransportOut(FIXConstants.TRANSPORT_NAME).getSender();

            MessageContext msgCtx = trpSender.createResponseMessageContext(outMsgCtx);

            try {
                //Put the FIX message in a SOAPEnvelope
                FIXUtils.getInstance().setSOAPEnvelope(message, counter, sessionID.toString(), msgCtx);
                msgCtx.setServerSide(true);
                trpSender.handleIncomingMessage(
                        msgCtx,
                        FIXUtils.getTransportHeaders(service.getName(), FIXConstants.FIX_INITIATOR),
                        null,
                        FIXConstants.FIX_DEFAULT_CONTENT_TYPE
                );
            } catch (AxisFault e) {
                handleException("Error while processing response FIX message", e);
            }
        }

        public void run() {

            if (allNewApproach) {
                //treat all messages (including responses) as new messages
                handleIncomingRequest();
            }
            else {
                if (acceptor) {
                    //treat messages coming from an acceptor as new request messages
                    handleIncomingRequest();
                }
                else {
                    MessageContext outMsgCtx = outgoingMessages.poll();
                    if (outMsgCtx != null) {
                        //handle as a response to an outgoing message
                        handleIncomingResponse(outMsgCtx);
                    }
                    else {
                        //handle as a new request message
                        handleIncomingRequest();
                    }
                }
            }
        }

    }

}