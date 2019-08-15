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

import com.rabbitmq.client.*;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.amqp.*;
import org.apache.synapse.transport.amqp.ha.AMQPTransportHABrokerEntry;
import org.apache.synapse.transport.amqp.ha.AMQPTransportHAEntry;
import org.apache.synapse.transport.amqp.ha.AMQPTransportReconnectHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * The polling task deploy for each services exposed on AMQP transport. This task
 */
public class AMQPTransportPollingTask {

    private static Log log = LogFactory.getLog(AMQPTransportPollingTask.class);

    /**
     * State of the current polling task.
     */
    private enum TASK_STATE {
        STOPPED, STARTED, FAILURE

    }

    /**
     * The name of the service this polling task belongs to.
     */
    private String serviceName;

    /**
     * The exchange of this polling task belongs to.
     * {@link AMQPTransportConstant#PARAMETER_EXCHANGE_NAME}
     */
    private String exchangeName = null;


    /**
     * The durability of this exchange. Default non durable(i.e. transient).
     * {@link AMQPTransportConstant#PARAMETER_EXCHANGE_IS_DURABLE}
     */
    private boolean isExchangeDurable = false;


    /**
     * Should the exchange that will be declared by this polling task should be auto deleted ?
     * {@link AMQPTransportConstant#PARAMETER_EXCHANGE_IS_AUTO_DELETE}
     */
    private boolean isExchangeAutoDelete = true;


    /**
     * The type of the exchange. Default to direct type.
     * {@link AMQPTransportConstant#PARAMETER_EXCHANGE_TYPE}
     */
    private String exchangeType = "direct";

    /**
     * True if this is an internal exchange.
     * {@link AMQPTransportConstant#PARAMETER_EXCHANGE_INTERNAL}
     */
    private boolean isInternalExchange = false;

    /**
     * The name of the exchange to which the consumer should bind into.
     * {@link AMQPTransportConstant#PARAMETER_BIND_EXCHANGE} at consumer side.
     */
    private String consumerExchangeName = null;

    /**
     * The list of binding keys at consumer side.
     * {@link AMQPTransportConstant#PARAMETER_BINDING_KEYS}
     */
    private String[] bindingsKeys = null;

    /**
     * Should this task manager be participated in a tx(not distributed)?, by default this is false
     * and auto acknowledgment will be used
     */
    private boolean isUseTx = false;


    /**
     * The name of the queue this consumer should bind to.{
     *
     * @link AMQPTransportConstant#PARAMETER_QUEUE_NAME}
     */
    private String queueName = null;


    /**
     * True if this queue should be durable. {@link AMQPTransportConstant#PARAMETER_QUEUE_DURABLE}
     */
    private boolean isQueueDurable = false;

    /**
     * True if this queue should be restricted.
     * {@link AMQPTransportConstant#PARAMETER_QUEUE_RESTRICTED}
     */
    private boolean isQueueRestricted = false;

    /**
     * True if this queue should be deleted automatically.
     * {@link AMQPTransportConstant#PARAMETER_QUEUE_AUTO_DELETE}
     */
    private boolean isQueueAutoDelete = true;

    /**
     * Should this polling task should wait until processed the current message.
     * {@link AMQPTransportConstant#PARAMETER_OPERATE_ON_BLOCKING_MODE}
     */
    private boolean isBlockingMode = false;


    /**
     * Number of concurrent consumers per this polling task.
     * {@link AMQPTransportConstant#PARAMETER_NO_OF_CONCURRENT_CONSUMERS}
     */
    private int noOfConcurrentConsumers = 2;

    /**
     * The name of the connectionFactory this service is bound to.
     * {@link AMQPTransportConstant#PARAMETER_CONNECTION_FACTORY_NAME}
     */
    private String connectionFactoryName;

    /**
     * The name of the connection factory that this service should be using to send
     * the response in a two way scenario. Default to null
     * {@link AMQPTransportConstant#PARAMETER_RESPONSE_CONNECTION_FACTORY_NAME}
     */
    private String responseConnectionFactory = null;

    /**
     * The initial delay the scheduled task should wait. Don't wait by default.
     * {@link AMQPTransportConstant#PARAMETER_SCHEDULED_TASK_INITIAL_DELAY}
     */
    private long scheduledTaskInitialDelay = 0;

    /**
     * The delay that the scheduled task should wait.
     * {@link AMQPTransportConstant#PARAMETER_SCHEDULED_TASK_DELAY}
     */
    private long scheduledTaskDelay = 1;

    /**
     * The time unit for scheduled task delay.
     * {@link AMQPTransportConstant#PARAMETER_SCHEDULED_TASK_TIME_UNIT}.
     */
    private TimeUnit scheduledTaskTimeUnit = TimeUnit.MILLISECONDS;

    /**
     * The number of tasks per deployed service for dispatching request messages into worker tasks.
     */
    private int noOfDispatchingTask = 2;

    /**
     * The worker pool for I/O, dispatching and actual processing.
     */
    private ScheduledExecutorService pollingTaskScheduler = null;

    private AMQPTransportEndpoint endpoint = null;

    /**
     * The buffers which keeps request/response messages until pick by processing/response tasks.
     */
    private AMQPTransportBuffers buffers = null;

    /**
     * The AMQP channel to use.
     */
    private Channel channel;

    private String configuredContentType = AMQPTransportConstant.DEFAULT_CONTENT_TYPE;

    private List<ScheduledFuture<?>> taskFutureList = new ArrayList<ScheduledFuture<?>>();


    private AMQPTransportReconnectHandler haHandler;

    public void setUseTx(boolean useTx) {
        isUseTx = useTx;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setConfiguredContentType(String configuredContentType) {
        this.configuredContentType = configuredContentType;
    }

    public void setBuffers(AMQPTransportBuffers buffers) {
        this.buffers = buffers;
    }

    public void setEndpoint(AMQPTransportEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public void setExchangeDurable(boolean isExchangeDurable) {
        this.isExchangeDurable = isExchangeDurable;
    }

    public void setExchangeAutoDelete(boolean exchangeAutoDelete) {
        isExchangeAutoDelete = exchangeAutoDelete;
    }

    public void setExchangeType(String exchangeType) {
        this.exchangeType = exchangeType;
    }

    public void setInternalExchange(boolean internalExchange) {
        isInternalExchange = internalExchange;
    }

    public void setConsumerExchangeName(String consumerExchangeName) {
        this.consumerExchangeName = consumerExchangeName;
    }

    public void setBindingsKeys(String[] bindingsKeys) {
        this.bindingsKeys = bindingsKeys;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public void setQueueDurable(boolean queueDurable) {
        isQueueDurable = queueDurable;
    }

    public void setQueueRestricted(boolean queueRestricted) {
        isQueueRestricted = queueRestricted;
    }

    public void setQueueAutoDelete(boolean queueAutoDelete) {
        isQueueAutoDelete = queueAutoDelete;
    }

    public void setBlockingMode(boolean blockingMode) {
        isBlockingMode = blockingMode;
    }

    public void setNoOfConcurrentConsumers(int noOfConcurrentConsumers) {
        this.noOfConcurrentConsumers = noOfConcurrentConsumers;
    }

    public void setConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName = connectionFactoryName;
    }

    public void setScheduledTaskInitialDelay(int scheduledTaskInitialDelay) {
        this.scheduledTaskInitialDelay = scheduledTaskInitialDelay;
    }

    public void setScheduledTaskDelay(int scheduledTaskDelay) {
        this.scheduledTaskDelay = scheduledTaskDelay;
    }

    public void setScheduledTaskTimeUnit(TimeUnit scheduledTaskTimeUnit) {
        this.scheduledTaskTimeUnit = scheduledTaskTimeUnit;
    }

    public void setNoOfDispatchingTask(int noOfDispatchingTask) {
        this.noOfDispatchingTask = noOfDispatchingTask;
    }

    public void setPollingTaskScheduler(ScheduledExecutorService pollingTaskScheduler) {
        this.pollingTaskScheduler = pollingTaskScheduler;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public boolean isExchangeDurable() {
        return isExchangeDurable;
    }

    public boolean isExchangeAutoDelete() {
        return isExchangeAutoDelete;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public boolean isInternalExchange() {
        return isInternalExchange;
    }

    public String getConsumerExchangeName() {
        return consumerExchangeName;
    }

    public String[] getBindingsKeys() {
        return bindingsKeys;
    }

    public String getQueueName() {
        return queueName;
    }

    public boolean isQueueDurable() {
        return isQueueDurable;
    }

    public boolean isQueueRestricted() {
        return isQueueRestricted;
    }

    public boolean isQueueAutoDelete() {
        return isQueueAutoDelete;
    }

    public boolean isBlockingMode() {
        return isBlockingMode;
    }

    public int getNoOfConcurrentConsumers() {
        return noOfConcurrentConsumers;
    }

    public TimeUnit getScheduledTaskTimeUnit() {
        return scheduledTaskTimeUnit;
    }

    public int getNoOfDispatchingTask() {
        return noOfDispatchingTask;
    }

    public ExecutorService getPollingTaskScheduler() {
        return pollingTaskScheduler;
    }

    public AMQPTransportEndpoint getEndpoint() {
        return endpoint;
    }

    public void setResponseConnectionFactory(String responseConnectionFactory) {
        this.responseConnectionFactory = responseConnectionFactory;
    }

    public void setHaHandler(AMQPTransportReconnectHandler haHandler) {
        this.haHandler = haHandler;
    }

    /**
     * Start the polling task for this service
     */
    public synchronized void start() throws AMQPTransportException {

        try {
            if (exchangeName != null) {

                channel.exchangeDeclare(
                        exchangeName,
                        exchangeType,
                        isExchangeDurable,
                        isExchangeAutoDelete,
                        isInternalExchange,
                        null);

                String newQueueName = channel.queueDeclare().getQueue();
                log.info("QueueName is set to '" + newQueueName + "' for service '" + serviceName + "'");
                queueName = newQueueName; // when there is an exchange, it generates a queue name for us

                if (bindingsKeys != null) {
                    // routing
                    for (String bindingKey : bindingsKeys) {
                        channel.queueBind(queueName, exchangeName, bindingKey);
                    }
                } else {
                    // subscriber
                    channel.queueBind(queueName, exchangeName, "");
                }

            } else {
                // assume default exchange and bindings - simple consumer
                channel.queueDeclare(
                        queueName,
                        isQueueDurable,
                        isQueueRestricted,
                        isQueueAutoDelete,
                        null);

            }
        } catch (IOException e) {
            handleException(e.getMessage(), e);
        }

        // schedule dispatching tasks to handover messages from the internal buffer to actual
        // processing task
        for (int i = 0; i < noOfDispatchingTask; i++) {
            pollingTaskScheduler.execute(new MessageDispatchTask(buffers));
        }

        // schedule IO task to pull messages from the broker
        for (int i = 0; i < noOfConcurrentConsumers; i++) {
            // only channels are thread safe, so create consumer per thread
            try {
                QueueingConsumer consumer = new QueueingConsumer(channel);
                boolean isAutoAck = isUseTx == true ? false : true; // increase readability
                channel.basicConsume(queueName, isAutoAck, consumer);
                ScheduledFuture<?> pollingTaskFuture = pollingTaskScheduler.scheduleWithFixedDelay(
                        new MessageIOTask(consumer, buffers, isUseTx),
                        scheduledTaskInitialDelay,
                        scheduledTaskDelay,
                        scheduledTaskTimeUnit);
                taskFutureList.add(pollingTaskFuture);

            } catch (IOException e) {
                handleException(e.getMessage(), e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("A polling task started listening on the queue '" + queueName + "' on " +
                    "behalf of the service '" + serviceName + "'");
        }
    }

    /**
     * Stop the polling tasks
     */
    public synchronized void stop() {
        for (ScheduledFuture<?> pollingTaskFuture : taskFutureList) {
            pollingTaskFuture.cancel(false);
        }
    }

    /**
     * The message dispatch task which dispatch messages from the source buffers to actual
     * processing logic
     */
    private final class MessageIOTask implements Runnable {

        private AMQPTransportBuffers buffers;
        private QueueingConsumer queueingConsumer;
        private boolean isUseTx;

        private MessageIOTask(QueueingConsumer queueingConsumer,
                              AMQPTransportBuffers buffers,
                              boolean isAutoAck) {
            this.queueingConsumer = queueingConsumer;
            this.buffers = buffers;
            this.isUseTx = isAutoAck;
        }

        public void run() {
            try {
                if (isUseTx) {
                    channel.txSelect();
                }
                QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
                if (delivery != null) {
                    buffers.addRequestMessage(new AMQPTransportMessage(delivery));
                    if (isUseTx) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        channel.txCommit();
                    }
                } else {
                    if (isUseTx) {
                        channel.txRollback();
                    }
                }
            } catch (InterruptedException e) {
                log.error("Polling task was interrupted for service '" + serviceName + "'", e);
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.error("I/O error occurs for the polling tasks for service '" + serviceName +
                        "'", e);
            } catch (ShutdownSignalException e) {

                if (e.isHardError()) {
                    // broker is offline
                    log.error("Polling task for service '" + serviceName + "' received a " +
                            "shutdown signal", e);
                    Semaphore available = new Semaphore(0, true);
                    String key = UUID.randomUUID().toString();
                    haHandler.getBlockedTasks().add(new AMQPTransportHAEntry(
                            available, key, connectionFactoryName));
                    try {
                        available.acquire();
                    } catch (InterruptedException ie) {
                        log.error("The blocking semaphore was interrupted", e);
                        Thread.currentThread().interrupt();
                        return;
                    }

                    AMQPTransportHABrokerEntry brokerEntry = haHandler.getConnectionMap().get(key);
                    if (brokerEntry == null) {
                        log.error("No new connection factory was found for key '" + key + "'");
                    } else {
                        setChannel(brokerEntry.getChannel());
                        stop();
                        try {
                            start();
                            log.info("Worker task for service '" + serviceName + "' is re-deployed");
                        } catch (AMQPTransportException ex) {
                            log.error("Start of polling tasks failed. System must be restarted!");
                        }
                    }
                } else {
                    // this is a shutdown signal for ctrl+c
                }

            } catch (ConsumerCancelledException e) {
                log.error("Polling task for service '" + serviceName + "' received a " +
                        "cancellation signal", e);
            }
        }
    }

    /**
     * The message dispatch task which dispatch messages from the source buffers to actual
     * processing logic
     */
    private final class MessageDispatchTask implements Runnable {
        private AMQPTransportBuffers buffers;

        private MessageDispatchTask(AMQPTransportBuffers buffers) {
            this.buffers = buffers;
        }

        public void run() {
            while (true) {
                AMQPTransportMessage msg = buffers.getRequestMessage();
                if (msg != null) {
                    pollingTaskScheduler.execute(new MessageProcessingTask(msg, buffers));
                }
            }
        }
    }

    /**
     * Process any request messages
     */
    private final class MessageProcessingTask implements Runnable {

        private AMQPTransportMessage message;
        private AMQPTransportBuffers buffers;
        private boolean isSOAP11;

        private MessageProcessingTask(
                AMQPTransportMessage message,
                AMQPTransportBuffers buffers) {

            this.message = message;
            this.buffers = buffers;
        }

        public void run() {
            try {
                handleIncomingMessage(message, buffers);
            } catch (AxisFault axisFault) {
                // there seems to be a fault while trying to execute the back end service
                // send a fault to the client
                try {
                    handleFaultMessage(message, buffers, axisFault);
                } catch (Exception e) {
                    // do not let the task die
                    log.error("Error while sending the fault message to the client. Client will " +
                            "not receive any errors!", e);
                }
            }
        }

        private boolean handleIncomingMessage(AMQPTransportMessage message,
                                              AMQPTransportBuffers buffers) throws AxisFault {
            if (message == null) {
                throw new AxisFault("A null message received!");
            } else {
                try {
                    MessageContext msgContext = endpoint.createMessageContext();
                    String msgId = message.getMessageId();
                    msgContext.setMessageID(msgId);
                    msgContext.setProperty(AMQPTransportConstant.AMQP_CORRELATION_ID,
                            message.getCorrelationId());
                    msgContext.setProperty(AMQPTransportConstant.AMQP_TRANSPORT_BUFFER_KEY, buffers);

                    String contentType = message.getContentType();
                    if (contentType == null) {
                        // use the configured value for content type
                        contentType = configuredContentType;
                    }

                    Map<String, Object> trpHeaders = message.getHeaders();

                    if (message.getReplyTo() != null) {
                        // this may not be the optimal way to check if this message should send
                        // a reply a one way message can be send with 'reply to' set
                        msgContext.setProperty(Constants.OUT_TRANSPORT_INFO,
                                new AMQPOutTransportInfo(contentType, responseConnectionFactory,
                                        message.getReplyTo()));
                        msgContext.setProperty(AMQPTransportConstant.PROPERTY_AMQP_REPLY_TO,
                                message.getReplyTo());
                        // cache the connection factory so that it can be used for sending the
                        // response
                        msgContext.setProperty(
                                AMQPTransportConstant.RESPONSE_CONNECTION_FACTORY_NAME,
                                responseConnectionFactory);
                    }

                    HTTPTransportUtils.initializeMessageContext(
                            msgContext,
                            message.getSoapAction(),
                            null,
                            contentType);

                    ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBody());

                    msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);
                    msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, trpHeaders);

                    Builder builder = BuilderUtil.getBuilderFromSelector(contentType, msgContext);
                    InputStream gzipInputStream = HTTPTransportUtils.handleGZip(
                            msgContext, inputStream);
                    OMElement documentElement = builder.processDocument(
                            gzipInputStream, contentType, msgContext);
                    msgContext.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));
                    isSOAP11 = msgContext.isSOAP11();

                    AxisEngine.receive(msgContext);

                    return true;

                } catch (IOException e) {
                    throw new AxisFault(e.getMessage(), e);
                }
            }
        }

        private void handleFaultMessage(AMQPTransportMessage originalMsg,
                                        AMQPTransportBuffers buffers,
                                        AxisFault axisFault) throws Exception {

            SOAPFactory factory = (isSOAP11 ?
                    OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory());
            OMDocument soapFaultDocument = factory.createOMDocument();
            SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
            soapFaultDocument.addChild(faultEnvelope);

            // create the fault element  if it is needed
            SOAPFault fault = faultEnvelope.getBody().getFault();
            if (fault == null) {
                fault = factory.createSOAPFault();
            }
            SOAPFaultCode code = factory.createSOAPFaultCode();
            code.setText(axisFault.getMessage());
            fault.setCode(code);

            SOAPFaultReason reason = factory.createSOAPFaultReason();
            reason.setText(axisFault.getMessage());
            fault.setReason(reason);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            faultEnvelope.serialize(out);
            AMQPTransportMessage msg = new AMQPTransportMessage(
                    new AMQP.BasicProperties(), out.toByteArray());
            try {
                buffers.addResponseMessage(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private void handleException(String msg, Throwable t) throws AMQPTransportException {
        log.error(msg, t);
        throw new AMQPTransportException(msg, t);
    }
}