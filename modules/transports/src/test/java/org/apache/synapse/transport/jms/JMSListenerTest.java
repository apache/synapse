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

import java.util.LinkedList;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import junit.framework.TestSuite;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.TransportListenerTestTemplate;
import org.apache.synapse.transport.base.BaseConstants;
import org.mockejb.jndi.MockContextFactory;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockTopicConnectionFactory;

public class JMSListenerTest extends TransportListenerTestTemplate {
    public static class TestStrategyImpl extends TestStrategy {
        private final OMFactory factory = OMAbstractFactory.getOMFactory();
        
        private final boolean useTopic;
        private final boolean useContentTypeHeader;
        
        public TestStrategyImpl(boolean useTopic, boolean useContentTypeHeader) {
            super((useTopic ? "Topic" : "Queue") +
                    (useContentTypeHeader ? "WithContentTypeHeader" : "WithoutContentTypeHeader"));
            this.useTopic = useTopic;
            this.useContentTypeHeader = useContentTypeHeader;
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
                context.bind("ConnectionFactory",
                        new MockTopicConnectionFactory(destinationManager, configurationManager));
                context.bind("TestService", destinationManager.createTopic("TestService"));
            } else {
                context.bind("ConnectionFactory",
                        new MockQueueConnectionFactory(destinationManager, configurationManager));
                context.bind("TestService", destinationManager.createQueue("TestService"));
            }
            
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
        protected List<Parameter> getServiceParameters(String contentType) throws Exception {
            List<Parameter> params = new LinkedList<Parameter>();
            params.add(new Parameter(JMSConstants.DEST_PARAM_TYPE,
                    useTopic ? JMSConstants.DESTINATION_TYPE_TOPIC
                             : JMSConstants.DESTINATION_TYPE_QUEUE));
            return params;
        }
    
        @Override
        protected void sendMessage(String endpointReference,
                                   String contentType,
                                   byte[] content) throws Exception {
            Context context = new InitialContext();
            if (useTopic) {
                TopicConnectionFactory connFactory
                    = (TopicConnectionFactory)context.lookup("ConnectionFactory");
                Topic topic = (Topic)context.lookup("TestService");
                TopicConnection connection = connFactory.createTopicConnection();
                TopicSession session
                    = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                TopicPublisher publisher = session.createPublisher(topic);
                BytesMessage message = session.createBytesMessage();
                if (useContentTypeHeader) {
                    message.setStringProperty(BaseConstants.CONTENT_TYPE, contentType);
                }
                message.writeBytes(content);
                publisher.send(message);
            } else {
                QueueConnectionFactory connFactory
                    = (QueueConnectionFactory)context.lookup("ConnectionFactory");
                Queue queue = (Queue)context.lookup("TestService");
                QueueConnection connection = connFactory.createQueueConnection();
                QueueSession session
                    = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                QueueSender sender = session.createSender(queue);
                BytesMessage message = session.createBytesMessage();
                if (useContentTypeHeader) {
                    message.setStringProperty(BaseConstants.CONTENT_TYPE, contentType);
                }
                message.writeBytes(content);
                sender.send(message);
            }
        }
    }
    
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        for (boolean useTopic : new boolean[] { false, true }) {
            for (boolean useContentTypeHeader : new boolean[] { false, true }) {
                TestStrategy strategy = new TestStrategyImpl(useTopic, useContentTypeHeader);
                if (useContentTypeHeader) {
                    addSOAPTests(strategy, suite);
                    addPOXTests(strategy, suite);
                    addSwATests(strategy, suite);
                } else {
                    // If no content type header is used, SwA can't be used and the JMS transport
                    // always uses the default charset encoding
                    suite.addTest(new SOAP11TestCaseImpl(strategy, "SOAP11", testString,
                            MessageContext.DEFAULT_CHAR_SET_ENCODING));
                    suite.addTest(new SOAP12TestCaseImpl(strategy, "SOAP12", testString,
                            MessageContext.DEFAULT_CHAR_SET_ENCODING));
                    suite.addTest(new POXTestCaseImpl(strategy, "POX", testString,
                            MessageContext.DEFAULT_CHAR_SET_ENCODING));
                }
                // TODO: these tests are temporarily disabled because of SYNAPSE-304
                // addTextPlainTests(strategy, suite);
                addBinaryTest(strategy, suite);
            }
        }
        return suite;
    }
}
