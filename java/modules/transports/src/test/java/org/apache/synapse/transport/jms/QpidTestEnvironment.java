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
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;

import org.apache.qpid.client.AMQConnectionFactory;
import org.apache.qpid.client.AMQQueue;
import org.apache.qpid.client.AMQTopic;
import org.apache.qpid.client.transport.TransportConnection;
import org.apache.qpid.framing.AMQShortString;
import org.apache.synapse.transport.testkit.name.Name;

@Name("qpid")
public class QpidTestEnvironment extends JMSTestEnvironment {
    @SuppressWarnings("unused")
    private void setUp() throws Exception {
        TransportConnection.createVMBroker(1);
    }

    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        TransportConnection.killVMBroker(1);
    }

    @Override
    protected QueueConnectionFactory createQueueConnectionFactory() throws Exception {
        return createConnectionFactory();
    }

    @Override
    protected TopicConnectionFactory createTopicConnectionFactory() throws Exception {
        return createConnectionFactory();
    }
    
    private AMQConnectionFactory createConnectionFactory() throws Exception {
        return new AMQConnectionFactory("vm://:1", "guest", "guest", "fred", "test");
    }

    @Override
    public Queue createQueue(String name) {
        return new AMQQueue(name, name);
    }

    @Override
    public Topic createTopic(String name) {
        return new AMQTopic(new AMQShortString(name), name);
    }
}
