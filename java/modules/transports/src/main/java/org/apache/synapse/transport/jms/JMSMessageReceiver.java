/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.transport.jms;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.synapse.transport.base.threads.WorkerPool;
import org.apache.synapse.transport.base.BaseUtils;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.MetricsCollector;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import javax.xml.namespace.QName;

/**
 * This is the actual receiver which listens for and accepts JMS messages, and
 * hands them over to be processed by a worker thread. An instance of this
 * class is created for each JMSConnectionFactory, but all instances may and
 * will share the same worker thread pool held by the JMSListener
 */
public class JMSMessageReceiver implements MessageListener {

    private static final Log log = LogFactory.getLog(JMSMessageReceiver.class);

    /** The JMSListener */
    private JMSListener jmsListener = null;
    /** The thread pool of workers */
    private WorkerPool workerPool = null;
    /** The Axis configuration context */
    private ConfigurationContext cfgCtx = null;
    /** A reference to the JMS Connection Factory to which this applies */
    private JMSConnectionFactory jmsConnectionFactory = null;
    /** The name of the service this message receiver is bound to. */
    final String serviceName;
    /** Metrics collector */
    private MetricsCollector metrics = null;

    /**
     * Create a new JMSMessage receiver
     *
     * @param jmsListener the JMS transport Listener
     * @param jmsConFac the JMS connection factory we are associated with
     * @param workerPool the worker thread pool to be used
     * @param cfgCtx the axis ConfigurationContext
     * @param serviceName the name of the Axis service
     */
    JMSMessageReceiver(JMSListener jmsListener, JMSConnectionFactory jmsConFac,
                       WorkerPool workerPool, ConfigurationContext cfgCtx, String serviceName) {
        this.jmsListener = jmsListener;
        this.jmsConnectionFactory = jmsConFac;
        this.workerPool = workerPool;
        this.cfgCtx = cfgCtx;
        this.serviceName = serviceName;
        this.metrics = jmsListener.getMetricsCollector();
    }

    /**
     * The entry point on the reception of each JMS message
     *
     * @param message the JMS message received
     */
    public void onMessage(Message message) {
        // directly create a new worker and delegate processing
        try {
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Received JMS message to destination : " + message.getJMSDestination());                
                sb.append("\nMessage ID : " + message.getJMSMessageID());
                sb.append("\nCorrelation ID : " + message.getJMSCorrelationID());
                sb.append("\nReplyTo ID : " + message.getJMSReplyTo());
                log.debug(sb.toString());
                if (log.isTraceEnabled() && message instanceof TextMessage) {
                    log.trace("\nMessage : " + ((TextMessage) message).getText());    
                }
            }
        } catch (JMSException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error reading JMS message headers for debug logging", e);
            }
        }

        // update transport level metrics
        try {
            if (message instanceof BytesMessage) {
                metrics.incrementBytesReceived((JMSUtils.getBodyLength((BytesMessage) message)));
            } else if (message instanceof TextMessage) {
                metrics.incrementBytesReceived(((TextMessage) message).getText().getBytes().length);
            } else {
                handleException("Unsupported JMS message type : " + message.getClass().getName());
            }
        } catch (JMSException e) {
            log.warn("Error reading JMS message size to update transport metrics", e);
        }

        // has this message already expired? expiration time == 0 means never expires
        try {
            long expiryTime = message.getJMSExpiration();                        
            if (expiryTime > 0 && System.currentTimeMillis() > expiryTime) {
                if (log.isDebugEnabled()) {
                    log.debug("Discard expired message with ID : " + message.getJMSMessageID());
                }
                return;
            }
        } catch (JMSException ignore) {}

        workerPool.execute(new Worker(message));
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new AxisJMSException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new AxisJMSException(msg);
    }


    /**
     * The actual Worker implementation which will process the
     * received JMS messages in the worker thread pool
     */
    class Worker implements Runnable {

        private Message message = null;

        Worker(Message message) {
            this.message = message;
        }

        public void run() {

            MessageContext msgContext = jmsListener.createMessageContext();

            // set the JMS Message ID as the Message ID of the MessageContext
            try {
                msgContext.setMessageID(message.getJMSMessageID());
                msgContext.setProperty(JMSConstants.JMS_COORELATION_ID, message.getJMSMessageID());
            } catch (JMSException ignore) {}

            try {
                String soapAction = JMSUtils.getInstace().
                    getProperty(message, BaseConstants.SOAPACTION);
                AxisService service = null;

                // set to bypass dispatching if we know the service - we already should!
                if (serviceName != null) {
                    service = cfgCtx.getAxisConfiguration().getService(serviceName);
                    msgContext.setAxisService(service);

                    // find the operation for the message, or default to one
                    Parameter operationParam = service.getParameter(BaseConstants.OPERATION_PARAM);
                    QName operationQName = (
                        operationParam != null ?
                            BaseUtils.getQNameFromString(operationParam.getValue()) :
                            BaseConstants.DEFAULT_OPERATION);

                    AxisOperation operation = service.getOperation(operationQName);
                    if (operation != null) {
                        msgContext.setAxisOperation(operation);
                        msgContext.setSoapAction("urn:" + operation.getName().getLocalPart());
                    }
                }

                // set the message property OUT_TRANSPORT_INFO
                // the reply is assumed to be over the JMSReplyTo destination, using
                // the same incoming connection factory, if a JMSReplyTo is available
                if (message.getJMSReplyTo() != null) {
                    msgContext.setProperty(
                        Constants.OUT_TRANSPORT_INFO,
                        new JMSOutTransportInfo(jmsConnectionFactory, message.getJMSReplyTo()));

                } else if (service != null) {
                    // does the service specify a default reply destination ?
                    Parameter param = service.getParameter(JMSConstants.REPLY_PARAM);
                    if (param != null && param.getValue() != null) {
                        msgContext.setProperty(
                            Constants.OUT_TRANSPORT_INFO,
                            new JMSOutTransportInfo(
                                jmsConnectionFactory,
                                jmsConnectionFactory.getDestination((String) param.getValue())));
                    }
                }

                String contentType =
                    JMSUtils.getInstace().getProperty(message, BaseConstants.CONTENT_TYPE);
                
                // set the message payload to the message context
                JMSUtils.getInstace().setSOAPEnvelope(message, msgContext, contentType);

                jmsListener.handleIncomingMessage(
                    msgContext,
                    JMSUtils.getTransportHeaders(message),
                    soapAction,
                    contentType
                );
                metrics.incrementMessagesReceived();

            } catch (JMSException e) {
                handleException("JMS Exception reading the message Destination or JMS ReplyTo", e);
                metrics.incrementFaultsReceiving();
            } catch (AxisFault e) {
                handleException("Axis fault creating a MessageContext", e);
                metrics.incrementFaultsReceiving();
            }
        }
    }
}
