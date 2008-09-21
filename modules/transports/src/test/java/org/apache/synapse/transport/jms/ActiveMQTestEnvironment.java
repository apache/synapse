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

package org.apache.synapse.transport.jms;

import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.synapse.transport.testkit.name.Name;

@Name("ActiveMQ")
public class ActiveMQTestEnvironment extends JMSTestEnvironment {
    private static final String BROKER_NAME = "test";
    
    private BrokerService broker;
    
    @SuppressWarnings("unused")
    private void setUp() throws Exception {
        broker = new BrokerService();
        broker.setBrokerName(BROKER_NAME);
        broker.setDataDirectory("target/activemq-data");
        broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
        broker.start();
    }

    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        broker.stop();
        broker = null;
    }

    @Override
    protected ActiveMQConnectionFactory createConnectionFactory() throws Exception {
        return new ActiveMQConnectionFactory("vm://" + BROKER_NAME);
    }


    @Override
    public Queue createQueue(String name) {
        return new ActiveMQQueue(name);
    }

    @Override
    public Topic createTopic(String name) {
        return new ActiveMQTopic(name);
    }
}
