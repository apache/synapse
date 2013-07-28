package org.apache.synapse.tranport.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;

/**
 * The producer for AMQP transport.
 */
public class AMQPProducerClient {

    //    private static final String QUEUE_NAME = "ConsumerTxProxy";
//    private static final String QUEUE_NAME = "worker-queue";
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

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();

        //channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        AMQPProducerClient.produce(MESSAGE2, channel, QUEUE_NAME);
//        AMQPProducerClient.publish(MESSAGE2, channel, "subscriber-exchange");
//        AMQPProducerClient.route(MESSAGE2, channel, "route-exchange", "fatal");
//        AMQPProducerClient.route(MESSAGE2, channel, "topic-exchange", "kern.critical");

        channel.close();
        connection.close();

    }


    private static void produce(String message, Channel channel, String queueName) throws IOException {
        channel.basicPublish("", queueName, null, message.getBytes());
    }

    private static void publish(String message, Channel channel, String exchangeName) throws IOException {
        channel.basicPublish(exchangeName, "", null, message.getBytes());
    }

    private static void route(String message, Channel channel, String exchangeName, String routeKey) throws IOException {
        channel.basicPublish(exchangeName, routeKey, null, message.getBytes());
    }

}
