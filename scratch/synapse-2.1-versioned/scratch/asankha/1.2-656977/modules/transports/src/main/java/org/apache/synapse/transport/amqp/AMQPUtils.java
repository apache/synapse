package org.apache.synapse.transport.amqp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterIncludeImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.qpidity.api.Message;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.jms.JMSConnectionFactory;
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
        Map<String,Object> map = new HashMap<String,Object>();

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

    /**
     * Get the AMQP destination used by this service
     *
     * @param service the Axis Service
     * @return the name of the JMS destination
     */
    public static List<AMQPBinding> getBindingsForService(AxisService service) {
        Parameter bindingsParam = service.getParameter(AMQPConstants.BINDINGS_PARAM);
        ParameterIncludeImpl pi = new ParameterIncludeImpl();
        try {
            pi.deserializeParameters((OMElement) bindingsParam.getValue());
        } catch (AxisFault axisFault) {
            log.error("Error reading parameters for AMQP binding definitions" +
                    bindingsParam.getName(), axisFault);
        }

        Iterator params = pi.getParameters().iterator();
        ArrayList<AMQPBinding> list = new ArrayList<AMQPBinding>();
        if(params.hasNext())
        {
            while (params.hasNext())
            {
                Parameter p = (Parameter) params.next();
                AMQPBinding binding = new AMQPBinding();
                OMAttribute exchangeTypeAttr = p.getParameterElement().getAttribute(new QName(AMQPConstants.BINDING_EXCHANGE_TYPE_ATTR));
                OMAttribute exchangeNameAttr = p.getParameterElement().getAttribute(new QName(AMQPConstants.BINDING_EXCHANGE_NAME_ATTR));
                OMAttribute routingKeyAttr = p.getParameterElement().getAttribute(new QName(AMQPConstants.BINDING_ROUTING_KEY_ATTR));
                OMAttribute primaryAttr = p.getParameterElement().getAttribute(new QName(AMQPConstants.BINDINGS_PRIMARY_ATTR));

                if ( exchangeTypeAttr != null) {
                    binding.setExchangeType(exchangeTypeAttr.getAttributeValue());
                }else if ( exchangeNameAttr != null) {
                    binding.setExchangeName(exchangeNameAttr.getAttributeValue());
                }else if ( primaryAttr != null) {
                    binding.setPrimary(true);
                }
                list.add(binding);
            }
        }else{
            // go for the defaults
            AMQPBinding binding = new AMQPBinding();
            binding.setRoutingKey(service.getName());
            list.add(binding);
        }

        return list;
    }

}
