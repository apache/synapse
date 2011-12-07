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
        currentLocation = System.getProperty("user.dir") + File.separator;
        sampleDescriptor = File.separator + "sample" + sampleId + ".xml";
        configuration = new SampleConfiguration();
        backendServerControllers = new ArrayList<BackEndServerController>();
        System.setProperty("java.io.tmpdir", currentLocation + "modules" + File.separator +
                "integration" + File.separator + "target" + File.separator + "temp");

    }

    /**
     * Executed before this test case. That means, this will be executed before each test.
     * Loads all configuration info. and starts the servers.
     */
    public void setUp() {
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
                fail("Error starting the server: " + bsc.getServerName());
            }
        }

        if (!pc.startProcess()) {
            doCleanup();
            fail("Error starting synapse server");
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
                fail("Cannot read sample descriptor file");
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
        int fileId = -1;
        Iterator itr = sampleConfigElement.getChildrenWithLocalName(
                SampleConfigConstants.TAG_SAMPLE_ID);
        while (itr.hasNext()) {
            fileId = Integer.parseInt(((OMElement) itr.next()).getText());
        }
        itr = sampleConfigElement.getChildrenWithLocalName(SampleConfigConstants.TAG_SAMPLE_NAME);
        while (itr.hasNext()) {
            String sampleName = ((OMElement) itr.next()).getText();
            configuration.setSampleName(sampleName);
        }

        return sampleId == fileId;
    }

    /**
     * Reads and stores synapse specific configuration information from descriptor
     *
     * @return true If the initialization is successful
     */
    private boolean initSynapseConfigInfo() {
        Properties synapseProperties = new Properties();
        OMElement synEle = null;
        Iterator itr = sampleConfigElement.getChildrenWithLocalName(
                SampleConfigConstants.TAG_SYNAPSE_CONF);
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
            configuration.getSynapseConfig().setAxis2Repo(synapseHome +
                    SampleConfigConstants.DEFAULT_SYNAPSE_CONF_AXIS2_REPO);
        } else {
            log.info("Using Synapse Axis2 repository: " + axis2Repo);
            configuration.getSynapseConfig().setAxis2Repo(synapseHome + axis2Repo);
        }

        if (axis2Xml == null) {
            configuration.getSynapseConfig().setAxis2Xml(synapseHome +
                    SampleConfigConstants.DEFAULT_SYNAPSE_CONF_AXIS2_XML);
        } else {
            log.info("Using Synapse Axis2 XML: " + axis2Xml);
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
        Iterator itrJmsServers = bESConfigEle.getChildrenWithLocalName(
                SampleConfigConstants.TAG_BE_SERVER_CONF_JMS_BROKER);
        while (itrJmsServers.hasNext()) {
            OMElement jmsServer = (OMElement) itrJmsServers.next();
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
                configuration.getJMSConfig(serverName).setProviderURL(
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_JMS_PROVIDER_URL);
            } else {
                log.info("Using provider URL: " + providerURL);
                configuration.getJMSConfig(serverName).setProviderURL(providerURL);
            }

            if (initialNF == null) {
                configuration.getJMSConfig(serverName).setInitialNamingFactory(
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_JMS_INITIAL_NAMING_FACTORY);
            } else {
                log.info("Using initial context factory: " + initialNF);
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
        Iterator itrAxis2Servers = bESConfigEle.getChildrenWithLocalName(
                SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_SERVER);
        while (itrAxis2Servers.hasNext()) {
            OMElement axis2Server = (OMElement) itrAxis2Servers.next();
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
                configuration.getAxis2Config(serverName).setAxis2Repo(axis2Home +
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_AXIS2_REPO);
            } else {
                log.info("Using Axis2 repository: " + relAxis2Repo);
                configuration.getAxis2Config(serverName).setAxis2Repo(axis2Home + relAxis2Repo);
            }

            if (relAxis2Xml == null) {
                configuration.getAxis2Config(serverName).setAxis2Xml(axis2Home +
                        SampleConfigConstants.DEFAULT_BE_SERVER_CONF_AXIS2_XML);
            } else {
                log.info("Using Axis2 XML: " + relAxis2Xml);
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
            configuration.getClientConfig().setClientRepo(currentLocation +
                    SampleConfigConstants.DEFAULT_CLIENT_CONF_REPO);
        } else {
            log.info("Using client Axis2 repository location: " + clientRepo);
            configuration.getClientConfig().setClientRepo(currentLocation + clientRepo);
        }

        if (clientAxis2Xml == null) {
            configuration.getClientConfig().setAxis2Xml(currentLocation +
                    SampleConfigConstants.DEFAULT_CLIENT_CONF_AXIS2_XML);
        } else {
            log.info("Using client Axis2 XML: " + clientAxis2Xml);
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

            for (BackEndServerController controller : backendServerControllers) {
                String serverName = controller.getServerName();
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