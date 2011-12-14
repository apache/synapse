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

package org.apache.synapse.samples.framework.config;

import java.util.HashMap;
import java.util.Set;

public class SampleConfiguration {

    private String sampleName;
    private SynapseServerConfiguration synapseServerConfig;
    private HashMap<String, Axis2ServerConfiguration> axis2ServerConfigs;
    private HashMap<String, JMSBrokerConfiguration> jmsConfigs;
    private HashMap<String, DerbyConfiguration> derbyConfigs;
    private Axis2ClientConfiguration axis2ClientConfig;

    public SampleConfiguration() {
        this.synapseServerConfig = new SynapseServerConfiguration();
        this.axis2ServerConfigs = new HashMap<String, Axis2ServerConfiguration>();
        this.jmsConfigs = new HashMap<String, JMSBrokerConfiguration>();
        this.derbyConfigs = new HashMap<String, DerbyConfiguration>();
        this.axis2ClientConfig = new Axis2ClientConfiguration();
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public Axis2ClientConfiguration getAxis2ClientConfig() {
        return axis2ClientConfig;
    }

    public void setAxis2Client(Axis2ClientConfiguration axis2ClientConfig) {
        this.axis2ClientConfig = axis2ClientConfig;
    }

    public SynapseServerConfiguration getSynapseServerConfig() {
        return synapseServerConfig;
    }

    public void setSynapseServerConfig(SynapseServerConfiguration synapseServerConfig) {
        this.synapseServerConfig = synapseServerConfig;
    }

    public void addNewAxis2Server(String name) {
        axis2ServerConfigs.put(name, new Axis2ServerConfiguration());
    }

    public void addNewJMSBroker(String name) {
        jmsConfigs.put(name, new JMSBrokerConfiguration());
    }

    public void addNewDerbyServer(String name) {
        derbyConfigs.put(name, new DerbyConfiguration());
    }

    public Axis2ServerConfiguration getAxis2Config(String name) {
        return axis2ServerConfigs.get(name);
    }

    public JMSBrokerConfiguration getJMSConfig(String name) {
        return jmsConfigs.get(name);
    }

    public DerbyConfiguration getDerbyConfig(String name) {
        return derbyConfigs.get(name);
    }

    public Set<String> getAxis2ServersList() {
        return axis2ServerConfigs.keySet();
    }

    public int getBackEndServerCount() {
        return axis2ServerConfigs.size() + jmsConfigs.size();
    }

}
