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
package org.apache.synapse.transport.amqp;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.base.AbstractTransportSender;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.synapse.transport.amqp.connectionfactory.AMQPTransportConnectionFactoryManager;
import org.apache.synapse.transport.amqp.pollingtask.AMQPSimpleConsumerTask;
import org.apache.synapse.transport.amqp.sendertask.AMQPSender;
import org.apache.synapse.transport.amqp.sendertask.AMQPSenderCache;
import org.apache.synapse.transport.amqp.sendertask.AMQPSenderFactory;
import org.apache.synapse.transport.amqp.tx.AMQPTransportProducerTx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * The transport sender implementation for AMQP transport. A message can end up here as
 * part of directly calling an AMQP endpoint or sending a response to a two way request message.
 */
public class AMQPTransportSender extends AbstractTransportSender {

    /**
     * The connection factory definitions defined in axis2.xml for transport sender section
     */
    private AMQPTransportConnectionFactoryManager connectionFactoryManager;

    private ExecutorService connectionFactoryES;

    private AMQPSenderCache cache;

    /**
     * Keep tracks of the responses for two in/out messages
     */
    private Map<String, Semaphore> responseTracker;

    /**
     * Store the response messages for in/out messages until further process
     */
    private Map<String, AMQPTransportMessage> responseMessage;

    private ExecutorService responseHandlingPool;

    private long semaphoreTimeOut;

    @Override
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut)
            throws AxisFault {
        super.init(cfgCtx, transportOut);

        connectionFactoryES = Executors.newFixedThreadPool(AMQPTransportUtils.getIntProperty(
                AMQPTransportConstant.PARAM_CONNECTION_FACTORY_POOL_SIZE,
                AMQPTransportConstant.CONNECTION_FACTORY_POOL_DEFAULT));

        responseHandlingPool = Executors.newFixedThreadPool(AMQPTransportUtils.getIntProperty(
                AMQPTransportConstant.PARAM_RESPONSE_HANDLING_POOL_SIZE,
                AMQPTransportConstant.RESPONSE_HANDLING_POOL_DEFAULT));

        connectionFactoryManager = new AMQPTransportConnectionFactoryManager();
        connectionFactoryManager.addConnectionFactories(transportOut, connectionFactoryES);

        semaphoreTimeOut = AMQPTransportUtils.getLongProperty(
                AMQPTransportConstant.PARAM_SEMAPHORE_TIME_OUT, 86400L);

        cache = new AMQPSenderCache(new ConcurrentHashMap<Integer, AMQPSender>());
        responseTracker = new ConcurrentHashMap<String, Semaphore>();
        responseMessage = new ConcurrentHashMap<String, AMQPTransportMessage>();

        log.info("AMQP transport sender initializing..");
    }

    @Override
    public void stop() {
        super.stop();
        try {
            connectionFactoryManager.shutDownConnectionFactories();
        } catch (AMQPTransportException e) {
            log.error("Error while shutting down connection factories, continue anyway...", e);
        }
        cache.clean();
        responseTracker.clear();
        responseMessage.clear();
        connectionFactoryES.shutdown();
        responseHandlingPool.shutdown();
    }

    @Override
    public void sendMessage(MessageContext msgCtx,
                            String targetEPR,
                            OutTransportInfo outTransportInfo) throws AxisFault {

        AMQPSender amqpSender;
        Integer hashKey = null;
        Map<String, String> params = null;
        String replyTo = null;
        AMQPTransportProducerTx tx;
        MessageContext replyMsgCtx = msgCtx.getOperationContext().getMessageContext(
                WSDL2Constants.MESSAGE_LABEL_IN);
        if (replyMsgCtx != null) {
            replyTo = (String) replyMsgCtx.getProperty(AMQPTransportConstant.PROPERTY_AMQP_REPLY_TO);
        }

        if (replyTo != null) {
            // this is a response for a request message(request/response semantic message)
            hashKey = replyTo.hashCode();
            params = new HashMap<String, String>();
            params.put(AMQPTransportConstant.PARAMETER_QUEUE_NAME, replyTo);

            String conFacName = (String) msgCtx.getOperationContext().
                    getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN).
                    getProperty(AMQPTransportConstant.RESPONSE_CONNECTION_FACTORY_NAME);
            if (conFacName == null) {
                throw new AxisFault("A message was received with 'reply to' set. But no reply " +
                        "connection factory name found. Define the parameter '" +
                        AMQPTransportConstant.PARAMETER_RESPONSE_CONNECTION_FACTORY_NAME +
                        "' as a service parameter. This response message will be dropped!");
            } else {
                params.put(AMQPTransportConstant.PARAMETER_CONNECTION_FACTORY_NAME, conFacName);
            }
        } else {
            // this is a normal one way out message
            if (targetEPR != null) {
                hashKey = new Integer(targetEPR.hashCode());
                try {
                    params = AMQPTransportUtils.parseAMQPUri(targetEPR);
                } catch (AMQPTransportException e) {
                    throw new AxisFault("Error while parsing the AMQP epr '" + targetEPR + "'", e);
                }
            } else if (outTransportInfo != null && outTransportInfo instanceof AMQPOutTransportInfo) {
                AMQPOutTransportInfo info = (AMQPOutTransportInfo) outTransportInfo;
                params = info.getParams();

            } else {
                throw new AxisFault("Could not determine the endpoint information to deliver the message");
            }
        }

        if (cache.hit(hashKey)) {
            amqpSender = cache.get(hashKey);
        } else {
            try {
                amqpSender = AMQPSenderFactory.createAMQPSender(connectionFactoryManager, params);
                cache.add(hashKey, amqpSender);
            } catch (IOException e) {
                throw new AxisFault("Could not create the AMQP sender", e);
            }
        }

        try {
            String correlationId = (String)
                    msgCtx.getProperty(AMQPTransportConstant.PROPERTY_AMQP_CORRELATION_ID);
            if (correlationId == null) {
                correlationId = msgCtx.getMessageID();
            }

            boolean isInOut = waitForSynchronousResponse(msgCtx);
            Semaphore available = null;
            if (isInOut) {
                replyTo = (String) msgCtx.getProperty(
                        AMQPTransportConstant.PROPERTY_AMQP_REPLY_TO);
                if (replyTo == null) {
                    replyTo = UUID.randomUUID().toString();
                }
                available = new Semaphore(0, true);
                responseTracker.put(correlationId, available);
            }

            String useTx = (String) msgCtx.getProperty(AMQPTransportConstant.PROPERTY_PRODUCER_TX);

            if (AMQPTransportConstant.AMQP_USE_LWPC.equals(useTx)) {
                tx = new AMQPTransportProducerTx(true, amqpSender.getChannel());
            } else if (AMQPTransportConstant.AMQP_USE_TX.equals(useTx)) {
                tx = new AMQPTransportProducerTx(false, amqpSender.getChannel());
            } else {
                tx = null;
            }

            if (tx != null) {
                try {
                    tx.start();
                } catch (IOException e) {
                    throw new AxisFault("Error while initiation tx for message '" +
                            msgCtx.getMessageID() + "'", e);
                }
            }

            amqpSender.sendAMQPMessage(msgCtx, correlationId, replyTo);

            if (tx != null) {
                try {
                    tx.end();
                } catch (IOException e) {
                    throw new AxisFault("Error while terminating tx for message '" +
                            msgCtx.getMessageID() + "'", e);
                } catch (InterruptedException e) {
                    log.error("Error while terminating tx for message '" +
                            msgCtx.getMessageID() + "'", e);
                    Thread.currentThread().interrupt();
                }
            }

            if (isInOut) {
                // block and process the response
                new AMQPSimpleConsumerTask(
                        responseHandlingPool,
                        amqpSender.getChannel(),
                        replyTo,
                        responseTracker,
                        responseMessage).
                        consume();
                try {
                    available.tryAcquire(semaphoreTimeOut, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                responseTracker.remove(correlationId);
                AMQPTransportMessage msg = responseMessage.get(correlationId);
                if (msg != null) {
                    handleSyncResponse(msgCtx, msg, msg.getContentType());
                } else {
                    // we don't have a response yet, so send a fault to client
                    log.warn("The semaphore with id '" + correlationId + "' was time out while "
                            + "waiting for a response, sending a fault to client..");
                    sendFault(msgCtx,
                            new Exception("Times out occurs while waiting for a response"));
                }
            }
        } catch (AMQPTransportException e) {
            throw new AxisFault("Could not retrieve the connection factory information", e);
        } catch (IOException e) {
            throw new AxisFault("Could not produce the message into the destination", e);
        }
    }

    private void handleSyncResponse(
            MessageContext requestMsgCtx,
            AMQPTransportMessage message,
            String requestContentType)
            throws AxisFault {
        try {
            MessageContext responseMsgCtx = createResponseMessageContext(requestMsgCtx);
            responseMsgCtx.setProperty(Constants.Configuration.MESSAGE_TYPE,
                    requestMsgCtx.getProperty(Constants.Configuration.MESSAGE_TYPE));

            responseMsgCtx.setProperty(Constants.Configuration.CONTENT_TYPE,
                    requestMsgCtx.getProperty(Constants.Configuration.CONTENT_TYPE));

            String contentType = message.getContentType();
            if (contentType == null) {
                contentType = inferContentType(requestContentType, responseMsgCtx);
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBody());
            Builder builder = BuilderUtil.getBuilderFromSelector(contentType, requestMsgCtx);
            SOAPEnvelope envelope = (SOAPEnvelope) builder.processDocument(
                    inputStream, contentType, responseMsgCtx);
            responseMsgCtx.setEnvelope(envelope);

            String charSetEnc = BuilderUtil.getCharSetEncoding(contentType);
            if (charSetEnc == null) {
                charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
            }
            responseMsgCtx.setProperty(
                    Constants.Configuration.CHARACTER_SET_ENCODING,
                    contentType.indexOf("; charset=") > 0
                            ? charSetEnc : MessageContext.DEFAULT_CHAR_SET_ENCODING);
            responseMsgCtx.setProperty(
                    MessageContext.TRANSPORT_HEADERS, message.getHeaders());

            if (message.getSoapAction() != null) {
                responseMsgCtx.setSoapAction(message.getSoapAction());
            }
            AxisEngine.receive(responseMsgCtx);

        } catch (AxisFault axisFault) {
            handleException("Could not handle the response message ", axisFault);
        }
    }

    private void sendFault(MessageContext msgContext, Exception e) {
        try {
            MessageContext faultContext = MessageContextBuilder.createFaultMessageContext(
                    msgContext, e);
            faultContext.setProperty("ERROR_MESSAGE", e.getMessage());
            faultContext.setProperty("SENDING_FAULT", Boolean.TRUE);
            AxisEngine.sendFault(faultContext);
        } catch (AxisFault axisFault) {
            log.fatal("Could not create the fault message.", axisFault);
        }
    }

    private String inferContentType(String requestContentType, MessageContext responseMsgCtx) {
        // Try to get the content type from the message context
        Object cTypeProperty = responseMsgCtx.getProperty(Constants.Configuration.CONTENT_TYPE);
        if (cTypeProperty != null) {
            return cTypeProperty.toString();
        }
        // Try to get the content type from the axis configuration
        Parameter cTypeParam = cfgCtx.getAxisConfiguration().getParameter(
                Constants.Configuration.CONTENT_TYPE);
        if (cTypeParam != null) {
            return cTypeParam.getValue().toString();
        }

        if (requestContentType != null) {
            return requestContentType;
        }

        // Unable to determine the content type - Return default value
        return AMQPTransportConstant.DEFAULT_CONTENT_TYPE;
    }
}
