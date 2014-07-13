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

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2BlockingClient;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.message.store.MessageStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MessageForwardingProcessorView implements MessageForwardingProcessorViewMBean {

    private static final Log log = LogFactory.getLog(MessageForwardingProcessorView.class);

    private MessageStore messageStore;
    private Axis2BlockingClient sender;
    private ScheduledMessageForwardingProcessor processor;

    public MessageForwardingProcessorView(MessageStore messageStore, Axis2BlockingClient sender,
                                          ScheduledMessageForwardingProcessor processor) {
        if (messageStore != null) {
            this.messageStore = messageStore;
        } else {
            throw new SynapseException("Cannot create Message Forwarding Processor " +
                    "view with null message store");
        }

        if (sender != null) {
            this.sender = sender;
        } else {
            throw new SynapseException("Cannot create Message Forwarding Processor " +
                    "view with null message sender");
        }


        if (processor != null) {
            this.processor = processor;
        } else {
            throw new SynapseException("Cannot create Message Forwarding Processor " +
                    "view with null message processor");
        }

    }

    public void resendAll() throws Exception {
        if (!processor.isActive()) {

            while (messageStore.peek() != null) {
                sendMessage(messageStore.peek() , true);
            }
        } else {
            throw new Exception("Message Processor is Active, Manual operations are " +
                    "not supported!");
        }
    }

    public void deleteAll() throws Exception {
        if (!processor.isActive()) {
            messageStore.clear();
        } else {
            throw new Exception("Message Processor is Active, Manual operations are " +
                    "not supported!");
        }
    }

    public List<String> messageIdList() throws Exception {
        if (!processor.isActive()) {
            int size = messageStore.size();
            List<String> idList = new ArrayList<String>();
            for (int i = 0; i < size; i++) {
                MessageContext context = messageStore.get(i);
                if (context != null) {
                    idList.add(context.getMessageID());
                } else {
                    break;
                }
            }
            return idList;
        } else {
            throw new Exception("Message Processor is Active, Manual operations are " +
                    "not supported!");
        }

    }

    public void resend(String messageID) throws Exception {
        if (!processor.isActive()) {
            if (messageID != null && !"".equals(messageID.trim())) {
                MessageContext msgCtx = messageStore.get(messageID);
                if (msgCtx != null) {
                    sendMessage(msgCtx ,false);
                    messageStore.remove(messageID);
                }
            }
        } else {
            throw new Exception("Message Processor is Active, Manual operations are " +
                    "not supported!");
        }
    }

    public void delete(String messageID) throws Exception {
        if (!processor.isActive()) {
             if (messageID != null && !"".equals(messageID.trim())) {
               messageStore.remove(messageID);
            }
        } else {
            throw new Exception("Message Processor is Active, Manual operations are " +
                    "not supported!");
        }
    }

    public String getEnvelope(String messageID) throws Exception {
        if (!processor.isActive()) {
             if (messageID != null && !"".equals(messageID.trim())) {
                MessageContext msgCtx = messageStore.get(messageID);
                if (msgCtx != null) {
                   SOAPEnvelope env =
                           ((Axis2MessageContext) msgCtx).getAxis2MessageContext().getEnvelope();
                   if(env != null) {
                       return env.toString();
                   }
                }
            }
        } else {
            throw new Exception("Message Processor is Active, Manual operations are " +
                    "not supported!");
        }

        return null;
    }

    public int getSize() {
        return messageStore.size();
    }


    public boolean isActive() {
        assert processor != null;
        return processor.isActive();
    }

    public void activate() {
        assert processor != null;
        processor.resetSentAttemptCount();
        processor.activate();
    }

    public void deactivate() {
        assert processor != null;
        processor.deactivate();
    }

    private void sendMessage(MessageContext messageContext, boolean delete) throws Exception {
        if (messageContext != null) {
            Set proSet = messageContext.getPropertyKeySet();

            if (proSet != null && proSet.contains(SynapseConstants.BLOCKING_CLIENT_ERROR)) {
                proSet.remove(SynapseConstants.BLOCKING_CLIENT_ERROR);
            }

            String targetEp = (String) messageContext.getProperty(
                    ForwardingProcessorConstants.TARGET_ENDPOINT);

            if (targetEp != null) {
                Endpoint ep = messageContext.getEndpoint(targetEp);
                if ((ep != null) && (((AbstractEndpoint) ep).isLeafEndpoint())) {
                    try {
                        sender.send(ep, messageContext);
                        // If no Exception Occurred We remove the Message
                        if (delete) {
                            messageStore.poll();
                        }
                    } catch (Exception e) {
                        log.error("Error Forwarding Message ", e);
                        throw new Exception(e);
                    }
                } else {
                    String logMsg;
                    if (ep == null) {
                        logMsg = "Endpoint named " + targetEp + "not found. Hence removing " +
                                "the message form store";
                    } else {
                        logMsg = "Unsupported endpoint type. Only address/wsdl/default endpoint " +
                                "types supported";
                    }

                    log.warn(logMsg);
                    if (delete) {
                        messageStore.poll();
                    }
                    throw new Exception(logMsg);
                }

            } else {
                //No Target Endpoint defined for the Message
                //So we do not have a place to deliver.
                //Here we log a warning and remove the message
                //todo: we can improve this by implementing a target inferring mechanism

                String logMsg = "Property " + ForwardingProcessorConstants.TARGET_ENDPOINT +
                        " not found in the message context , Hence removing the message ";
                log.warn(logMsg);
                if (delete) {
                    messageStore.poll();
                }
                throw new Exception(logMsg);

            }

        } else {
            throw new Exception("MessageContext must not be null");
        }
    }

}
