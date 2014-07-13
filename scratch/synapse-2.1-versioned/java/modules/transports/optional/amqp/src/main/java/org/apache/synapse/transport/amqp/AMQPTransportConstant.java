/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.transport.amqp;

/**
 * Constant decelerations for the AMQP transport.
 */
public final class AMQPTransportConstant {

    /**
     * The transport prefix for AMQP transport.
     */
    public static final String AMQP_TRANSPORT_PREFIX = "amqp://";

    /**
     * The default connection factory name.
     */
    public static final String DEFAULT_CONNECTION_FACTORY_NAME = "default";

    /**
     * The parameter to specify the Uri of the form amqp://userName:password@hostName:portNumber/virtualHost.
     */
    public static final String PARAMETER_CONNECTION_URI = "transport.amqp.Uri";


    /**
     * The list of broker of the form, host1:port1,host2:port2... which will be used as the address array in AMQP
     * connection to the broker.
     */
    public static final String PARAMETER_BROKER_LIST = "transport.amqp.BrokerList";


    /**
     * The name of the exchange to be used.
     */
    public static final String PARAMETER_EXCHANGE_NAME = "transport.amqp.ExchangeName";


    /**
     * The durability of the exchange. One of durable, transient or auto-deleted.
     */
    public static final String PARAMETER_EXCHANGE_IS_DURABLE = "transport.amqp.IsExchangeDurable";


    /**
     * Should the exchange be deleted if it is no longer in use
     */
    public static final String PARAMETER_EXCHANGE_IS_AUTO_DELETE = "transport.amqp.IsExchangeAutoDelete";

    /**
     * The channel pre fetch size for fair dispatch
     */
    public static final String PARAMETER_CHANNEL_PREFETCH_SIZE = "transport.amqp.ChannelPreFetchSize";

    /**
     * The channel prefetch count for fair dispatch
     */
    public static final String PARAMETER_CHANNEL_PREFETCH_COUNT = "transport.amqp.ChannelPreFetchCountSize";

    /**
     * Should the configuration be used in globally ?
     */
    public static final String PARAMETER_CHANNEL_QOS_GLOBAL = "transport.amqp.IsQoSGlobally";

    /**
     * The type of the exchange. One of fanout, direct, header or topic.
     */
    public static final String PARAMETER_EXCHANGE_TYPE = "transport.amqp.ExchangeType";


    /**
     * Should the exchange be declared as internal? One of true of false.
     */
    public static final String PARAMETER_EXCHANGE_INTERNAL = "transport.amqp.ExchangeInternal";


    /**
     * The name of the exchange that the publisher/consumer should publish/consume message to.
     */
    public static final String PARAMETER_BIND_EXCHANGE = "transport.amqp.BindExchange";


    /**
     * The comma separated binding keys this queue should be bound into exchange.
     */
    public static final String PARAMETER_BINDING_KEYS = "transport.amqp.BindingKeys";


    /**
     * The routing key to be used by the publisher
     */
    public static final String PARAMETER_ROUTING_KEY = "transport.amqp.RoutingKey";

    /**
     * True if requesting a mandatory publishing.
     */
    public static final String PARAMETER_PUBLISHER_MANDATORY_PUBLISH =
            "transport.amqp.MandatoryPublish";


    /**
     * True if requesting an immediate publishing.
     */
    public static final String PARAMETER_PUBLISHER_IMMEDIATE_PUBLISH =
            "transport.amqp.ImmediatePublish";


    /**
     * Use transactions at consumer side if set to true. By default this will be considered false
     * and explicit acknowledgement will be done
     */
    public static final String PARAMETER_CONSUMER_TX = "transport.amqp.ConsumerTx";

    /**
     * Use transactions at producer side, possible values are lwpc(light weight publisher confirm),
     * tx(transaction). tx should be able to set per message basis
     */
    public static final String PROPERTY_PRODUCER_TX = "AMQP_PRODUCER_TX";

    /**
     * The name of the queue
     */
    public static final String PARAMETER_QUEUE_NAME = "transport.amqp.QueueName";

    /**
     * True if the queue is durable
     */
    public static final String PARAMETER_QUEUE_DURABLE = "transport.amqp.IsQueueDurable";


    /**
     * True of the queue is restricted(only within this connection)
     */
    public static final String PARAMETER_QUEUE_RESTRICTED = "transport.amqp.IsQueueRestricted";


    /**
     * True if the queue should be auto deleted
     */
    public static final String PARAMETER_QUEUE_AUTO_DELETE = "transport.amqp.IsQueueAutoDelete";


    /**
     * True if the polling task should wait until the she processed the accepted message.
     * This can be used in conjunction with a single thread polling task(in the whole transport,
     * i.e. only a single AMQP proxy per flow) to achieve in order delivery.
     */
    public static final String PARAMETER_OPERATE_ON_BLOCKING_MODE =
            "transport.amqp.OperateOnBlockingMode";


    /**
     * If a polling task encounter an exception due to some reason(most probably due to broker
     * outage) the number of milliseconds it should be suspended before next re-try.
     */
    public static final String PARAM_INITIAL_RE_CONNECTION_DURATION =
            "initial-reconnect-duration";

    /**
     * If the polling task fails again after the initial re-connection duration
     * {@link AMQPTransportConstant#PARAM_INITIAL_RE_CONNECTION_DURATION}
     * next suspend duration will be calculated using this
     * (PARAM_RE_CONNECTION_PROGRESSION_FACTOR * PARAM_INITIAL_RE_CONNECTION_DURATION).
     */
    public static final String PARAM_RE_CONNECTION_PROGRESSION_FACTOR =
            "reconnection-progression-factor";


    /**
     * The maximum duration to suspend the polling task in case of an error. The current suspend
     * duration will reach this
     * value by following the series,
     * PARAM_RE_CONNECTION_PROGRESSION_FACTOR * PARAM_INITIAL_RE_CONNECTION_DURATION.
     * This upper bound is there
     * because nobody wants to wait a long time until the next re-try if the broker is alive.
     */
    public static final String PARAM_MAX_RE_CONNECTION_DURATION =
            "maximum-reconnection-duration";


    /**
     * The connection factory to be used either with consumer or producer.
     */
    public static final String PARAMETER_CONNECTION_FACTORY_NAME =
            "transport.amqp.ConnectionFactoryName";

    /**
     * In a two-way scenario which connection factory of the senders' should be used to send
     * the response
     */
    public static final String PARAMETER_RESPONSE_CONNECTION_FACTORY_NAME =
            "transport.amqp.ResponseConnectionFactoryName";


    /**
     * The initial delay(in milliseconds) that the polling task should delay before initial attempt.
     * http://docs.oracle.com/javase/6/docs/api/index.html?java/util/concurrent/ScheduledExecutorService.html
     */
    public static final String PARAMETER_SCHEDULED_TASK_INITIAL_DELAY =
            "transport.amqp.ScheduledTaskInitialDelay";

    /**
     * The delay(in milliseconds) that the polling task should delay before next attempt.
     * http://docs.oracle.com/javase/6/docs/api/index.html?java/util/concurrent/ScheduledExecutorService.html
     */
    public static final String PARAMETER_SCHEDULED_TASK_DELAY = "transport.amqp.ScheduledTaskDelay";

    /**
     * The time unit which should use to calculate,
     * {@link AMQPTransportConstant#PARAMETER_SCHEDULED_TASK_INITIAL_DELAY} and
     * {@link AMQPTransportConstant#PARAMETER_SCHEDULED_TASK_DELAY}.
     */
    public static final String PARAMETER_SCHEDULED_TASK_TIME_UNIT =
            "transport.amqp.ScheduledTaskTimeUnit";

    /**
     * Number of concurrent consumers per polling task.
     */
    public static final String PARAMETER_NO_OF_CONCURRENT_CONSUMERS =
            "transport.amqp.NoOfConcurrentConsumers";

    /**
     * Number of dispatching task to use any request messages to actual processing task.
     */
    public static final String PARAMETER_DISPATCHING_TASK_SIZE =
            "transport.amqp.NoOfDispatchingTask";

    /**
     * Use the given channel number if possible. See
     * http://www.rabbitmq.com/releases/rabbitmq-java-client/v3.0.1/rabbitmq-java-client-javadoc-3.0.1/com/rabbitmq/client/Connection.html#createChannel(int)
     */
    public static final String PARAMETER_AMQP_CHANNEL_NUMBER =
            "transport.amqp.ChannelNumber";

    /**
     * Configure the content type as a service parameter
     */
    public static final String PARAMETER_CONFIGURED_CONTENT_TYPE = "transport.amqp.ContentType";

    /**
     * Message context property to set the AMQP message content type.
     */
    public static final String PROPERTY_AMQP_CONTENT_TYPE = "AMQP_CONTENT_TYPE";

    /**
     * Message context property to set the AMQP message encoding.
     */
    public static final String PROPERTY_AMQP_CONTENT_ENCODING = "AMQP_CONTENT_ENCODING";


    public static final String AMQP_HEADER = "AMQP_HEADER";

    /**
     * Message context property to set the AMQP message delivery mode.
     */
    public static final String PROPERTY_AMQP_DELIVER_MODE = "AMQP_DELIVERY_MODE";

    /**
     * Message context property to set the AMQP message priority.
     */
    public static final String PROPERTY_AMQP_PRIORITY = "AMQP_PRIORITY";

    /**
     * Message context property to set the AMQP message correlation id.
     */
    public static final String PROPERTY_AMQP_CORRELATION_ID = "AMQP_CORRELATION_ID";


    /**
     * Message context property to set the AMQP message reply to header.
     */
    public static final String PROPERTY_AMQP_REPLY_TO = "AMQP_REPLY_TO";


    /**
     * Message context property to set the AMQP expiration.
     */
    public static final String PROPERTY_AMQP_EXPIRATION = "AMQP_EXPIRATION";


    /**
     * Message context property to set the message id of the AMQP message.
     */
    public static final String PROPERTY_AMQP_MESSAGE_ID = "AMQP_MESSAGE_ID";


    /**
     * Message context property to set the timestamp of the AMQP message.
     */
    public static final String PROPERTY_AMQP_TIME_STAMP = "AMQP_TIME_STAMP";


    /**
     * Message context property to set the type of the AMQP message.
     */
    public static final String PROPERTY_AMQP_TYPE = "AMQP_TYPE";


    /**
     * Message context property to set the AMQP user id.
     */
    public static final String PROPERTY_AMQP_USER_ID = "AMQP_USER_ID";


    /**
     * Message context property to set the AMQP app id.
     */
    public static final String PROPERTY_AMQP_APP_ID = "AMQP_APP_ID";


    /**
     * Message context property to set the AMQP cluster id.
     */
    public static final String PROPERTY_AMQP_CLUSTER_ID = "AMQP_CLUSTER_ID";


    /**
     * Configure the executor service worker pool size.
     */

    public static final String PARAM_CONNECTION_FACTORY_POOL_SIZE = "connection-factory-pool-size";

    public static final int CONNECTION_FACTORY_POOL_DEFAULT = 20;

    public static final String PARAM_RESPONSE_HANDLING_POOL_SIZE = "response-handling-pool-size";

    public static final int RESPONSE_HANDLING_POOL_DEFAULT = 20;

    public static final String PARAM_WORKER_POOL_SIZE = "worker-pool-size";

    public static final int WORKER_POOL_DEFAULT = 75;

    public static final String PARAM_SEMAPHORE_TIME_OUT = "semaphore-time-out";

    public static final String AMQP_CORRELATION_ID = "AMQP_CORRELATION_ID";

    public static final String AMQP_TRANSPORT_BUFFER_KEY = "AMQP_TRANSPORT_BUFFER_KEY";

    public static final String AMQP_USE_TX = "tx";

    public static final String AMQP_USE_LWPC = "lwpc";

    public static final String DEFAULT_CONTENT_TYPE = "application/xml";

    public static final String ROUTING_KEY_DELIMITER = ",";

    public static final String RESPONSE_CONNECTION_FACTORY_NAME = "RESPONSE_CONNECTION_FACTORY_NAME";

}