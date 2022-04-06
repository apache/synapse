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

package org.apache.synapse.unittest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.apache.synapse.unittest.testcase.data.classes.SynapseTestCase;
import org.apache.synapse.unittest.testcase.data.classes.TestCaseSummary;
import org.apache.synapse.unittest.testcase.data.classes.TestSuiteSummary;
import org.apache.synapse.unittest.testcase.data.holders.ArtifactData;
import org.apache.synapse.unittest.testcase.data.holders.TestCaseData;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Map;

/**
 * Class is implements with Runnable.
 * Handle multiple requests from the client
 * Process receive message and response to the particular client
 */
public class RequestHandler implements Runnable {

    private static Logger log = Logger.getLogger(UnitTestingExecutor.class.getName());

    private Socket socket;
    private boolean isTransportPassThroughPortChecked = false;
    private String exception;
    private TestSuiteSummary testSuiteSummary = new TestSuiteSummary();

    /**
     * Initializing RequestHandler withe the client socket connection.
     */
    RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            log.info("Start processing test-case handler\n");
            checkTransportPassThroughPortAvailability();

            String receivedData = readData();
            SynapseTestCase synapseTestCases = preProcessingData(receivedData);

            if (synapseTestCases != null) {
                runTestingAgent(synapseTestCases);
            } else {
                log.error("Reading Synapse testcase data failed");
                testSuiteSummary.setDescription("Failed while reading synapseTestCase data");
                testSuiteSummary.setDeploymentStatus(Constants.SKIPPED_KEY);
                testSuiteSummary.setDeploymentException(exception);
            }

            writeData(testSuiteSummary);
            log.info("End processing test-case handler\n");
        } catch (Exception e) {
            log.error("Error while running client request in test agent", e);
        } finally {
            closeSocket();
        }
    }

    /**
     * Read input data from the unit testing client.
     *
     * @return read data from the client
     */
    private String readData() {
        String inputFromClient = null;

        try {
            InputStream inputStream = socket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            inputFromClient = (String) objectInputStream.readObject();

        } catch (Exception e) {
            log.error("Failed to get input stream from TCP connection", e);
        }

        //receiving message from the client
        return inputFromClient;

    }

    /**
     * Processed received message data and stores those data in relevant data holders.
     * Uses configModifier if there are some mock services to start
     *
     * @param receivedMessage received synapseTestcase data message as String
     * @return SynapaseTestCase object contains artifact, test cases and mock services data
     */
    private SynapseTestCase preProcessingData(String receivedMessage) {

        try {
            //Read relevant data from the received message and add to the relevant data holders
            SynapseTestcaseDataReader synapseTestcaseDataReader = new SynapseTestcaseDataReader(receivedMessage);

            ArtifactData readArtifactData = synapseTestcaseDataReader.readAndStoreArtifactData();
            TestCaseData readTestCaseData = synapseTestcaseDataReader.readAndStoreTestCaseData();

            //wrap the artifact data, testcase data and mock service data as one
            SynapseTestCase synapseTestCases = new SynapseTestCase();
            synapseTestCases.setArtifacts(readArtifactData);
            synapseTestCases.setTestCases(readTestCaseData);

            return synapseTestCases;

        } catch (Exception e) {
            log.error("Error while reading data from received message", e);
            exception = CommonUtils.stackTraceToString(e);
            return null;
        }

    }

    /**
     * Execute test agent for artifact deployment and mediation using receiving JSON message.
     *
     * @param synapseTestCase test cases data received from client
     */
    private void runTestingAgent(SynapseTestCase synapseTestCase) {

        TestingAgent agent = new TestingAgent();
        Map.Entry<Boolean, TestSuiteSummary> supportiveArtifactDeployment =
                new AbstractMap.SimpleEntry<Boolean, TestSuiteSummary>(false, null);
        Map.Entry<Boolean, TestSuiteSummary> testArtifactDeployment =
                new AbstractMap.SimpleEntry<Boolean, TestSuiteSummary>(false, null);

        //get results of supportive-artifact deployment if exists
        if (synapseTestCase.getArtifacts().getSupportiveArtifactCount() > 0) {
            log.info("Supportive artifacts deployment started");
            supportiveArtifactDeployment = agent.processSupportiveArtifacts(synapseTestCase, testSuiteSummary);
        }

        //check supportive-artifact deployment is success or not
        if (supportiveArtifactDeployment.getKey() || synapseTestCase.getArtifacts().getSupportiveArtifactCount() == 0) {
            log.info("Test artifact deployment started");
            testSuiteSummary.setDeploymentStatus(Constants.PASSED_KEY);
            testArtifactDeployment = agent.processTestArtifact(synapseTestCase, testSuiteSummary);

        } else if (!supportiveArtifactDeployment.getKey() &&
                supportiveArtifactDeployment.getValue().getDeploymentException() != null) {
            testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);

        } else {
            testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
            testSuiteSummary.setDescription("supportive artifact deployment failed");
        }

        //check test-artifact deployment is success or not
        if (testArtifactDeployment.getKey()) {

            log.info("Synapse testing agent ready to mediate test cases through deployments");
            //performs test cases through the deployed synapse configuration
            agent.processTestCases(synapseTestCase, testSuiteSummary);

        } else if (!testArtifactDeployment.getKey() && testArtifactDeployment.getValue() != null) {
            testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
        } else {
            testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
            testSuiteSummary.setDescription("test artifact deployment failed");
        }

        //undeploy all the deployed artifacts
        agent.artifactUndeployer();
    }

    /**
     * Write output data to the unit testing client.
     */
    private void writeData(TestSuiteSummary testSummary) throws IOException {
        JsonObject jsonObject = createResponseJSON(testSummary);
        OutputStream out = socket.getOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(out);
        o.writeObject(jsonObject.toString());
        out.flush();
    }

    /**
     * Create a json response message including all the details of the test suite.
     *
     * @return json message
     */
    private JsonObject createResponseJSON(TestSuiteSummary testSummary) {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty(Constants.DEPLOYMENT_STATUS, testSummary.getDeploymentStatus());
        jsonResponse.addProperty(Constants.DEPLOYMENT_EXCEPTION, testSummary.getDeploymentException());
        jsonResponse.addProperty(Constants.DEPLOYMENT_DESCRIPTION, testSummary.getDescription());
        jsonResponse.addProperty(Constants.MEDIATION_STATUS, testSummary.getMediationStatus());
        jsonResponse.addProperty(Constants.CURRENT_TESTCASE, testSummary.getRecentTestCaseName());
        jsonResponse.addProperty(Constants.MEDIATION_EXCEPTION, testSummary.getMediationException());

        JsonArray jsonArray = new JsonArray();
        for (TestCaseSummary summary : testSummary.getTestCaseSumamryList()) {
            JsonObject testObject = new JsonObject();
            testObject.addProperty(Constants.TEST_CASE_NAME, summary.getTestCaseName());
            testObject.addProperty(Constants.MEDIATION_STATUS, summary.getMediationStatus());
            testObject.addProperty(Constants.ASSERTION_STATUS, summary.getAssertionStatus());
            testObject.addProperty(Constants.ASSERTION_EXCEPTION, summary.getTestException());

            jsonArray.add(testObject);
        }

        jsonResponse.add("testCases", jsonArray);
        return jsonResponse;
    }

    /**
     * Check transport pass through port is started or not.
     * Waits until port started to receive the request from client
     */
    private void checkTransportPassThroughPortAvailability() throws IOException {

        if (!isTransportPassThroughPortChecked) {
            log.info("Unit testing agent checks transport Pass-through HTTP Listener port");

            boolean isPassThroughPortNotOccupied = true;
            int transportPassThroughPort = 8280;
            long timeoutExpiredMs = System.currentTimeMillis() + 10000;

            while (isPassThroughPortNotOccupied) {
                long waitMillis = timeoutExpiredMs - System.currentTimeMillis();
                isPassThroughPortNotOccupied = checkPortAvailability(transportPassThroughPort);

                if (waitMillis <= 0) {
                    // timeout expired
                    throw new IOException("Connection refused for http Pass-through HTTP Listener port - "
                            + transportPassThroughPort);
                }
            }

            isTransportPassThroughPortChecked = true;
        }
    }

    /**
     * Check port availability.
     *
     * @param port port which want to check availability
     * @return if available true else false
     */
    private boolean checkPortAvailability(int port) {
        boolean isPortAvailable;
        Socket socketTester = new Socket();
        try {
            socketTester.connect(new InetSocketAddress("127.0.0.1", port));
            isPortAvailable = false;
        } catch (IOException e) {
            isPortAvailable = true;
        } finally {
            try {
                socketTester.close();
            } catch (IOException e) {
                log.error("Error when closing socket testing connection", e);
            }
        }

        return isPortAvailable;
    }

    /**
     * close the TCP connection with client.
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            log.error("Error when closing socket connection", e);
        }
    }

}
