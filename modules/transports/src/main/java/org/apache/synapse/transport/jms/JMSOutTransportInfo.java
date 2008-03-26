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

import org.apache.axis2.transport.OutTransportInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * The JMS OutTransportInfo is a holder of information to send an outgoing message
 * (e.g. a Response) to a JMS destination. Thus at a minimum a reference to a
 * ConnectionFactory and a Destination are held
 */
public class JMSOutTransportInfo implements OutTransportInfo {

    private static final Log log = LogFactory.getLog(JMSOutTransportInfo.class);

    /**
     * this is a reference to the underlying JMS connection factory when sending messages
     * through connection factories not defined to the transport sender
     */
    private ConnectionFactory connectionFactory = null;
    /**
     * this is a reference to a JMS Connection Factory instance, which has a reference
     * to the underlying actual connection factory, an open connection to the JMS provider
     * and optionally a session already available for use
     */
    private JMSConnectionFactory jmsConnectionFactory = null;
    /** the Destination queue or topic for the outgoing message */
    private Destination destination = null;
    /** the Destination queue or topic for the outgoing message i.e. JMSConstants.DESTINATION_TYPE_QUEUE, DESTINATION_TYPE_TOPIC */
    private String destinationType = JMSConstants.DESTINATION_TYPE_QUEUE;
    /** the Reply Destination queue or topic for the outgoing message */
    private Destination replyDestination = null;
    /** the Reply Destination name */
    private String replyDestinationName = null;
    /** the Reply Destination queue or topic for the outgoing message i.e. JMSConstants.DESTINATION_TYPE_QUEUE, DESTINATION_TYPE_TOPIC */
    private String replyDestinationType = JMSConstants.DESTINATION_TYPE_QUEUE;
    /** the EPR properties when the out-transport info is generated from a target EPR */
    private Hashtable properties = null;
    /** the target EPR string where applicable */
    private String targetEPR = null;
    private String contentType = null;

    /**
     * Creates an instance using the given connection factory and destination
     *
     * @param connectionFactory the connection factory
     * @param dest the destination
     */
    JMSOutTransportInfo(ConnectionFactory connectionFactory, Destination dest) {
        this.connectionFactory = connectionFactory;
        this.destination = dest;
    }

    /**
     * Creates an instance using the given JMS connection factory and destination
     *
     * @param jmsConnectionFactory the JMS connection factory
     * @param dest the destination
     */
    JMSOutTransportInfo(JMSConnectionFactory jmsConnectionFactory, Destination dest) {
        this.jmsConnectionFactory = jmsConnectionFactory;
        this.destination = dest;
    }

    /**
     * Creates and instance using the given URL
     *
     * @param targetEPR the target EPR
     */
    JMSOutTransportInfo(String targetEPR) {
        this.targetEPR = targetEPR;
        if (!targetEPR.startsWith(JMSConstants.JMS_PREFIX)) {
            handleException("Invalid prefix for a JMS EPR : " + targetEPR);
        } else {
            properties = JMSUtils.getProperties(targetEPR);
            String destinationType = (String) properties.get(JMSConstants.DEST_PARAM_TYPE);
            if(destinationType != null) {
                setDestinationType(destinationType);
            }
            String replyDestinationType = (String) properties.get(JMSConstants.REPLY_PARAM_TYPE);
            if(replyDestinationType != null) {
                setReplyDestinationType(replyDestinationType);
            }
            String replyDestinationName = (String) properties.get(JMSConstants.REPLY_PARAM);
            if(replyDestinationName != null) {
                setReplyDestinationName(replyDestinationName);
            }
        }
    }

    /**
     * Provides a lazy load when created with a target EPR. This method performs actual
     * lookup for the connection factory and desination
     */
    public void loadConnectionFactoryFromProperies() {
        if (properties != null) {
            Context context = null;
            try {
                context = new InitialContext(properties);
            } catch (NamingException e) {
                handleException("Could not get an initial context using " + properties, e);
            }
            connectionFactory = getConnectionFactory(context, properties);
            destination = getDestination(context, targetEPR);
            replyDestination = getReplyDestination(context, targetEPR);
        }
    }

    /**
     * Get the referenced ConnectionFactory using the properties from the context
     *
     * @param context the context to use for lookup
     * @param props   the properties which contains the JNDI name of the factory
     * @return the connection factory
     */
    private ConnectionFactory getConnectionFactory(Context context, Hashtable props) {
        try {

            String conFacJndiName = (String) props.get(JMSConstants.CONFAC_JNDI_NAME_PARAM);
            if (conFacJndiName != null) {
                return (ConnectionFactory) context.lookup(conFacJndiName);
            } else {
                handleException("Connection Factory JNDI name cannot be determined");
            }
        } catch (NamingException e) {
            handleException("Connection Factory JNDI name cannot be determined");
        }
        return null;
    }

    /**
     * Get the JMS destination specified by the given URL from the context
     *
     * @param context the Context to lookup
     * @param url     URL
     * @return the JMS destination, or null if it does not exist
     */
    private Destination getDestination(Context context, String url) {
        String destinationName = JMSUtils.getDestination(url);
        try {
            return (Destination) context.lookup(destinationName);
        } catch (NameNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot locate destination : " + destinationName + " using " + url);
            }
        } catch (NamingException e) {
            handleException("Cannot locate destination : " + destinationName + " using " + url, e);
        }
        return null;
    }

    /**
     * Get the JMS reply destination specified by the given URL from the context
     *
     * @param context the Context to lookup
     * @param url     URL
     * @return the JMS destination, or null if it does not exist
     */
    private Destination getReplyDestination(Context context, String url) {
        String replyDestinationName = (String) properties.get(JMSConstants.REPLY_PARAM);
        if(replyDestinationName == null) {
            return null;
        }
        try {
            return (Destination) context.lookup(replyDestinationName);
        } catch (NameNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot locate destination : " + replyDestinationName + " using " + url);
            }
        } catch (NamingException e) {
            handleException("Cannot locate destination : " + replyDestinationName + " using " + url, e);
        }
        return null;
    }

    /**
     * Look up for the given destination
     * @param replyDest
     * @return
     */
    public Destination getReplyDestination(String replyDest) {
        try {
            return (Destination) jmsConnectionFactory.getContext().lookup(replyDest);
        } catch (NameNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot locate reply destination : " + replyDest, e);
            }
        } catch (NamingException e) {
            handleException("Cannot locate reply destination : " + replyDest, e);
        }
        return null;
    }


    private void handleException(String s) {
        log.error(s);
        throw new AxisJMSException(s);
    }

    private void handleException(String s, Exception e) {
        log.error(s, e);
        throw new AxisJMSException(s, e);
    }

    public Destination getDestination() {
        return destination;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public JMSConnectionFactory getJmsConnectionFactory() {
        return jmsConnectionFactory;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Hashtable getProperties() {
        return properties;
    }

    public String getTargetEPR() {
        return targetEPR;
    }

    public String getDestinationType() {
      return destinationType;
    }

    public void setDestinationType(String destinationType) {
      if (destinationType != null) {
        this.destinationType = destinationType;
      }
    }

    public Destination getReplyDestination() {
        return replyDestination;
    }

    public void setReplyDestination(Destination replyDestination) {
        this.replyDestination = replyDestination;
    }

    public String getReplyDestinationType() {
        return replyDestinationType;
    }

    public void setReplyDestinationType(String replyDestinationType) {
        this.replyDestinationType = replyDestinationType;
    }

    public String getReplyDestinationName() {
        return replyDestinationName;
    }

    public void setReplyDestinationName(String replyDestinationName) {
        this.replyDestinationName = replyDestinationName;
    }
}
