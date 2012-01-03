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

import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractMessageStore implements MessageStore {

    /**
     * message store name
     */
    protected String name;

    /**
     * name of the sequence to be executed before storing the message
     */
    protected String sequence;

    /**
     * Message store JMX view
     */
    protected MessageStoreView messageStoreMBean;

    /**
     * synapse configuration reference
     */
    protected SynapseConfiguration synapseConfiguration;

    /**
     * synapse environment reference
     */
    protected SynapseEnvironment synapseEnvironment;

    /**
     * Message store parameters
     */
    protected Map<String,Object> parameters;

    /**
     * Message Store description
     */
    protected String description;

    /**
     * Name of the file where this message store is defined
     */
    protected String fileName;

    /**
     * List that holds the MessageStore observers registered with the Message Store
     */
    protected List<MessageStoreObserver> messageStoreObservers =
            new ArrayList<MessageStoreObserver>();

    protected Lock lock = new ReentrantLock();



    public void init(SynapseEnvironment se) {
        this.synapseEnvironment = se;
        this.synapseConfiguration = synapseEnvironment.getSynapseConfiguration();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        messageStoreMBean = new MessageStoreView(name, this);
        MBeanRegistrar.getInstance().registerMBean(messageStoreMBean,
                "MessageStore", this.name);
    }


    public void registerObserver(MessageStoreObserver observer) {
        if(observer != null && !messageStoreObservers.contains(observer)) {
            messageStoreObservers.add(observer);
        }
    }

    public void unregisterObserver(MessageStoreObserver observer) {
        if(observer != null && messageStoreObservers.contains(observer)) {
            messageStoreObservers.remove(observer);
        }
    }

    /**
     * Notify Message Addition to the observers
     * @param messageId of the Message added.
     */
    protected void notifyMessageAddition(String messageId) {
        for(MessageStoreObserver o : messageStoreObservers) {
            o.messageAdded(messageId);
        }
    }

    /**
     * Notify Message removal to the observers
     * @param messageId of the Message added
     */
    protected void notifyMessageRemoval(String messageId) {
        for(MessageStoreObserver o : messageStoreObservers) {
            o.messageRemoved(messageId);
        }
    }
    public int size() {
        return -1;
    }


    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }


    public void destroy() {
        MBeanRegistrar.getInstance().unRegisterMBean("MessageStore", this.name);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setFileName(String filename) {
        this.fileName = filename;
    }

    public String getFileName() {
        return this.fileName;
    }

    public Lock getLock() {
        return lock;
    }
}
