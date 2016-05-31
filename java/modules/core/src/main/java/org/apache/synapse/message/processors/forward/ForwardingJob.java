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
import org.apache.synapse.message.processors.MessageProcessorConsents;
import org.apache.synapse.message.store.MessageStore;
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


    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();

        //Get the Global Objects from DataMap
        MessageStore messageStore = (MessageStore) jdm.get(MessageProcessorConsents.MESSAGE_STORE);
        Map<String, Object> parameters = (Map<String, Object>) jdm.get(
                MessageProcessorConsents.PARAMETERS);
        Axis2BlockingClient sender = (Axis2BlockingClient) jdm.get(
                ScheduledMessageForwardingProcessor.BLOCKING_SENDER);
        ScheduledMessageForwardingProcessor processor = (ScheduledMessageForwardingProcessor) jdm.get(
                ScheduledMessageForwardingProcessor.PROCESSOR_INSTANCE);

        int maxDeliverAttempts = -1;
        String mdaParam = null;
        if (parameters != null) {
            mdaParam = (String) parameters.get(MessageProcessorConsents.MAX_DELIVER_ATTEMPTS);
        }

        if (mdaParam != null) {
            maxDeliverAttempts = Integer.parseInt(mdaParam);

            // Here we look for the edge case
            if(maxDeliverAttempts == 0) {
                processor.deactivate();
            }
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

                            if (outCtx != null && "true".equals(outCtx.
                                    getProperty(SynapseConstants.BLOCKING_CLIENT_ERROR))) {
                                // This Means an Error has occurred

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
                                    if(processor.getSendAttemptCount() >= maxDeliverAttempts) {
                                        deactivate(processor, messageContext, parameters);
                                    }
                                }
                                errorStop = true;
                                continue;

                            } else if (outCtx == null) {
                                // This Means we have invoked an out only operation
                                // remove the message and reset the count
                                messageStore.poll();
                                processor.resetSentAttemptCount();
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
                                    deactivate(processor, messageContext, parameters);
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
}
