package org.apache.synapse.transport.amqp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterIncludeImpl;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.qpidity.api.Message;

public class AMQPUtils extends BaseUtils
{

    private static final Log log = LogFactory.getLog(AMQPUtils.class);

    private static AMQPUtils _instance = new AMQPUtils();

    public static AMQPUtils getInstace() {
        return _instance;
    }

    /**
     * Create a SOAPEnvelope from the given message and set it into
     * the axis MessageContext passed
     *
     * @param message the message object
     * @param msgContext the axis MessageContext
     * @param contentType
     * @throws AxisFault on errors encountered while setting the envelope to the message context
     */
    public void setSOAPEnvelope(Object message, MessageContext msgContext, String contentType)
            throws AxisFault {

        SOAPEnvelope envelope = null;
        StAXBuilder builder = null;

        String charSetEnc = null;
        try {
            if (contentType != null) {
                charSetEnc = new ContentType(contentType).getParameter("charset");
            }
        } catch (ParseException ex) {
            // ignore
        }
        
        InputStream in = getInputStream(message);

        // handle SOAP payloads when a correct content type is available
        try {
            if (contentType != null) {
                if (contentType.indexOf(BaseConstants.MULTIPART_RELATED) > -1) {
                    builder = BuilderUtil.getAttachmentsBuilder(msgContext, in, contentType, true);
                    envelope = (SOAPEnvelope) builder.getDocumentElement();
                    msgContext.setDoingSwA(true);

                } else {
                    builder = BuilderUtil.getSOAPBuilder(in, charSetEnc);
                    envelope = (SOAPEnvelope) builder.getDocumentElement();
                }
            }
        } catch (Exception ignore) {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
            in = getInputStream(message);
        }


        // handle SOAP when content type is missing, or any other POX, binary or text payload
        if (builder == null) {

            SOAPFactory soapFactory = new SOAP11Factory();
            try {
                builder = new StAXOMBuilder(StAXUtils.createXMLStreamReader(in, charSetEnc));
                builder.setOMBuilderFactory(OMAbstractFactory.getOMFactory());
                OMNamespace ns = builder.getDocumentElement().getNamespace();
                if (ns != null) {
                    String nsUri = ns.getNamespaceURI();

                    if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsUri)) {
                        envelope = BaseUtils.getEnvelope(in, SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    
                    } else if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(nsUri)) {
                        envelope = BaseUtils.getEnvelope(in, SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    
                    }
                }
                if (envelope == null) {
                    // this is POX ... mark message as REST
                    msgContext.setDoingREST(true);
                    envelope = soapFactory.getDefaultEnvelope();
                    envelope.getBody().addChild(builder.getDocumentElement());
                }

            } catch (Exception e) {
                envelope = handleLegacyMessage(msgContext, message);
            }
        }

        // Set the encoding scheme in the message context
        msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEnc);

        String charEncOfMessage =
            builder == null ? null :
                builder.getDocument() == null ? null : builder.getDocument().getCharsetEncoding();

        if (!isBlank(charEncOfMessage) &&
            !isBlank(charSetEnc) &&
            !charEncOfMessage.equalsIgnoreCase(charSetEnc)) {
            handleException("Charset encoding of transport differs from that of the payload");
        }

        msgContext.setEnvelope(envelope);
    }

    /**
     * Handle a non SOAP and non XML message, and create a SOAPEnvelope to hold the
     * pure text or binary content as necessary
     *
     * @param msgContext the axis message context
     * @param message the legacy message
     * @return the SOAP envelope
     */
    private SOAPEnvelope handleLegacyMessage(MessageContext msgContext, Object message) {

        SOAPFactory soapFactory = new SOAP11Factory();
        SOAPEnvelope envelope;

        if (log.isDebugEnabled()) {
            log.debug("Non SOAP/XML message received");
        }

        // pick the name of the element that will act as the wrapper element for the
        // non-xml payload. If service doesn't define one, default
        Parameter wrapperParam = msgContext.getAxisService().
            getParameter(BaseConstants.WRAPPER_PARAM);

        QName wrapperQName = null;
        OMElement wrapper = null;
        if (wrapperParam != null) {
            wrapperQName = BaseUtils.getQNameFromString(wrapperParam.getValue());
        }

        String textPayload = getMessageTextPayload(message);
        if (textPayload != null) {
            OMTextImpl textData = (OMTextImpl) soapFactory.createOMText(textPayload);

            if (wrapperQName == null) {
                wrapperQName = BaseConstants.DEFAULT_TEXT_WRAPPER;
            }
            wrapper = soapFactory.createOMElement(wrapperQName, null);
            wrapper.addChild(textData);

        } else {
            byte[] msgBytes = getMessageBinaryPayload(message);
            if (msgBytes != null) {
                DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(msgBytes));
                OMText textData = soapFactory.createOMText(dataHandler, true);
                if (wrapperQName == null) {
                    wrapperQName = BaseConstants.DEFAULT_BINARY_WRAPPER;
                }
                wrapper = soapFactory.createOMElement(wrapperQName, null);
                wrapper.addChild(textData);
                msgContext.setDoingMTOM(true);
                
            } else {
                handleException("Unable to read payload from message of type : "
                    + message.getClass().getName());
            }
        }

        envelope = soapFactory.getDefaultEnvelope();
        envelope.getBody().addChild(wrapper);

        return envelope;
    }

    /**
     * Get an InputStream to the message payload
     *
     * @param message Object
     * @return an InputStream to the payload
     */
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

    /**
     * Get the message payload as a byte[], if the message is a non-SOAP, non-XML, binary message
     *
     * @param message the message Object
     * @return the payload of the message as a byte[]
     */
    public byte[] getMessageBinaryPayload(Object message)
    {
        return null;
    }

    /**
     * Get the message payload as a String, if the message is a non-SOAP, non-XML, plain text message
     *
     * @param message the message Object
     * @return the plain text payload of the message if applicable
     */
    public String getMessageTextPayload(Object message)
    {
        return null;
    }

    public static String getProperty(Message message, String property)
    {
        try {
            return (String)message.getMessageProperties().getApplicationHeaders().get(property);
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
