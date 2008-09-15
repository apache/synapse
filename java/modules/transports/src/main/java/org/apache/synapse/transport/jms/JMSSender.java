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
package org.apache.synapse.transport.jms;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMNode;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.synapse.transport.base.*;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.jms.Queue;
import javax.activation.DataHandler;
import javax.naming.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * The TransportSender for JMS
 */
public class JMSSender extends AbstractTransportSender implements ManagementSupport {

    public static final String TRANSPORT_NAME = "jms";
    
    private JMSConnectionFactoryManager connFacManager;

    public JMSSender() {
        log = LogFactory.getLog(JMSSender.class);
    }

    /**
     * Initialize the transport sender by reading pre-defined connection factories for
     * outgoing messages. These will create sessions (one per each destination dealth with)
     * to be used when messages are being sent.
     * @param cfgCtx the configuration context
     * @param transportOut the transport sender definition from axis2.xml
     * @throws AxisFault on error
     */
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut) throws AxisFault {
        super.init(cfgCtx, transportOut);
        connFacManager = new JMSConnectionFactoryManager(cfgCtx);
        // read the connection factory definitions and create them
        connFacManager.loadConnectionFactoryDefinitions(transportOut);
        connFacManager.start();
    }

    @Override
    public void stop() {
        connFacManager.stop();
        super.stop();
    }

    /**
     * Get corresponding JMS connection factory defined within the transport sender for the
     * transport-out information - usually constructed from a targetEPR
     *
     * @param trpInfo the transport-out information
     * @return the corresponding JMS connection factory, if any
     */
    private JMSConnectionFactory getJMSConnectionFactory(JMSOutTransportInfo trpInfo) {
        Map<String,String> props = trpInfo.getProperties();
        if(trpInfo.getProperties() != null) {
            String jmsConnectionFactoryName = props.get(JMSConstants.CONFAC_PARAM);
            if(jmsConnectionFactoryName != null) {
                return connFacManager.getJMSConnectionFactory(jmsConnectionFactoryName);
            } else {
                return connFacManager.getJMSConnectionFactory(props);
            }
        } else {
            return null;
        }
    }

    /**
     * Performs the actual sending of the JMS message
     */
    public void sendMessage(MessageContext msgCtx, String targetAddress,
        OutTransportInfo outTransportInfo) throws AxisFault {

        JMSConnectionFactory jmsConnectionFactory = null;
        Connection connection = null;   // holds a one time connection if used
        JMSOutTransportInfo jmsOut = null;
        Session session = null;
        Destination replyDestination = null;

        try {
            if (targetAddress != null) {

                jmsOut = new JMSOutTransportInfo(targetAddress);
                // do we have a definition for a connection factory to use for this address?
                jmsConnectionFactory = getJMSConnectionFactory(jmsOut);

                if (jmsConnectionFactory != null) {
                    // create new or get existing session to send to the destination from the CF
                    session = jmsConnectionFactory.getSessionForDestination(
                        JMSUtils.getDestination(targetAddress));

                } else {
                    // digest the targetAddress and locate CF from the EPR
                    jmsOut.loadConnectionFactoryFromProperies();
                    try {
                        // create a one time connection and session to be used
                        Hashtable<String,String> jndiProps = jmsOut.getProperties();
                        String user = jndiProps.get(Context.SECURITY_PRINCIPAL);
                        String pass = jndiProps.get(Context.SECURITY_CREDENTIALS);

                        QueueConnectionFactory qConFac = null;
                        TopicConnectionFactory tConFac = null;

                        if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(jmsOut.getDestinationType())) {
                            qConFac = (QueueConnectionFactory) jmsOut.getConnectionFactory();
                        } else if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(jmsOut.getDestinationType())) {
                            tConFac = (TopicConnectionFactory) jmsOut.getConnectionFactory();
                        } else {
                            handleException("Unable to determine type of JMS " +
                                "Connection Factory - i.e Queue/Topic");
                        }

                        if (user != null && pass != null) {
                            if (qConFac != null) {
                                connection = qConFac.createQueueConnection(user, pass);
                            } else if (tConFac != null) {
                                connection = tConFac.createTopicConnection(user, pass);
                            }
                        } else {
                           if (qConFac != null) {
                                connection = qConFac.createQueueConnection();
                            } else if (tConFac != null) {
                                connection = tConFac.createTopicConnection();
                            }
                        }

                        if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(jmsOut.getDestinationType())) {
                            session = ((QueueConnection)connection).
                                createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                        } else if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(jmsOut.getDestinationType())) {
                            session = ((TopicConnection)connection).
                                createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                        }

                    } catch (JMSException e) {
                        handleException("Error creating a connection/session for : " + targetAddress, e);
                    }
                }
                replyDestination = jmsOut.getReplyDestination();

            } else if (outTransportInfo != null && outTransportInfo instanceof JMSOutTransportInfo) {

                jmsOut = (JMSOutTransportInfo) outTransportInfo;
                jmsConnectionFactory = jmsOut.getJmsConnectionFactory();

                session = jmsConnectionFactory.getSessionForDestination(
                    jmsOut.getDestination().toString());
            }
            
            Destination destination = jmsOut.getDestination();

            String replyDestName = (String) msgCtx.getProperty(JMSConstants.JMS_REPLY_TO);
            if (replyDestName != null) {
                if (jmsConnectionFactory != null) {
                    replyDestination = jmsConnectionFactory.getDestination(replyDestName);
                } else {
                    replyDestination = jmsOut.getReplyDestination(replyDestName);
                }
            }

            if(session == null) {
               handleException("Could not create JMS session");
            }

            // now we are going to use the JMS session, but if this was a session from a
            // defined JMS connection factory, we need to synchronize as sessions are not
            // thread safe
            synchronized(session) {

                // convert the axis message context into a JMS Message that we can send over JMS
                Message message = null;
                String correlationId = null;
                try {
                    message = createJMSMessage(msgCtx, session);
                } catch (JMSException e) {
                    handleException("Error creating a JMS message from the axis message context", e);
                }

                String destinationType = jmsOut.getDestinationType();

                // if the destination does not exist, see if we can create it
                destination = JMSUtils.createDestinationIfRequired(
                    destination, destinationType, targetAddress, session);

                if(jmsOut.getReplyDestinationName() != null) {
                    replyDestination = JMSUtils.createReplyDestinationIfRequired(
                        replyDestination, jmsOut.getReplyDestinationName(),
                        jmsOut.getReplyDestinationType(), targetAddress, session);
                }

                // should we wait for a synchronous response on this same thread?
                boolean waitForResponse = waitForSynchronousResponse(msgCtx);

                // if this is a synchronous out-in, prepare to listen on the response destination
                if (waitForResponse) {
                    replyDestination = JMSUtils.setReplyDestination(
                        replyDestination, session, message);
                }

                // send the outgoing message over JMS to the destination selected
                try {
                    JMSUtils.sendMessageToJMSDestination(session, destination, destinationType, message);

                    // set the actual MessageID to the message context for use by any others
                    try {
                        String msgId = message.getJMSMessageID();
                        if (msgId != null) {
                            msgCtx.setProperty(JMSConstants.JMS_MESSAGE_ID, msgId);
                        }
                    } catch (JMSException ignore) {}

                    metrics.incrementMessagesSent();
                    try {
                        if (message instanceof BytesMessage) {
                            metrics.incrementBytesSent(JMSUtils.getBodyLength((BytesMessage) message));
                        } else if (message instanceof TextMessage) {
                            metrics.incrementBytesSent((
                                (TextMessage) message).getText().getBytes().length);
                        } else {
                            handleException("Unsupported JMS message type : " +
                                message.getClass().getName());
                        }
                    } catch (JMSException e) {
                        log.warn("Error reading JMS message size to update transport metrics", e);
                    }
                } catch (BaseTransportException e) {
                    metrics.incrementFaultsSending();
                    throw e;
                }

                // if we are expecting a synchronous response back for the message sent out
                if (waitForResponse) {
                    if (connection != null) {
                        try {
                            connection.start();
                        } catch (JMSException ignore) {}
                    } else {
                        // If connection is null, we are using a cached session and the underlying
                        // connection is already started. Thus, there is nothing to do here.
                    }
                    try {
                        correlationId = message.getJMSMessageID();
                    } catch(JMSException ignore) {}
                        waitForResponseAndProcess(session, replyDestination,
                                jmsOut.getReplyDestinationType(), msgCtx, correlationId);
                }
            }

        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException ignore) {}
            }
        }
    }

    /**
     * Create a Consumer for the reply destination and wait for the response JMS message
     * synchronously. If a message arrives within the specified time interval, process it
     * through Axis2
     * @param session the session to use to listen for the response
     * @param replyDestination the JMS reply Destination
     * @param msgCtx the outgoing message for which we are expecting the response
     * @throws AxisFault on error
     */
    private void waitForResponseAndProcess(Session session, Destination replyDestination,
        String replyDestinationType, MessageContext msgCtx, String correlationId) throws AxisFault {

        try {
            MessageConsumer consumer = null;
            if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(replyDestinationType)) {
                if (correlationId != null) {
                    consumer = ((QueueSession) session).createReceiver((Queue) replyDestination,
                        "JMSCorrelationID = '" + correlationId + "'");
                } else {
                    consumer = ((QueueSession) session).createReceiver((Queue) replyDestination);
                }
            } else {
                if (correlationId != null) {
                    consumer = ((TopicSession) session).createSubscriber((Topic) replyDestination,
                        "JMSCorrelationID = '" + correlationId + "'", false);
                } else {
                    consumer = ((TopicSession) session).createSubscriber((Topic) replyDestination);
                }
            }

            // how long are we willing to wait for the sync response
            long timeout = JMSConstants.DEFAULT_JMS_TIMEOUT;
            String waitReply = (String) msgCtx.getProperty(JMSConstants.JMS_WAIT_REPLY);
            if (waitReply != null) {
                timeout = Long.valueOf(waitReply).longValue();
            }

            if (log.isDebugEnabled()) {
                log.debug("Waiting for a maximum of " + timeout +
                    "ms for a response message to destination : " + replyDestination +
                    " with JMS correlation ID : " + correlationId);
            }

            Message reply = consumer.receive(timeout);

            if (reply != null) {

                // update transport level metrics
                metrics.incrementMessagesReceived();                
                try {
                    if (reply instanceof BytesMessage) {
                        metrics.incrementBytesReceived(JMSUtils.getBodyLength((BytesMessage) reply));
                    } else if (reply instanceof TextMessage) {
                        metrics.incrementBytesReceived((
                            (TextMessage) reply).getText().getBytes().length);
                    } else {
                        handleException("Unsupported JMS message type : " +
                            reply.getClass().getName());
                    }
                } catch (JMSException e) {
                    log.warn("Error reading JMS message size to update transport metrics", e);
                }

                try {
                    processSyncResponse(msgCtx, reply);
                    metrics.incrementMessagesReceived();
                } catch (AxisFault e) {
                    metrics.incrementFaultsReceiving();
                    throw e;
                }

            } else {
                log.warn("Did not receive a JMS response within " +
                    timeout + " ms to destination : " + replyDestination +
                    " with JMS correlation ID : " + correlationId);
                metrics.incrementTimeoutsReceiving();
            }

        } catch (JMSException e) {
            metrics.incrementFaultsReceiving();
            handleException("Error creating consumer or receiving reply to : " +
                replyDestination, e);
        }
    }

    /**
     * Create a JMS Message from the given MessageContext and using the given
     * session
     *
     * @param msgContext the MessageContext
     * @param session    the JMS session
     * @return a JMS message from the context and session
     * @throws JMSException on exception
     * @throws AxisFault on exception
     */
    private Message createJMSMessage(MessageContext msgContext, Session session)
            throws JMSException, AxisFault {

        Message message = null;
        String msgType = getProperty(msgContext, JMSConstants.JMS_MESSAGE_TYPE);

        // check the first element of the SOAP body, do we have content wrapped using the
        // default wrapper elements for binary (BaseConstants.DEFAULT_BINARY_WRAPPER) or
        // text (BaseConstants.DEFAULT_TEXT_WRAPPER) ? If so, do not create SOAP messages
        // for JMS but just get the payload in its native format
        String jmsPayloadType = guessMessageType(msgContext);

        if (jmsPayloadType == null) {

            OMOutputFormat format = BaseUtils.getOMOutputFormat(msgContext);
            MessageFormatter messageFormatter = null;
            try {
                messageFormatter = TransportUtils.getMessageFormatter(msgContext);
            } catch (AxisFault axisFault) {
                throw new JMSException("Unable to get the message formatter to use");
            }

            String contentType = messageFormatter.getContentType(
                msgContext, format, msgContext.getSoapAction());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                messageFormatter.writeTo(msgContext, format, baos, true);
                baos.flush();
            } catch (IOException e) {
                handleException("IO Error while creating BytesMessage", e);
            }

            if (msgType != null && JMSConstants.JMS_BYTE_MESSAGE.equals(msgType) ||
                contentType.indexOf(HTTPConstants.HEADER_ACCEPT_MULTIPART_RELATED) > -1) {
                message = session.createBytesMessage();
                BytesMessage bytesMsg = (BytesMessage) message;
                bytesMsg.writeBytes(baos.toByteArray());
            } else {
                message = session.createTextMessage();  // default
                TextMessage txtMsg = (TextMessage) message;
                try {
                    txtMsg.setText(new String(baos.toByteArray(), format.getCharSetEncoding()));
                } catch (UnsupportedEncodingException ex) {
                    handleException("Unsupported encoding " + format.getCharSetEncoding(), ex);
                }
            }
            message.setStringProperty(BaseConstants.CONTENT_TYPE, contentType);

        } else if (JMSConstants.JMS_BYTE_MESSAGE.equals(jmsPayloadType)) {
            message = session.createBytesMessage();
            BytesMessage bytesMsg = (BytesMessage) message;
            OMElement wrapper = msgContext.getEnvelope().getBody().
                getFirstChildWithName(BaseConstants.DEFAULT_BINARY_WRAPPER);
            OMNode omNode = wrapper.getFirstOMChild();
            if (omNode != null && omNode instanceof OMText) {
                Object dh = ((OMText) omNode).getDataHandler();
                if (dh != null && dh instanceof DataHandler) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        ((DataHandler) dh).writeTo(baos);
                    } catch (IOException e) {
                        handleException("Error serializing binary content of element : " +
                            BaseConstants.DEFAULT_BINARY_WRAPPER, e);
                    }
                    bytesMsg.writeBytes(baos.toByteArray());
                }
            }

        } else if (JMSConstants.JMS_TEXT_MESSAGE.equals(jmsPayloadType)) {
            message = session.createTextMessage();
            TextMessage txtMsg = (TextMessage) message;
            txtMsg.setText(msgContext.getEnvelope().getBody().
                getFirstChildWithName(BaseConstants.DEFAULT_TEXT_WRAPPER).getText());
        }

        // set the JMS correlation ID if specified
        String correlationId = getProperty(msgContext, JMSConstants.JMS_COORELATION_ID);
        if (correlationId == null && msgContext.getRelatesTo() != null) {
            correlationId = msgContext.getRelatesTo().getValue();
        }

        if (correlationId != null) {
            message.setJMSCorrelationID(correlationId);
        }

        if (msgContext.isServerSide()) {
            // set SOAP Action as a property on the JMS message
            setProperty(message, msgContext, BaseConstants.SOAPACTION);
        } else {
            String action = msgContext.getOptions().getAction();
            if (action != null) {
                message.setStringProperty(BaseConstants.SOAPACTION, action);
            }
        }

        JMSUtils.setTransportHeaders(msgContext, message);
        return message;
    }

    /**
     * Guess the message type to use for JMS looking at the message contexts' envelope
     * @param msgContext the message context
     * @return JMSConstants.JMS_BYTE_MESSAGE or JMSConstants.JMS_TEXT_MESSAGE or null
     */
    private String guessMessageType(MessageContext msgContext) {
        OMElement firstChild = msgContext.getEnvelope().getBody().getFirstElement();
        if (firstChild != null) {
            if (BaseConstants.DEFAULT_BINARY_WRAPPER.equals(firstChild.getQName())) {
                return JMSConstants.JMS_BYTE_MESSAGE;
            } else if (BaseConstants.DEFAULT_TEXT_WRAPPER.equals(firstChild.getQName())) {
                return JMSConstants.JMS_TEXT_MESSAGE;
            }
        }
        return null;
    }

    /**
     * Creates an Axis MessageContext for the received JMS message and
     * sets up the transports and various properties
     *
     * @param outMsgCtx the outgoing message for which we are expecting the response
     * @param message the JMS response message received
     * @throws AxisFault on error
     */
    private void processSyncResponse(MessageContext outMsgCtx, Message message) throws AxisFault {

        MessageContext responseMsgCtx = createResponseMessageContext(outMsgCtx);

        // load any transport headers from received message
        JMSUtils.loadTransportHeaders(message, responseMsgCtx);

        // workaround for Axis2 TransportUtils.createSOAPMessage() issue, where a response
        // of content type "text/xml" is thought to be REST if !MC.isServerSide(). This
        // question is still under debate and due to the timelines, I am commiting this
        // workaround as Axis2 1.2 is about to be released and Synapse 1.0
        responseMsgCtx.setServerSide(false);

        String contentType = JMSUtils.getInstace().getProperty(message, BaseConstants.CONTENT_TYPE);

        JMSUtils.getInstace().setSOAPEnvelope(message, responseMsgCtx, contentType);
//        responseMsgCtx.setServerSide(true);

        handleIncomingMessage(
            responseMsgCtx,
            JMSUtils.getTransportHeaders(message),
            JMSUtils.getInstace().getProperty(message, BaseConstants.SOAPACTION),
            contentType
        );
    }

    private void setProperty(Message message, MessageContext msgCtx, String key) {

        String value = getProperty(msgCtx, key);
        if (value != null) {
            try {
                message.setStringProperty(key, value);
            } catch (JMSException e) {
                log.warn("Couldn't set message property : " + key + " = " + value, e);
            }
        }
    }

    private String getProperty(MessageContext mc, String key) {
        return (String) mc.getProperty(key);
    }
}
