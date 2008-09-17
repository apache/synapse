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

import javax.naming.Context;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.testkit.TransportDescriptionFactory;
import org.apache.synapse.transport.testkit.name.Key;
import org.mockejb.jndi.MockContextFactory;

public class JMSTransportDescriptionFactory implements TransportDescriptionFactory {
    private static final OMFactory factory = OMAbstractFactory.getOMFactory();
    
    private final boolean cfOnSender;
    
    /**
     * Constructor.
     * @param cfOnSender Determine whether the connection factories (JMS providers)
     *                   should also be configured on the sender. This switch allows
     *                   us to build regression tests for SYNAPSE-448. 
     */
    public JMSTransportDescriptionFactory(boolean cfOnSender) {
        this.cfOnSender = cfOnSender;
    }

    @SuppressWarnings("unused")
    // We implicitly depend on the environment; make this explicit
    private void setUp(JMSTestEnvironment env) {
        
    }
    
    @Key("cfOnSender")
    public boolean isCfOnSender() {
        return cfOnSender;
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
        setupConnectionFactoryConfig(trpDesc, "queue", JMSTestEnvironment.QUEUE_CONNECTION_FACTORY, "queue");
        setupConnectionFactoryConfig(trpDesc, "topic", JMSTestEnvironment.TOPIC_CONNECTION_FACTORY, "topic");
    }
    
    public TransportInDescription createTransportInDescription() throws Exception {
        TransportInDescription trpInDesc = new TransportInDescription(JMSListener.TRANSPORT_NAME);
        setupTransport(trpInDesc);
        trpInDesc.setReceiver(new JMSListener());
        return trpInDesc;
    }
    
    public TransportOutDescription createTransportOutDescription() throws Exception {
        TransportOutDescription trpOutDesc = new TransportOutDescription(JMSSender.TRANSPORT_NAME);
        if (cfOnSender) {
            setupTransport(trpOutDesc);
        }
        trpOutDesc.setSender(new JMSSender());
        return trpOutDesc;
    }
}
