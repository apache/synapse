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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.testkit.listener.AbstractChannel;
import org.mockejb.jndi.MockContextFactory;

public abstract class JMSChannel extends AbstractChannel<JMSListenerSetup> {
    private static final OMFactory factory = OMAbstractFactory.getOMFactory();
    
    private final String destinationType;
    private String destinationName;
    private Destination destination;
    
    public JMSChannel(JMSListenerSetup setup, String name, String destinationType) {
        super(name, setup);
        this.destinationType = destinationType;
    }
    
    @Override
    public void setUp() throws Exception {
        destinationName = "request" + destinationType;
        destination = setup.createDestination(destinationType, destinationName);
        setup.getContext().bind(destinationName, destination);
    }

    @Override
    public void tearDown() throws Exception {
        setup.getContext().unbind(destinationName);
        destinationName = null;
        destination = null;
    }

    private OMElement createParameterElement(String name, String value) {
        OMElement element = factory.createOMElement(new QName("parameter"));
        element.addAttribute("name", name, null);
        if (value != null) {
            element.setText(value);
        }
        return element;
    }
    
    private void setupConnectionFactoryConfig(ParameterInclude trpDesc, String name, String connFactName, String type) throws AxisFault {
        OMElement element = createParameterElement(JMSConstants.DEFAULT_CONFAC_NAME, null);
        element.addChild(createParameterElement(Context.INITIAL_CONTEXT_FACTORY,
                MockContextFactory.class.getName()));
        element.addChild(createParameterElement(JMSConstants.CONFAC_JNDI_NAME_PARAM,
                connFactName));
        element.addChild(createParameterElement(JMSConstants.CONFAC_TYPE, type));
        trpDesc.addParameter(new Parameter(name, element));
    }
    
    private void setupTransport(ParameterInclude trpDesc) throws AxisFault {
        setupConnectionFactoryConfig(trpDesc, "queue", JMSListenerSetup.QUEUE_CONNECTION_FACTORY, "queue");
        setupConnectionFactoryConfig(trpDesc, "topic", JMSListenerSetup.TOPIC_CONNECTION_FACTORY, "topic");
    }
    
    public TransportInDescription createTransportInDescription() throws Exception {
        TransportInDescription trpInDesc = new TransportInDescription(JMSListener.TRANSPORT_NAME);
        setupTransport(trpInDesc);
        trpInDesc.setReceiver(new JMSListener());
        return trpInDesc;
    }
    
    @Override
    public TransportOutDescription createTransportOutDescription() throws Exception {
        TransportOutDescription trpOutDesc = new TransportOutDescription(JMSSender.TRANSPORT_NAME);
//        setupTransport(trpOutDesc);
        trpOutDesc.setSender(new JMSSender());
        return trpOutDesc;
    }

    @Override
    public void setupService(AxisService service) throws Exception {
        service.addParameter(JMSConstants.CONFAC_PARAM, destinationType);
        service.addParameter(JMSConstants.DEST_PARAM_TYPE, destinationType);
        service.addParameter(JMSConstants.DEST_PARAM, destinationName);
    }

    public Session createSession() throws JMSException {
        if (destinationType.equals(JMSConstants.DESTINATION_TYPE_TOPIC)) {
            TopicConnection connection = setup.getTopicConnectionFactory().createTopicConnection();
            return connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        } else {
            QueueConnection connection = setup.getQueueConnectionFactory().createQueueConnection();
            return connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        }
    }

    public void send(Session session, Message message) throws JMSException {
        if (destinationType.equals(JMSConstants.DESTINATION_TYPE_TOPIC)) {
            ((TopicSession)session).createPublisher((Topic)destination).send(message);
        } else {
            ((QueueSession)session).createProducer(destination).send(message);
        }
    }
}
