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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterIncludeImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.BaseUtils;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Miscallaneous methods used for the JMS transport
 */
public class JMSUtils extends BaseUtils {

    private static final Log log = LogFactory.getLog(JMSUtils.class);
    private static final Class[]  NOARGS  = new Class[] {};
    private static final Object[] NOPARMS = new Object[] {};

    private static BaseUtils _instance = new JMSUtils();

    public static BaseUtils getInstace() {
        return _instance;
    }

    /**
     * Create a JMS Queue using the given connection with the JNDI destination name, and return the
     * JMS Destination name of the created queue
     *
     * @param con the JMS Connection to be used
     * @param destinationJNDIName the JNDI name of the Queue to be created
     * @return the JMS Destination name of the created Queue
     * @throws JMSException on error
     */
    public static String createJMSQueue(Connection con, String destinationJNDIName)
            throws JMSException {
        
        try {
            QueueSession session
                    = ((QueueConnection) con).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(destinationJNDIName);
            log.info("JMS Queue with JNDI name : " + destinationJNDIName + " created");
            return queue.getQueueName();

        } finally {
            try {
                con.close();
            } catch (JMSException ignore) {}
        }
    }

    /**
     * Create a JMS Topic using the given connection with the JNDI destination name, and return the
     * JMS Destination name of the created queue
     *
     * @param con the JMS Connection to be used
     * @param destinationJNDIName the JNDI name of the Topic to be created
     * @return the JMS Destination name of the created Topic
     * @throws JMSException on error
     */
    public static String createJMSTopic(Connection con, String destinationJNDIName)
            throws JMSException {
        
        try {
            TopicSession session
                    = ((TopicConnection) con).createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(destinationJNDIName);
            log.info("JMS Topic with JNDI name : " + destinationJNDIName + " created");
            return topic.getTopicName();

        } finally {
            try {
                con.close();
            } catch (JMSException ignore) {}
        }
    }

    /**
     * Should this service be enabled over the JMS transport?
     *
     * @param service the Axis service
     * @return true if JMS should be enabled
     */
    public static boolean isJMSService(AxisService service) {
        if (service.isEnableAllTransports()) {
            return true;

        } else {
            List transports = service.getExposedTransports();
            for (Object transport : transports) {
                if (JMSListener.TRANSPORT_NAME.equals(transport)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the JMS destination used by this service
     *
     * @param service the Axis Service
     * @return the name of the JMS destination
     */
    public static String getJNDIDestinationNameForService(AxisService service) {
        Parameter destParam = service.getParameter(JMSConstants.DEST_PARAM);
        if (destParam != null) {
            return (String) destParam.getValue();
        } else {
            return service.getName();
        }
    }

    /**
     * Get the JMS destination type of this service
     *
     * @param service the Axis Service
     * @return the name of the JMS destination
     */
    public static String getDestinationTypeForService(AxisService service) {
        Parameter destTypeParam = service.getParameter(JMSConstants.DEST_PARAM_TYPE);
        if (destTypeParam != null) {
            String paramValue = (String) destTypeParam.getValue();
            if(JMSConstants.DESTINATION_TYPE_QUEUE.equals(paramValue) ||
                    JMSConstants.DESTINATION_TYPE_TOPIC.equals(paramValue) )  {
                return paramValue;
            } else {
               handleException("Invalid destinaton type value " + paramValue);
               return null;
            }
        } else {
            log.debug("JMS destination type not given. default queue");
            return JMSConstants.DESTINATION_TYPE_QUEUE;
        }
    }
    
    /**
     * Extract connection factory properties from a given URL
     *
     * @param url a JMS URL of the form jms:/<destination>?[<key>=<value>&]*
     * @return a Hashtable of extracted properties
     */
    public static Hashtable<String,String> getProperties(String url) {
        Hashtable<String,String> h = new Hashtable<String,String>();
        int propPos = url.indexOf("?");
        if (propPos != -1) {
            StringTokenizer st = new StringTokenizer(url.substring(propPos + 1), "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int sep = token.indexOf("=");
                if (sep != -1) {
                    h.put(token.substring(0, sep), token.substring(sep + 1));
                } else {
                    // ignore, what else can we do?
                }
            }
        }
        return h;
    }

    /**
     * Get the EPR for the given JMS connection factory and destination
     * the form of the URL is
     * jms:/<destination>?[<key>=<value>&]*
     *
     * @param cf          the Axis2 JMS connection factory
     * @param destination the JNDI name of the destination
     * @return the EPR as a String
     */
    // TODO: duplicate code (see JMSConnectionFactory#getEPRForDestination)
    static String getEPR(JMSConnectionFactory cf, String destinationType, String destination) {
        StringBuffer sb = new StringBuffer();
        sb.append(JMSConstants.JMS_PREFIX).append(destination);
        sb.append("?").append(JMSConstants.DEST_PARAM_TYPE).append("=").append(destinationType);
        for (Map.Entry<String,String> entry : cf.getJndiProperties().entrySet()) {
            sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Get a String property from the JMS message
     *
     * @param message  JMS message
     * @param property property name
     * @return property value
     */
    @Override
    public String getProperty(Object message, String property) {
        try {
            return ((Message)message).getStringProperty(property);
        } catch (JMSException e) {
            return null;
        }
    }

    /**
     * Return the destination name from the given URL
     *
     * @param url the URL
     * @return the destination name
     */
    public static String getDestination(String url) {
        String tempUrl = url.substring(JMSConstants.JMS_PREFIX.length());
        int propPos = tempUrl.indexOf("?");

        if (propPos == -1) {
            return tempUrl;
        } else {
            return tempUrl.substring(0, propPos);
        }
    }

    /**
     * Set JNDI properties and any other connection factory parameters to the connection factory
     * passed in, looking at the parameter in axis2.xml
     * @param param the axis parameter that holds the connection factory settings
     * @param jmsConFactory the JMS connection factory to which the parameters should be applied
     */
    public static void setConnectionFactoryParameters(
        Parameter param, JMSConnectionFactory jmsConFactory) {

        ParameterIncludeImpl pi = new ParameterIncludeImpl();
        try {
            pi.deserializeParameters((OMElement) param.getValue());
        } catch (AxisFault axisFault) {
            log.error("Error reading parameters for JMS connection factory" +
                jmsConFactory.getName(), axisFault);
        }

        for (Object o : pi.getParameters()) {

            Parameter p = (Parameter) o;

            if (JMSConstants.CONFAC_TYPE.equals(p.getName())) {
                String connectionFactoryType = (String) p.getValue();
                jmsConFactory.setConnectionFactoryType(connectionFactoryType);

            } else if (JMSConstants.RECONNECT_TIMEOUT.equals(p.getName())) {
                String strTimeout = (String) p.getValue();
                int reconnectTimeoutSeconds = Integer.parseInt(strTimeout);
                long reconnectTimeoutMillis = reconnectTimeoutSeconds * 1000;
                jmsConFactory.setReconnectTimeout(reconnectTimeoutMillis);

            } else if (Context.INITIAL_CONTEXT_FACTORY.equals(p.getName())) {
                jmsConFactory.addJNDIContextProperty(
                        Context.INITIAL_CONTEXT_FACTORY, (String) p.getValue());
            } else if (Context.PROVIDER_URL.equals(p.getName())) {
                jmsConFactory.addJNDIContextProperty(
                        Context.PROVIDER_URL, (String) p.getValue());
            } else if (Context.SECURITY_PRINCIPAL.equals(p.getName())) {
                jmsConFactory.addJNDIContextProperty(
                        Context.SECURITY_PRINCIPAL, (String) p.getValue());
            } else if (Context.SECURITY_CREDENTIALS.equals(p.getName())) {
                jmsConFactory.addJNDIContextProperty(
                        Context.SECURITY_CREDENTIALS, (String) p.getValue());
            } else if (JMSConstants.CONFAC_JNDI_NAME_PARAM.equals(p.getName())) {
                jmsConFactory.setConnFactoryJNDIName((String) p.getValue());
                jmsConFactory.addJNDIContextProperty(
                        JMSConstants.CONFAC_JNDI_NAME_PARAM, (String) p.getValue());
            } else {
                jmsConFactory.addJNDIContextProperty( p.getName(), (String) p.getValue());
            }
        }
    }

    /**
     * Get an InputStream to the JMS message payload
     *
     * @param message the JMS message
     * @return an InputStream to the payload
     */
    @Override
    public InputStream getInputStream(Object message) {

        try {
            if (message instanceof BytesMessage) {
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                BytesMessage byteMsg = (BytesMessage) message;
                byteMsg.reset();
                for (int bytesRead = byteMsg.readBytes(buffer); bytesRead != -1;
                     bytesRead = byteMsg.readBytes(buffer)) {
                    out.write(buffer, 0, bytesRead);
                }
                return new ByteArrayInputStream(out.toByteArray());

            } else if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage) message;
                String contentType = getProperty(txtMsg, BaseConstants.CONTENT_TYPE);
                
                if (contentType != null) {
                    return new ByteArrayInputStream(
                        txtMsg.getText().getBytes(BuilderUtil.getCharSetEncoding(contentType)));
                } else {
                    return new ByteArrayInputStream(
                            txtMsg.getText().getBytes(MessageContext.DEFAULT_CHAR_SET_ENCODING));
                }

            } else {
                handleException("Unsupported JMS message type : " + message.getClass().getName());
            }

        } catch (JMSException e) {
            handleException("JMS Exception reading message payload", e);
        } catch (UnsupportedEncodingException e) {
            handleException("Encoding exception getting InputStream into message", e);
        }
        return null;
    }

    /**
     * Set the JMS ReplyTo for the message
     *
     * @param replyDestination the JMS Destination where the reply is expected
     * @param session the session to use to create a temp Queue if a response is expected
     * but a Destination has not been specified
     * @param message the JMS message where the final Destinatio would be set as the JMS ReplyTo
     * @return the JMS ReplyTo Destination for the message
     */
    public static Destination setReplyDestination(Destination replyDestination, Session session,
        Message message) {
        if (replyDestination == null) {
           try {
               // create temporary queue to receive the reply
               replyDestination = createTemporaryDestination(session);
           } catch (JMSException e) {
               handleException("Error creating temporary queue for response");
           }
        }

        try {
            message.setJMSReplyTo(replyDestination);
        } catch (JMSException e) {
            log.warn("Error setting JMS ReplyTo destination to : " + replyDestination, e);
        }

        if (log.isDebugEnabled()) {
            try {
                assert replyDestination != null;
                log.debug("Expecting a response to JMS Destination : " +
                    (replyDestination instanceof Queue ?
                        ((Queue) replyDestination).getQueueName() :
                        ((Topic) replyDestination).getTopicName()));
            } catch (JMSException ignore) {}
        }
        return replyDestination;
    }

    /**
     * When trying to send a message to a destination, if it does not exist, try to create it
     *
     * @param destination the JMS destination to send messages
     * @param destinationType type of the destination (can be a queue or a topic)
     * @param targetAddress the target JMS EPR to find the Destination to be created if required
     * @param session the JMS session to use
     * @return the JMS Destination where messages could be posted
     * @throws AxisFault if the target Destination does not exist and cannot be created
     */
    public static Destination createDestinationIfRequired(Destination destination,
        String destinationType, String targetAddress, Session session) throws AxisFault {
        
        if (destination == null) {
            if (targetAddress != null) {
                String name = JMSUtils.getDestination(targetAddress);
                if (log.isDebugEnabled()) {
                    log.debug("Creating JMS Destination : " + name);
                }

                try {
                    destination = createDestination(session, name, destinationType);
                } catch (JMSException e) {
                    handleException("Error creating destination Queue : " + name, e);
                }
            } else {
                handleException("Cannot send reply to null JMS Destination");
            }
        }
        return destination;
    }

    /**
     * If reply destination does not exist, try to create it
     * 
     * @param destination the destination queue or topic
     * @param replyDestinationName name of the reply destination queue or topic
     * @param destinationType type of the destination (can be queue or topic)
     * @param targetAddress target address of the queue or topic
     * @param session JMS session with the message to be sent
     * @return destination created if the destination is null or the destination otherwise
     * @throws org.apache.axis2.AxisFault in case of an error in creating the destination
     */
    public static Destination createReplyDestinationIfRequired(Destination destination,
        String replyDestinationName, String destinationType, String targetAddress, Session session)
            throws AxisFault {
        
        if (destination == null) {
            if (targetAddress != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Creating JMS Reply Destination : " + replyDestinationName);
                }

                try {
                    destination = createDestination(session, replyDestinationName, destinationType);
                } catch (JMSException e) {
                    handleException("Error creating reply destination : "
                            + replyDestinationName, e);
                }
            } else {
                handleException("Cannot send reply to null reply JMS Destination");
            }
        }
        return destination;
    }

    /**
     * Send the given message to the Destination using the given session
     * 
     * @param session the session to use to send
     * @param destination the Destination
     * @param destinationType type of the destination (can be a queue or a topic)
     * @param message the JMS Message
     * @throws AxisFault on error
     */
    public static void sendMessageToJMSDestination(Session session,
        Destination destination, String destinationType, Message message) throws AxisFault {

        MessageProducer producer = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending message to destination : " + destination);
            }

            if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(destinationType)) {
                producer = ((TopicSession) session).createPublisher((Topic) destination);
                ((TopicPublisher) producer).publish(message);
            } else {                
                producer = ((QueueSession) session).createSender((Queue) destination);
                producer.send(message);
            }

            if (log.isDebugEnabled()) {
                log.debug("Sent message to destination : " + destination +
                    "\nMessage ID : " + message.getJMSMessageID() +
                    "\nCorrelation ID : " + message.getJMSCorrelationID() +
                    "\nReplyTo ID : " + message.getJMSReplyTo());
            }

        } catch (JMSException e) {
            handleException("Error creating a producer or sending to : " + destination, e);
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException ignore) {}
            }
        }
    }

    /**
     * Set transport headers from the axis message context, into the JMS message
     *
     * @param msgContext the axis message context
     * @param message the JMS Message
     * @throws JMSException on exception
     */
    public static void setTransportHeaders(MessageContext msgContext, Message message)
        throws JMSException {

        Map headerMap = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (headerMap == null) {
            return;
        }

        for (Object headerName : headerMap.keySet()) {

            String name = (String) headerName;

            if (JMSConstants.JMS_COORELATION_ID.equals(name)) {
                message.setJMSCorrelationID(
                        (String) headerMap.get(JMSConstants.JMS_COORELATION_ID));
            } else if (JMSConstants.JMS_DELIVERY_MODE.equals(name)) {
                Object o = headerMap.get(JMSConstants.JMS_DELIVERY_MODE);
                if (o instanceof Integer) {
                    message.setJMSDeliveryMode((Integer) o);
                } else if (o instanceof String) {
                    try {
                        message.setJMSDeliveryMode(Integer.parseInt((String) o));
                    } catch (NumberFormatException nfe) {
                        log.warn("Invalid delivery mode ignored : " + o, nfe);
                    }
                } else {
                    log.warn("Invalid delivery mode ignored : " + o);
                }
            } else if (JMSConstants.JMS_EXPIRATION.equals(name)) {
                message.setJMSExpiration(
                        Long.parseLong((String) headerMap.get(JMSConstants.JMS_EXPIRATION)));
            } else if (JMSConstants.JMS_MESSAGE_ID.equals(name)) {
                message.setJMSMessageID((String) headerMap.get(JMSConstants.JMS_MESSAGE_ID));
            } else if (JMSConstants.JMS_PRIORITY.equals(name)) {
                message.setJMSPriority(
                        Integer.parseInt((String) headerMap.get(JMSConstants.JMS_PRIORITY)));
            } else if (JMSConstants.JMS_TIMESTAMP.equals(name)) {
                message.setJMSTimestamp(
                        Long.parseLong((String) headerMap.get(JMSConstants.JMS_TIMESTAMP)));
            } else if (JMSConstants.JMS_MESSAGE_TYPE.equals(name)) {
                message.setJMSType((String) headerMap.get(JMSConstants.JMS_MESSAGE_TYPE));
            } else {
                Object value = headerMap.get(name);
                if (value instanceof String) {
                    message.setStringProperty(name, (String) value);
                } else if (value instanceof Boolean) {
                    message.setBooleanProperty(name, (Boolean) value);
                } else if (value instanceof Integer) {
                    message.setIntProperty(name, (Integer) value);
                } else if (value instanceof Long) {
                    message.setLongProperty(name, (Long) value);
                } else if (value instanceof Double) {
                    message.setDoubleProperty(name, (Double) value);
                } else if (value instanceof Float) {
                    message.setFloatProperty(name, (Float) value);
                }
            }
        }
    }

    /**
     * Read the transport headers from the JMS Message and set them to the axis2 message context
     *
     * @param message the JMS Message received
     * @param responseMsgCtx the axis message context
     * @throws AxisFault on error
     */
    public static void loadTransportHeaders(Message message, MessageContext responseMsgCtx)
        throws AxisFault {
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, getTransportHeaders(message));
    }

    /**
     * Extract transport level headers for JMS from the given message into a Map
     *
     * @param message the JMS message
     * @return a Map of the transport headers
     */
    public static Map<String, Object> getTransportHeaders(Message message) {
        // create a Map to hold transport headers
        Map<String, Object> map = new HashMap<String, Object>();

        // correlation ID
        try {
            if (message.getJMSCorrelationID() != null) {
                map.put(JMSConstants.JMS_COORELATION_ID, message.getJMSCorrelationID());
            }
        } catch (JMSException ignore) {}

        // set the delivery mode as persistent or not
        try {
            map.put(JMSConstants.JMS_DELIVERY_MODE, Integer.toString(message.getJMSDeliveryMode()));
        } catch (JMSException ignore) {}

        // destination name
        try {
            if (message.getJMSDestination() != null) {
                Destination dest = message.getJMSDestination();
                map.put(JMSConstants.JMS_DESTINATION,
                    dest instanceof Queue ?
                        ((Queue) dest).getQueueName() : ((Topic) dest).getTopicName());
            }
        } catch (JMSException ignore) {}

        // expiration
        try {
            map.put(JMSConstants.JMS_EXPIRATION, Long.toString(message.getJMSExpiration()));
        } catch (JMSException ignore) {}

        // if a JMS message ID is found
        try {
            if (message.getJMSMessageID() != null) {
                map.put(JMSConstants.JMS_MESSAGE_ID, message.getJMSMessageID());
            }
        } catch (JMSException ignore) {}

        // priority
        try {
            map.put(JMSConstants.JMS_PRIORITY, Long.toString(message.getJMSPriority()));
        } catch (JMSException ignore) {}

        // redelivered
        try {
            map.put(JMSConstants.JMS_REDELIVERED, Boolean.toString(message.getJMSRedelivered()));
        } catch (JMSException ignore) {}

        // replyto destination name
        try {
            if (message.getJMSReplyTo() != null) {
                Destination dest = message.getJMSReplyTo();
                map.put(JMSConstants.JMS_REPLY_TO,
                    dest instanceof Queue ?
                        ((Queue) dest).getQueueName() : ((Topic) dest).getTopicName());
            }
        } catch (JMSException ignore) {}

        // priority
        try {
            map.put(JMSConstants.JMS_TIMESTAMP, Long.toString(message.getJMSTimestamp()));
        } catch (JMSException ignore) {}

        // message type
        try {
            if (message.getJMSType() != null) {
                map.put(JMSConstants.JMS_TYPE, message.getJMSType());
            }
        } catch (JMSException ignore) {}

        // any other transport properties / headers
        Enumeration e = null;
        try {
            e = message.getPropertyNames();
        } catch (JMSException ignore) {}

        if (e != null) {
            while (e.hasMoreElements()) {
                String headerName = (String) e.nextElement();
                try {
                    map.put(headerName, message.getStringProperty(headerName));
                    continue;
                } catch (JMSException ignore) {}
                try {
                    map.put(headerName, message.getBooleanProperty(headerName));
                    continue;
                } catch (JMSException ignore) {}
                try {
                    map.put(headerName, message.getIntProperty(headerName));
                    continue;
                } catch (JMSException ignore) {}
                try {
                    map.put(headerName, message.getLongProperty(headerName));
                    continue;
                } catch (JMSException ignore) {}
                try {
                    map.put(headerName, message.getDoubleProperty(headerName));
                    continue;
                } catch (JMSException ignore) {}
                try {
                    map.put(headerName, message.getFloatProperty(headerName));
                } catch (JMSException ignore) {}
            }
        }

        return map;
    }


    @Override
    public String getMessageTextPayload(Object message) {
        if (message instanceof TextMessage) {
            try {
                return ((TextMessage) message).getText();
            } catch (JMSException e) {
                handleException("Error reading JMS text message payload", e);
            }
        }
        return null;
    }

    @Override
    public byte[] getMessageBinaryPayload(Object message) {

        if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;

            try {
                bytesMessage.reset();

                byte[] buffer = new byte[1024];
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                for (int bytesRead = bytesMessage.readBytes(buffer); bytesRead != -1;
                     bytesRead = bytesMessage.readBytes(buffer)) {
                    out.write(buffer, 0, bytesRead);
                }
                return out.toByteArray();
                
            } catch (JMSException e) {
                handleException("Error reading JMS binary message payload", e);
            }
        }
        return null;
    }

    // ----------- JMS 1.0.2b compatibility methods -------------
    public static Connection createConnection(ConnectionFactory conFactory, String user,
        String pass, String destinationType) throws JMSException {

        if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(destinationType) ) {
            if (user != null && pass != null) {
                return ((QueueConnectionFactory) conFactory).createQueueConnection(user, pass);
            } else {
                return ((QueueConnectionFactory) conFactory).createQueueConnection();
            }
            
        } else if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(destinationType) ) {
            if (user != null && pass != null) {
                return ((TopicConnectionFactory) conFactory).createTopicConnection(user, pass);
            } else {
                return ((TopicConnectionFactory) conFactory).createTopicConnection();
            }
        } else {
            handleException("Unable to determine type of JMS Connection Factory - i.e Queue/Topic");
        }
        return null;
    }

    public static Session createSession(Connection con,
        boolean transacted, int acknowledgeMode, String destinationType) throws JMSException {

        if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(destinationType) ) {
            return ((QueueConnection) con).createQueueSession(transacted, acknowledgeMode);
        } else if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(destinationType) ) {
            return ((TopicConnection) con).createTopicSession(transacted, acknowledgeMode);
        } else {
            log.debug("JMS destination type not given or invalid, was '" + destinationType +
                    "'. Taking the default value as queue");
            return ((QueueConnection) con).createQueueSession(transacted, acknowledgeMode);
        }
    }

    public static Destination createDestination(Session session, String destName,
        String destinationType) throws JMSException {

        if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(destinationType)) {
            return session.createQueue(destName);
        } else if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(destinationType) ) {
            return session.createTopic(destName);
        } else {
            log.debug("JMS destination type not given or invalid, was '" + destinationType +
                    "'. Taking the default value as queue");
            return session.createQueue(destName);          
        }
    }

    public static void createDestination(ConnectionFactory conFactory,
        String destinationJNDIName, String destinationType) throws JMSException {

        if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(destinationType)) {
            JMSUtils.createJMSQueue(
                ((QueueConnectionFactory) conFactory).createQueueConnection(),
                destinationJNDIName);
        } else if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(destinationType) ) {
            JMSUtils.createJMSTopic(
                ((TopicConnectionFactory) conFactory).createTopicConnection(),
                destinationJNDIName);
        }
    }
    public static MessageConsumer createConsumer(Session session, Destination dest)
        throws JMSException {

        if (dest instanceof Queue) {
            return ((QueueSession) session).createReceiver((Queue) dest);
        } else {
            return ((TopicSession) session).createSubscriber((Topic) dest);
        }
    }

    public static Destination createTemporaryDestination(Session session) throws JMSException {

        if (session instanceof QueueSession) {
            return session.createTemporaryQueue();
        } else {
            return session.createTemporaryTopic();
        }
    }

    public static long getBodyLength(BytesMessage bMsg) {
        try {
            Method mtd = bMsg.getClass().getMethod("getBodyLength", NOARGS);
            if (mtd != null) {
                return (Long) mtd.invoke(bMsg, NOPARMS);
            }
        } catch (Exception e) {
            // JMS 1.0
            if (log.isDebugEnabled()) {
                log.debug("Error trying to determine JMS BytesMessage body length", e);
            }
        }

        // if JMS 1.0
        long length = 0;
        try {
            byte[] buffer = new byte[2048];
            bMsg.reset();
            for (int bytesRead = bMsg.readBytes(buffer); bytesRead != -1;
                 bytesRead = bMsg.readBytes(buffer)) {
                    length += bytesRead;
            }
        } catch (JMSException ignore) {}
        return length;
    }
    
    public static <T> T lookup(Context context, Class<T> clazz, String name)
        throws NamingException {
        
        Object object = context.lookup(name);
        try {
            return clazz.cast(object);
        } catch (ClassCastException ex) {
            // Instead of a ClassCastException, throw an exception with some
            // more information.
            if (object instanceof Reference) {
                Reference ref = (Reference)object;
                handleException("JNDI failed to de-reference Reference with name " +
                        name + "; is the factory " + ref.getFactoryClassName() +
                        " in your classpath?");
                return null;
            } else {
                handleException("JNDI lookup of name " + name + " returned a " +
                        object.getClass().getName() + " while a " + clazz + " was expected");
                return null;
            }
        }
    }
}
