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

import org.apache.axis2.client.Options;

public class JMSConstants {

    /**
     * The prefix indicating an Axis JMS URL
     */
    public static final String JMS_PREFIX = "jms:/";

    public static final String ACTIVEMQ_DYNAMIC_QUEUE = "dynamicQueues/";
    public static final String ACTIVEMQ_DYNAMIC_TOPIC = "dynamicTopics/";

    //------------------------------------ defaults ------------------------------------
    /**
     * The local (Axis2) JMS connection factory name of the default connection
     * factory to be used, if a service does not explicitly state the connection
     * factory it should be using by a Parameter named JMSConstants.CONFAC_PARAM
     */
    public static final String DEFAULT_CONFAC_NAME = "default";
    /**
     * The default JMS time out waiting for a reply
     */
    public static final long DEFAULT_JMS_TIMEOUT = Options.DEFAULT_TIMEOUT_MILLISECONDS;

    //-------------------------- services.xml parameters --------------------------------
    /**
     * The Parameter name indicating a JMS destination for requests
     */
    public static final String DEST_PARAM = "transport.jms.Destination";
    /**
     * The Parameter name indicating a JMS destination type for requests. i.e. DESTINATION_TYPE_QUEUE, DESTINATION_TYPE_TOPIC
     */
    public static final String DEST_PARAM_TYPE = "transport.jms.DestinationType";
    /**
     * The Parameter name indicating the response JMS destination
     */
    public static final String REPLY_PARAM = "transport.jms.ReplyDestination";
    /**
     * The Parameter name indicating the response JMS destination. i.e. DESTINATION_TYPE_QUEUE, DESTINATION_TYPE_TOPIC
     */
    public static final String REPLY_PARAM_TYPE = "transport.jms.ReplyDestinationType";
    
    /**
     * Values used for DEST_PARAM_TYPE, REPLY_PARAM_TYPE
     */
    public static final String DESTINATION_TYPE_QUEUE = "queue";
    public static final String DESTINATION_TYPE_TOPIC = "topic";

    /**
     * The Parameter name of an Axis2 service, indicating the JMS connection
     * factory which should be used to listen for messages for it. This is
     * the local (Axis2) name of the connection factory and not the JNDI name
     */
    public static final String CONFAC_PARAM = "transport.jms.ConnectionFactory";
    /**
     * If reconnect timeout if connection error occurs in seconds
     */
    public static final String RECONNECT_TIMEOUT = "transport.jms.ReconnectTimeout";
    /**
     * Connection factory type if using JMS 1.0, either DESTINATION_TYPE_QUEUE or DESTINATION_TYPE_TOPIC
     */
    public static final String CONFAC_TYPE = "transport.jms.ConnectionFactoryType";
    /**
     * The Parameter name indicating the JMS connection factory JNDI name
     */
    public static final String CONFAC_JNDI_NAME_PARAM = "transport.jms.ConnectionFactoryJNDIName";
    

    //------------ message context / transport header properties and client options ------------
    /**
     * A MessageContext property or client Option stating the JMS message type
     */
    public static final String JMS_MESSAGE_TYPE = "JMS_MESSAGE_TYPE";
    /**
     * The message type indicating a BytesMessage. See JMS_MESSAGE_TYPE
     */
    public static final String JMS_BYTE_MESSAGE = "JMS_BYTE_MESSAGE";
    /**
     * The message type indicating a TextMessage. See JMS_MESSAGE_TYPE
     */
    public static final String JMS_TEXT_MESSAGE = "JMS_TEXT_MESSAGE";
    /**
     * A MessageContext property or client Option stating the time to wait for a response JMS message
     */
    public static final String JMS_WAIT_REPLY = "JMS_WAIT_REPLY";
    /**
     * A MessageContext property or client Option stating the JMS correlation id
     */
    public static final String JMS_COORELATION_ID = "JMS_COORELATION_ID";
    /**
     * A MessageContext property or client Option stating the JMS message id
     */
    public static final String JMS_MESSAGE_ID = "JMS_MESSAGE_ID";
    /**
     * A MessageContext property or client Option stating the JMS delivery mode
     */
    public static final String JMS_DELIVERY_MODE = "JMS_DELIVERY_MODE";
    /**
     * A MessageContext property or client Option stating the JMS destination
     */
    public static final String JMS_DESTINATION = "JMS_DESTINATION";
    /**
     * A MessageContext property or client Option stating the JMS expiration
     */
    public static final String JMS_EXPIRATION = "JMS_EXPIRATION";
    /**
     * A MessageContext property or client Option stating the JMS priority
     */
    public static final String JMS_PRIORITY = "JMS_PRIORITY";
    /**
     * A MessageContext property stating if the message is a redelivery
     */
    public static final String JMS_REDELIVERED = "JMS_REDELIVERED";
    /**
     * A MessageContext property or client Option stating the JMS replyTo
     */
    public static final String JMS_REPLY_TO = "JMS_REPLY_TO";
    /**
     * A MessageContext property or client Option stating the JMS timestamp
     */
    public static final String JMS_TIMESTAMP = "JMS_TIMESTAMP";
    /**
     * A MessageContext property or client Option stating the JMS type
     */
    public static final String JMS_TYPE = "JMS_TYPE";
}
