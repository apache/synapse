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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.testkit.listener.ListenerTestSetup;
import org.mockejb.jndi.MockContextFactory;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockConnectionFactory;
import com.mockrunner.mock.jms.MockDestination;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockTopicConnectionFactory;

public class JMSListenerSetup extends ListenerTestSetup {
    private final OMFactory factory = OMAbstractFactory.getOMFactory();
    
    private final boolean useTopic;
    
    private MockConnectionFactory connectionFactory;
    private MockDestination destination;
    
    public JMSListenerSetup(boolean useTopic) {
        super(useTopic ? "Topic" : "Queue");
        this.useTopic = useTopic;
    }

    private OMElement createParameterElement(String name, String value) {
        OMElement element = factory.createOMElement(new QName("parameter"));
        element.addAttribute("name", name, null);
        if (value != null) {
            element.setText(value);
        }
        return element;
    }
    
    private Parameter createTransportConfiguration() {
        OMElement element = createParameterElement(JMSConstants.DEFAULT_CONFAC_NAME, null);
        element.addChild(createParameterElement(Context.INITIAL_CONTEXT_FACTORY,
                MockContextFactory.class.getName()));
        element.addChild(createParameterElement(JMSConstants.CONFAC_JNDI_NAME_PARAM,
                "ConnectionFactory"));
        element.addChild(createParameterElement(JMSConstants.CONFAC_TYPE,
                useTopic ? "topic" : "queue"));
        return new Parameter(JMSConstants.DEFAULT_CONFAC_NAME, element);
    }
    
    @Override
    public TransportInDescription createTransportInDescription() throws Exception {
        MockContextFactory.setAsInitial();
        Context context = new InitialContext();
        DestinationManager destinationManager = new DestinationManager();
        ConfigurationManager configurationManager = new ConfigurationManager();
        if (useTopic) {
            connectionFactory = new MockTopicConnectionFactory(destinationManager, configurationManager);
            destination = destinationManager.createTopic("TestService");
        } else {
            connectionFactory = new MockQueueConnectionFactory(destinationManager, configurationManager);
            destination = destinationManager.createQueue("TestService");
        }
        context.bind("ConnectionFactory", connectionFactory);
        context.bind("TestService", destination);
        
        TransportInDescription trpInDesc = new TransportInDescription(JMSListener.TRANSPORT_NAME);
        trpInDesc.addParameter(createTransportConfiguration());
        trpInDesc.setReceiver(new JMSListener());
        return trpInDesc;
    }
    
    @Override
    public TransportOutDescription createTransportOutDescription() throws Exception {
        TransportOutDescription trpOutDesc = new TransportOutDescription(JMSSender.TRANSPORT_NAME);
        trpOutDesc.addParameter(createTransportConfiguration());
        trpOutDesc.setSender(new JMSSender());
        return trpOutDesc;
    }

    @Override
    public void setupService(AxisService service) throws Exception {
        service.addParameter(JMSConstants.DEST_PARAM_TYPE,
                useTopic ? JMSConstants.DESTINATION_TYPE_TOPIC
                         : JMSConstants.DESTINATION_TYPE_QUEUE);
    }

    @Override
    public void setupContentType(AxisService service, String contentType) throws Exception {
        service.addParameter("transport.jms.contentType", contentType);
        // TODO: use this once the changes for SYNAPSE-304 is implemented:
//            service.addParameter(JMSConstants.CONTENT_TYPE_PARAM, contentType);
    }

    public boolean isUseTopic() {
        return useTopic;
    }
    
    public Session createSession() throws JMSException {
        if (useTopic) {
            TopicConnection connection = ((TopicConnectionFactory)connectionFactory).createTopicConnection();
            return connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        } else {
            QueueConnection connection = ((QueueConnectionFactory)connectionFactory).createQueueConnection();
            return connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        }
    }

    public void send(Session session, Message message) throws JMSException {
        if (useTopic) {
            ((TopicSession)session).createPublisher((Topic)destination).send(message);
        } else {
            ((QueueSession)session).createProducer((Queue)destination).send(message);
        }
    }
}
