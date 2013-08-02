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
package org.apache.synapse.transport.amqp.pollingtask;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.amqp.*;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactory;
import org.apache.synapse.transport.amqp.ha.AMQPTransportReconnectHandler;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The factory implementation for {@link AMQPTransportPollingTask}. Polling task(with multiple)
 * consumers will be deployed for each deployed service.
 */
public class AMQPTransportPollingTaskFactory {

    private static Log log = LogFactory.getLog(AMQPTransportPollingTaskFactory.class);

    public static AMQPTransportPollingTask createPollingTaskForService(
            AxisService service,
            ScheduledExecutorService pool,
            AMQPTransportEndpoint endpoint,
            AMQPTransportConnectionFactory connectionFactory,
            AMQPTransportReconnectHandler haHandler) throws AxisFault {

        Map<String, String> svcParam =
                AMQPTransportUtils.getServiceStringParameters(service.getParameters());
        Map<String, String> conFacParam = connectionFactory.getParameters();


        AMQPTransportPollingTask pt = new AMQPTransportPollingTask();

        pt.setServiceName(service.getName());
        pt.setEndpoint(endpoint);
        pt.setPollingTaskScheduler(pool);
        pt.setHaHandler(haHandler);

        // set buffers to hold request/response messages for this task
        pt.setBuffers(new AMQPTransportBuffers());

        String exchangeName = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_NAME, svcParam, conFacParam);
        pt.setExchangeName(exchangeName);

        Boolean isDurable = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_IS_DURABLE, svcParam, conFacParam);
        if (isDurable != null) {
            pt.setExchangeDurable(isDurable);
        }

        Boolean isAutoDelete = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_IS_AUTO_DELETE, svcParam, conFacParam);
        if (isAutoDelete != null) {
            pt.setExchangeAutoDelete(isAutoDelete);
        }

        String exchangeType = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_TYPE, svcParam, conFacParam);
        if (exchangeType != null) {
            if (exchangeName == null) {
                throw new AxisFault("Possible configuration error. No exchange name provided but " +
                        "exchange type is set to '" + exchangeType + "'");
            }

            pt.setExchangeType(exchangeType);
        }

        Boolean isInternalExchange = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_EXCHANGE_INTERNAL, svcParam, conFacParam);
        if (isInternalExchange != null) {
            if (exchangeName == null) {
                throw new AxisFault("Possible configuration error. No exchange name provided but " +
                        "exchange restricted as " + (isInternalExchange ? "internal." : "external."));
            }
            pt.setInternalExchange(isInternalExchange);
        }

        pt.setChannel(connectionFactory.getChannel());
        pt.setConnectionFactoryName(connectionFactory.getName());

        String responseConFac = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_RESPONSE_CONNECTION_FACTORY_NAME,
                svcParam, conFacParam);
        if (responseConFac != null) {
            pt.setResponseConnectionFactory(responseConFac);
        }

        String consumerExchange = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_BIND_EXCHANGE, svcParam, conFacParam);
        if (consumerExchange != null) {
            if (exchangeName != null && !consumerExchange.equals(exchangeName)) {
                log.warn("Possible configuration error? Exchange name is set to '" +
                        exchangeName + "' and consumer's exchange name is set to '" +
                        consumerExchange + "'");
            }
            pt.setConsumerExchangeName(consumerExchange);
        }

        String bindingKeyString = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_BINDING_KEYS, svcParam, conFacParam);

        if (bindingKeyString != null) {
            pt.setBindingsKeys(AMQPTransportUtils.split(
                    bindingKeyString, AMQPTransportConstant.ROUTING_KEY_DELIMITER));
        }

        String queueName = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_QUEUE_NAME, svcParam, conFacParam);
        if (queueName == null) {
            queueName = service.getName(); // set the service name as the queue name for default.
        }
        pt.setQueueName(queueName);

        String configuredContentType = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_CONFIGURED_CONTENT_TYPE, svcParam, conFacParam);
        if (configuredContentType != null) {
            pt.setConfiguredContentType(configuredContentType);
        }

        Boolean isQueueDurable = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_QUEUE_DURABLE, svcParam, conFacParam);
        if (isDurable != null) {
            pt.setQueueDurable(isQueueDurable);
        }

        Boolean isQueueRestricted = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_QUEUE_RESTRICTED, svcParam, conFacParam);
        if (isQueueRestricted != null) {
            pt.setQueueRestricted(isQueueRestricted);
        }

        Boolean isQueueAutoDelete = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_QUEUE_AUTO_DELETE, svcParam, conFacParam);
        if (isQueueAutoDelete != null) {
            pt.setQueueAutoDelete(isQueueAutoDelete);
        }

        Boolean isBlockingMode = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_OPERATE_ON_BLOCKING_MODE, svcParam, conFacParam);
        if (isBlockingMode != null) {
            pt.setBlockingMode(isBlockingMode);
        }

        try {
            Integer noOfConsumers = AMQPTransportUtils.getOptionalIntParameter(
                    AMQPTransportConstant.PARAMETER_NO_OF_CONCURRENT_CONSUMERS,
                    svcParam, conFacParam);
            if (noOfConsumers != null) {
                pt.setNoOfConcurrentConsumers(noOfConsumers);
            }
        } catch (AMQPTransportException e) {
            throw new AxisFault("Could not assign the number of concurrent consumers", e);
        }

        try {
            Integer dispatchingTask = AMQPTransportUtils.getOptionalIntParameter(
                    AMQPTransportConstant.PARAMETER_DISPATCHING_TASK_SIZE,
                    svcParam, conFacParam);
            if (dispatchingTask != null) {
                pt.setNoOfDispatchingTask(dispatchingTask);
            }
        } catch (AMQPTransportException e) {
            throw new AxisFault("Could not assign number of dispatching task value", e);
        }

        Boolean isUseTx = AMQPTransportUtils.getOptionalBooleanParameter(
                AMQPTransportConstant.PARAMETER_CONSUMER_TX, svcParam, conFacParam);
        if (isUseTx != null) {
            pt.setUseTx(isUseTx);
        }

        try {
            Integer initialDelay = AMQPTransportUtils.getOptionalIntParameter(
                    AMQPTransportConstant.PARAMETER_SCHEDULED_TASK_INITIAL_DELAY,
                    svcParam, conFacParam);
            if (initialDelay != null) {
                pt.setScheduledTaskInitialDelay(initialDelay.intValue());
            }
        } catch (AMQPTransportException e) {
            throw new AxisFault("Could not assign the scheduled task initial delay value", e);
        }

        try {
            Integer delay = AMQPTransportUtils.getOptionalIntParameter(
                    AMQPTransportConstant.PARAMETER_SCHEDULED_TASK_INITIAL_DELAY,
                    svcParam, conFacParam);
            if (delay != null) {
                pt.setScheduledTaskDelay(delay.intValue());
            }
        } catch (AMQPTransportException e) {
            throw new AxisFault("Could not assign the scheduled task delay value", e);
        }

        String timeUnit = AMQPTransportUtils.getOptionalStringParameter(
                AMQPTransportConstant.PARAMETER_SCHEDULED_TASK_TIME_UNIT,
                svcParam, conFacParam);

        if (timeUnit != null) {
            pt.setScheduledTaskTimeUnit(getTimeUnit(timeUnit));
        }

        if (log.isDebugEnabled()) {
            log.debug("A polling task for the service '" + service.getName() + "' was produced with " +
                    "following parameters.\n" +
                    "Exchange Name: '" + pt.getExchangeName() + "'\n" +
                    "Exchange Type: '" + pt.getExchangeType() + "'\n" +
                    "Exchange Durable?: '" + pt.isExchangeDurable() + "'\n" +
                    "Exchange AutoDelete?: '" + pt.isExchangeAutoDelete() + "\n" +
                    "Is internal exchange: '" + pt.isInternalExchange() + "'\n" +
                    "Consumer Exchange: " + pt.getConsumerExchangeName() + "'\n" +
                    "Routing Keys: '" + bindingKeyString + "'\n" +
                    "QueueName: '" + pt.getQueueName() + "'\n" +
                    "Is queue durable: '" + pt.isQueueDurable() + "'\n" +
                    "Is queue restricted: '" + pt.isQueueRestricted() + "'\n" +
                    "Is queue auto deleted: '" + pt.isQueueAutoDelete() + "'\n" +
                    "Is blocking mode: '" + pt.isBlockingMode() + "'\n" +
                    "Number of concurrent consumers: '" + pt.getNoOfConcurrentConsumers() + "'\n" +
                    "Number of dispatching task: '" + pt.getNoOfDispatchingTask() + "'");
        }

        return pt;
    }

    private static TimeUnit getTimeUnit(String timeUnit) {

        if ("days".equals(timeUnit)) {
            return TimeUnit.DAYS;
        } else if ("hours".equals(timeUnit)) {
            return TimeUnit.HOURS;
        } else if ("minutes".equals(timeUnit)) {
            return TimeUnit.MINUTES;
        } else if ("seconds".equals(timeUnit)) {
            return TimeUnit.SECONDS;
        } else if ("milliseconds".equals(timeUnit)) {
            return TimeUnit.MILLISECONDS;
        } else {
            return TimeUnit.MICROSECONDS;
        }
    }
}