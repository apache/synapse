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

import java.util.HashMap;
import java.util.Set;

/**
* Stores the configuration information for Synapse, Axis2 Server and Clients.
* All the information loaded from sample descriptor files are stored here.
*/
public class SampleConfiguration {
    private String sampleName;
    private SynapseSampleConfiguration synapseConfig;
    private HashMap<String, Axis2SampleConfiguration> axis2Configs;
    private HashMap<String, JMSBrokerSampleConfiguration> jmsConfigs;
    private HashMap<String, DerbyServerSampleConfiguration> derbyConfigs;
    private HashMap<String, FIXExecutorSampleConfiguration> executorConfigs;
    private ClientSampleConfiguration clientConfig;

    public SampleConfiguration() {
        this.synapseConfig = new SynapseSampleConfiguration();
        this.axis2Configs = new HashMap<String, Axis2SampleConfiguration>();
        this.jmsConfigs = new HashMap<String, JMSBrokerSampleConfiguration>();
        this.derbyConfigs = new HashMap<String, DerbyServerSampleConfiguration>();
        this.executorConfigs = new HashMap<String, FIXExecutorSampleConfiguration>();
        this.clientConfig = new ClientSampleConfiguration();
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String name) {
        this.sampleName = sampleName;
    }

    public SynapseSampleConfiguration getSynapseConfig() {
        return synapseConfig;
    }

    public void addNewAxis2Server(String name) {
        axis2Configs.put(name, new Axis2SampleConfiguration());
    }

    public void addNewJMSBroker(String name) {
        jmsConfigs.put(name, new JMSBrokerSampleConfiguration());
    }

    public void addNewDerbyServer(String name) {
        derbyConfigs.put(name, new DerbyServerSampleConfiguration());
    }

    public void addNewFIXExecutor(String name) {
        executorConfigs.put(name, new FIXExecutorSampleConfiguration());
    }

    public Axis2SampleConfiguration getAxis2Config(String name) {
        return axis2Configs.get(name);
    }

    public JMSBrokerSampleConfiguration getJMSConfig(String name) {
        return jmsConfigs.get(name);
    }

    public DerbyServerSampleConfiguration getDerbyConfig(String name) {
        return derbyConfigs.get(name);
    }

    public FIXExecutorSampleConfiguration getFIXExecutorConfig(String name) {
        return executorConfigs.get(name);
    }

    public Set<String> getAxis2ServersList() {
        return axis2Configs.keySet();
    }

    public int getBackEndServerCount() {
        return axis2Configs.size() + jmsConfigs.size();
    }

    public ClientSampleConfiguration getClientConfig() {
        return clientConfig;
    }

    class SynapseSampleConfiguration {
        private String serverName;
        private String synapseHome;
        private String axis2Repo;
        private String axis2Xml;
        private String synapseXml;
        private boolean clusteringEnabled;

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getSynapseHome() {
            return synapseHome;
        }

        public void setSynapseHome(String synapseHome) {
            this.synapseHome = synapseHome;
        }

        public String getAxis2Repo() {
            return axis2Repo;
        }

        public void setAxis2Repo(String axis2Repo) {
            this.axis2Repo = axis2Repo;
        }

        public String getAxis2Xml() {
            return axis2Xml;
        }

        public void setAxis2Xml(String axis2Xml) {
            this.axis2Xml = axis2Xml;
        }

        public String getSynapseXml() {
            return synapseXml;
        }

        public void setSynapseXml(String synapseXml) {
            this.synapseXml = synapseXml;
        }

        public boolean isClusteringEnabled() {
            return clusteringEnabled;
        }

        public void setClusteringEnabled(boolean clusteringEnabled) {
            this.clusteringEnabled = clusteringEnabled;
        }
    }

    class Axis2SampleConfiguration {
        private String serverName;
        private String axis2Repo;
        private String axis2Xml;
        private String httpPort;
        private String httpsPort;
        private boolean clusteringEnabled;

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getAxis2Repo() {
            return axis2Repo;
        }

        public void setAxis2Repo(String axis2Repo) {
            this.axis2Repo = axis2Repo;
        }

        public String getAxis2Xml() {
            return axis2Xml;
        }

        public void setAxis2Xml(String axis2Xml) {
            this.axis2Xml = axis2Xml;
        }

        public String getHttpPort() {
            return httpPort;
        }

        public void setHttpPort(String httpPort) {
            this.httpPort = httpPort;
        }

        public String getHttpsPort() {
            return httpsPort;
        }

        public void setHttpsPort(String httpsPort) {
            this.httpsPort = httpsPort;
        }

        public boolean isClusteringEnabled() {
            return clusteringEnabled;
        }

        public void setClusteringEnabled(boolean clusteringEnabled) {
            this.clusteringEnabled = clusteringEnabled;
        }
    }

    class JMSBrokerSampleConfiguration {
        private String serverName;
        private String providerURL;
        private String initialNamingFactory;

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getProviderURL() {
            return providerURL;
        }

        public void setProviderURL(String providerURL) {
            this.providerURL = providerURL;
        }

        public String getInitialNamingFactory() {
            return initialNamingFactory;
        }

        public void setInitialNamingFactory(String initialNamingFactory) {
            this.initialNamingFactory = initialNamingFactory;
        }
    }


    class DerbyServerSampleConfiguration {
        private String serverName;

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }
    }

    class FIXExecutorSampleConfiguration {
        private String serverName;

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }
    }

    public class ClientSampleConfiguration {
        private String clientRepo;
        private String fileName;
        private String axis2Xml;

        public String getClientRepo() {
            return clientRepo;
        }

        public void setClientRepo(String clientRepo) {
            this.clientRepo = clientRepo;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getAxis2Xml() {
            return axis2Xml;
        }

        public void setAxis2Xml(String axis2Xml) {
            this.axis2Xml = axis2Xml;
        }
    }

}