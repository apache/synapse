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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.synapse.transport.base.BaseUtils;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Encapsulate a JMS Connection factory definition within an Axis2.xml
 * <p/>
 * More than one JMS connection factory could be defined within an Axis2 XML
 * specifying the JMSListener as the transportReceiver.
 * <p/>
 * These connection factories are created at the initialization of the
 * transportReceiver, and any service interested in using any of these could
 * specify the name of the factory and the destination through Parameters named
 * JMSConstants.CONFAC_PARAM and JMSConstants.DEST_PARAM as shown below.
 * <p/>
 * <parameter name="transport.jms.ConnectionFactory" locked="true">myQueueConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="true">TestQueue</parameter>
 * <p/>
 * If a connection factory is defined by a parameter named
 * JMSConstants.DEFAULT_CONFAC_NAME in the Axis2 XML, services which does not
 * explicitly specify a connection factory will be defaulted to it - if it is
 * defined in the Axis2 configuration.
 * <p/>
 * e.g.
 * <transportReceiver name="jms" class="org.apache.axis2.transport.jms.JMSListener">
 * <parameter name="myTopicConnectionFactory" locked="false">
 * <parameter name="java.naming.factory.initial" locked="false">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>
 * <parameter name="java.naming.provider.url" locked="false">tcp://localhost:61616</parameter>
 * <parameter name="transport.jms.ConnectionFactoryJNDIName" locked="false">TopicConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="false">myTopicOne, myTopicTwo</parameter>
 * </parameter>
 * <parameter name="myQueueConnectionFactory" locked="false">
 * <parameter name="java.naming.factory.initial" locked="false">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>
 * <parameter name="java.naming.provider.url" locked="false">tcp://localhost:61616</parameter>
 * <parameter name="transport.jms.ConnectionFactoryJNDIName" locked="false">QueueConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="false">myQueueOne, myQueueTwo</parameter>
 * </parameter>
 * <parameter name="default" locked="false">
 * <parameter name="java.naming.factory.initial" locked="false">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>
 * <parameter name="java.naming.provider.url" locked="false">tcp://localhost:61616</parameter>
 * <parameter name="transport.jms.ConnectionFactoryJNDIName" locked="false">ConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="false">myDestinationOne, myDestinationTwo</parameter>
 * </parameter>
 * </transportReceiver>
 */
public class JMSConnectionFactory implements ExceptionListener {

    private static final Log log = LogFactory.getLog(JMSConnectionFactory.class);

    /** The name used for the connection factory definition within Axis2 */
    private String name = null;
    /** The JNDI name of the actual connection factory */
    private String connFactoryJNDIName = null;
    /** Map of destination JNDI names to service names */
    private Map serviceJNDINameMapping = null;
    /** Map of destination JNDI names to destination types*/
    private Map destinationTypeMapping = null;
    /** Map of JMS destination names to service names */
    private Map serviceDestinationNameMapping = null;
    /** JMS Sessions currently active. One session for each Destination / Service */
    private Map jmsSessions = null;
    /** Properties of the connection factory to acquire the initial context */
    private Hashtable jndiProperties = null;
    /** The JNDI Context used - created using the properties */
    private Context context = null;
    /** The actual ConnectionFactory instance held within */
    private ConnectionFactory conFactory = null;
    /** The JMS connection factory type */
    private String connectionFactoryType = null;
    /** The JMS Connection opened */
    private Connection connection = null;
    /** The JMS Message receiver for this connection factory */
    private JMSMessageReceiver jmsMessageReceiver = null;
    /** The axis2 configuration context */
    private ConfigurationContext cfgCtx = null;
    /** if connection dropped, reconnect timeout in milliseconds; default 30 seconds */
    private long reconnectTimeout = 30000;

    /**
     * Create a JMSConnectionFactory for the given [axis2] name the
     * JNDI name of the actual ConnectionFactory
     *
     * @param name the connection factory name specified in the axis2.xml for the
     * TransportListener or the TransportSender using this
     * @param cfgCtx the axis2 configuration context
     */
    public JMSConnectionFactory(String name, ConfigurationContext cfgCtx) {
        this.name = name;
        this.cfgCtx = cfgCtx;
        serviceJNDINameMapping = new HashMap();
        destinationTypeMapping = new HashMap();
        serviceDestinationNameMapping = new HashMap();
        jndiProperties = new Hashtable();
        jmsSessions = new HashMap();
    }


    /**
     * Add a listen destination on this connection factory on behalf of the given service
     *
     * @param destinationJNDIName destination JNDI name
     * @param serviceName     the service to which it belongs
     */
    public void addDestination(String destinationJNDIName, String destinationType, String serviceName) {

        String destinationName = getPhysicalDestinationName(destinationJNDIName);

        if (destinationName == null) {
            log.warn("JMS Destination with JNDI name : " + destinationJNDIName + " does not exist");

            try {
                log.info("Creating a JMS Queue with the JNDI name : " + destinationJNDIName +
                    " using the connection factory definition named : " + name);
                JMSUtils.createDestination(conFactory, destinationJNDIName, destinationType);

                destinationName = getPhysicalDestinationName(destinationJNDIName);
                
            } catch (JMSException e) {
                log.error("Unable to create Destination with JNDI name : " + destinationJNDIName, e);
                BaseUtils.markServiceAsFaulty(
                    serviceName,
                    "Error creating JMS destination : " + destinationJNDIName,
                    cfgCtx.getAxisConfiguration());
                return;
            }
        }

        serviceJNDINameMapping.put(destinationJNDIName, serviceName);
        destinationTypeMapping.put(destinationJNDIName, destinationType);
        serviceDestinationNameMapping.put(destinationName, serviceName);

        log.info("Mapped JNDI name : " + destinationJNDIName + " and JMS Destination name : " +
            destinationName + " against service : " + serviceName);
    }

    /**
     * Abort listening on the JMS destination from this connection factory
     *
     * @param jndiDestinationName the JNDI name of the JMS destination to be removed
     */
    public void removeDestination(String jndiDestinationName) {

        // find and save provider specific Destination name before we delete
        String providerSpecificDestination = getPhysicalDestinationName(jndiDestinationName);
        stoplisteningOnDestination(jndiDestinationName);

        serviceJNDINameMapping.remove(jndiDestinationName);
        if (providerSpecificDestination != null) {
            serviceDestinationNameMapping.remove(providerSpecificDestination);
        }
    }

    /**
     * Begin [or restart] listening for messages on the list of destinations associated
     * with this connection factory. (Called during Axis2 initialization of
     * the Transport receivers, or after a disconnection has been detected)
     *
     * When called from the JMS transport sender, this call simply acquires the actual
     * JMS connection factory from the JNDI, creates a new connection and starts it.
     *
     * @throws JMSException on exceptions
     * @throws NamingException on exceptions
     */
    public synchronized void connectAndListen() throws JMSException, NamingException {

        // if this is a reconnection/re-initialization effort after the detection of a
        // disconnection, close all sessions and the CF connection and re-initialize
        if (connection != null) {
            log.info("Re-initializing the JMS connection factory : " + name);

            Iterator sessionIter = jmsSessions.values().iterator();
            while (sessionIter.hasNext()) {
                try {
                    ((Session) sessionIter.next()).close();
                } catch (JMSException ignore) {}
            }
            try {
                connection.stop();
            } catch (JMSException ignore) {}

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Initializing the JMS connection factory : " + name);
            }
        }

        // get the CF reference freshly [again] from JNDI
        context = new InitialContext(jndiProperties);
        conFactory = (ConnectionFactory) context.lookup(connFactoryJNDIName);
        log.info("Connected to the JMS connection factory : " + connFactoryJNDIName);

        try {
            ConnectionFactory conFac = null;
            QueueConnectionFactory qConFac = null;
            TopicConnectionFactory tConFac = null;
            if (JMSConstants.DESTINATION_TYPE_QUEUE.equals(getConnectionFactoryType())) {
                qConFac = (QueueConnectionFactory) conFactory;
            } else if (JMSConstants.DESTINATION_TYPE_TOPIC.equals(getConnectionFactoryType())) {
                tConFac = (TopicConnectionFactory) conFactory;
            } else {
                handleException("Unable to determine type of Connection Factory - i.e. Queue/Topic", null);
            }

            String user = (String) jndiProperties.get(Context.SECURITY_PRINCIPAL);
            String pass = (String) jndiProperties.get(Context.SECURITY_CREDENTIALS);

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
            
            connection.setExceptionListener(this);

        } catch (JMSException e) {
            handleException("Error connecting to Connection Factory : " + connFactoryJNDIName, e);
        }

        Iterator destJNDINameIter = serviceJNDINameMapping.keySet().iterator();
        while (destJNDINameIter.hasNext()) {
            String destJNDIName = (String) destJNDINameIter.next();
            String destinationType = (String) destinationTypeMapping.get(destJNDIName);
            startListeningOnDestination(destJNDIName, destinationType);
        }

        connection.start(); // indicate readyness to start receiving messages
        log.info("Connection factory : " + name + " initialized...");
    }

    /**
     * Create a session for sending to the given destination and save it on the jmsSessions Map
     * keyed by the destinatin JNDI name
     * @param destinationJNDIname the destination JNDI name
     * @return a JMS Session to send messages to the destination using this connection factory
     */
    public Session getSessionForDestination(String destinationJNDIname) {

        Session session = (Session) jmsSessions.get(destinationJNDIname);

        if (session == null) {
            try {                
                Destination dest = (Destination) getPhysicalDestination(destinationJNDIname);

                if (dest instanceof Topic) {
                    session = ((TopicConnection) connection).
                        createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                } else {
                    session = ((QueueConnection) connection).
                        createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                }

                jmsSessions.put(destinationJNDIname, session);

            } catch (JMSException e) {
                handleException("Unable to create a session using connection factory : " + name, e);
            }
        }
        return session;
    }

    /**
     * Listen on the given destination from this connection factory. Used to
     * start listening on a destination associated with a newly deployed service
     *
     * @param destinationJNDIname the JMS destination to listen on
     */
    public void startListeningOnDestination(String destinationJNDIname, String destinationType) {

        Session session = (Session) jmsSessions.get(destinationJNDIname);
        // if we already had a session open, close it first
        if (session != null) {
            try {
                session.close();
            } catch (JMSException ignore) {}
        }

        try {
            session = JMSUtils.createSession(connection, false, Session.AUTO_ACKNOWLEDGE, destinationType);
            Destination destination = null;

            try {
                destination = (Destination) context.lookup(destinationJNDIname);

            } catch (NameNotFoundException e) {
                log.warn("Cannot find destination : " + destinationJNDIname + ". Creating a Queue");
                destination = JMSUtils.createDestination(session, destinationJNDIname, destinationType);
            }

            MessageConsumer consumer = JMSUtils.createConsumer(session, destination);
            consumer.setMessageListener(jmsMessageReceiver);
            jmsSessions.put(destinationJNDIname, session);

        // catches NameNotFound and JMSExceptions and marks service as faulty    
        } catch (Exception e) {
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException ignore) {}
            }

            BaseUtils.markServiceAsFaulty(
                (String) serviceJNDINameMapping.get(destinationJNDIname),
                "Error looking up JMS destination : " + destinationJNDIname,
                cfgCtx.getAxisConfiguration());
        }
    }

    /**
     * Stop listening on the given destination - for undeployment or stopping of services
     * closes the underlying Session opened to subscribe to the destination
     *
     * @param destinationJNDIname the JNDI name of the JMS destination
     */
    private void stoplisteningOnDestination(String destinationJNDIname) {
        Session session = (Session) jmsSessions.get(destinationJNDIname);
        if (session != null) {
            try {
                session.close();
            } catch (JMSException ignore) {}
        }
    }


    /**
     * Close all connections, sessions etc.. and stop this connection factory
     */
    public void stop() {
        if (connection != null) {
            Iterator sessionIter = jmsSessions.values().iterator();
            while (sessionIter.hasNext()) {
                try {
                    ((Session) sessionIter.next()).close();
                } catch (JMSException ignore) {}
            }
            try {
                connection.close();
            } catch (JMSException e) {
                log.warn("Error shutting down connection factory : " + name, e);
            }
        }
    }

    /**
     * Return the provider specific [physical] Destination name if any
     * for the destination with the given JNDI name
     *
     * @param destinationJndi the JNDI name of the destination
     * @return the provider specific Destination name or null if cannot be found
     */
    private String getPhysicalDestinationName(String destinationJndi) {
        Destination destination = getPhysicalDestination(destinationJndi);

        if (destination != null) {
            try {
                if (destination instanceof Queue) {
                    return ((Queue) destination).getQueueName();
                } else if (destination instanceof Topic) {
                    return ((Topic) destination).getTopicName();
                }
            } catch (JMSException e) {
                log.warn("Error reading Destination name for JNDI destination : " + destinationJndi, e);
            }
        }
        return null;
    }
    
    /**
     * Return the provider specific [physical] Destination if any
     * for the destination with the given JNDI name
     *
     * @param destinationJndi the JNDI name of the destination
     * @return the provider specific Destination or null if cannot be found
     */
    private Destination getPhysicalDestination(String destinationJndi) {
        Destination destination = null;

        try {
            destination = (Destination) context.lookup(destinationJndi);
        } catch (NamingException e) {

            // if we are using ActiveMQ, check for dynamic Queues and Topics
            String provider = (String) jndiProperties.get(Context.INITIAL_CONTEXT_FACTORY);
            if (provider.indexOf("activemq") != -1) {
                try {
                    destination = (Destination) context.lookup(
                        JMSConstants.ACTIVEMQ_DYNAMIC_QUEUE + destinationJndi);
                } catch (NamingException ne) {
                    try {
                        destination = (Destination) context.lookup(
                            JMSConstants.ACTIVEMQ_DYNAMIC_TOPIC + destinationJndi);
                    } catch (NamingException e1) {
                        log.warn("Error looking up destination for JNDI name : " + destinationJndi);
                    }
                }
            }
        }

        return destination;
    }

    /**
     * Return the EPR for the JMS Destination with the given JNDI name
     * when using this connection factory
     * @param jndiDestination the JNDI name of the JMS Destionation
     * @return the EPR for a service using this destination
     */
    public EndpointReference getEPRForDestination(String jndiDestination) {

        StringBuffer sb = new StringBuffer();
        sb.append(JMSConstants.JMS_PREFIX).append(jndiDestination);
        sb.append("?").
            append(JMSConstants.CONFAC_JNDI_NAME_PARAM).
            append("=").append(getConnFactoryJNDIName());
        Iterator props = getJndiProperties().keySet().iterator();
        while (props.hasNext()) {
            String key = (String) props.next();
            String value = (String) getJndiProperties().get(key);
            sb.append("&").append(key).append("=").append(value);
        }

        return new EndpointReference(sb.toString());
    }

    /**
     * Is this connection factory referring to the same underlying connection factory passed in
     *
     * @param o a JMSOutTransport object which specifies a connection factory
     * @return true if this instance could be substituted for the out-transport
     */
    public boolean equals(Object o) {
        if (o instanceof JMSOutTransportInfo) {
            JMSOutTransportInfo trpInfo = (JMSOutTransportInfo) o;

            Map trpProps = trpInfo.getProperties();
            if (equals(trpProps.get(JMSConstants.CONFAC_JNDI_NAME_PARAM), jndiProperties.get(JMSConstants.CONFAC_JNDI_NAME_PARAM))
                &&
                equals(trpProps.get(Context.INITIAL_CONTEXT_FACTORY), jndiProperties.get(Context.INITIAL_CONTEXT_FACTORY))
                &&
                equals(trpProps.get(Context.PROVIDER_URL), jndiProperties.get(Context.PROVIDER_URL))
                &&
                equals(trpProps.get(Context.SECURITY_PRINCIPAL), jndiProperties.get(Context.SECURITY_PRINCIPAL))
                &&
                equals(trpProps.get(Context.SECURITY_CREDENTIALS), jndiProperties.get(Context.SECURITY_CREDENTIALS))) {
                return true;
            }
        }
        return false;
    }

    /**
     *     Prevents NullPointerException when s1 is null.
     *     If both values are null this returns true 
     */
    private boolean equals(Object s1, Object s2) {
        if(s1 == s2) {
            return true;
        } else if(s1 != null && s1.equals(s2)) {
            return true;
        } else {
            return false;
        }
    }

    // -------------------- getters and setters and trivial methods --------------------
    /**
     * Return the service name using the JMS destination given by the JNDI name
     *
     * @param jmsDestinationName the JMS destination name
     * @return the name of the service using the destination
     */
    public String getServiceNameForDestinationName(String jmsDestinationName) {
        return (String) serviceDestinationNameMapping.get(jmsDestinationName);
    }

    /**
     * Return the service name using the JMS destination and its JNDI name
     *
     * @param dest the JMS Destination Queue or Topic
     * @param jmsDestinationName the JMS destination name
     * @return the name of the service using the destination
     */
    public String getServiceNameForDestination(Destination dest, String jmsDestinationName) {
        String serviceName = (String) serviceDestinationNameMapping.get(jmsDestinationName);

        // hack to get around the crazy Active MQ dynamic queue and topic issues
        if (serviceName == null) {
            String provider = (String) getJndiProperties().get(Context.INITIAL_CONTEXT_FACTORY);
            if (provider.indexOf("activemq") != -1) {
                serviceName = getServiceNameForJNDIName(
                    (dest instanceof Queue ?
                        JMSConstants.ACTIVEMQ_DYNAMIC_QUEUE :
                        JMSConstants.ACTIVEMQ_DYNAMIC_TOPIC) + jmsDestinationName);
            }
        }
        return serviceName;
    }

    /**
     * Return the service name using the JMS destination given by the JNDI name
     *
     * @param jndiDestinationName the JNDI name of the destination
     * @return the name of the service using the destination
     */
    public String getServiceNameForJNDIName(String jndiDestinationName) {
        return (String) serviceJNDINameMapping.get(jndiDestinationName);
    }

    public void setConnFactoryJNDIName(String connFactoryJNDIName) {
        this.connFactoryJNDIName = connFactoryJNDIName;
    }

    public Destination getDestination(String destinationJNDIName) {
        try {
            return (Destination) context.lookup(destinationJNDIName);
        } catch (NamingException ignore) {}
        return null;
    }

    public void addJNDIContextProperty(String key, String value) {
        jndiProperties.put(key, value);
    }

    public String getName() {
        return name;
    }

    public String getConnFactoryJNDIName() {
        return connFactoryJNDIName;
    }

    public ConnectionFactory getConFactory() {
        return conFactory;
    }

    public Hashtable getJndiProperties() {
        return jndiProperties;
    }

    public JMSMessageReceiver getJmsMessageReceiver() {
        return jmsMessageReceiver;
    }
    
    public Context getContext() {
        return context;
    }

    public void setJmsMessageReceiver(JMSMessageReceiver jmsMessageReceiver) {
        this.jmsMessageReceiver = jmsMessageReceiver;
    }

    private void handleException(String msg, Exception e) throws AxisJMSException {
        log.error(msg, e);
        throw new AxisJMSException(msg, e);
    }

    public String getConnectionFactoryType() {
      return connectionFactoryType;
    }

    public void setConnectionFactoryType(String connectionFactoryType) {
      this.connectionFactoryType = connectionFactoryType;
    }
    
    public long getReconnectTimeout() {
      return reconnectTimeout;
    }

    public void setReconnectTimeout(long reconnectTimeout) {
      this.reconnectTimeout = reconnectTimeout;
    }

    public void onException(JMSException e) {
        log.error("JMS connection factory " + name + " encountered an error", e);
        boolean wasError = true;

        // try to connect
        // if error occurs wait and try again
        while (wasError == true) {

            try {
                connectAndListen();
                wasError = false;

            } catch (Exception e1) {
                log.warn("JMS reconnection attempt failed for connection factory : " + name, e);
            }

            if (wasError == true) {
                try {
                    log.info("Attempting reconnection for connection factory " + name +
                        " in " + getReconnectTimeout()/1000 +  " seconds");
                    Thread.sleep(getReconnectTimeout());
                } catch (InterruptedException ignore) {}
            }
        } // wasError

    }

    /**
     * Temporarily pause receiving new messages
     */
    public void pause() {
        try {
            connection.stop();
        } catch (JMSException e) {
            handleException("Error pausing JMS connection for factory : " + name, e);
        }
    }

    /**
     * Resume frm temporarily pause
     */
    public void resume() {
        try {
            connection.start();
        } catch (JMSException e) {
            handleException("Error resuming JMS connection for factory : " + name, e);
        }
    }
}
