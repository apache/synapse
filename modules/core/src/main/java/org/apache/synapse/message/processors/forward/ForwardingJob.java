/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.message.processors.forward;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2BlockingClient;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.message.processors.MessageProcessorConstants;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import java.util.Map;
import java.util.Set;

/**
 * Redelivery Job will replay all the Messages in the Message Store when executed
 * Excluding ones that are already tried redelivering more than max number of tries
 */
public class ForwardingJob implements StatefulJob {

    private static final Log log = LogFactory.getLog(ForwardingJob.class);

    /**
     * Includes HTTP status for which message processor should retry
     */
    private String[] retryHttpStatusCodes;

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();

        //Get the Global Objects from DataMap
        MessageStore messageStore = (MessageStore) jdm.get(MessageProcessorConstants.MESSAGE_STORE);
        Map<String, Object> parameters = (Map<String, Object>) jdm.get(
                MessageProcessorConstants.PARAMETERS);
        Axis2BlockingClient sender = (Axis2BlockingClient) jdm.get(
                ScheduledMessageForwardingProcessor.BLOCKING_SENDER);
        ScheduledMessageForwardingProcessor processor = (ScheduledMessageForwardingProcessor) jdm.get(
                ScheduledMessageForwardingProcessor.PROCESSOR_INSTANCE);

        int maxDeliverAttempts = -1;

        boolean isMaxDeliverAttemptDropEnabled = false;

        String mdaParam = null;
        if (parameters != null) {
            mdaParam = (String) parameters.get(MessageProcessorConstants.MAX_DELIVER_ATTEMPTS);
        }

        if (mdaParam != null) {
            maxDeliverAttempts = Integer.parseInt(mdaParam);

            // Here we look for the edge case
            if(maxDeliverAttempts == 0) {
                processor.deactivate();
            }
        }
        if (maxDeliverAttempts > 0 && parameters.get(ForwardingProcessorConstants.MAX_DELIVER_DROP) != null &&
                parameters.get(ForwardingProcessorConstants.MAX_DELIVER_DROP).toString()
                        .equalsIgnoreCase("true")) {
	        //Configuration to continue the message processor even without stopping the message processor
	        // after maximum number of delivery
            isMaxDeliverAttemptDropEnabled = true;
        }

        if (parameters != null && parameters.get(ForwardingProcessorConstants.RETRY_HTTP_STATUS_CODES) != null) {
            retryHttpStatusCodes = parameters
                    .get(ForwardingProcessorConstants.RETRY_HTTP_STATUS_CODES).toString().split(",");
        }

        // WE do not try to process if the processor is inactive or
        // there is no message store attached.
        if(!processor.isActive() || messageStore == null) {
            return;
        }

        boolean errorStop = false;
        while (!errorStop) {

            MessageContext messageContext = messageStore.peek();
            if (messageContext != null) {


                //If The Message not belongs to this server we ignore it.
                String serverName = (String)
                        messageContext.getProperty(SynapseConstants.Axis2Param.SYNAPSE_SERVER_NAME);

                if(serverName != null && messageContext instanceof Axis2MessageContext) {

                    AxisConfiguration configuration = ((Axis2MessageContext)messageContext).
                            getAxis2MessageContext().
                            getConfigurationContext().getAxisConfiguration();

                    String myServerName = getAxis2ParameterValue(configuration,
                            SynapseConstants.Axis2Param.SYNAPSE_SERVER_NAME);

                    if(!serverName.equals(myServerName)) {
                        return;
                    }

                }

                Set proSet = messageContext.getPropertyKeySet();

                if (proSet != null) {
                    if (proSet.contains(SynapseConstants.BLOCKING_CLIENT_ERROR)) {
                        proSet.remove(SynapseConstants.BLOCKING_CLIENT_ERROR);
                    }
                }

                String targetEp =
                        (String) messageContext.getProperty(ForwardingProcessorConstants.TARGET_ENDPOINT);

                if (targetEp != null) {
                    Endpoint ep = messageContext.getEndpoint(targetEp);

                    // stop processing if endpoint is not ready to send
                    if(!ep.getContext().readyToSend()) {
                        return;
                    }

                    if ((ep != null) && (((AbstractEndpoint) ep).isLeafEndpoint())) {

                        try {
                            MessageContext outCtx = sender.send(ep, messageContext);

                            if (outCtx != null) {
                                handle400and500statusCodes(outCtx);

                                if ("true".equals(outCtx.getProperty(SynapseConstants.BLOCKING_CLIENT_ERROR))) {
                                    // This Means an Error has occurred
                                    if (!retryForHttpStatusCodes(messageStore, processor, outCtx)) {
                                        continue;
                                    }

                                    if (maxDeliverAttempts > 0) {
                                        processor.incrementSendAttemptCount();
                                    }

                                    if (parameters != null &&
                                            parameters.get(
                                                    ForwardingProcessorConstants.FAULT_SEQUENCE) != null) {

                                        String seq = (String) parameters.get(
                                                ForwardingProcessorConstants.FAULT_SEQUENCE);
                                        Mediator mediator = outCtx.getSequence(seq);
                                        if (mediator != null) {
                                            mediator.mediate(outCtx);
                                        } else {
                                            log.warn("Can't Send the fault Message , Sequence " + seq +
                                                             " Does not Exist");
                                        }

                                    }

                                    if (maxDeliverAttempts > 0) {
                                        if (processor.getSendAttemptCount() >= maxDeliverAttempts) {
                                            deactivate(processor, messageContext, parameters);
                                        }
                                    }
                                    errorStop = true;
                                } else {
                                    // This Means we have invoked an out only operation
                                    // remove the message and reset the count
                                    messageStore.poll();
                                    processor.resetSentAttemptCount();
                                }
                                continue;
                            }

                            // If there is a sequence defined to send success replies,
                            // we must send the message to it
                            if (parameters != null &&
                                    parameters.get(
                                            ForwardingProcessorConstants.REPLY_SEQUENCE) != null) {
                                if (outCtx != null) {
                                    String seq = (String) parameters.get(
                                            ForwardingProcessorConstants.REPLY_SEQUENCE);
                                    Mediator mediator = outCtx.getSequence(seq);
                                    if (mediator != null) {
                                        mediator.mediate(outCtx);
                                    } else {
                                        log.warn("Can't Send the Out Message , Sequence " + seq +
                                                " Does not Exist");
                                    }
                                }
                            }

                            // If no Exception Occurred We remove the Message
                            // and reset the delivery attempt count
                            processor.resetSentAttemptCount();
                            messageStore.poll();
                        } catch (Exception e) {

                            if (maxDeliverAttempts > 0) {
                                processor.incrementSendAttemptCount();
                                if (processor.getSendAttemptCount() >= maxDeliverAttempts) {
                                    if (isMaxDeliverAttemptDropEnabled) {
                                        //Since explicitly enabled the message drop after max delivery attempt
                                        // message has been removed and reset the delivery attempt count of the processor
                                        processor.resetSentAttemptCount();
                                        messageStore.poll();
                                    } else {
                                        deactivate(processor, messageContext, parameters);
                                    }
                                }
                            }
                            errorStop = true;
                            log.error("Error Forwarding Message ", e);
                            continue;
                        }
                    } else {
                        String logMsg;
                        if (ep == null) {
                            logMsg = "Endpoint named " + targetEp + " not found.Hence removing " +
                                    "the message form store";
                        } else {
                            logMsg = "Unsupported endpoint type. Only address/wsdl/default " +
                                    "endpoint types supported";
                        }
                        log.warn(logMsg);
                        messageStore.poll();
                    }


                } else {
                    //No Target Endpoint defined for the Message
                    //So we do not have a place to deliver.
                    //Here we log a warning and remove the message
                    //todo: we can improve this by implementing a target inferring mechanism

                    log.warn("Property " + ForwardingProcessorConstants.TARGET_ENDPOINT +
                            " not found in the message context , Hence removing the message ");
                    messageStore.poll();

                }

            } else {
                errorStop = true;
            }
        }
    }

    private boolean retryForHttpStatusCodes(MessageStore messageStore, ScheduledMessageForwardingProcessor processor,
                                            MessageContext outCtx) {
        // No need to retry for application level failures
        if (outCtx.getProperty(SynapseConstants.ERROR_MESSAGE) != null) {
            String errorMsg = outCtx.getProperty(SynapseConstants.ERROR_MESSAGE).toString();
            if (errorMsg.matches(".*[3-5]\\d\\d.*")) {
                if (!isRetryHttpStatusCode(errorMsg)) {
                    messageStore.poll();
                    processor.resetSentAttemptCount();
                    return false;
                }
            }
        }
        return true;
    }

    private void handle400and500statusCodes(MessageContext outCtx) {
        if ((outCtx.getProperty(NhttpConstants.HTTP_SC) != null)) {
            String httpStatusCode =  outCtx.getProperty(NhttpConstants.HTTP_SC).toString();
            if (httpStatusCode.equals(MessageProcessorConstants.HTTP_INTERNAL_SERVER_ERROR)) {
                outCtx.setProperty(SynapseConstants.BLOCKING_CLIENT_ERROR, "true");
                outCtx.setProperty(SynapseConstants.ERROR_MESSAGE, MessageProcessorConstants.HTTP_INTERNAL_SERVER_ERROR);
            } else if (httpStatusCode.equals(MessageProcessorConstants.HTTP_BAD_REQUEST_ERROR)) {
                outCtx.setProperty(SynapseConstants.BLOCKING_CLIENT_ERROR, "true");
                outCtx.setProperty(SynapseConstants.ERROR_MESSAGE, MessageProcessorConstants.HTTP_BAD_REQUEST_ERROR);
            }
        }
    }

    /**
     * Helper method to get a value of a parameters in the AxisConfiguration
     *
     * @param axisConfiguration AxisConfiguration instance
     * @param paramKey The name / key of the parameter
     * @return The value of the parameter
     */
    private static String getAxis2ParameterValue(AxisConfiguration axisConfiguration,
                                                 String paramKey) {

        Parameter parameter = axisConfiguration.getParameter(paramKey);
        if (parameter == null) {
            return null;
        }
        Object value = parameter.getValue();
        if (value != null && value instanceof String) {
            return (String) parameter.getValue();
        } else {
            return null;
        }
    }

    private void deactivate(ScheduledMessageForwardingProcessor processor,
                            MessageContext msgContext, Map<String, Object> parameters) {
        processor.deactivate();
        if (parameters != null && parameters.get(ForwardingProcessorConstants.DEACTIVATE_SEQUENCE) != null) {
            if (msgContext != null) {
                String seq = (String) parameters.get(ForwardingProcessorConstants.DEACTIVATE_SEQUENCE);
                Mediator mediator = msgContext.getSequence(seq);
                if (mediator != null) {
                    mediator.mediate(msgContext);
                } else {
                    log.warn("Deactivate sequence: " + seq + " does not exist");
                }
            }
        }
    }

    private boolean isRetryHttpStatusCode(String message) {
        if (retryHttpStatusCodes == null) {
            return false;
        }
        for (String statsCode : retryHttpStatusCodes) {
            if (message.contains(statsCode)) {
                return true;
            }
        }
        return false;
    }
}
