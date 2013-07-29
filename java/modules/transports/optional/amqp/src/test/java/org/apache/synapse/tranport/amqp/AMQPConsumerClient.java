package org.apache.synapse.tranport.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;

/**
 * The consumer client for AMQP transport
 */
public class AMQPConsumerClient {

    private static final String QUEUE_NAME = "ProducerProxy";

    public static void main(String[] args) throws IOException, InterruptedException {

        String queueName;

        if (args.length < 1) {
            System.out.println("Usage: java AMQPConsumerClient <queue-name>");
            System.out.println("Default arguments will be used");
            queueName = QUEUE_NAME;
        }
        queueName = args[1];

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        System.out.println("Waiting for message on queue '" + queueName + "'");

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());
            System.out.println("[x] received '" + message + "'");
        }
    }
}
