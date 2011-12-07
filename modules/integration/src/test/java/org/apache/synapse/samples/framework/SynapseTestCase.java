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

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.clients.EventSampleClient;
import org.apache.synapse.samples.framework.clients.MTOMSwASampleClient;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * This is the class from which all sample tests are derived. Loads and stores necessary
 * configuration information. Starts the mediation engine and backend server(s) before each test.
 * Shuts down running servers after a test is complete.
 */
public abstract class SynapseTestCase extends TestCase {

    private static final Log log = LogFactory.getLog(SynapseTestCase.class);

    private SampleConfiguration configuration;
    private String sampleDescriptor;
    private int sampleId;
    private ProcessController pc;
    private ArrayList<BackEndServerController> backendServerControllers;
    private OMElement sampleConfigElement;

    private String currentLocation;

    protected SynapseTestCase(int sampleId) {
        this.sampleId = sampleId;
        log.info("Creating Synapse TestCase for test " + sampleId);
        currentLocation = System.getProperty("user.dir") + "/";
        sampleDescriptor = "/sample" + sampleId + ".xml";
        configuration = new SampleConfiguration();
        backendServerControllers = new ArrayList<BackEndServerController>();
        System.setProperty("java.io.tmpdir", currentLocation + "modules/integration/target/temp");
    }

    /**
     * Executed before this test case. That means, this will be executed before each test.
     * Loads all configuration info. and starts the servers.
     */
    public void setUp() {
        log.info("SynapseTestCase: Performing necessary steps to run test " + sampleId);

        assertTrue("Could not load the global descriptor file for sample " + sampleId,
                loadDescriptorInfoFile());
        assertTrue("There are errors in global descriptor file for sample " + sampleId,
                processDescriptorFile());
        assertTrue("Could not load synapse configuration settings for the sample " + sampleId,
                initSynapseConfigInfo());
        assertTrue("Could not load axis2 configuration settings for the sample " + sampleId,
                initBackEndServersConfigInfo());
        assertTrue("Could not load client configuration settings for the sample " + sampleId,
                initClientConfigInfo());

        if (configuration.getSynapseConfig().isClusteringEnabled()) {
            assertTrue("Could not properly configure clustering", configureClustering());
        }

        for (BackEndServerController bsc : backendServerControllers) {
            if (!bsc.start()) {
                doCleanup();
                fail("There was an error starting the server: " + bsc.getServerName());
            }
        }

        if (!pc.startProcess()) {
            doCleanup();
            fail("There was an error starting synapse server");
        }
    }

    /**
     * Executed after this test case. That means, This will be executed after each test
     */
    public void tearDown() {
        log.info("Test " + sampleId + " is finished");
        doCleanup();
    }

    /**
     * shutting down servers, cleaning temp files
     */
    private void doCleanup() {
        if (pc != null) {
            log.info("Stopping Synapse");
            pc.stopProcess();
        }

        ArrayList<BackEndServerController> clonedControllers = (ArrayList<BackEndServerController>)
                backendServerControllers.clone();
        for (BackEndServerController bsc : clonedControllers) {
            if (bsc instanceof Axis2BackEndServerController) {
                log.info("Stopping Server: " + bsc.getServerName());
                bsc.stop();
                backendServerControllers.remove(bsc);
            }
        }

        for (BackEndServerController bsc : backendServerControllers) {
            log.info("Stopping Server: " + bsc.getServerName());
            bsc.stop();
        }

        //cleaning up temp dir
        try {
            FileUtils.cleanDirectory(new File(System.getProperty("java.io.tmpdir")));
        } catch (IOException e) {
            log.warn("Error while cleaning temp directory", e);
        }
    }

    /**
     * Reads the specific descriptor file for the particular sample
     * from resource directory
     *
     * @return true if the configuration was loaded successfully
     */
    private boolean loadDescriptorInfoFile() {
        log.info("Reading sample descriptor file from " + sampleDescriptor);
        sampleConfigElement = null;
        try {
            InputStream in = this.getClass().getResourceAsStream(sampleDescriptor);
            if (in == null) {
                fail("Cannot read sample descriptor file. Verify that it exists in the resource dir");
            }
            StAXOMBuilder builder = new StAXOMBuilder(in);
            sampleConfigElement = builder.getDocumentElement();
        } catch (Exception e) {
            log.error("Error loading test descriptor", e);
            return false;
        }
        return sampleConfigElement != null;
    }

    /**
     * Checks if sample id is matched
     *
     * @return true If the sample ID matches
     */
    private boolean processDescriptorFile() {
        int fileID = -1;
        Iterator itr = sampleConfigElement.getChildrenWithLocalName(SampleConfigConstants.TAG_SAMPLE_ID);
        while (itr.hasNext()) {
            fileID = Integer.parseInt(((OMElement) itr.next()).getText());
        }
        itr = sampleConfigElement.getChildrenWithLocalName(SampleConfigConstants.TAG_SAMPLE_NAME);
        while (itr.hasNext()) {
            String sampleName = ((OMElement) itr.next()).getText();
            configuration.setSampleName(sampleName);
        }

        return sampleId == fileID;
    }

    /**
     * Reads and stores synapse specific configuration information from descriptor
     *
     * @return true If the initialization is successful
     */
    private boolean initSynapseConfigInfo() {
        Properties synapseProperties = new Properties();
        OMElement synEle = null;
        Iterator itr = sampleConfigElement.getChildrenWithLocalName(SampleConfigConstants.TAG_SYNAPSE_CONF);
        while (itr.hasNext()) {
            synEle = (OMElement) itr.next();
        }
        if (synEle == null) {
            log.error("Cannot find synapse configuration information in sample descriptor file");
            return false;
        } else {
            itr = synEle.getChildElements();
        }
        while (itr.hasNext()) {
            OMElement ele = (OMElement) itr.next();
            synapseProperties.setProperty(ele.getLocalName(), ele.getText());
        }
        log.info("Initializing Configuration information for synapse server...");
        String synapseHome = currentLocation;

        String synapseXml = synapseProperties.getProperty(SampleConfigConstants.TAG_SYNAPSE_CONF_XML);
        String axis2Repo = synapseProperties.getProperty(SampleConfigConstants.TAG_SYNAPSE_CONF_AXIS2_REPO);
        String axis2Xml = synapseProperties.getProperty(SampleConfigConstants.TAG_SYNAPSE_CONF_AXIS2_XML);
        Boolean clusteringEnabled = Boolean.parseBoolean(
                (String) synapseProperties.get(SampleConfigConstants.TAG_ENABLE_CLUSTERING));

        configuration.getSynapseConfig().setServerName("SynapseServerForSample" + sampleId);

        if (synapseXml == null) {
            log.error("synapse config file must be specified for the sample");
            return false;
        } else {
            configuration.getSynapseConfig().setSynapseXml(synapseHome + synapseXml);
        }
        if (axis2Repo == null) {
            log.info("synapse repository is not specified in the descriptor. using default value...");
            configuration.getSynapseConfig().setAxis2Repo(synapseHome +
                    SampleConfigConstants.DEFAULT_SYNAPSE_CONF_AXIS2_REPO);
        } else {
            configuration.getSynapseConfig().setAxis2Repo(synapseHome + axis2Repo);
        }
        if (axis2Xml == null) {
            log.info("synapse axis2.xml is not specified in the descriptor. using default value...");
            configuration.getSynapseConfig().setAxis2Xml(synapseHome +
                    SampleConfigConstants.DEFAULT_SYNAPSE_CONF_AXIS2_XML);
        } else {
            configuration.getSynapseConfig().setAxis2Xml(synapseHome + axis2Xml);
        }

        configuration.getSynapseConfig().setSynapseHome(synapseHome);
        configuration.getSynapseConfig().setClusteringEnabled(clusteringEnabled);

        pc = new SynapseProcessController(configuration.getSynapseConfig());
        return true;
    }

    /**
     * Reads and stores backend server specific configuration information from descriptor
     *
     * @return true If the initialization is successful
     */
    private boolean initBackEndServersConfigInfo() {
        OMElement bESConfigEle = null;
        Iterator itr_BEEle = sampleConfigElement.getChildrenWithLocalName(
                SampleConfigConstants.TAG_BE_SERVER_CONF);
        while (itr_BEEle.hasNext()) {
            bESConfigEle = (OMElement) itr_BEEle.next();
        }
        if (bESConfigEle == null) {
            log.warn("No backend servers are defined");
            return false;
        }
        log.info("Initializing Configuration information for backend servers...");

        // Processing JMS servers
        Properties jmsProperties = new Properties();
        Iterator itr_JMS_Servers = bESConfigEle.getChildrenWithLocalName(
                SampleConfigConstants.TAG_BE_SERVER_CONF_JMS_BROKER);
        while (itr_JMS_Servers.hasNext()) {
            OMElement jmsServer = (OMElement) itr_JMS_Servers.next();
            String serverID = jmsServer.getAttributeValue(new QName("id"));
            String serverName = "SampleJMSServer" + serverID;
            configuration.addNewJMSBroker(serverName);

            Iterator serverConfig = jmsServer.getChildElements();
            while (serverConfig.hasNext()) {
                OMElement ele = (OMElement) serverConfig.next();
                jmsProperties.setProperty(ele.getLocalName(), ele.getText());
            }

            String providerURL = jmsProperties.getProperty(
                    SampleConfigConstants.TAG_BE_SERVER_CONF_JMS_PROVIDER_URL);
            String initialNF = jmsProperties.getProperty(
                    SampleConfigConstants.TAG_BE_SERVER_CONF_JMS_INITIAL_NAMING_FACTORY);

            if (providerURL == null) {
                log.info("Using default provider url");
                configuration.getJMSConfig(serverName).setProviderURL(
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_JMS_PROVIDER_URL);
            } else {
                configuration.getJMSConfig(serverName).setProviderURL(providerURL);
            }
            if (initialNF == null) {
                log.info("Using default initial naming factory");
                configuration.getJMSConfig(serverName).setInitialNamingFactory(
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_JMS_INITIAL_NAMING_FACTORY);
            } else {
                configuration.getJMSConfig(serverName).setInitialNamingFactory(initialNF);
            }

            configuration.getJMSConfig(serverName).setServerName(serverName);

            backendServerControllers.add(new JMSBrokerController(serverName,
                    configuration.getJMSConfig(serverName)));
        }


        // Processing derby servers
        Properties derbyProperties = new Properties();
        Iterator itrDerbyServers = bESConfigEle.getChildrenWithLocalName(
                SampleConfigConstants.TAG_BE_SERVER_CONF_DERBY_SERVER);
        while (itrDerbyServers.hasNext()) {
            OMElement derbyServer = (OMElement) itrDerbyServers.next();
            String serverID = derbyServer.getAttributeValue(new QName("id"));
            String serverName = "SampleDerbyServer" + serverID;
            configuration.addNewDerbyServer(serverName);

            Iterator serverConfig = derbyServer.getChildElements();
            while (serverConfig.hasNext()) {
                OMElement ele = (OMElement) serverConfig.next();
                derbyProperties.setProperty(ele.getLocalName(), ele.getText());
            }

            configuration.getDerbyConfig(serverName).setServerName(serverName);
            backendServerControllers.add(new DerbyServerController(serverName,
                    configuration.getDerbyConfig(serverName)));
        }

        // Processing axis2 servers
        Properties axis2Properties = new Properties();
        Iterator itr_Axis2_Servers = bESConfigEle.getChildrenWithLocalName(
                SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_SERVER);
        while (itr_Axis2_Servers.hasNext()) {
            OMElement axis2Server = (OMElement) itr_Axis2_Servers.next();
            String serverID = axis2Server.getAttributeValue(new QName("id"));
            String serverName = "SampleAxis2Server" + serverID;
            configuration.addNewAxis2Server(serverName);

            Iterator serverConfig = axis2Server.getChildElements();
            while (serverConfig.hasNext()) {
                OMElement ele = (OMElement) serverConfig.next();
                axis2Properties.setProperty(ele.getLocalName(), ele.getText());
            }
            String axis2Home = currentLocation;
            String relAxis2Repo = axis2Properties.getProperty(
                    SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_REPO);
            String relAxis2Xml = axis2Properties.getProperty(
                    SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_XML);
            String axis2HttpPort = axis2Properties.getProperty(
                    SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_HTTP_PORT);
            String axis2HttpsPort = axis2Properties.getProperty(
                    SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_HTTPS_PORT);

            configuration.getAxis2Config(serverName).setServerName(serverName);

            if (relAxis2Repo == null) {
                log.info("axis2 repository is not specified in the descriptor. using default value...");
                configuration.getAxis2Config(serverName).setAxis2Repo(axis2Home +
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_AXIS2_REPO);
            } else {
                configuration.getAxis2Config(serverName).setAxis2Repo(axis2Home + relAxis2Repo);
            }
            if (relAxis2Xml == null) {
                log.info("axis2.xml is not specified in the descriptor. using default value...");
                configuration.getAxis2Config(serverName).setAxis2Xml(axis2Home +
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_AXIS2_XML);
            } else {
                configuration.getAxis2Config(serverName).setAxis2Xml(axis2Home + relAxis2Xml);
            }

            configuration.getAxis2Config(serverName).setHttpPort(axis2HttpPort);
            configuration.getAxis2Config(serverName).setHttpsPort(axis2HttpsPort);

            backendServerControllers.add(new Axis2BackEndServerController(serverName,
                    configuration.getAxis2Config(serverName)));
        }

        return true;
    }

    /*
     * reads and stores client specific configuration information from descriptor
     */
    private boolean initClientConfigInfo() {
        Properties clientProperties = new Properties();
        OMElement cliEle = null;
        Iterator itr = sampleConfigElement.getChildrenWithLocalName(
                SampleConfigConstants.TAG_CLIENT_CONF);
        while (itr.hasNext()) {
            cliEle = (OMElement) itr.next();
        }
        if (cliEle == null) {
            return false;
        } else {
            itr = cliEle.getChildElements();
        }
        while (itr.hasNext()) {
            OMElement ele = (OMElement) itr.next();
            clientProperties.setProperty(ele.getLocalName(), ele.getText());
        }

        log.info("Initializing Configuration information for clients...");
        String clientRepo = clientProperties.getProperty(
                SampleConfigConstants.TAG_CLIENT_CONF_REPO);
        String clientAxis2Xml = clientProperties.getProperty(
                SampleConfigConstants.TAG_CLIENT_CONF_AXIS2_XML);

        if (clientRepo == null) {
            log.info("client repository location is not specified in the descriptor. using default value...");
            configuration.getClientConfig().setClientRepo(currentLocation +
                    SampleConfigConstants.DEFAULT_CLIENT_CONF_REPO);

        } else {
            configuration.getClientConfig().setClientRepo(currentLocation + clientRepo);
        }

        if (clientAxis2Xml == null) {
            log.info("client axis2.xml is not specified in the descriptor. using default value...");
            configuration.getClientConfig().setAxis2Xml(currentLocation +
                    SampleConfigConstants.DEFAULT_CLIENT_CONF_AXIS2_XML);

        } else {
            configuration.getClientConfig().setAxis2Xml(currentLocation + clientAxis2Xml);
        }
        return true;

    }

    private boolean configureClustering() {
        try {
            String ip = SynapseTestUtils.getIPAddress();
            if (ip == null || ip.isEmpty()) {
                log.fatal("Could not detect an active IP address");
                return false;
            }
            log.info(" Using the IP :" + ip);

            String synapseAxis2Xml = configuration.getSynapseConfig().getAxis2Xml();
            String axis2Config = FileUtils.readFileToString(new File(synapseAxis2Xml));
            String modifiedSynapseAxis2 = SynapseTestUtils.replace(axis2Config, "${replace.me}", ip);
            File tempSynapseAxis2 = File.createTempFile("axis2Syn-", "xml");
            tempSynapseAxis2.deleteOnExit();
            FileUtils.writeStringToFile(tempSynapseAxis2, modifiedSynapseAxis2);
            configuration.getSynapseConfig().setAxis2Xml(tempSynapseAxis2.getAbsolutePath());

            for (BackEndServerController besc : backendServerControllers) {
                String serverName = besc.getServerName();
                String beAxis2Xml = configuration.getAxis2Config(serverName).getAxis2Xml();
                String beAxis2Config = FileUtils.readFileToString(new File(beAxis2Xml));
                String modifiedBEAxis2 = SynapseTestUtils.replace(beAxis2Config, "${replace.me}", ip);
                File tempBEAxis2 = File.createTempFile("axis2BE-", "xml");
                tempBEAxis2.deleteOnExit();
                FileUtils.writeStringToFile(tempBEAxis2, modifiedBEAxis2);
                configuration.getAxis2Config(serverName).setAxis2Xml(tempBEAxis2.getAbsolutePath());
            }
            return true;

        } catch (Exception e) {
            log.error("Error configuring clustering", e);
            return false;
        }


    }

    protected SampleConfiguration getConfiguration() {
        return configuration;
    }

    protected ArrayList<BackEndServerController> getBackendServerControllers() {
        return backendServerControllers;
    }

    public StockQuoteSampleClient getStockQuoteClient() {
        return new StockQuoteSampleClient(configuration.getClientConfig());
    }

    public EventSampleClient getEventSubscriberSampleClient() {
        return new EventSampleClient(configuration.getClientConfig());
    }

    public MTOMSwASampleClient getMTOMSwASampleClient() {
        return new MTOMSwASampleClient(configuration.getClientConfig());
    }
}