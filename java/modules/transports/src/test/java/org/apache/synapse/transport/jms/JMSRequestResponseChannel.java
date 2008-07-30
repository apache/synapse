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

import org.apache.axis2.description.AxisService;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;

import com.mockrunner.mock.jms.MockDestination;

public class JMSRequestResponseChannel extends JMSChannel implements RequestResponseChannel<JMSListenerSetup> {
    private final String replyDestinationType;
    private String replyDestinationName;
    private MockDestination replyDestination;
    
    public JMSRequestResponseChannel(JMSListenerSetup setup, String destinationType, String replyDestinationType) {
        super(setup, destinationType + "-" + replyDestinationType, destinationType);
        this.replyDestinationType = replyDestinationType;
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        replyDestinationName = "response";
        replyDestination = setup.createDestination(replyDestinationType, replyDestinationName);
        setup.getContext().bind(replyDestinationName, replyDestination);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        setup.getContext().unbind(replyDestinationName);
        replyDestinationName = null;
        replyDestination = null;
    }

    @Override
    public void setupService(AxisService service) throws Exception {
        super.setupService(service);
        service.addParameter(JMSConstants.REPLY_PARAM_TYPE, replyDestinationType);
        service.addParameter(JMSConstants.REPLY_PARAM, replyDestinationName);
    }
}
