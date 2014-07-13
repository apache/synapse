/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.synapse.transport.amqp.pollingtask;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.amqp.AMQPTransportMessage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class AMQPSimpleConsumerTask {

    private static Log log = LogFactory.getLog(AMQPSimpleConsumerTask.class);

    private Channel channel;

    private String queueName;

    private Map<String, Semaphore> responseTracker;

    private Map<String, AMQPTransportMessage> responseMessage;

    private ExecutorService workerPool;

    public AMQPSimpleConsumerTask(
            ExecutorService workerPool,
            Channel channel,
            String queueName,
            Map<String, Semaphore> responseTracker,
            Map<String, AMQPTransportMessage> responseMessage) {
        this.workerPool = workerPool;
        this.channel = channel;
        this.queueName = queueName;
        this.responseTracker = responseTracker;
        this.responseMessage = responseMessage;
    }

    public void consume() {
        workerPool.submit(new Consumer(channel, queueName, responseTracker, responseMessage));
    }

    private static class Consumer implements Runnable {
        Channel channel;
        String queueName;
        Map<String, Semaphore> responseTracker;
        Map<String, AMQPTransportMessage> responseMessage;


        private Consumer(
                Channel channel,
                String queueName,
                Map<String, Semaphore> responseTracker,
                Map<String, AMQPTransportMessage> responseMessage) {
            this.channel = channel;
            this.queueName = queueName;
            this.responseTracker = responseTracker;
            this.responseMessage = responseMessage;
        }

        public void run() {
            try {
                channel.queueDeclare(queueName, false, false, false, null);
                QueueingConsumer queueingConsumer = new QueueingConsumer(channel);
                channel.basicConsume(queueName, true, queueingConsumer);

                QueueingConsumer.Delivery delivery = queueingConsumer.nextDelivery();
                AMQPTransportMessage msg = new AMQPTransportMessage(delivery);
                responseMessage.put(msg.getCorrelationId(), msg);
                Semaphore semaphore = responseTracker.get(msg.getCorrelationId());
                semaphore.release();

            } catch (IOException e) {
                log.error("I/O error occurred", e);
            } catch (InterruptedException e) {
                log.error("Retrieving task was interrupted", e);
                Thread.currentThread().interrupt();
            } catch (ShutdownSignalException e) {
                log.error("Shutdown signal was received for simple consumer task", e);
            }
        }
    }
}