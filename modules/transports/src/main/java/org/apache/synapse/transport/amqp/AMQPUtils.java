package org.apache.synapse.transport.amqp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.qpidity.api.Message;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.jms.JMSConstants;

public class AMQPUtils extends BaseUtils
{

    private static final Log log = LogFactory.getLog(AMQPUtils.class);

    private static BaseUtils _instance = new AMQPUtils();

    public static BaseUtils getInstace() {
        return _instance;
    }

    @Override
    public InputStream getInputStream(Object message)
    {
        Message msg = (Message)message;
        try{
            final ByteBuffer buf = msg.readData();
            return new InputStream() {
                public synchronized int read() throws IOException {
                    if (!buf.hasRemaining()) {
                        return -1;
                    }
                    return buf.get();
                }

                public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                    // Read only what's left
                    len = Math.min(len, buf.remaining());
                    buf.get(bytes, off, len);
                    return len;
                }
            };
        }catch(IOException e){
            throw new AMQPSynapseException("Error reading payload",e);
        }
    }

    @Override
    public byte[] getMessageBinaryPayload(Object message)
    {
        return null;
    }

    @Override
    public String getMessageTextPayload(Object message)
    {
        return null;
    }

    @Override
    public String getProperty(Object message, String property)
    {
        try {
            return (String)((Message)message).getMessageProperties().getApplicationHeaders().get(property);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract transport level headers for JMS from the given message into a Map
     *
     * @param message the JMS message
     * @return a Map of the transport headers
     */
    public static Map getTransportHeaders(Message message) {
        // create a Map to hold transport headers
        Map map = new HashMap();

        // correlation ID
        if (message.getMessageProperties().getCorrelationId() != null) {
            map.put(AMQPConstants.AMQP_CORELATION_ID, message.getMessageProperties().getCorrelationId());
        }

        // set the delivery mode as persistent or not
        try {
            map.put(AMQPConstants.AMQP_DELIVERY_MODE,message.getDeliveryProperties().getDeliveryMode());
        } catch (Exception ignore) {}

        // destination name
        map.put(AMQPConstants.AMQP_EXCHANGE_NAME,message.getDeliveryProperties().getExchange());
        map.put(AMQPConstants.AMQP_ROUTING_KEY,message.getDeliveryProperties().getRoutingKey());

        // expiration
        try {
            map.put(AMQPConstants.AMQP_EXPIRATION, message.getDeliveryProperties().getExpiration());
        } catch (Exception ignore) {}

        // if a JMS message ID is found
        if (message.getMessageProperties().getMessageId() != null) {
            map.put(AMQPConstants.AMQP_MESSAGE_ID, message.getMessageProperties().getMessageId());
        }

        // priority
        map.put(AMQPConstants.AMQP_PRIORITY,message.getDeliveryProperties().getPriority());

        // redelivered
        map.put(AMQPConstants.AMQP_REDELIVERED, message.getDeliveryProperties().getRedelivered());

        // replyto destination name
        if (message.getMessageProperties().getReplyTo() != null) {
            map.put(AMQPConstants.AMQP_REPLY_TO_EXCHANGE_NAME, message.getMessageProperties().getReplyTo().getExchangeName());
            map.put(AMQPConstants.AMQP_REPLY_TO_ROUTING_KEY, message.getMessageProperties().getReplyTo().getRoutingKey());
        }

        // priority
        map.put(AMQPConstants.AMQP_TIMESTAMP,message.getDeliveryProperties().getTimestamp());

        // any other transport properties / headers
        map.putAll(message.getMessageProperties().getApplicationHeaders());

        return map;
    }
}
