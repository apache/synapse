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
import javax.jms.Topic;

import org.apache.synapse.transport.testkit.name.Key;

@Key("broker")
public abstract class JMSTestEnvironment {
    private ConnectionFactory connectionFactory;
    
    @SuppressWarnings("unused")
    private void setUp() throws Exception {
        connectionFactory = createConnectionFactory();
    }
    
    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        connectionFactory = null;
    }
    
    protected abstract ConnectionFactory createConnectionFactory() throws Exception;
    
    public Destination createDestination(String destinationType, String name) {
        if (destinationType.equals(JMSConstants.DESTINATION_TYPE_TOPIC)) {
            return createTopic(name);
        } else {
            return createQueue(name);
        }
    }

    public abstract Queue createQueue(String name);
    public abstract Topic createTopic(String name);
    
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
