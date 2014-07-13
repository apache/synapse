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
import java.util.NoSuchElementException;

/**
 * This is the interface  for the Synapse Message Store
 * Message Store is used to store Messages.
 */
public interface MessageStore extends SynapseArtifact, Nameable, ManagedLifecycle {



    /**
     * Inserts the Message into this store if it is possible to do so immediately
     * without violating capacity restrictions.
     * @param messageContext  MessageContext to be saved
     */
    public boolean offer(MessageContext messageContext);

    /**
     * Retrieves and removes the first Message in this store.
     * Message ordering will depend on the underlying implementation
     * @return first message context in the store
     */
    public MessageContext poll();

    /**
     * Retrieves but not removes the first Message in this store.
     * Message ordering will depend on the underlying implementation
     *
     * @return first message context in the store
     */
    public MessageContext peek();


    /**
     * Retrieves and removes the first Message in this store.
     * Message ordering will depend on the underlying implementation
     *
     * @return first message context in the store
     * @throws NoSuchElementException if store is empty
     */
    public MessageContext remove() throws NoSuchElementException;

    /**
     * Delete all the Messages in the Message Store
     *
     */
    public void clear();


    /**
     * Delete and return the MessageContext with given Message id
     * @param messageID  message id of the Message
     * @return  MessageContext instance
     */
    public MessageContext remove(String messageID);


    /**
     *  Returns the number of Messages  in this store.
     * @return the number of Messages in this Store
     */
    public int size();

    /**
     * Return the Message in given index position
     * (this may depend on the implementation)
     * @param index position of the message
     * @return Message in given index position
     */
    public MessageContext get(int index);

    /**
     * Get the All messages in the Message store without removing them from the queue
     * @return List of all Messages
     */
    public List<MessageContext> getAll();

    /**
     * Get the Message with the given ID from the Message store without removing it
     * @param messageId A message ID string
     * @return Message with given ID
     */
    public MessageContext get(String messageId);


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
     * Register a MessageStore observer instance with the MessageStore
     * to receive events.
     * @param observer instance to be registered
     */
    public void registerObserver(MessageStoreObserver observer);

    /**
     * Un register an Message store instance from the message store
     * to stop receiving events
     * @param observer  instance to be unregistered
     */
    public void unregisterObserver(MessageStoreObserver observer);
}