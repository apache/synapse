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

import org.apache.synapse.config.SynapseConfiguration;

import java.util.List;
import java.util.Map;

/**
 * This is the interface  for the Synapse Message Store
 * Message Store used to store failed Messages.
 */
public interface MessageStore {

    /**
     * store the Message in the Message Store
     * Underlying message store implementation must handle the efficient way of storing the Message
     * @param storableMessage wrapper of the Message context
     */
    public void store(StorableMessage storableMessage);

    /**
     * get the Message Store name.
     * Each Message Store must have a unique name.
     * @return  name
     */
    public String getName();

    /**
     * Set the Message Store Name
     *
     * @param name Name of the message store
     */
    public void setName(String name);

    /**
     * Store the Message in schedule queue to redeliver
     *
     * @param storableMessage A StorableMessage instance
     */
    public void schedule(StorableMessage storableMessage);


    public StorableMessage dequeueScheduledQueue();

    /**
     * return the Message That is on top of the queue
     *
     * @return A StorableMessage instance or null
     */
    public StorableMessage getFirstSheduledMessage();

    /**
     * Unstore the Message with Given Message Id from the MessageStore
     * @param messageID a message ID string
     * @return unstored Message
     */
    public StorableMessage unstore(String messageID);

    /**
     * Delete all the Messages in the Message Store
     * @return  List of all messages in store
     */
    public List<StorableMessage> unstoreAll();

    /**
     * Get the All messages in the Message store without removing them from the queue
     * @return List of all Messages
     */
    public List<StorableMessage> getAllMessages();

    /**
     * Get the Message with the given ID from the Message store without removing it
     * @param messageId A message ID string
     * @return Message with given ID
     */
    public StorableMessage getMessage(String messageId);

    /**
     * Set the redelivery processor instance associated with the Message Store
     * redelivery processor have the responsibility of redelivery message according
     * to a policy defined
     *
     * @param redeliveryProcessor The redelivery processor to be registered
     */
    public void setRedeliveryProcessor(RedeliveryProcessor redeliveryProcessor);

    /**
    * Return the redelivery processor instance associated with the message store
     *
     * @return A RedlieveryProcessor or null
     */
    public RedeliveryProcessor getRedeliveryProcessor();

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
}