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

import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.mockejb.jndi.MockContextFactory;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockDestination;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockTopicConnectionFactory;

public class JMSListenerSetup extends ListenerTestSetup {
    public static final String QUEUE_CONNECTION_FACTORY = "QueueConnectionFactory";
    public static final String TOPIC_CONNECTION_FACTORY = "TopicConnectionFactory";
    
    private Context context;
    private DestinationManager destinationManager;
    private QueueConnectionFactory queueConnectionFactory;
    private TopicConnectionFactory topicConnectionFactory;
    
    @Override
    public void beforeStartup() throws Exception {
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        destinationManager = new DestinationManager();
        ConfigurationManager configurationManager = new ConfigurationManager();
        topicConnectionFactory = new MockTopicConnectionFactory(destinationManager, configurationManager);
        queueConnectionFactory = new MockQueueConnectionFactory(destinationManager, configurationManager);
        context.bind(QUEUE_CONNECTION_FACTORY, queueConnectionFactory);
        context.bind(TOPIC_CONNECTION_FACTORY, topicConnectionFactory);
    }

    @Override
    public void setupContentType(AxisService service, String contentType) throws Exception {
        service.addParameter("transport.jms.contentType", contentType);
        // TODO: use this once the changes for SYNAPSE-304 is implemented:
//            service.addParameter(JMSConstants.CONTENT_TYPE_PARAM, contentType);
    }

    public Context getContext() {
        return context;
    }

    public MockDestination createDestination(String destinationType, String name) {
        if (destinationType.equals(JMSConstants.DESTINATION_TYPE_TOPIC)) {
            return destinationManager.createTopic(name);
        } else {
            return destinationManager.createQueue(name);
        }
    }

    public QueueConnectionFactory getQueueConnectionFactory() {
        return queueConnectionFactory;
    }
    
    public TopicConnectionFactory getTopicConnectionFactory() {
        return topicConnectionFactory;
    }
}
