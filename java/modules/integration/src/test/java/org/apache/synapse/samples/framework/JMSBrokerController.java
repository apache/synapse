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

package org.apache.synapse.samples.framework;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.config.JMSBrokerConfiguration;

/**
 * Responsible for starting up and shutting down
 * a JMS broker instance in order to run a sample test.
 */
public class JMSBrokerController implements BackEndServerController {

    private static final Log log = LogFactory.getLog(JMSBrokerController.class);

    private String serverName;
    private JMSBrokerConfiguration configuration;
    private BrokerService broker;

    public JMSBrokerController(String serverName, JMSBrokerConfiguration configuration) {
        this.serverName = serverName;
        this.configuration = configuration;
    }

    public String getServerName() {
        return serverName;
    }

    public boolean start() {
        try {
            log.info("JMSServerController: Preparing to start JMS Broker: " + serverName);
            //using embedded jms broker
            broker = new BrokerService();
            // configure the broker
            broker.setBrokerName("synapseSampleBroker");
            broker.addConnector(configuration.getProviderURL());
            broker.start();
            log.info("JMSServerController: Broker is Successfully started. continuing tests");
            return true;
        } catch (Exception e) {
            log.error("JMSServerController: There was an error starting JMS broker: " +
                    serverName, e);
            return false;
        }
    }

    public boolean stop() {
        try {
            broker.stop();
            return true;
        } catch (Exception e) {
            log.error("Error while shutting down the broker", e);
            return false;
        }
    }

}