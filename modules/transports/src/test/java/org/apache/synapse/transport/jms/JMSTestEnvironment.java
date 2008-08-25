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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.synapse.transport.testkit.name.Key;
import org.mockejb.jndi.MockContextFactory;

@Key("broker")
public abstract class JMSTestEnvironment {
    public static final String QUEUE_CONNECTION_FACTORY = "QueueConnectionFactory";
    public static final String TOPIC_CONNECTION_FACTORY = "TopicConnectionFactory";
    
    private Context context;
    private QueueConnectionFactory queueConnectionFactory;
    private TopicConnectionFactory topicConnectionFactory;
    
    @SuppressWarnings("unused")
    private void setUp() throws Exception {
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        queueConnectionFactory = createQueueConnectionFactory();
        topicConnectionFactory = createTopicConnectionFactory();
        context.bind(QUEUE_CONNECTION_FACTORY, queueConnectionFactory);
        context.bind(TOPIC_CONNECTION_FACTORY, topicConnectionFactory);
    }
    
    protected abstract QueueConnectionFactory createQueueConnectionFactory() throws Exception;
    protected abstract TopicConnectionFactory createTopicConnectionFactory() throws Exception;
    
    public Context getContext() {
        return context;
    }

    public Destination createDestination(String destinationType, String name) {
        if (destinationType.equals(JMSConstants.DESTINATION_TYPE_TOPIC)) {
            return createTopic(name);
        } else {
            return createQueue(name);
        }
    }

    public abstract Queue createQueue(String name);
    public abstract Topic createTopic(String name);
    
    public QueueConnectionFactory getQueueConnectionFactory() {
        return queueConnectionFactory;
    }
    
    public TopicConnectionFactory getTopicConnectionFactory() {
        return topicConnectionFactory;
    }
    
    public ConnectionFactory getConnectionFactory(Destination destination) {
        if (destination instanceof Queue) {
            return queueConnectionFactory;
        } else {
            return topicConnectionFactory;
        }
    }
}
