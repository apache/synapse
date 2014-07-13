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
package org.apache.synapse.message.processors;

import org.apache.synapse.*;
import org.apache.synapse.message.store.MessageStore;

import java.util.HashMap;
import java.util.Map;

/**
 *All Synapse Message Processors must implement <code>MessageProcessor</code> interface
 *Message processors will process the Message using a Message Store.
 *Message processing logic and process will depend on the
 *concrete implementation of the MessageStore
 */
public interface MessageProcessor extends ManagedLifecycle , Nameable , SynapseArtifact{

    /**
     * Start Message Processor
     */
    public void start();

    /**
     * Stop MessageProcessor
     */
    public void stop();

    /**
     * Set the Message Store name that backs the Message processor
     * @param messageStore name the underlying MessageStore instance
     */
    public void setMessageStoreName(String  messageStore);

    /**
     * Get message store name associated with the Message processor
     * @return  message store name associated with message processor
     */
    public String getMessageStoreName();

    /**
     * Set the Message processor parameters that will be used by the specific implementation
     * @param parameters
     */
    public void setParameters(Map<String,Object> parameters);

    /**
     * Get the Message processor Parameters
     * @return
     */
    public Map<String , Object> getParameters();

    /**
     * Returns weather a Message processor is started or not
     * @return
     */
    public boolean isStarted();

     /**
     * Set the name of the file that the Message Processor is configured
     *
     * @param filename Name of the file where this artifact is defined
     */
    public void setFileName(String filename);

    /**
     * get the file name that the message processor is configured
     *
     * @return Name of the file where this artifact is defined
     */
    public String getFileName();
}
