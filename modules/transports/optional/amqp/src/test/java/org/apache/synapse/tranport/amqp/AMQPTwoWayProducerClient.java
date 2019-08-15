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
package org.apache.synapse.tranport.amqp;

import com.rabbitmq.client.*;

import java.io.IOException;

/**
 * A request/response producer client
 * author : rajika.kumarasiri@gmail.com
 */
public class AMQPTwoWayProducerClient {

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

    public static void main(String[] args) throws IOException, InterruptedException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();

        AMQPTwoWayProducerClient.produceAndConsume(
                MESSAGE2, channel, "consumer", "consumerReply");

        channel.close();
        connection.close();

    }


    private static void produceAndConsume(
            String message,
            Channel channel,
            String requestQueueName,
            String replyQueueName) throws IOException, InterruptedException {

        AMQP.BasicProperties.Builder builder = new
                AMQP.BasicProperties().builder();
        String restCoID = Math.random() + "";
        builder.correlationId(restCoID);
        System.out.println("Request correlation Id : " + restCoID);
        builder.replyTo(replyQueueName);
        channel.basicPublish("", requestQueueName, builder.build(), message.getBytes());
        System.out.println("[x] sent to '" + requestQueueName + "' the message '\n" + message + "'");

        channel.queueDeclare(replyQueueName, false, false, false, null);
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(replyQueueName, true, consumer);
        System.out.println("Waiting for message on queue '" + replyQueueName + "'");
        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String replyMessage = new String(delivery.getBody());
            System.out.println("[x] received '" + replyMessage + "'");
            System.out.println("Response correlation Id : " + delivery.getProperties().getCorrelationId());
            break;

        }
    }
}
