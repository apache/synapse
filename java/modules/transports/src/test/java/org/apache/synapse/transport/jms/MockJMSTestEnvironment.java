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

import org.apache.synapse.transport.testkit.name.DisplayName;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockTopicConnectionFactory;

@DisplayName("mockrunner")
public class MockJMSTestEnvironment extends JMSTestEnvironment {
    private DestinationManager destinationManager;
    private ConfigurationManager configurationManager;
    
    @SuppressWarnings("unused")
    private void setUp() throws Exception {
        destinationManager = new DestinationManager();
        configurationManager = new ConfigurationManager();
    }

    @Override
    protected QueueConnectionFactory createQueueConnectionFactory() throws Exception {
        return new MockQueueConnectionFactory(destinationManager, configurationManager);
    }

    @Override
    protected TopicConnectionFactory createTopicConnectionFactory() throws Exception {
        return new MockTopicConnectionFactory(destinationManager, configurationManager);
    }
    
    @Override
    public Queue createQueue(String name) {
        return destinationManager.createQueue(name);
    }

    @Override
    public Topic createTopic(String name) {
        return destinationManager.createTopic(name);
    }
}
