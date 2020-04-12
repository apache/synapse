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

    private boolean isMaxDeliverAttemptDropEnabled;
    private int maxDeliverAttempts;
    private String deactivateSequence;
    private String faultSequence;
    private String replySequence;
    private String[] retryHttpStatusCodes;
    private MessageStore messageStore;
    private Axis2BlockingClient sender;
    private ScheduledMessageForwardingProcessor processor;
    private boolean errorStop = false;

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //Get the Global Objects from DataMap
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
        configureForwardingJob(jdm);

        // WE do not try to process if the processor is inactive or
        // there is no message store attached.
        if(!processor.isActive() || messageStore == null) {
            return;
        }

        startProcessingMsgs();
    }

    private void configureForwardingJob(JobDataMap jdm) {
        messageStore = (MessageStore) jdm.get(MessageProcessorConstants.MESSAGE_STORE);
        sender = (Axis2BlockingClient) jdm.get(
                ScheduledMessageForwardingProcessor.BLOCKING_SENDER);
        processor = (ScheduledMessageForwardingProcessor) jdm.get(
                ScheduledMessageForwardingProcessor.PROCESSOR_INSTANCE);
        Map<String, Object> parameters = (Map<String, Object>) jdm.get(MessageProcessorConstants.PARAMETERS);
        maxDeliverAttempts = extractMaxDeliveryAttempts(parameters, processor);
        isMaxDeliverAttemptDropEnabled = isMaxDeliverAttemptDropEnabled(parameters);
        retryHttpStatusCodes(parameters);
        setSequences(parameters);
    }

    private int extractMaxDeliveryAttempts(Map<String, Object> parameters,
                                           ScheduledMessageForwardingProcessor processor) {
        int maxDeliverAttempts = -1;
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
        return maxDeliverAttempts;
    }

    private boolean isMaxDeliverAttemptDropEnabled(Map<String, Object> parameters) {
        boolean isMaxDeliverAttemptDropEnabled = false;
        if (maxDeliverAttempts > 0 && parameters.get(ForwardingProcessorConstants.MAX_DELIVER_DROP) != null &&
                parameters.get(ForwardingProcessorConstants.MAX_DELIVER_DROP).toString()
                        .equalsIgnoreCase("true")) {
            //Configuration to continue the message processor even without stopping the message processor
            // after maximum number of delivery
            isMaxDeliverAttemptDropEnabled = true;
        }
        return isMaxDeliverAttemptDropEnabled;
    }

    private void retryHttpStatusCodes(Map<String, Object> parameters) {
        if (parameters != null && parameters.get(ForwardingProcessorConstants.RETRY_HTTP_STATUS_CODES) != null) {
            retryHttpStatusCodes = parameters
                    .get(ForwardingProcessorConstants.RETRY_HTTP_STATUS_CODES).toString().split(",");
        }
    }

    private void setSequences(Map<String, Object> parameters) {
        if (parameters != null) {
            if (parameters.get(ForwardingProcessorConstants.FAULT_SEQUENCE) != null) {
                faultSequence = (String) parameters.get(ForwardingProcessorConstants.FAULT_SEQUENCE);
            }
            if (parameters.get(ForwardingProcessorConstants.DEACTIVATE_SEQUENCE) != null) {
                deactivateSequence = (String) parameters.get(ForwardingProcessorConstants.DEACTIVATE_SEQUENCE);
            }
            if (parameters.get(ForwardingProcessorConstants.REPLY_SEQUENCE) != null) {
                replySequence = (String) parameters.get(
                        ForwardingProcessorConstants.REPLY_SEQUENCE);
            }
        }
    }

    private void startProcessingMsgs() {
        errorStop = false;
        while (!errorStop) {
            MessageContext inMsgCtx = messageStore.peek();
            if (inMsgCtx != null) {
                if (isMsgRelatedToThisServer(inMsgCtx)) {
                    handleNewMessage(inMsgCtx);
                }
            } else {
                errorStop = true;
            }
        }
    }

    private boolean isMsgRelatedToThisServer(MessageContext inMsgCtx) {
        String serverName = (String) inMsgCtx.getProperty(SynapseConstants.Axis2Param.SYNAPSE_SERVER_NAME);
        if(serverName != null && inMsgCtx instanceof Axis2MessageContext) {
            AxisConfiguration configuration = ((Axis2MessageContext)inMsgCtx).
                    getAxis2MessageContext().getConfigurationContext().getAxisConfiguration();
            String myServerName = getAxis2ParameterValue(configuration,
                                                         SynapseConstants.Axis2Param.SYNAPSE_SERVER_NAME);

            return serverName.equals(myServerName);
        }
        return true;
    }

    /**
     * Helper method to get a value of a parameters in the AxisConfiguration
     *
     * @param axisConfiguration AxisConfiguration instance
     * @param paramKey The name / key of the parameter
     * @return The value of the parameter
     */
    private static String getAxis2ParameterValue(AxisConfiguration axisConfiguration, String paramKey) {
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

    private void handleNewMessage(MessageContext inMsgCtx) {
        sanitizeMsgContext(inMsgCtx);
        String targetEp = (String) inMsgCtx.getProperty(ForwardingProcessorConstants.TARGET_ENDPOINT);
        if (targetEp != null) {
            Endpoint ep = inMsgCtx.getEndpoint(targetEp);
            // stop processing if endpoint is not ready to send
            if(ep.getContext().readyToSend()) {
                if ((ep != null) && (((AbstractEndpoint) ep).isLeafEndpoint())) {
                    sendMsgToEndpoint(inMsgCtx, ep);
                } else {
                    logMsg(targetEp, ep);
                    messageStore.poll();
                }
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
    }

    private void sanitizeMsgContext(MessageContext messageContext) {
        Set proSet = messageContext.getPropertyKeySet();
        if (proSet != null) {
            if (proSet.contains(SynapseConstants.BLOCKING_CLIENT_ERROR)) {
                proSet.remove(SynapseConstants.BLOCKING_CLIENT_ERROR);
            }
        }
    }

    private void logMsg(String targetEp, Endpoint ep) {
        String logMsg;
        if (ep == null) {
            logMsg = "Endpoint named " + targetEp + " not found.Hence removing " +
                    "the message form store";
        } else {
            logMsg = "Unsupported endpoint type. Only address/wsdl/default " +
                    "endpoint types supported";
        }
        log.warn(logMsg);
    }

    private void sendMsgToEndpoint(MessageContext inMsgCtx, Endpoint ep) {
        try {
            MessageContext outCtx = sender.send(ep, inMsgCtx);
            if (outCtx != null) {
                handleResponse(inMsgCtx, outCtx);
            } else {
                // If no Exception Occurred We remove the Message
                // and reset the delivery attempt count
                messageStore.poll();
                processor.resetSentAttemptCount();
            }
        } catch (Exception e) {
            errorStop = handleOutOnlyError(inMsgCtx);
            log.error("Error Forwarding Message ", e);
        }
    }

    private void handleResponse(MessageContext inMsgCtx, MessageContext outCtx) {
        handle400and500statusCodes(outCtx);
        if ("true".equals(outCtx.getProperty(SynapseConstants.BLOCKING_CLIENT_ERROR))) {
            handleError(inMsgCtx, outCtx);
        } else {
            // This Means we have invoked an out only operation
            // remove the message and reset the count
            doPostSuccessTasks(outCtx);
        }
    }

    private void handle400and500statusCodes(MessageContext outCtx) {
        if ((outCtx.getProperty(NhttpConstants.HTTP_SC) != null)) {
            String httpStatusCode =  outCtx.getProperty(NhttpConstants.HTTP_SC).toString();
            if (httpStatusCode.equals(MessageProcessorConstants.HTTP_INTERNAL_SERVER_ERROR)) {
                outCtx.setProperty(SynapseConstants.BLOCKING_CLIENT_ERROR, "true");
                outCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                                   MessageProcessorConstants.HTTP_INTERNAL_SERVER_ERROR);
            } else if (httpStatusCode.equals(MessageProcessorConstants.HTTP_BAD_REQUEST_ERROR)) {
                outCtx.setProperty(SynapseConstants.BLOCKING_CLIENT_ERROR, "true");
                outCtx.setProperty(SynapseConstants.ERROR_MESSAGE, MessageProcessorConstants.HTTP_BAD_REQUEST_ERROR);
            }
        }
    }

    private void handleError(MessageContext inMsgCtx, MessageContext outCtx) {
        if (isHttpStatusCodeError(outCtx)) {
            if (isRetryHttpStatusCode(outCtx)) {
                doPostErrorTasks(inMsgCtx, outCtx);
            } else {
                doPostSuccessTasks(outCtx);
            }
        } else {
            doPostErrorTasks(inMsgCtx, outCtx);
        }
    }

    private void doPostSuccessTasks(MessageContext outCtx) {
        messageStore.poll();
        processor.resetSentAttemptCount();
        sendResponseToReplySeq(outCtx);
    }

    private void doPostErrorTasks(MessageContext inMsgCtx, MessageContext outCtx) {
        if (maxDeliverAttempts > 0) {
            processor.incrementSendAttemptCount();
        }
        sendItToFaultSequence(outCtx);
        if (maxDeliverAttempts > 0) {
            handleMaxDeliveryAttempts(inMsgCtx);
        }
        errorStop = true;
    }

    private void handleMaxDeliveryAttempts(MessageContext inMsgCtx) {
        if (processor.getSendAttemptCount() >= maxDeliverAttempts) {
            if (isMaxDeliverAttemptDropEnabled) {
                //Since explicitly enabled the message drop after max delivery attempt
                // message has been removed and reset the delivery attempt count of the processor
                processor.resetSentAttemptCount();
                messageStore.poll();
            } else {
                deactivate(processor, inMsgCtx);
            }
        }
    }

    private boolean handleOutOnlyError(MessageContext inMsgCtx) {
        if (maxDeliverAttempts > 0) {
            processor.incrementSendAttemptCount();
            handleMaxDeliveryAttempts(inMsgCtx);
        }
        return true;
    }

    private void sendResponseToReplySeq(MessageContext outCtx) {
        // If there is a sequence defined to send success replies,
        // we must send the message to it
        if (replySequence != null) {
            Mediator mediator = outCtx.getSequence(replySequence);
            if (mediator != null) {
                mediator.mediate(outCtx);
            } else {
                log.warn("Can't Send the Out Message , Sequence " + replySequence + " Does not Exist");
            }
        }
    }

    private void sendItToFaultSequence(MessageContext outCtx) {
        if (faultSequence != null) {
            Mediator mediator = outCtx.getSequence(faultSequence);
            if (mediator != null) {
                mediator.mediate(outCtx);
            } else {
                log.warn("Can't Send the fault Message , Sequence " + faultSequence +
                                 " Does not Exist");
            }
        }
    }

    private boolean isHttpStatusCodeError(MessageContext outCtx) {
        // No need to retry for application level failures
        if (outCtx.getProperty(SynapseConstants.ERROR_MESSAGE) != null) {
            String errorMsg = outCtx.getProperty(SynapseConstants.ERROR_MESSAGE).toString();
            return errorMsg.matches(".*[3-5]\\d\\d.*");
        }
        return false;
    }

    private boolean isRetryHttpStatusCode(MessageContext outCtx) {
        String errorMsg = outCtx.getProperty(SynapseConstants.ERROR_MESSAGE).toString();
        if (retryHttpStatusCodes == null) {
            return false;
        }
        for (String statsCode : retryHttpStatusCodes) {
            if (errorMsg.contains(statsCode)) {
                return true;
            }
        }
        return false;
    }

    private void deactivate(ScheduledMessageForwardingProcessor processor, MessageContext inMsgCtx) {
        processor.deactivate();
        if (deactivateSequence != null) {
            if (inMsgCtx != null) {
                sendMsgToDeactivateSeq(inMsgCtx);
            }
        }
    }

    private void sendMsgToDeactivateSeq(MessageContext inMsgCtx) {
        Mediator mediator = inMsgCtx.getSequence(deactivateSequence);
        if (mediator != null) {
            mediator.mediate(inMsgCtx);
        } else {
            log.warn("Deactivate sequence: " + deactivateSequence + " does not exist");
        }
    }
}
