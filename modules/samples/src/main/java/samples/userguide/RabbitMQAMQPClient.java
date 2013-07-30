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
package samples.userguide;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;

/**
 * A generic client for RabbitMQ
 */
public class RabbitMQAMQPClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        String queueName = System.getProperty("queueName");
        String mode = System.getProperty("mode");
        String routingKey = System.getProperty("routingKey");
        String exchangeName = System.getProperty("exchangeName");

        String quote = System.getProperty("payLoad");
        if (quote == null) {
            quote = "IBM";
        }
        String msg =
                "<m:placeOrder xmlns:m=\"http://services.samples\">\n" +
                        "    <m:order>\n" +
                        "        <m:price>" + getRandom(100, 0.9, true) + "</m:price>\n" +
                        "        <m:quantity>" + (int) getRandom(10000, 1.0, true) + "</m:quantity>\n" +
                        "        <m:symbol>" + quote + "</m:symbol>\n" +
                        "    </m:order>\n" +
                        "</m:placeOrder>";


        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);


        if (mode == null) {
            mode = "producer";
        }

        if ("producer".equals(mode)) {
            if (queueName != null) {
                channel.basicPublish("", queueName, null, msg.getBytes());
            } else {
                if (routingKey != null) {
                    if (exchangeName == null) {
                        exchangeName = "topic-exchange";
                    }
                    channel.basicPublish(exchangeName, routingKey, null, msg.getBytes());

                } else {
                    if (exchangeName == null) {
                        exchangeName = "subscriber-exchange";
                    }
                    channel.basicPublish(exchangeName, "", null, msg.getBytes());
                }
            }
        } else {
            if (queueName == null) {
                queueName = "ConsumerProxy";
            }
            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, true, consumer);

            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("[x] received '" + message + "'");
        }

        channel.close();
        connection.close();
    }

    private static double getRandom(double base, double varience, boolean onlypositive) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * varience * base * rand))
                * (onlypositive ? 1 : (rand > 0.5 ? 1 : -1));
    }

}