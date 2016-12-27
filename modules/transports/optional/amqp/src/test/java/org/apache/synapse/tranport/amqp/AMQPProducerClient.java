/*
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
 */

package org.apache.synapse.tranport.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

/**
 * The producer for AMQP transport.
 */
public class AMQPProducerClient {

    private static final String QUEUE_NAME = "ProducerProxy";

    private static final String MESSAGE =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "   <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "      <soapenv:Header xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n" +
                    "         <wsa:To>http://localhost:8281/services/StockQuoteProxy</wsa:To>\n" +
                    "         <wsa:MessageID>urn:uuid:44d578a8-20e9-4ee4-8407-9b0a0768e5a8</wsa:MessageID>\n" +
                    "         <wsa:Action>urn:getQuote</wsa:Action>\n" +
                    "      </soapenv:Header>\n" +
                    "      <soapenv:Body>\n" +
                    "         <m0:getQuote xmlns:m0=\"http://services.samples\">\n" +
                    "            <m0:request>\n" +
                    "               <m0:symbol>IBM</m0:symbol>\n" +
                    "            </m0:request>\n" +
                    "         </m0:getQuote>\n" +
                    "      </soapenv:Body>\n" +
                    "   </soapenv:Envelope>";

    private static final String MESSAGE2 =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "         <m0:getQuote xmlns:m0=\"http://services.samples\">\n" +
                    "            <m0:request>\n" +
                    "               <m0:symbol>IBM</m0:symbol>\n" +
                    "            </m0:request>\n" +
                    "         </m0:getQuote>";

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Usage: java AMQPProducerClient <queue?> " +
                    "<queue|exchange-name> <routing-key>");
            System.out.println("Default arguments will be used");
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();

        String queueName = null, exchangeName = null, routingKey = null;

        if ("y".equals(args[0])) {
            if (args[1] != null) {
                queueName = args[1];
            } else {
                queueName = QUEUE_NAME;
            }
        } else {
            if (args[2] != null) {
                exchangeName = args[2];
                if (args[3] != null) {
                    routingKey = args[3];
                } else {
                    routingKey = "kern.critical";
                }
            } else {
                exchangeName = "subscriber-exchange";
            }
        }

        if (queueName != null) {
            AMQPProducerClient.produce(MESSAGE2, channel, queueName);
        } else {
            if (routingKey != null) {
                AMQPProducerClient.route(MESSAGE2, channel, exchangeName, routingKey);
            } else {
                AMQPProducerClient.publish(MESSAGE2, channel, exchangeName);
            }
        }

        channel.close();
        connection.close();
    }


    private static void produce(String message, Channel channel, String queueName)
            throws IOException {
        channel.basicPublish("", queueName, null, message.getBytes());
    }

    private static void publish(String message, Channel channel, String exchangeName)
            throws IOException {
        channel.basicPublish(exchangeName, "", null, message.getBytes());
    }

    private static void route(String message, Channel channel, String exchangeName,
                              String routeKey)
            throws IOException {
        channel.basicPublish(exchangeName, routeKey, null, message.getBytes());
    }

}
