package org.apache.synapse.transport.amqp;

import org.apache.axis2.client.Options;

public class AMQPConstants
{

    /**
     * The prefix indicating an Axis JMS URL
     */
    public static final String AMQP_PREFIX = "amqp:";

    //------------------------------------ defaults ------------------------------------
    /**
     * The local (Axis2) AMQP connection name of the default connection
     * to be used.
     */
    public static final String DEFAULT_CONNECTION = "default";
    /**
     * The default AMQP time out waiting for a reply
     */
    public static final long DEFAULT_AMQP_TIMEOUT = Options.DEFAULT_TIMEOUT_MILLISECONDS;

    //-------------------------- axis2.xml parameters --------------------------------
    /** Connection URL specified in the axis2.xml or services.xml */
    public static final String CONNECTION_URL_PARAM = "transport.amqp.ConnectionURL";

    /** default exchange name specified axis2.xml */
    public static final String EXCHANGE_NAME_PARAM = "transport.amqp.ExchangeName";

    /** default exchange type specified axis2.xml */
    public static final String EXCHANGE_TYPE_PARAM = "transport.amqp.ExchangeType";

    //-------------------------- services.xml parameters --------------------------------
    /** routing key specified in the services.xml */
    public static final String BINDING_ROUTING_KEY_ATTR = "routingKey";

    /** exchange name specified in the services.xml */
    public static final String BINDING_EXCHANGE_NAME_ATTR = "exchangeName";

    /** exchange type specified in the services.xml */
    public static final String BINDING_EXCHANGE_TYPE_ATTR = "exchangeType";

    /** bindings specified in the services.xml */
    public static final String BINDINGS_PARAM = "transport.amqp.Bindings";

    /** bindings specified in the services.xml */
    public static final String BINDINGS_PRIMARY_ATTR = "primary";

    /**
     * The Parameter name indicating the response AMQP destination
     */
    public static final String REPLY_EXCHANGE_TYPE_PARAM = "transport.amqp.ReplyExchangeType";
    public static final String REPLY_EXCHANGE_NAME_PARAM = "transport.amqp.ReplyExchangeName";
    /**
     * The Parameter name indicating the response AMQP destination class.Ex direct,topic,fannot ..etc
     */
    public static final String REPLY_ROUTING_KEY_PARAM = "transport.amqp.ReplyRoutingKey";

    /**
     * The Parameter name of an Axis2 service, indicating the AMQP connection
     * which should be used to listen for messages for it.
     */
    public static final String CONNECTION_NAME_PARAM = "transport.amqp.ConnectionName";
    /**
     * If reconnect timeout if connection error occurs in seconds
     */
    public static final String RECONNECT_TIMEOUT = "transport.amqp.ReconnectTimeout";

    //------------ message context / transport header properties and client options ------------
    /**
     * A MessageContext property or client Option stating the time to wait for a response JMS message
     */
    public static final String AMQP_WAIT_REPLY = "AMQP_WAIT_REPLY";
    /**
     * A MessageContext property or client Option stating the AMQP correlation id
     */
    public static final String AMQP_CORELATION_ID = "AMQP_CORELATION_ID";
    /**
     * A MessageContext property or client Option stating the AMQP message id
     */
    public static final String AMQP_MESSAGE_ID = "AMQP_MESSAGE_ID";
    /**
     * A MessageContext property or client Option stating the AMQP delivery mode
     */
    public static final String AMQP_DELIVERY_MODE = "AMQP_DELIVERY_MODE";
    /**
     * A MessageContext property or client Option stating the AMQP destination
     */
    public static final String AMQP_EXCHANGE_NAME = "AMQP_EXCHANGE_NAME";

    public static final String AMQP_EXCHANGE_TYPE = "AMQP_EXCHANGE_TYPE";

    public static final String AMQP_ROUTING_KEY = "AMQP_ROUTING_KEY";
    /**
     * A MessageContext property or client Option stating the AMQP expiration
     */
    public static final String AMQP_EXPIRATION = "AMQP_EXPIRATION";
    /**
     * A MessageContext property or client Option stating the AMQP priority
     */
    public static final String AMQP_PRIORITY = "AMQP_PRIORITY";
    /**
     * A MessageContext property stating if the message is a redelivery
     */
    public static final String AMQP_REDELIVERED = "AMQP_REDELIVERED";
    /**
     * A MessageContext property or client Option stating the AMQP replyTo
     */
    public static final String AMQP_REPLY_TO_EXCHANGE_NAME = "AMQP_REPLY_TO_EXCHANGE_NAME";

    public static final String AMQP_REPLY_TO_EXCHANGE_TYPE = "AMQP_REPLY_TO_EXCHANGE_TYPE";

    public static final String AMQP_REPLY_TO_ROUTING_KEY = "AMQP_REPLY_TO_ROUTING_KEY";
    /**
     * A MessageContext property or client Option stating the AMQP timestamp
     */
    public static final String AMQP_TIMESTAMP = "AMQP_TIMESTAMP";
    /**
     * A MessageContext property or client Option stating the AMQP type
     */
    public static final String AMQP_CONTENT_TYPE = "AMQP_CONTENT_TYPE";
}
