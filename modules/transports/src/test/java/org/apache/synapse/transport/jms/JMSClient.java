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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class JMSClient {
    protected Connection connection;
    protected Session session;
    protected MessageProducer producer;
    
    @SuppressWarnings("unused")
    private void setUp(JMSTestEnvironment env, JMSAsyncChannel channel) throws Exception {
        Destination destination = channel.getDestination();
        ConnectionFactory connectionFactory = env.getConnectionFactory(destination);
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = session.createProducer(destination);
    }

    @SuppressWarnings("unused")
    private void tearDown() throws Exception {
        producer.close();
        session.close();
        connection.close();
    }
}
