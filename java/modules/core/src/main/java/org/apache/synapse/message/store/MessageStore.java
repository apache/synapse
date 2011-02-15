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

package org.apache.synapse.message.store;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.SynapseArtifact;
import org.apache.synapse.Nameable;
import org.apache.synapse.message.processors.MessageProcessor;

import java.util.List;
import java.util.Map;

/**
 * This is the interface  for the Synapse Message Store
 * Message Store is used to store failed Messages.
 */
public interface MessageStore extends SynapseArtifact, Nameable, ManagedLifecycle {

    /**
     * Store the Message in the Message Store
     * @param messageContext  MessageContext to be saved
     */
    public void store(MessageContext messageContext);

    /**
     * Delete and return the MessageContext with given Message id
     * @param messageID  message id of the Message
     * @return  MessageContext instance
     */
    public MessageContext unstore(String messageID);

    /**
     * Delete all the Messages in the Message Store
     * @return  List of all messages in store
     */
    public List<MessageContext> unstoreAll();


    /**
     * Unstore Messages from index 'from' to index 'to'
     * Message ordering will be depend on the implementation
     * @param from start index
     * @param to  stop index
     * @return   list of messages that are belong to given range
     */
    public List<MessageContext> unstore(int from , int to);

    /**
     * Get the All messages in the Message store without removing them from the queue
     * @return List of all Messages
     */
    public List<MessageContext> getAllMessages();

    /**
     * Get the Message with the given ID from the Message store without removing it
     * @param messageId A message ID string
     * @return Message with given ID
     */
    public MessageContext getMessage(String messageId);


    /**
     * Get Messages from index 'from' to index 'to'
     * Message ordering will be depend on the implementation
     * @param from start index
     * @param to  stop index
     * @return   list of messages that are belong to given range
     */
    public List<MessageContext> getMessages(int from , int to);
    /**
     * set the implementation specific parameters
     * @param parameters A map of parameters or null
     */
    public void setParameters(Map<String,Object> parameters);

    /**
     * get the implementation specific parameters of the Message store
     * @return a properties map
     */
    public Map<String,Object> getParameters();

    /**
     * return the number of Messages stored in the Message store
     * @return the number of messages in the store
     */
    public int getSize();

    /**
     * set a Mediator sequence  name
     * This sequence will be executed if the redelivery attempts fail and Message is going to add to the Message store
     * @param sequence a Sequence name
     */
    public void setSequence(String sequence);

    /**
     * get the implementation class name of the message
     *
     * @return Name of the implementation class
     */
    public String getProviderClass ();

    /**
    * Get Mediator sequence name
     * @return Name of the sequence
     */
    public String getSequence();

    /**
     * Add the Synapse configuration reference for the Message Store
     *
     * @param configuration Current SynapseConfiguration
     */
    public void setConfiguration(SynapseConfiguration configuration);

    /**
     * Set the name of the file that the Message store is configured
     *
     * @param filename Name of the file where this artifact is defined
     */
    public void setFileName(String filename);

    /**
     * get the file name that the message store is configured
     *
     * @return Name of the file where this artifact is defined
     */
    public String getFileName();


    /**
     * Set the Message Processor Associated with the Message Store
     * @param messageProcessor message processor instance associated with message store
     */
    public void setMessageProcessor(MessageProcessor messageProcessor);

    /**
     * Get the Message Processor associated with the MessageStore
     * @return   message processor instance associated with the message store
     */
    public MessageProcessor getMessageProcessor();
}