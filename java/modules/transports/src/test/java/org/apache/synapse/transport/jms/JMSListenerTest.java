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

import java.io.StringWriter;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import junit.framework.TestSuite;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.ContentTypeMode;
import org.apache.synapse.transport.TransportListenerTestTemplate;
import org.apache.synapse.transport.base.BaseConstants;
import org.mockejb.jndi.MockContextFactory;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockConnectionFactory;
import com.mockrunner.mock.jms.MockDestination;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockTopicConnectionFactory;

public class JMSListenerTest extends TransportListenerTestTemplate {
    public static class TestStrategyImpl extends TestStrategy {
        private final OMFactory factory = OMAbstractFactory.getOMFactory();
        
        private final boolean useTopic;
        
        private MockConnectionFactory connectionFactory;
        private MockDestination destination;
        
        public TestStrategyImpl(boolean useTopic) {
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
        
        @Override
        protected TransportInDescription createTransportInDescription() throws Exception {
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
            OMElement element = createParameterElement(JMSConstants.DEFAULT_CONFAC_NAME, null);
            element.addChild(createParameterElement(Context.INITIAL_CONTEXT_FACTORY,
                    MockContextFactory.class.getName()));
            element.addChild(createParameterElement(JMSConstants.CONFAC_JNDI_NAME_PARAM,
                    "ConnectionFactory"));
            element.addChild(createParameterElement(JMSConstants.CONFAC_TYPE,
                    useTopic ? "topic" : "queue"));
            trpInDesc.addParameter(new Parameter(JMSConstants.DEFAULT_CONFAC_NAME, element));
            trpInDesc.setReceiver(new JMSListener());
            return trpInDesc;
        }
    
        @Override
        protected void setupService(AxisService service) throws Exception {
            service.addParameter(JMSConstants.DEST_PARAM_TYPE,
                    useTopic ? JMSConstants.DESTINATION_TYPE_TOPIC
                             : JMSConstants.DESTINATION_TYPE_QUEUE);
        }

        @Override
        protected void setupContentType(AxisService service, String contentType) throws Exception {
            // TODO: this is not yet supported.
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
    
    private static class BytesMessageSender extends MessageSender {
        @Override
        public void sendMessage(TestStrategy _strategy,
                                String endpointReference,
                                String contentType,
                                byte[] content) throws Exception {
            TestStrategyImpl strategy = (TestStrategyImpl)_strategy;
            Session session = strategy.createSession();
            BytesMessage message = session.createBytesMessage();
            if (contentType != null) {
                message.setStringProperty(BaseConstants.CONTENT_TYPE, contentType);
            }
            message.writeBytes(content);
            strategy.send(session, message);
        }
    }
    
    private static class TextMessageSender implements XMLMessageSender {
        public void sendMessage(TestStrategy _strategy,
                String endpointReference, String contentType, String charset,
                OMElement omMessage) throws Exception {
            TestStrategyImpl strategy = (TestStrategyImpl)_strategy;
            Session session = strategy.createSession();
            TextMessage message = session.createTextMessage();
            if (contentType != null) {
                message.setStringProperty(BaseConstants.CONTENT_TYPE, contentType);
            }
            OMOutputFormat format = new OMOutputFormat();
            format.setIgnoreXMLDeclaration(true);
            StringWriter sw = new StringWriter();
            omMessage.serializeAndConsume(sw, format);
            message.setText(sw.toString());
            strategy.send(session, message);
        }
    }
    
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        BytesMessageSender bytesMessageSender = new BytesMessageSender();
        TextMessageSender textMessageSender = new TextMessageSender();
        for (boolean useTopic : new boolean[] { false, true }) {
            TestStrategy strategy = new TestStrategyImpl(useTopic);
            for (ContentTypeMode contentTypeMode : ContentTypeMode.values()) {
                for (XMLMessageSender sender : new XMLMessageSender[] { bytesMessageSender, textMessageSender }) {
                    if (contentTypeMode == ContentTypeMode.TRANSPORT) {
                        addSOAPTests(strategy, sender, suite, contentTypeMode);
                        addPOXTests(strategy, sender, suite, contentTypeMode);
                    } else {
                        // If no content type header is used, SwA can't be used and the JMS transport
                        // always uses the default charset encoding
                        suite.addTest(new SOAP11TestCaseImpl(strategy, sender, "SOAP11", contentTypeMode, testString,
                                MessageContext.DEFAULT_CHAR_SET_ENCODING));
                        suite.addTest(new SOAP12TestCaseImpl(strategy, sender, "SOAP12", contentTypeMode, testString,
                                MessageContext.DEFAULT_CHAR_SET_ENCODING));
                        suite.addTest(new POXTestCaseImpl(strategy, sender, "POX", contentTypeMode, testString,
                                MessageContext.DEFAULT_CHAR_SET_ENCODING));
                    }
                }
                if (contentTypeMode == ContentTypeMode.TRANSPORT) {
                    addSwATests(strategy, bytesMessageSender, suite);
                }
                // TODO: these tests are temporarily disabled because of SYNAPSE-304
                // addTextPlainTests(strategy, suite);
                addBinaryTest(strategy, bytesMessageSender, suite, contentTypeMode);
            }
        }
        return suite;
    }
}
