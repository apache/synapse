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
package org.apache.axis2.transport.jms;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.base.AbstractTransportSender;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * The TransportSender for JMS
 */
public class JMSSender extends AbstractTransportSender {

    public static final String TRANSPORT_NAME = "jms";

    /** A Map containing the JMS connection factories managed by this, keyed by name */
    private Map connectionFactories = new HashMap();

    static {
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
        setTransportName(TRANSPORT_NAME);
        super.init(cfgCtx, transportOut);
        // read the connection factory definitions and create them
        loadConnectionFactoryDefinitions(transportOut);
    }

    /**
     * Get corresponding JMS connection factory defined within the transport sender for the
     * transport-out information - usually constructed from a targetEPR
     * 
     * @param trpInfo the transport-out information
     * @return the corresponding JMS connection factory, if any
     */
    private JMSConnectionFactory getJMSConnectionFactory(JMSOutTransportInfo trpInfo) {
        Iterator cfNames = connectionFactories.keySet().iterator();
        while (cfNames.hasNext()) {
            String cfName = (String) cfNames.next();
            JMSConnectionFactory cf = (JMSConnectionFactory) connectionFactories.get(cfName);
            if (cf.equals(trpInfo)) {
                return cf;
            }
        }
        return null;
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
        Destination destination = null;
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
                        connection = jmsOut.getConnectionFactory().createConnection();
                        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                    } catch (JMSException e) {
                        handleException("Error creating a connection/session for : " + targetAddress);
                    }
                }
                destination = jmsOut.getDestination();

            } else if (outTransportInfo != null && outTransportInfo instanceof JMSOutTransportInfo) {

                jmsOut = (JMSOutTransportInfo) outTransportInfo;
                jmsConnectionFactory = jmsOut.getJmsConnectionFactory();

                session = jmsConnectionFactory.getSessionForDestination(
                    jmsOut.getDestination().toString());
                destination = jmsOut.getDestination();
            }

            String replyDestName = (String) msgCtx.getProperty(JMSConstants.JMS_WAIT_REPLY);
            if (replyDestName != null) {
                replyDestination = jmsOut.getReplyDestination(replyDestName);
            }

            // now we are going to use the JMS session, but if this was a session from a
            // defined JMS connection factory, we need to synchronize as sessions are not
            // thread safe
            synchronized(session) {

                // convert the axis message context into a JMS Message that we can send over JMS
                Message message = null;
                try {
                    message = createJMSMessage(msgCtx, session);
                } catch (JMSException e) {
                    handleException("Error creating a JMS message from the axis message context", e);
                }

                // if the destination does not exist, see if we can create it
                destination = JMSUtils.createDestinationIfRequired(
                    destination, targetAddress, session);

                // should we wait for a synchronous response on this same thread?
                boolean waitForResponse = waitForSynchronousResponse(msgCtx);

                // if this is a synchronous out-in, prepare to listen on the response destination
                if (waitForResponse) {
                    replyDestination = JMSUtils.setReplyDestination(
                        replyDestination, session, message);
                }

                // send the outgoing message over JMS to the destination selected
                JMSUtils.sendMessageToJMSDestination(session, destination, message);

                // if we are expecting a synchronous response back for the message sent out
                if (waitForResponse) {
                    waitForResponseAndProcess(session, replyDestination, msgCtx);
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
        MessageContext msgCtx) throws AxisFault {
        try {
            MessageConsumer consumer = session.createConsumer(replyDestination);

            // how long are we willing to wait for the sync response
            long timeout = JMSConstants.DEFAULT_JMS_TIMEOUT;
            String waitReply = (String) msgCtx.getProperty(JMSConstants.JMS_WAIT_REPLY);
            if (waitReply != null) {
                timeout = Long.valueOf(waitReply).longValue();
            }

            if (log.isDebugEnabled()) {
                log.debug("Waiting for a maximum of " + timeout +
                    "ms for a response message to destination : " + replyDestination);
            }

            Message reply = consumer.receive(timeout);
            if (reply != null) {
                processSyncResponse(msgCtx, reply);

            } else {
                log.warn("Did not receive a JMS response within " +
                    timeout + " ms to destination : " + replyDestination);
            }

        } catch (JMSException e) {
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
            txtMsg.setText(new String(baos.toByteArray()));
        }

        message.setStringProperty(BaseConstants.CONTENT_TYPE, contentType);

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
        responseMsgCtx.setServerSide(true);

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

    /**
     * Create JMSConnectionFactory instances for the definitions in the transport sender,
     * and add these into our collection of connectionFactories map keyed by name
     *
     * @param transportOut the transport-in description for JMS
     */
    private void loadConnectionFactoryDefinitions(TransportOutDescription transportOut) {

        // iterate through all defined connection factories
        Iterator conFacIter = transportOut.getParameters().iterator();

        while (conFacIter.hasNext()) {
            Parameter conFacParams = (Parameter) conFacIter.next();

            JMSConnectionFactory jmsConFactory =
                new JMSConnectionFactory(conFacParams.getName(), cfgCtx);
            JMSUtils.setConnectionFactoryParameters(conFacParams, jmsConFactory);

            connectionFactories.put(jmsConFactory.getName(), jmsConFactory);
        }
    }

}
